import logging
import time
from collections import OrderedDict
from uuid import uuid1

import numpy as np
import pandas as pd
import requests
from gensim import models, similarities
from gensim.corpora import Dictionary
from sentence_transformers import SentenceTransformer, util

from data import split_portrait_train_test_by_count, split_train_test_dic_by_relation, split_model_validation_repo_dic, get_split_corpus
from gensim_process import trans_single_doc
from util.FileUtils import *
from util.utils import *

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
ch = logging.FileHandler(os.path.join(os.getcwd(), "..", 'log', "github.py.log"), mode='a', encoding=None, delay=False)
ch.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
ch.setFormatter(formatter)
logger.addHandler(ch)


def reco_packages_by_content(kwargs):
    repo_model_content_dic, repo_vali_content_dic, repo_model_content_record_list, repo_vali_content_record_list = get_split_corpus(kwargs)
    # 读取用户画像信息（实验用）
    total_repo_portrait_json_dic = read_json_file(kwargs.get('repo_portrait_json_file'))
    # 1） 加载 gensim 的 dictionary model index
    if kwargs.get('use_nlp_model') == 'tfidf' or kwargs.get('use_nlp_model') == 'lda':
        use_dictionary, use_model, use_query_index = load_gensim_file(kwargs)
    # 2） 加载 sbert trans
    elif kwargs.get('use_nlp_model') == 'sbert':
        sbert_trans_model = SentenceTransformer('paraphrase-distilroberta-base-v1')
        trans_model_set_json_dic = load_sbert_model_file(kwargs.get('trans_model_set_file'))
    # 划分训练集测试集（基于内容推荐，package全测试集）
    repo_portrait_train_test_dic = split_portrait_train_test_by_count(kwargs)
    payload_repo_set = set()
    if kwargs.get('filter_empty_content'):
        for record in repo_vali_content_record_list:
            if record['content'] != '':
                payload_repo_set.add(record['nameWithOwner'])
    else:
        payload_repo_set = repo_vali_content_dic.keys()
    payload_repo_set = sorted(payload_repo_set)
    # 推荐结果
    total_reco_entity_result_dic = OrderedDict()
    start_time = time.time()
    for index, nameWithOwner in enumerate(payload_repo_set):
        print()
        print('\033[31m' + 'index: ' + str(index + 1) + "/" + str(len(payload_repo_set)) + ", repo: " + str(nameWithOwner) + '-' * 100 + '\033[0m')
        # 取出repo画像中的训练集
        input_repo_portrait_dic = repo_portrait_train_test_dic[nameWithOwner]['train']
        # 获取输入内容
        if kwargs.get('init_input_type') == 'keyword':
            input_doc = ' '.join(repo_vali_content_dic[nameWithOwner].split(' ')[:kwargs.get('init_keyword_count')])
        elif kwargs.get('init_input_type') == 'entity':
            input_entityKey_list = []
            for entity_record_list in input_repo_portrait_dic.values():
                input_entityKey_list.extend([entity_record['key'] for entity_record in entity_record_list])
            input_doc = ' '.join(input_entityKey_list)
        # 使用gensim模型推荐
        if kwargs.get('use_nlp_model') == 'tfidf' or kwargs.get('use_nlp_model') == 'lda':
            reco_repo_dic = find_knn_repos_use_gensim(input_doc, use_dictionary, use_model, use_query_index, repo_model_content_record_list,
                                                      kwargs.get('UCF_KNN'))
        # 使用 sbert 模型推荐
        elif kwargs.get('use_nlp_model') == 'sbert':
            reco_repo_dic = find_knn_repos_use_trans(input_doc, sbert_trans_model, trans_model_set_json_dic, repo_model_content_record_list,
                                                     kwargs.get('UCF_KNN'))
        # UCF算法平权score=1，相加得到package推荐列表
        reco_package_dic = defaultdict(int)
        for repo in sorted(reco_repo_dic.keys()):
            dependency_list = total_repo_portrait_json_dic[repo]['package']
            for package in dependency_list:
                reco_package_dic[package] += reco_repo_dic[repo] * 1.0
        reco_package_record_list = []
        for package, score in reco_package_dic.items():
            reco_package_record_list.append({'key': package, 'score': score})
        reco_package_record_list = sorted(reco_package_record_list, key=lambda x: x.get('score'), reverse=True)[:kwargs.get('topN')]
        response_json = defaultdict(dict)
        response_json['data']['package'] = reco_package_record_list
        # 交互式实体探索推荐框架
        train_entityKey_dic = {}
        test_entityKey_dic = {}
        reco_entityRecord_dic = {}
        reco_entityKey_dic = {}
        hit_entity_dic = {}
        interaction_topN_hit_entity_dic = {}
        # 推荐结果不论是否迭代都一定要保存初始版本
        # 如果需要迭代就后面更新 repo_portrait_train_test_dic
        for entity in response_json.get('data').keys():
            reco_entityRecord_dic[entity] = response_json.get('data').get(entity, [])
            reco_entityKey_dic[entity] = [dic['key'] for dic in reco_entityRecord_dic[entity]]
            print('\033[33m' + str(entity) + '-' * 25 + '\033[0m')
            train_entityKey_dic[entity] = [dic['key'] for dic in input_repo_portrait_dic[entity]]
            test_entityKey_dic[entity] = repo_portrait_train_test_dic[nameWithOwner]['test'][entity]
            # 更新 reco_entityRecord_dic 每次从 response_json
            reco_entityRecord_dic[entity] = response_json.get('data').get(entity, [])
            reco_entityKey_dic[entity] = [dic['key'] for dic in reco_entityRecord_dic[entity]]
            print('train_' + entity + '_set(' + str(len(train_entityKey_dic[entity])) + '): ' + str(train_entityKey_dic[entity]))
            print('test_' + entity + '_set(' + str(len(test_entityKey_dic[entity])) + '): ' + str(test_entityKey_dic[entity]))
            print('reco_' + entity + '_set(' + str(len(reco_entityRecord_dic[entity])) + '): '
                  + str([x['key'] + ': ' + str(round(x['score'], 4)) for x in reco_entityRecord_dic[entity]])
                  # + str(reco_entity_record_dic[entity])
                  )
            hit_entity_dic[entity] = set(reco_entityKey_dic[entity]) & set(test_entityKey_dic[entity])
            print('hit_list(' + str(len(hit_entity_dic[entity])) + '): ' + str(hit_entity_dic[entity]))
            print('single_precision: ' + str(len(hit_entity_dic[entity])) + '/' + str(len(reco_entityKey_dic[entity])))
            print('single_recall: ' + str(len(hit_entity_dic[entity])) + '/' + str(len(test_entityKey_dic[entity])))
            # 新发现的结果： reco_set 中 排名前 interaction_topN 的结果中 hit的结果
            interaction_topN_hit_entity_dic[entity] = set(reco_entityKey_dic[entity][:kwargs.get('interaction_topN')]) & set(test_entityKey_dic[entity])
            print('interaction_topN_hit_entity_dic(' + str(len(interaction_topN_hit_entity_dic[entity])) + '): '
                  + str(interaction_topN_hit_entity_dic[entity]))
        # 更新最终推荐结果 total_reco_entity_result_dic
        total_reco_entity_result_dic[nameWithOwner] = defaultdict(list)
        if kwargs.get('save_score'):
            total_reco_entity_result_dic[nameWithOwner] = reco_entityRecord_dic
        else:
            total_reco_entity_result_dic[nameWithOwner] = reco_entityKey_dic
    cost_time = time.time() - start_time
    print('cost_time: ' + str(cost_time))
    # 序列化推荐结果
    to_reco_csv_file_name_no_ext = get_fileName_no_ext(kwargs.get('to_reco_csv_file'))
    out_dir = os.path.join(kwargs.get('out_dir'), to_reco_csv_file_name_no_ext, 'init_' + str(kwargs.get('init_keyword_count')), 'iter_' + str(
        kwargs.get('interaction_iter_count')), 'top' + str(kwargs.get('topN')))
    if kwargs.get('save_score'):
        out_dir = os.path.join(out_dir, 'save_score')
    dump_reco_result_file_path = os.path.join(out_dir, kwargs.get('reco_method') + '_' + str(uuid1()) + '.json')
    write_json_file(out_dir, dump_reco_result_file_path, total_reco_entity_result_dic)
    # dump 更新后的训练集测试集
    out_dir = os.path.join(out_dir, 'train_test')
    dump_repo_train_test_path = os.path.join(out_dir, kwargs.get('reco_method') + '_' + str(uuid1()) + '.json')
    write_json_file(out_dir, dump_repo_train_test_path, repo_portrait_train_test_dic)
    return


