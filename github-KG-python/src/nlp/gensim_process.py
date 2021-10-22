# spacy for lemmatization
import en_core_web_sm
import logging

from gensim.utils import simple_preprocess
from nltk.corpus import stopwords

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)


def trans_single_doc(input_doc, dictionary, input_model):
    pre_doc = pre_process([input_doc])[0]
    vec_bow = dictionary.doc2bow(pre_doc)
    # convert the query to xxx_model space
    trans_vec = input_model[vec_bow]
    return trans_vec


# 预处理
def pre_process(docs):
    # 分词，去停用词，小写，去特殊符号
    stop_words = stopwords.words('english')
    # [文档[单词]]
    token_list_list = [[word for word in simple_preprocess(str(doc), deacc=True, min_len=2, max_len=45) if word not in stop_words] for doc in docs]
    # Lemmatization
    nlp = en_core_web_sm.load()
    # 只保留POS处理后的n、v、adj、adv，再做词形还原
    allowed_postags = ['NOUN', 'ADJ', 'VERB', 'ADV']
    tmp2 = []
    for token_list in token_list_list:
        doc = nlp(" ".join(token_list))
        tmp2.append([token.lemma_ for token in doc if token.pos_ in allowed_postags])
    return tmp2


# 小写，去特殊符号
def sent_to_words(docs):
    for doc in docs:
        yield simple_preprocess(str(doc), deacc=True)


# 去停用词
def remove_stopwords(docs):
    stop_words = stopwords.words('english')
    return [[word for word in simple_preprocess(str(doc)) if word not in stop_words] for doc in docs]


# 词形还原
def lemmatisation(token_list_list):
    nlp = en_core_web_sm.load()
    # 只保留POS处理后的n、v、adj、adv，再做词形还原
    allowed_postags = ['NOUN', 'ADJ', 'VERB', 'ADV']
    res = []
    for token_list in token_list_list:
        nlp_doc = nlp(" ".join(token_list))
        res.append([token.lemma_ for token in nlp_doc if token.pos_ in allowed_postags])
    return res
