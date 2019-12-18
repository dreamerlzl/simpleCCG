import re
import os
from typing import List

# all the considered punctuations can be found on 
# https://grammar.yourdictionary.com/punctuation/what/fourteen-punctuation-marks.html

# def raw_to_tags(files: List):
#     os.system("autoToSupertags.sh")

def omitted_sentence(l: str):
    return l.startswith('ID')

def post_processe_cat(cat: str):
    # remove head index and unnecessary features
    cat = re.sub(r'\[nb\]', '', cat)
    cat = re.sub(r'_\d+', '', cat)
    cat = re.sub(r':([BU])', '', cat)
    return cat
    # bound = re.findall(r':([BU])', cat)
    # if bound:
    #     bound = bound[0]
    # else:
    #     bound = ''
    # return cat, bound

def post_process_word(word: str):
    if word in {'!', '?'}:
        word = '.'
    return word.lower()

# there are weird sentences like 
# 'moving rapidly through school , he graduated phi beta kappa from the university of kentucky 
# at age 18 , after spending only 2 1\/2 years in college.''
# in ccgbank, where I am not sure '1\/2' is an error.
def omitted_word(word: str):
    return word in {'.'}
    # return re.search(r'\\/', word)

def to_sequence_pair(file_path: str, output_path: str):
    with open(file_path, 'r') as processed_file, open(output_path, 'a') as output:
        sentences, cat_sequences, bound_sequences = [], [], []
        for line in processed_file:
            line = line.strip()
            if line and not omitted_sentence(line):
                to_sequence_pair.total_sentence += 1
                word_cat_pairs = line.split(' ')
                sentence, cats, bounds = [], [], []
                for pair in word_cat_pairs:
                    word, cat = pair.split('||')
                    word = post_process_word(word)
                    # ignore periods .
                    if omitted_word(word):
                        continue
                    sentence.append(word)
                    # cat, bound = post_processe_cat(cat)
                    # bounds.append(bound)
                    cat = post_processe_cat(cat)
                    to_sequence_pair.cat_vocab.add(cat)
                    cats.append(cat)
                if sentence and sentence[0]:
                    sentences.append(sentence)
                    cat_sequences.append(cats)
                else:
                    to_sequence_pair.abandoned += 1
                # bound_sequences.append(bounds)
        # output.write('\n'.join([' '.join(s) + '\n' + ' '.join(c) + '\n' + ' '.join(b)
        #     for s, c, b in zip(sentences, cat_sequences, bound_sequences)]))
        output.write('\n'.join([' '.join(s) + '\n' + ' '.join(c)
            for s, c in zip(sentences, cat_sequences)]) + '\n')

if __name__ == '__main__':
    root_path = '/Users/lin/Documents/linguistics/LIN424/project'
    project_path = root_path + '/simpleCCG'
    auto_path = root_path + '/' + 'ccgbank_1_1/data/AUTO'
    output_dir = project_path + '/data'
    to_sequence_pair.cat_vocab = set()
    print(f'preprocessing the ccgbank ...')
    to_sequence_pair.total_sentence = 0
    to_sequence_pair.abandoned = 0
    for i, auto_dir in enumerate(os.listdir(auto_path)):
        data_path = auto_path + '/' + auto_dir
        output_path = output_dir + f'/temp_{i}.txt'
        processed_path = output_dir + f'/{i}.txt'
        for file in os.listdir(data_path):
            input_path = data_path + '/' + file
            os.system(f'./preprocess/autoToSupertags.sh {input_path} > {output_path}')
            to_sequence_pair(output_path, processed_path)
    
    os.system(f'rm {output_dir}/temp*')
    print(f'total sentence processed: {to_sequence_pair.total_sentence}')
    print(f'abandon {to_sequence_pair.abandoned} sentences')
    print('preprocess finished.')      
    print(f'the number of categories: {len(to_sequence_pair.cat_vocab)}') 
    with open('./data/cat_vocab.txt', 'w', encoding='utf-8') as f:
        f.write('\n'.join(to_sequence_pair.cat_vocab)) 