def find_knn_repos_use_trans(input_doc, sbert_trans_model, trans_model_set_json_dic, repo_model_content_record_list, UCF_KNN):
    emb1 = sbert_trans_model.encode(input_doc)
    sims = []
    for index, key in enumerate(trans_model_set_json_dic.keys()):
        cos_sim = util.pytorch_cos_sim(emb1, trans_model_set_json_dic[key])
        sims.append(cos_sim[0].item())
    reco_repo_dic = {}
    # 找UCF_KNN个相似用户
    for doc_num, score in sorted(enumerate(sims), key=lambda x: x[1], reverse=True)[:UCF_KNN]:
        # 用输出的编号找到 nwo
        reco_repo_dic[repo_model_content_record_list[doc_num]['nameWithOwner']] = score
    return reco_repo_dic


def find_knn_repos_use_gensim(input_doc, use_dictionary, use_model, use_query_index, repo_model_content_record_list, UCF_KNN):
    # 包括预处理、编码
    trans_vec = trans_single_doc(input_doc, use_dictionary, use_model)
    # （预处理后）内容少于10个词的过滤掉
    # if len(trans_vec) < 2:
    #     continue
    # 输入内容，进行推荐
    sims = use_query_index[trans_vec]
    # reco_repo_record_list = []
    reco_repo_dic = {}
    # 找UCF_KNN个相似用户
    for doc_num, score in sorted(enumerate(sims), key=lambda x: x[1], reverse=True)[:UCF_KNN]:
        # 用输出的编号找到 nwo
        reco_repo_dic[repo_model_content_record_list[doc_num]['nameWithOwner']] = score
    return reco_repo_dic


