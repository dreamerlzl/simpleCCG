from typing import Dict, List, Tuple
import torch
import torch.nn as nn
import torch.nn.functional as F
import numpy as np
from torch.nn.utils.rnn import pack_sequence, pad_packed_sequence

methods = {'glove', 'elmo', 'bert'}
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')

class BiLSTM(nn.Module):
    def __init__(self, config: Dict):
        super(BiLSTM, self).__init__()
        self.bilstm = nn.LSTM(input_size=config['embed_dim'], hidden_size=config['hidden_size'], 
            bidirectional=True, batch_first=True, num_layers=config['rnn_layers'], bias=True)
        self.bn1 = nn.BatchNorm1d(2 * config['hidden_size'])
        self.a1 = nn.SELU()
        # concat hidden states of two directions

        self.linear = nn.Linear(2 * config['hidden_size'], config['num_cat'])
        print(f"tag vocabulary size: {config['num_cat']}")
        self.config = config
        if config['method'] == 'elmo':
            self.init_elmo()
        elif config['method'] == 'glove':
            self.init_glove()
        elif config['method'] == 'bert':
            self.init_bert()
        else:
            raise Exception(f'the method must be one of the following: {methods}')

    def forward(self, x):
        x = self.get_embedding(x)
        x = pack_sequence(x, enforce_sorted=False)
        packed_output, (h, c) = self.bilstm(x)
        x, each_len = pad_packed_sequence(packed_output)
        example_len = each_len[0]
        x = x.permute(1, 0, 2)
        # x of size (batch_size, max_len, 2 * hidden_size)
        x = torch.cat([x[i][:l] for i, l in enumerate(each_len)], dim=0)
        # reshape to (num_of_token, 2 * hidden_size)
        x = self.bn1(x)
        x = self.a1(x)
        x = self.linear(x)
        # x of size (num_of_token, num_cat)
        return x, each_len

    def init_bert(self):
        bert_shortcut = self.config['bert_shortcut']
        from pytorch_pretrained_bert import BertTokenizer, BertModel
        self.tokenizer = BertTokenizer.from_pretrained(bert_shortcut)
        self.bert_model = BertModel.from_pretrained(bert_shortcut).to(device)
        # self.bert_model.eval()
        self.bert_model.train()

    def get_bert(self, sentences: List[List[str]]):
        sentences = [' '.join(s) for s in sentences]
        tokenized_sentences = [self.tokenizer.tokenize(s) for s in sentences]
        sentences = [s.split(' ') for s in sentences]
        grouping_list = [group_subword(ts, s) for ts, s in zip(tokenized_sentences, sentences)]

        # padding with the default [PAD]
        max_len = max([len(ts) for ts in tokenized_sentences])
        tokenized_sentences = [ts + ['[PAD]'] * (max_len - len(ts)) for ts in tokenized_sentences]
        
        indexed_sentences = [self.tokenizer.convert_tokens_to_ids(s) for s in tokenized_sentences]
        idx_tensors = torch.tensor(indexed_sentences).to(device)
        with torch.no_grad():
            try:
                encoded_layers, _ = self.bert_model(idx_tensors)
                # compute the sum of last 4 layers
                token_embeddings = torch.stack(encoded_layers[-4:], dim=0)
                token_embeddings = token_embeddings.permute(1, 2, 0, 3)
                # token_embeddings of size (num_sentences, max_len, 4, num_of_hidden_features)
                # sum the last 4 layers to get the token embeddings
                token_embeddings = torch.sum(token_embeddings, dim=2)
                # print(token_embeddings.size())
                # print(token_embeddings)
                return [group_embeddings(g, te) for g, te 
                    in zip(grouping_list, token_embeddings)]
            except RuntimeError as e:
                print(e)
                # print('\n'.join([' '.join(s) for s in sentences]))
                print(idx_tensors)
                import sys
                sys.exit(0)
        

    def init_glove(self):
        self.word2id = np.load(self.config['word2id_path'], allow_pickle=True).tolist()
        glove_embeddings = torch.from_numpy(np.load(self.config['glove_path'], allow_pickle=True))
        glove_embeddings = glove_embeddings.to(device)
        self.glove = nn.Embedding(self.config['vocab_size'], self.config['embed_dim'])
        self.glove.weight.data.copy_(glove_embeddings)
        self.embed_dim = self.config['embed_dim']

    def get_glove(self, sentences: List[List[str]]):
        max_len = max(map(lambda x: len(x), sentence_lists))
        sentence_lists = list(map(lambda x: list(map(lambda w: self.word2id.get(w, 0), x)), sentence_lists))
        sentence_lists = list(map(lambda x: x + [self.opt.vocab_size-1] * (max_len - len(x)), sentence_lists))
        sentence_lists = torch.LongTensor(sentence_lists).to(device)
        embeddings = self.glove(sentence_lists)
        # pack
        return embeddings

    def init_elmo(self):
        from allennlp.modules.elmo import Elmo, batch_to_ids
        self.elmo = Elmo(self.config['elmo_options_file'], 
            self.config['elmo_weights_file'], 1)
        self.embed_dim = self.elmo.get_output_dim()

    def get_elmo(self, sentences: List[List[str]]):
        # sentences = [s.split(' ') for s in sentences]
        character_ids = batch_to_ids(sentences).to(device)
        embeddings = self.elmo(character_ids)['elmo_representations'][0]
        # pack
        return embeddings

    def get_embedding(self, sentences: List[List[str]]):
        if self.config['method'] == 'elmo':
            return self.get_elmo(sentences)
        elif self.config['method'] == 'glove':
            return self.get_glove(sentences)
        elif self.config['method'] == 'bert':
            return self.get_bert(sentences)
        else:
            raise SimpleCCGException(f'the method must be one of the following {methods}') 

class SimpleCCGException(Exception):
    def __init__(self, message):
        super().__init__(message)


def group_embeddings(groupings: List[Tuple[int]], embeddings):
    result = []
    for group in groupings:
        # the number of word embeddings = the number groups
        # so the padding will be removed automatically
        start, end = group
        embedding = embeddings[start]
        for i in range(start+1, end+1):
            embedding += embeddings[i]
        result.append(embedding)
    return torch.stack(result, dim=0)

# for subword/character based model
def group_subword(subwords: List[str], words: List[str])-> List[Tuple[int]]:
    subword_lens = [len(sbw.replace('#', '')) if sbw != '#' else len(sbw) for sbw in subwords]
    word_lens = [len(w) for w in words]

    subwords_index = 0
    result = []
    for wl in word_lens:
        l = 0
        for i, sbwl in enumerate(subword_lens[subwords_index:]):
            l += sbwl
            if l == wl:
                temp = subwords_index + i
                result.append((subwords_index, temp))
                subwords_index = temp + 1
                break 

    return result

if __name__ == '__main__':
    tokenized_text = ['[CLS]', 'boeing', 'co', '.', "'", 's', 'third', '-', 'quarter', 'profit', 'leaped', '68', '%', ',', 'but', 'wall', 'street', "'", 's', 'attention', 'was', 'focused', 'on', 'the', 'picket', 'line', ',', 'not', 'the', 'bottom', 'line', '[SEP]']
    text = ['[CLS]', 'boeing', 'co.', "'s", 'third-quarter', 'profit', 'leaped', '68', '%', ',', 'but', 'wall', 'street', "'s", 'attention', 'was', 'focused', 'on', 'the', 'picket', 'line', ',', 'not', 'the', 'bottom', 'line', '[SEP]']
    print(group_subword(tokenized_text, text))
