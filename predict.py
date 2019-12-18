import torch
import torch.nn as nn
import yaml
import sys
from typing import Dict, List
from torch.utils.data import DataLoader
from suppertagger.model import BiLSTM, group_embeddings
from preprocess.to_dataset import load_data, CCGBankData, get_cat2id, get_id2cat
from train import collate_fn, cat_labels

def load_model(config: Dict):
    config['device'] = torch.device(f"cuda:{config['device']}" if torch.cuda.is_available() else 'cpu')
    checkpoint = torch.load(config['model_path']+'/'+config['test_model'], map_location=config['device'])
    model = BiLSTM(config).to(config['device'])
    model.load_state_dict(checkpoint['model_state_dict'])
    model.eval()
    model.bert_model.eval()
    return model

def load_data(config: Dict):
    test_path = config['test_path']
    cat2id = get_cat2id(config)
    x_test, y_test = load_data(test_path, cat2id)
    test_data = CCGBankData(x_test, y_test)
    test_loader = DataLoader(test_data, batch_size=config['batch_size'], collate_fn=collate_fn)
    return test_loader

def test(config: Dict):
    data = load_data(config)
    model = load_model(config)
    total_num = 0
    total_correct = 0
    print('begin testing...')
    with torch.no_grad():
        for i, batch in enumerate(data):
            x_test, y_test = batch
            test_output, _ = model(x_test)
            test_labels = cat_labels(y_test).to(config['device'])
            total_num += test_labels.size(0)
            _, test_predicted = torch.max(test_output, 1)
            total_correct += (test_predicted == test_labels).sum().item()

        print(f'accuracy: {total_correct/total_num:.2f}')

def predict(config, sentences: List[List[str]]):
    model = load_model(config)
    id2cat = get_id2cat(config)
    topk = config['topk_predict']
    if config['method'] == 'bert':
        # deal with the punctuation first
        processed_sentences, punct_positions = [], []
        for s in sentences:
            processed_sentence, punct_pos = process_punct(s)
            processed_sentences.append(processed_sentence)
            punct_positions.append(punct_pos)
        output, each_len = model(processed_sentences)
        sort, indices = torch.sort(output, dim=1, descending=True)
        word2cat = indices[:, :topk]
        m = nn.LogSoftmax(dim=1)
        weight = -1 * m(sort)[:, :topk] 
        with open(config['predict_output'], 'w') as f:
            k = 0
            split_symbol = ' ' 
            for i, l in enumerate(each_len):
                f.write('\n'.join([f'{processed_sentences[i][j]}\t' + 
                    '\t'.join([f'{id2cat[index]}{split_symbol}{w}' 
                    for index, w in zip(word2cat[k+j].tolist(), weight[k+j].tolist())])
                    if j not in punct_positions[i] 
                    else f'{punct_positions[i][j]}\t{punct_positions[i][j]}{split_symbol}0.0'
                    for j in range(l)]) + '\n\n')
                k += l
            print('prediction finished.')


def process_punct(sentence: List[str]):
    # $ and & are reserved (have tags)

    # {} and [] are unexpected, so sentences containing them are removed
    if {'{', '}', '[', ']'} & set(sentence):
        return []

    sentence = ' '.join(sentence)
    # remove quotation
    sentence = sentence.replace('\'', '')
    sentence = sentence.replace('"', '')

    # convert ? and ! into periods
    sentence = sentence.replace('!', '.')
    sentence = sentence.replace('?', '.')

    # colons : and double dashes -- may have tags, so leave them alone
    
    # the tags of ellipses are colons
    # commas, periods, semi-colons and round brackets are just themselves 
    # have corresponding punctuation rules, so leave them alone as well
    sentence = sentence.split(' ')
    punct_pos = {i:':' if c == '...' else c for i, c in enumerate(sentence) 
        if c in {',', '.', ';', '(', ')', '...'}}

    return sentence, punct_pos

if __name__ == "__main__":
    with open('./config.yaml') as f:
        config = yaml.load(f, Loader=yaml.FullLoader)
        with open(config['predict_input'], 'r') as infile:
            test_sentences = [line.strip().split(' ') for line in infile.readlines() if line.strip()]
        # test_sentences = ['silences were lengthy -- nobody moved or gestured'.split(' '), 
        # 'nobody smiled onstage , and nobody in the audience was encouraged to laugh'.split(' ')]
            predict(config, test_sentences)