def load_sbert_model_file(trans_model_set_file):
    json_dic = read_json_file(trans_model_set_file)
    for key in json_dic.keys():
        json_dic[key] = np.array(json_dic[key], dtype=np.float32)
    return json_dic


def load_gensim_file(kwargs):
    use_dictionary = Dictionary.load(kwargs.get('use_nlp_dictionary_file'))
    if kwargs.get('use_nlp_model') == 'tfidf':
        use_model = models.TfidfModel.load(kwargs.get('use_nlp_model_file'))
    elif kwargs.get('use_nlp_model') == 'lda':
        use_model = models.LdaModel.load(kwargs.get('use_nlp_model_file'))
    use_query_index = similarities.MatrixSimilarity.load(kwargs.get('use_query_index_file'))
    return use_dictionary, use_model, use_query_index


def reco_entities_batch_experiment_interactive(kwargs):
    repo_portrait_train_test_dic = split_portrait_train_test_by_count(kwargs)
    vali_repo_list = repo_portrait_train_test_dic.keys()
    payload_repo_set = set()
    for nameWithOwner in vali_repo_list:
        # 有训练集 且 有测试集的仓库取交集，也就是保留有测试集的仓库
        # if len(repo_portrait_train_test_dic[nameWithOwner]['test']['package']) > 0:
        #     payload_repo_set.add(nameWithOwner)
        # 2 )在推荐时全保留，计算时忽略无测试集的仓库
        payload_repo_set.add(nameWithOwner)
    # 排序
    payload_repo_set = sorted(payload_repo_set)
    # 推荐结果
    total_reco_entity_result_dic = OrderedDict()
    # 时间指标
    start_time = time.time()
    # 开始repo进行推荐
    for index, nameWithOwner in enumerate(payload_repo_set):
        print()
        print('\033[31m' + 'index: ' + str(index + 1) + "/" + str(len(payload_repo_set)) + ", repo: " + str(nameWithOwner) + '-' * 100 + '\033[0m')
        # 多轮交互推荐
        for cur_iter_count in range(1, kwargs.get('interaction_iter_count') + 1):
            print('\033[32m' + 'iter: ' + str(cur_iter_count) + '-' * 50 + '\033[0m')
            # 取出repo画像中的训练集
            input_repo_portrait_dic = repo_portrait_train_test_dic[nameWithOwner]['train']
            # for entity in kwargs.get('payload_iter_entities'):
            #     train_entityKey_dic[entity] = set([dic['key'] for dic in input_repo_portrait_dic[entity]])
            #     test_entityKey_dic[entity] = set(repo_portrait_train_test_dic[nameWithOwner]['test'][entity])
            # 输入用户画像，进行推荐
            response = reco_entities_experiment_interactive(json.dumps(input_repo_portrait_dic),
                                                            json.dumps(kwargs))
            response_json = json.loads(response)
            if json.loads(response).get("status") != "success":
                print(json.loads(response))
                break
            # 交互式实体探索推荐框架
            train_entityKey_dic = {}
            test_entityKey_dic = {}
            reco_entity_record_dic = {}
            reco_entityKey_dic = {}
            hit_entity_dic = {}
            interaction_topN_hit_entity_dic = {}
            # 推荐结果不论是否迭代都一定要保存初始版本
            # 如果需要迭代就后面更新 repo_portrait_train_test_dic
            for entity in response_json.get('data').keys():
                reco_entity_record_dic[entity] = response_json.get('data').get(entity, [])
                reco_entityKey_dic[entity] = [dic['key'] for dic in reco_entity_record_dic[entity]]
                print('\033[33m' + str(entity) + '-' * 25 + '\033[0m')
                train_entityKey_dic[entity] = [dic['key'] for dic in input_repo_portrait_dic[entity]]
                test_entityKey_dic[entity] = repo_portrait_train_test_dic[nameWithOwner]['test'][entity]
                # 更新 reco_entity_record_dic 每次从 response_json
                reco_entity_record_dic[entity] = response_json.get('data').get(entity, [])
                reco_entityKey_dic[entity] = [dic['key'] for dic in reco_entity_record_dic[entity]]
                print('train_' + entity + '_set(' + str(len(train_entityKey_dic[entity])) + '): ' + str(train_entityKey_dic[entity]))
                print('test_' + entity + '_set(' + str(len(test_entityKey_dic[entity])) + '): ' + str(test_entityKey_dic[entity]))
                print('reco_' + entity + '_set(' + str(len(reco_entity_record_dic[entity])) + '): '
                      + str([x['key'] + ': ' + str(round(x['score'], 4)) for x in reco_entity_record_dic[entity]])
                      # + str(reco_entity_record_dic[entity])
                      )
                hit_entity_dic[entity] = set(reco_entityKey_dic[entity]) & set(test_entityKey_dic[entity])
                print('hit_list(' + str(len(hit_entity_dic[entity])) + '): ' + str(hit_entity_dic[entity]))
                print('single_precision: ' + str(len(hit_entity_dic[entity])) + '/' + str(len(reco_entityKey_dic[entity])))
                print('single_recall: ' + str(len(hit_entity_dic[entity])) + '/' + str(len(test_entityKey_dic[entity])))
                # 新发现的结果： reco_set 中 排名前 interaction_topN 的结果中 hit的结果
                # 1) 用set() & set()就是随机挑选了
                # interaction_topN_hit_entity_dic[entity] = set(reco_entityKey_dic[entity][:kwargs.get('interaction_topN')]) & set(test_entityKey_dic[entity])
                # 2) 想按得分排序挑选，得按推荐列表的顺序遍历
                interaction_topN_hit_entity_dic[entity] = []
                for record_list in reco_entity_record_dic[entity]:
                    if record_list['key'] in test_entityKey_dic[entity]:
                        interaction_topN_hit_entity_dic[entity].append(record_list['key'])
                    interaction_topN_hit_entity_dic[entity] = interaction_topN_hit_entity_dic[entity][:kwargs.get('interaction_topN')]
                print('interaction_topN_hit_entity_dic(' + str(len(interaction_topN_hit_entity_dic[entity])) + '): '
                      + str(interaction_topN_hit_entity_dic[entity]))
            # 需要迭代的实体，更新其训练集&测试集 repo_portrait_train_test_dic，准备进入下一轮迭代
            for entity in kwargs.get('payload_iter_entities'):
                # 如果当前迭代次数已经达到，不要更新
                if cur_iter_count == kwargs.get('interaction_iter_count'):
                    continue
                # 目前做法：新发现的结果中，每次选 interaction_add_count 个加入训练集, 从测试集删除 add_entity_set
                # 3种模拟方式：
                # 1）在命中列表中随机选
                # add_entity_list, _ = split_list_by_sub_count(sorted(list(interaction_topN_hit_entity_dic[entity])), kwargs.get('interaction_add_count'),
                #                                              kwargs.get('split_seed'))
                # 2）在命中列表中按排名从前到后选
                # add_entity_list = interaction_topN_hit_entity_dic[entity]
                # 3）在推荐列表中按排名从前到后选
                add_entity_list = reco_entityKey_dic[entity][:kwargs.get('interaction_add_count')]
                add_entity_set = set(add_entity_list)
                iter_train_entity_list = list(set(train_entityKey_dic[entity]) | add_entity_set)
                iter_test_entity_list = list(set(test_entityKey_dic[entity]) - add_entity_set)
                iter_train_entityPortrait_dic_list = []
                for entityKey in iter_train_entity_list:
                    train_target_dic = {'key': entityKey, 'tf': 1 / len(iter_train_entity_list)}
                    iter_train_entityPortrait_dic_list.append(train_target_dic)
                repo_portrait_train_test_dic[nameWithOwner]['train'][entity] = iter_train_entityPortrait_dic_list
                repo_portrait_train_test_dic[nameWithOwner]['test'][entity] = iter_test_entity_list
            # 更新最终推荐结果 total_reco_entity_result_dic
            total_reco_entity_result_dic[nameWithOwner] = defaultdict(list)
            if kwargs.get('save_score'):
                total_reco_entity_result_dic[nameWithOwner] = reco_entity_record_dic
            else:
                total_reco_entity_result_dic[nameWithOwner] = reco_entityKey_dic
    cost_time = time.time() - start_time
    print('cost_time: ' + str(cost_time))
    # logger.info('\nkwargs: ' + json.dumps(kwargs, indent=4) + '\ncost_time: ' + str(cost_time))
    # 序列化推荐结果
    to_reco_csv_file_name = kwargs.get('to_reco_csv_file').split('\\')[-1:][0]
    out_dir = os.path.join(kwargs.get('out_dir'), os.path.splitext(to_reco_csv_file_name)[0], 'init_' + str(kwargs.get('init_input_count')), 'iter_' + str(
        kwargs.get('interaction_iter_count')), 'top' + str(kwargs.get('topN')))
    if kwargs.get('save_score'):
        out_dir = os.path.join(out_dir, 'save_score')
    dump_reco_result_file_path = os.path.join(out_dir, kwargs.get('reco_method') + '_' + str(uuid1()) + '.json')
    write_json_file(out_dir, dump_reco_result_file_path, total_reco_entity_result_dic)
    # dump 更新后的训练集测试集
    out_dir = os.path.join(out_dir, 'train_test')
    dump_repo_train_test_path = os.path.join(out_dir, kwargs.get('reco_method') + '_' + str(uuid1()) + '.json')
    write_json_file(out_dir, dump_repo_train_test_path, repo_portrait_train_test_dic)
    return


