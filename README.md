## Content
This project consists of a neural suppertagger and a A* parser(implementing [https://pdfs.semanticscholar.org/d4e8/7e2e64da8d72a9dc91a16a60e7fdaf28f00e.pdf](https://pdfs.semanticscholar.org/d4e8/7e2e64da8d72a9dc91a16a60e7fdaf28f00e.pdf)) for Combinatory Categorical Grammar(CCG).

## Suppertagging Accurarcy
Using the current setting in `config.yaml` (but actually I don't fine-tune it very well, there could be better performance), the accuracy on train set is `95%` and that on test set is `93%`. 

This model can be downloaded on(too large to put on github):
[https://drive.google.com/file/d/1z9yp7DqKUkKMf3upQu_iNphevUM89Q4V/view?usp=sharing](https://drive.google.com/file/d/1z9yp7DqKUkKMf3upQu_iNphevUM89Q4V/view?usp=sharing)

If the link fails, please contact zlin25@ur.rochester.edu.

## Dependency
- Python 3.7
    - `torch`, `torchvision`
    - `sklearn`
    - `PyYaml` (for configuration)
    - `pytorch-bert-pretrained` (Google Bert)
-  Java 8
    - Maven and the `Exec` plugin [https://www.mojohaus.org/exec-maven-plugin/](https://www.mojohaus.org/exec-maven-plugin/)

## How to Use
All the arguments and parameters are specified in `config.yaml`. To train the model (with the default train set path `./data/train.txt`), type

    python3 train.py

To test the model (with the default test set path `./data/test.txt`), type

    python3 predict.py test

To make predictions (CCG tags for new sentences; the predict input path is specified in `config.yaml`), type

    python3 predict.py

To parse the generated predictions (the parse input path is specified in `config.yaml` via `predict_output`), first build the parser by

    cd Parser
    mvn compile

then get back to the root dir, and type

    ./parse 

`predict_input` is an example with the expected format, `predict_output` is the corresponding output, and `parse_output` is the corresponding parse output.

## Details
- Currently, the tagging and parsing are separate. 
- The suppertagging has already taken care of all the common punctuations.
- The CCG parsing is divided into the following modules:
    - `CCGCategory`: a class for the tags in CCG. Handling comparision and parsing of tags. 
    - `CCGRule`: encode the CCG rules used in the parsing. Here I include the rules used in the aforementioned paper plus some additional ones.
    - `Agent`: Actually a wrap of priority queue here, sorted by the f-cost of each search state in the A* searching.
    - `AgentEntry`: entries in the agent, representing states.
    - `Parse`: The actual parse algorithm.  
    - `Config`: for reading the configuration file.  