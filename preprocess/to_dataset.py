from torch.utils.data import Dataset
from typing import List, Dict
import sys
# from util import remove_punct

punctuation = {',', '.', '?', '!', ';', ':',
    '--', '---', '-', '[', ']', '{', '}', '(', ')', '...'}

def remove_punct(text_line, cat_line):
    new_text_line, new_cat_line = [], []
    for t, c in zip(text_line, cat_line):
        if t in punctuation and c in punctuation:
            continue
        else:
            new_text_line.append(t)
            new_cat_line.append(c)

    return new_text_line, new_cat_line

# & and $ have categories

# text_punc = {',', '.', '?', '!', ';', 
#     '--', '---', '-', '', '[', ']', '{', '}', '(', ')', '...'}

# cat_punc = {':', ','}

# def remove_punct_from_text(words: List[str]):
#     return [w for w in words if w not in text_punc]

# def remove_punct_from_cat(cats: List[str]):
#     return [c for c in cats if c not in cat_punc]

class CCGBankData(Dataset):

    def __init__(self, sentences: List[List[str]], categories: List[List[str]]):
        self.data = list(zip(sentences, categories))

    def __len__(self):
        return len(self.data)

    def __getitem__(self, idx: int):
        assert idx < len(self)
        return self.data[idx]

def get_cat2id(config: Dict):
    cat2id = dict()
    with open(config['tag_path'], 'r') as d:
        cat2id = {line.strip(): i for i, line in enumerate(d)}
    config['num_cat'] = len(cat2id)
    return cat2id

def get_id2cat(config: Dict):
    id2cat = dict()
    with open(config['tag_path'], 'r') as d:
        id2cat = {i: line.strip() for i, line in enumerate(d)}
    return id2cat

def load_data_in_range(config: Dict):
    sentences, categories = [], []
    start_index = int(config['ccgbank_auto_start_index'])
    end_index = int(config['ccgbank_auto_end_index'])
    cat2id = get_cat2id(config)
    for i in range(start_index, end_index+1):
        with open(f'../data/{i}.txt', 'r', encoding='utf-8') as f:
            for i, line in enumerate(f):
                line = line.strip().split(' ')
                if i % 2 == 0:
                    line = remove_punct_from_text(line)
                    sentences.append(line)
                else:
                    line = remove_punct_from_cat(line)
                    ids = [cat2id[cat] for cat in line]
                    # print(line, ids)
                    categories.append(ids)

    return sentences, categories

def load_train_val(config: Dict):
    cat2id = get_cat2id(config)
    x_train, y_train = load_data(config['train_path'], cat2id)
    x_val, y_val = load_data(config['val_path'], cat2id)
    return (x_train, y_train, x_val, y_val)

def load_data(data_path: str, cat2id: Dict):
    sentences, categories = [], []
    last_line = []
    last_sentence = []
    with open(data_path, 'r', encoding='utf-8') as f:
        i = 0
        while True:
            text_line = f.readline().strip()
            cat_line = f.readline().strip().split(' ')
            if not text_line:
                break
            text_line = text_line.split(' ')
            processed_text_line, processed_cat_line = remove_punct(text_line, cat_line)
            if not processed_text_line:
                continue
            sentences.append(processed_text_line)
            last_text_line = text_line[:]
            last_processed_text_line = processed_text_line[:]
            try:
                ids = [cat2id[cat] for cat in processed_cat_line]
            except KeyError:
                print('KeyError!')
                print(text_line)
                print(cat_line)
                print(processed_text_line)
                print(processed_cat_line)
                sys.exit()

            if len(ids) != len(last_processed_text_line):
                print('unmatched length!')
                print(last_text_line, len(last_text_line))
                print(last_processed_text_line, len(last_processed_text_line))
                print(cat_line, len(cat_line))
                print(processed_cat_line, len(processed_cat_line))
                sys.exit()
            categories.append(ids)

    return sentences, categories


if __name__ == '__main__':
    with open('./config.yaml') as file:
    # The FullLoader parameter handles the conversion from YAML
    # scalar values to Python the dictionary format
        import yaml
        config = yaml.load(file, Loader=yaml.FullLoader)
        load_data(config)