def reco_entities_experiment_interactive(input_repo_portrait_dic, kwargs):
    """

    :param kwargs:
    :param input_repo_portrait_dic:
    :return:
    """
    url = "http://localhost:8080/graph/exp/recommend/interactive/entities"
    payload = {"input_repo_portrait_dic": input_repo_portrait_dic, 'kwargs': kwargs}
    # header_dict = {"Content-Type": "application/json; charset=utf8"}
    return requests.post(url=url, data=payload).content.decode("utf-8")


def reco_packages_batch_experiment_split_relation(**kwargs):
    """
    :param repo_portrait_json_file:
    :param kwargs: 数据集和划分方式必须和最后推荐测试时相同
    :return:
    """
    train_row_list, test_row_list, train_dic, test_dic = split_train_test_dic_by_relation(**kwargs)
    total_test_repo_set = test_dic.keys()
    if len(kwargs.get('test_ks')) == kwargs.get('split_M'):
        payload_repo_set = sorted(total_test_repo_set)
    else:
        total_train_repo_set = train_dic.keys()
        total_inter_repo_set = total_train_repo_set & total_test_repo_set
        payload_repo_set = sorted(total_inter_repo_set)
    # 推荐结果
    reco_result_dic = OrderedDict()
    # 时间指标
    start_time = time.time()
    # 开始repo进行推荐
    for i, nameWithOwner in enumerate(payload_repo_set):
        print('-------------------------------------------------------------')
        print("index: " + str(i + 1) + "/" + str(len(payload_repo_set)) + ", repo: " + str(nameWithOwner))
        # 构建repo画像
        # 计算 trainset 中每个package的tf值，TfIdf = packageTF * packageRepoIDF
        input_repo_portrait_dic = {}
        input_repo_portrait_dic['nameWithOwner'] = nameWithOwner
        # 1） 划分法 找到 trainset 和 testset
        train_package_set = set(train_dic[nameWithOwner])
        test_package_set = set(test_dic[nameWithOwner])
        dependency_dic_list = []
        for package in train_package_set:
            dependency_dic = {}
            dependency_dic['nameWithManager'] = package
            dependency_dic['packageTf'] = 1 / len(train_package_set)
            # dependency_dic['dependedTF'] = 1
            dependency_dic_list.append(dependency_dic)
        input_repo_portrait_dic['dependency_dic_list'] = dependency_dic_list
        # 输入用户画像，进行推荐
        response = reco_packages_experiment(json.dumps(input_repo_portrait_dic),
                                            json.dumps(kwargs))
        response_json = json.loads(response)
        if json.loads(response).get("status") != "success":
            print(json.loads(response))
            break
        recoRecordList = response_json.get('data')
        reco_package_list = []
        for record in recoRecordList:
            reco_package_list.append(record.get('nameWithManager'))
        # 保存推荐结果，如果需要保存score，就保存recordList，否则只用保存topN
        if kwargs.get('save_score'):
            reco_result_dic[nameWithOwner] = recoRecordList
        else:
            reco_result_dic[nameWithOwner] = reco_package_list
        reco_package_set = set(reco_package_list)
        print("train_package_set: " + str(train_package_set))
        print("test_package_set: " + str(test_package_set))
        print("recoRecordList: " + str([str(x.get('nameWithManager')) + ': ' + str(x.get('score')) for x in
                                        recoRecordList]))
        # hit_set 更新 TP Tu Ru
        hit_set = reco_package_set & test_package_set
        single_hit_count = len(hit_set)
        print('hit_set: ' + str(hit_set))
        print('single_precision: ' + str(single_hit_count) + '/' + str(len(reco_package_set)))
        print('single_recall: ' + str(single_hit_count) + '/' + str(len(test_package_set)))
    over_time = time.time()
    cost_time = over_time - start_time
    print('cost_time: ' + str(cost_time))
    # logger.info('\nkwargs: ' + json.dumps(kwargs, indent=4) + '\ncost_time: ' + str(cost_time))
    # 序列化结果
    to_reco_csv_file = kwargs.get('to_reco_csv_file').split('\\')[-1:][0]
    split_dir_name = 'split_' + str(kwargs.get('split_M')) + '_test_' + "_".join(map(lambda x: str(x), kwargs.get(
        'test_ks')))
    out_dir = kwargs.get('out_dir') + '/' + os.path.join(os.path.splitext(to_reco_csv_file)[0], split_dir_name,
                                                         'top' + str(kwargs.get('topN')))
    dump_file = kwargs.get('reco_method') + '_' + str(uuid1()) + '.json'
    dump_file_path = out_dir + '/' + dump_file
    write_json_file(out_dir, dump_file_path, reco_result_dic)
    return


