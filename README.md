## Content
This project consists of a neural suppertagger and a A* parser(implementing [https://pdfs.semanticscholar.org/d4e8/7e2e64da8d72a9dc91a16a60e7fdaf28f00e.pdf](https://pdfs.semanticscholar.org/d4e8/7e2e64da8d72a9dc91a16a60e7fdaf28f00e.pdf)) for Combinatory Categorical Grammar(CCG).

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

    python3 test.py

To make predictions (CCG tags for new sentences; the predict input path is specified in `config.yaml`), type

    python3 predict.py

To parse the generated predictions (the parse input path is specified in `config.yaml` via `predict_output`), first build the parser by

    cd Parser
    mvn compile

then get back to the root dir, and type

    ./parse 

