import math
from collections import OrderedDict

import pandas as pd
from keybert import KeyBERT

from nlp.gensim_process import lemmatisation


def extract_paper_keywords(input_csv, out_csv, keywordCount):
    # record_list = pd.read_csv(input_csv).to_dict(orient='records')
    list_dic = pd.read_csv(input_csv).to_dict(orient='list')
    model = KeyBERT('distilbert-base-nli-mean-tokens')
    # res = []
    res = {}
    # for index, record in enumerate(record_list):
    keyword_list_list = []
    for index, doc in enumerate(list_dic['allTitleAndAbstract']):
        print('-' * 100)
        print("index: " + str(index + 1) + "/" + str(len(list_dic['nameWithOwner'])) + ", repo: " + str(list_dic['nameWithOwner'][index]))
        # 为了使结果多样化，我们可以使用最大余量相关性（MMR）创建也基于余弦相似度的关键字/关键词。具有高度多样性的结果：
        tuple_list = model.extract_keywords(doc, keyphrase_ngram_range=(1, 1), stop_words='english', top_n=keywordCount, use_mmr=True, diversity=0.7)
        # 拿尽量多的关键词
        tmp = keywordCount
        while len(tuple_list) == 0:
            tmp = tmp - 5
            tuple_list = model.extract_keywords(doc, keyphrase_ngram_range=(1, 1), stop_words='english', top_n=tmp, use_mmr=True, diversity=0.7)
        keyword_list_list.append([candidate[0] for candidate in tuple_list])
    pre_keyword_list_list = lemmatisation(keyword_list_list)
    res['nameWithOwner'] = list_dic['nameWithOwner']
    res['content'] = [' '.join(x) for x in pre_keyword_list_list]
    pd.DataFrame.from_dict(res, orient='columns').to_csv(out_csv, index=False)
    pass


def get_split_corpus(kwargs):
    df_model_set = pd.read_csv(kwargs.get('model_set_csv'))
    # df_vali_set = pd.read_csv(r'C:\Disk_Dev\Repository\github-KG\github-KG-python\result\reco\repo_has_model-package_split_2_validation_1.csv')
    # repo_model_set = [x[0] for x in df_model_set.drop_duplicates(subset=['nameWithOwner'],keep='first').iloc[:, :1].values.tolist()]
    # repo_vali_set = [x[0] for x in df_vali_set.drop_duplicates(subset=['nameWithOwner'],keep='first').iloc[:, :1].values.tolist()]
    repo_model_set = set(df_model_set.to_dict(orient='list')['nameWithOwner'])
    # repo_vali_set = set(df_vali_set.to_dict(orient='list')['nameWithOwner'])
    df_all_corpus = pd.read_csv(kwargs.get('all_corpus_csv'))
    # documents = df.to_dict(orient='list')['allTitleAndAbstract']
    record_list = df_all_corpus.to_dict(orient='records')
    repo_model_dic = OrderedDict()
    repo_vali_dic = OrderedDict()
    for record in record_list:
        # 去除空串和 isnan
        if type(record['content']) != str:
            if record['content'] is None or math.isnan(record['content']):
                record['content'] = ''
        if record['nameWithOwner'] in repo_model_set:
            repo_model_dic[record['nameWithOwner']] = record['content']
        else:
            repo_vali_dic[record['nameWithOwner']] = record['content']
    repo_model_record_list = []
    repo_vali_record_list = []
    for record in record_list:
        if record['nameWithOwner'] in repo_model_set:
            repo_model_record_list.append(record)
        else:
            repo_vali_record_list.append(record)
    return repo_model_dic, repo_vali_dic, repo_model_record_list, repo_vali_record_list