def reco_packages_experiment(repo_portrait_dic, kwargs):
    """

    :param kwargs:
    :param repo_portrait_dic:
    :return:
    """
    url = "http://localhost:8080/graph/exp/recommend/package"
    payload = {"repo_portrait_dic": repo_portrait_dic, 'kwargs': kwargs}
    # header_dict = {"Content-Type": "application/json; charset=utf8"}
    return requests.post(url=url, data=payload).content.decode("utf-8")


def refactorRepoCoPackageRepo_batch(package_csv_file):
    # 2）指定数据
    df = pd.read_csv(package_csv_file)
    need_set = df.iloc[:, 0:1].values.tolist()
    # 3) 测试数据
    # df = pd.read_csv(r'C:\Disk_Dev\Repository\github-KG\github-KG-python\result\test-packages.csv')
    # need_set = df.iloc[:, 0:1].values.tolist()
    for i, nameWithManager in enumerate(need_set):
        # if i <= 336:
        #     continue
        print("index: " + str(i + 1) + ", package: " + str(nameWithManager[0]))
        response = refactorRepoCoPackageRepo(nameWithManager[0])
        if json.loads(response).get("status") != "success":
            print(json.loads(response))
            break


def refactorRepoCoPackageRepo(nameWithManager):
    url = "http://localhost:8080/github/refactor/repo_co_package_repo"
    payload = {"nameWithManager": nameWithManager}
    return requests.post(url=url, data=payload).content.decode("utf-8")


