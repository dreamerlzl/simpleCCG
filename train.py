import yaml
import torch.optim as optim
import torch
import torch.nn as nn
from torch.optim.lr_scheduler import ReduceLROnPlateau
from typing import Dict, List
from sklearn.model_selection import train_test_split
from torch.utils.data import DataLoader
from torch.nn.utils.rnn import pad_sequence
from suppertagger.model import BiLSTM, SimpleCCGException
from preprocess.to_dataset import CCGBankData, load_data, load_train_val

def print_sentences(x):
    print('\n'.join([' '.join(o) for o in x]))

def collate_fn(batch):
    data, label = zip(*batch)
    return data, label

def train(config: Dict):
        device = torch.device(f"cuda:{config['device']}" if torch.cuda.is_available() else 'cpu')
        config['device'] = device
        # load the data

        if config['random_train_val'] == 'True':
            x, y = load_data(config['total_path'], config['tag_path'])
            x_train, x_test, y_train, y_test = train_test_split(x, y, test_size=config['test_ratio'])
            train_ratio = config['train_ratio']/(1 - config['test_ratio'])
            x_train, x_val, y_train, y_val = train_test_split(x, y, train_size=train_ratio)
        else:
            x_train, y_train, x_val, y_val = load_train_val(config) 

        train_data = CCGBankData(x_train, y_train)
        val_data = CCGBankData(x_val, y_val)

        train_loader = DataLoader(train_data, batch_size=config['batch_size'], shuffle=True, collate_fn=collate_fn)
        val_loader = DataLoader(val_data, batch_size=config['validate_batch_size'], shuffle=True, collate_fn=collate_fn)

        model = BiLSTM(config).to(device)
        criterion = nn.CrossEntropyLoss()
        all_parameters = [{'params': model.parameters(), 'weight_decay': 0.0}]
        optimizer = optim.AdamW(all_parameters, 
            lr=float(config['lr']), weight_decay=float(config['weight_decay']))

        model.train()

        scheduler = ReduceLROnPlateau(optimizer, mode='min', factor=0.1, patience=5, verbose=True)

        for epoch in range(config['epoch_num']):
            correct, total = 0, 0
            model.train()
            model.bert_model.train()
            for step, batch in enumerate(train_loader):
                optimizer.zero_grad()
                x, y = batch
                if len(x[0]) != len(y[0]):
                    print(x[0], y[0])
                    raise SimpleCCGException('unmatched x and y!')
                labels = cat_labels(y).to(device)
                total += labels.size(0)
                output, each_len = model(x)
                # example_len = each_len[0]
                # print(f'output size: {output.size()}')
                # print(f'labels size: {labels.size()}')
                _, predicted = torch.max(output, 1)
                # example = predicted[:example_len]
                try:
                    correct += (predicted == labels).sum().item()
                    loss = criterion(output, labels)
                    loss.backward()
                    optimizer.step()
                except RuntimeError:
                    print_sentences(x)
                    raise SimpleCCGException
            
            # print(f'predicted: {example}')
            # print(f'actual: {labels[:example_len]}')
            print(f'epoch {epoch}: loss: {loss.item()} accuracy: {correct/total:.2f}')
            scheduler.step(loss)
            validate(config, val_loader, model, epoch)
            if (epoch + 1) % 5 == 0:
                torch.save({
                    'model_state_dict': model.state_dict(),
                    'optimizer_state_dict': optimizer.state_dict(),
                    'loss': loss
                }, '{}/bert_bilstm_{}_{}_{}.pt'.format(config['model_path'], config['lr'], 'Adam', epoch+1))


def validate(config: Dict, val_loader, model, epoch):
    model.eval()
    model.bert_model.eval()
    val_total, val_correct = 0, 0
    for i, batch in enumerate(val_loader):
        x_val, y_val = batch
        val_output, _ = model(x_val)
        val_labels = cat_labels(y_val).to(config['device'])
        # print(type(val_labels.size(0)))
        val_total += val_labels.size(0)
        _, val_predicted = torch.max(val_output, 1)
        val_correct += (val_predicted == val_labels).sum().item()

    print(f'epoch {epoch}: val_accuracy: {val_correct/val_total:.4f}')
    return 


def cat_labels(y: List[List[int]]):
    # print(f'before padding: {len(y)}')
    y = [torch.LongTensor(o) for o in y]
    y = torch.cat(tuple(o for o in y), dim=0)
    # print(f'after padding: {len(y)}')
    return y


if __name__ == '__main__':
    with open('./config.yaml') as file:
    # The FullLoader parameter handles the conversion from YAML
    # scalar values to Python the dictionary format
        config = yaml.load(file, Loader=yaml.FullLoader)
        train(config)