def update_repo_depend_degree_IDF_batch(repo_csv_file):
    data = pd.read_csv(repo_csv_file)
    repo_row_list = sorted(data.iloc[:, 0:1].values.tolist())
    for i, nameWithOwner in enumerate(repo_row_list):
        # if i <= 336:
        #     continue
        print(
            "index: " + str(i + 1) + "/" + str(len(repo_row_list)) + ", repo: " + str(nameWithOwner[0]))
        response = update_repo_depend_degree_IDF(nameWithOwner[0])
        if json.loads(response).get("status") != "success":
            print(json.loads(response))
            break


def update_repo_depend_degree_IDF(nameWithOwner):
    url = "http://localhost:8080/github/update/repo/IDF"
    payload = {"nameWithOwner": nameWithOwner}
    response = requests.post(url=url, data=payload).content.decode("utf-8")
    return response


def update_package_depended_degree_IDF_batch(package_csv_file):
    data = pd.read_csv(package_csv_file)
    package_row_list = sorted(data.iloc[:, 0:1].values.tolist())
    for i, nameWithManager in enumerate(package_row_list):
        # if i <= 336:
        #     continue
        print(
            "index: " + str(i + 1) + "/" + str(len(package_row_list)) + ", package: " + str(nameWithManager[0]))
        response = update_package_depended_degree_IDF(nameWithManager[0])
        if json.loads(response).get("status") != "success":
            print(json.loads(response))
            break


def update_package_depended_degree_IDF(nameWithManager):
    url = "http://localhost:8080/github/update/package/IDF"
    payload = {"nameWithManager": nameWithManager}
    response = requests.post(url=url, data=payload).content.decode("utf-8")
    return response


def refactorPackageCoOccurrence_batch(repo_csv_file):
    """

    :param repo_csv_file:
    :return:
    """
    # 2）指定数据
    df = pd.read_csv(repo_csv_file)
    need_set = df.iloc[:, 0:1].values.tolist()
    for i, nameWithOwner in enumerate(need_set):
        # if i <= 336:
        #     continue
        print("index: " + str(i + 1) + '/' + str(len(need_set)) + ", repo: " + str(nameWithOwner[0]))
        response = refactorPackageCoOccurrence(nameWithOwner[0])
        if json.loads(response).get("status") != "success":
            print(json.loads(response))
            break


def refactorPackageCoOccurrence(nameWithOwner):
    url = "http://localhost:8080/github/refactor/package/CoOccurrence"
    payload = {"nameWithOwner": nameWithOwner}
    return requests.post(url=url, data=payload).content.decode("utf-8")


def delete_validationset_relation_batch(**kwargs):
    validation_dic, validation_row_list, model_dic, model_row_list = split_model_validation_repo_dic(**kwargs)
    validation_repo_set = validation_dic.keys()
    for i, nameWithOwner in enumerate(validation_repo_set):
        print('index: ' + str(i + 1) + '/' + str(len(validation_repo_set)) + ', deleting: ' + nameWithOwner)
        response = delete_relation(nameWithOwner)
        if json.loads(response).get("status") != "success":
            print(json.loads(response))
            break
    pass


def delete_relation(nameWithOwner):
    url = "http://localhost:8080/graph/delete/repo_all_relation"
    # header_dict = {"Content-Type": "application/json; charset=utf8"}
    payload = {"nameWithOwner": nameWithOwner}
    return requests.post(url=url, data=payload).content.decode("utf-8")


def delete_validationset_dependency_batch(**kwargs):
    validation_dic, validation_row_list, model_dic, model_row_list = split_model_validation_repo_dic(**kwargs)
    for i, row in enumerate(validation_row_list):
        # if i < 27495:
        #     continue
        print(
            "index: " + str(i + 1) + '/' + str(len(validation_row_list)) + ", deleting: " + str(row[0]) + " - " + str(row[1]))
        response = delete_dependency(row[0], row[1])
        if json.loads(response).get("status") != "success":
            print(json.loads(response))
            break


def delete_testset_dependency_batch(**kwargs):
    """

    :param kwargs: 数据集和划分方式必须和最后推荐测试时相同
    :return:
    """
    # 数据来源：to_reco_csv_file
    df = pd.read_csv(kwargs.get('to_reco_csv_file'))
    relation_row_list = df.iloc[:, 0:2].values.tolist()
    # 划分关系
    train_row_list, test_row_list = split_data_list(relation_row_list, kwargs.get('split_M'),
                                                    kwargs.get('test_ks'),
                                                    kwargs.get('split_seed'))
    for i, row in enumerate(test_row_list):
        # if i < 42758:
        #     continue
        print(
            "index: " + str(i + 1) + '/' + str(len(test_row_list)) + ", deleting: " + str(row[0]) + " - " + str(row[1]))
        response = delete_dependency(row[0], row[1])
        if json.loads(response).get("status") != "success":
            print(json.loads(response))
            break


def delete_dependency(nameWithOwner, nameWithManager):
    url = "http://localhost:8080/github/delete/depends_on"
    # header_dict = {"Content-Type": "application/json; charset=utf8"}
    payload = {"nameWithOwner": nameWithOwner, "nameWithManager": nameWithManager}
    return requests.post(url=url, data=payload).content.decode("utf-8")


def createRepoByJsonFile_batch(repo_dir, import_csv_file):
    # 1）划分
    # exist_repo_file_set = set(get_exist_repo_file_list(kwargs.get('dir')))
    # dup_repo_set, no_dependency_set, less_dependency_set = get_needless_repo_set(kwargs.get('dir'),
    #                                                                              exist_repo_file_set,
    #                                                                              1)
    # need_set = exist_repo_file_set - dup_repo_set - no_dependency_set - less_dependency_set
    # model_dataset, validation_dataset = split_data_list(sorted(list(need_set)), kwargs.get('model_M'),
    #                                                     kwargs.get('model_k'),
    #                                                     kwargs.get('model_seed'))
    # 2）不划分
    df = pd.read_csv(import_csv_file)
    need_set = df.iloc[:, 0:1].values.tolist()
    # 3）测试
    # need_set = ['densechen/AReLU', 'kenkai21/Image_Captioning', 'microsoft/IRNet',
    #             'muchafel/judgmentPrediction']
    for i, nameWithOwner in enumerate(need_set):
        # if i <= 336:
        #     continue
        print("index: " + str(i + 1) + '/' + str(len(need_set)) + ", repo: " + str(nameWithOwner[0]))
        repo_file = nameWithOwner[0].replace('/', '-$-') + '.json'
        file_path = os.path.join(repo_dir, repo_file)
        response = create_repo_by_jsonfile(file_path)
        if json.loads(response).get("status") != "success":
            print(json.loads(response))
            break


def create_repo_by_jsonfile(file_path):
    '''
    目前是传file_path，从中解析nameWithOwner等
    :param file_path:
    :return:
    '''
    url = "http://localhost:8080/github/create/repo"
    payload = {"filePath": file_path}
    return requests.post(url=url, data=payload).content.decode("utf-8")


def create_trainset_dependency_batch(**kwargs):
    """
    数据来源：数据集的所有依赖关系
    :param kwargs:
    :return:
    """
    df = pd.read_csv(kwargs.get('to_reco_csv_file'))
    relation_row_list = df.iloc[:, 0:3].values.tolist()
    train_row_list, test_row_list = split_data_list(relation_row_list,
                                                    kwargs.get('split_M'),
                                                    kwargs.get('test_ks'),
                                                    kwargs.get('split_seed'))
    for i, row in enumerate(train_row_list):
        # if i < 29030:
        #     continue
        print("index: " + str(i) + '/' + str(len(train_row_list)) + ", creating: " + str(row[0]) +
              " - [""REPO_DEPENDS_ON_PACKAGE] - " + str(row[1]))
        response = create_dependency(row[0], row[1], row[2])
        if json.loads(response).get("status") != "success":
            print(json.loads(response))
            break


def create_dependency(nameWithOwner, nameWithManager, requirements):
    url = "http://localhost:8080/github/create/depends_on"
    # header_dict = {"Content-Type": "application/json; charset=utf8"}
    payload = {"nameWithOwner": nameWithOwner, "nameWithManager": nameWithManager, "requirements": requirements}
    return requests.post(url=url, data=payload).content.decode("utf-8")
