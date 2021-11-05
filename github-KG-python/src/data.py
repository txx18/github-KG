import json
import os
from collections import OrderedDict, defaultdict
from uuid import uuid1

import jsonpath
import numpy as np
import pandas as pd

from statistic import get_file_name_not_match_nameWithOwner, get_needless_repo_set
from util.FileUtils import read_json_file, write_json_file, move_file
from util.utils import split_list_by_sub_count, split_data_list


def combine_repo_paper(input_csv, out_csv):
    # json_str = read_file(input_csv)
    # tmp1 = pd.read_csv(input_csv).to_dict(orient='list')
    # with open(input_csv, 'r', encoding="utf-8") as f:
    # json_dic = json.loads(json_str, strict=False)
    res_dic = OrderedDict()
    record_list = pd.read_csv(input_csv).to_dict(orient='records')
    for record in record_list:
        if res_dic.get(record['nameWithOwner']) is None:
            res_dic[record['nameWithOwner']] = ""
        res_dic[record['nameWithOwner']] += str(record['paperTitle']) + ' ' + str(record['abstract']) + ' '
    pd.DataFrame(list(res_dic.items()), columns=['nameWithOwner', 'allTitleAndAbstract']).to_csv(out_csv, index=False)
    pass


def get_payload_repo(to_reco_csv_file, gt, out_csv):
    df_to_reco_csv = pd.read_csv(to_reco_csv_file)
    relation_row_list = df_to_reco_csv.iloc[:, 0:2].values.tolist()
    raw_repo_dic = defaultdict(list)
    for row in relation_row_list:
        raw_repo_dic[row[0]].append(row[1])
    # 进行筛选
    payload_repo_dic = OrderedDict()
    for key, val in raw_repo_dic.items():
        if len(val) > gt:
            payload_repo_dic[key] = val
    # 转成dic_list
    dic_list = []
    for key, val in payload_repo_dic.items():
        for package in val:
            dic_list.append({'nameWithOwner': key, 'nameWithManager': package})
    pd.DataFrame(dic_list).to_csv(out_csv, index=False)
    pass


def construct_repo_portrait_data(repo_portrait_csv_file_dic, out_json):
    repo_portrait_df_dic = {}
    payload_entities = repo_portrait_csv_file_dic.keys()
    for target in repo_portrait_csv_file_dic.keys():
        repo_portrait_df_dic[target] = pd.read_csv(repo_portrait_csv_file_dic[target])
    repo_portrait_dic = defaultdict(dict)
    for target, df in repo_portrait_df_dic.items():
        row_list = df.iloc[:, 0:2].values.tolist()
        for row in row_list:
            nameWithOwner = row[0]
            # 如果新创建一项
            if repo_portrait_dic.get(nameWithOwner) is None:
                repo_portrait_dic[nameWithOwner] = defaultdict(list)
                # 先初始化好键 ，因为有些target不存在，应该保留其键，默认空列表才对
                for entity in payload_entities:
                    repo_portrait_dic[nameWithOwner][entity] = []
            value = row[1]
            repo_portrait_dic[nameWithOwner][target].append(value)
    with open(out_json, 'w', encoding='utf-8') as out_file:
        json.dump(repo_portrait_dic, out_file, ensure_ascii=False)
    pass


def construct_label_data(recommender_result_json_file_dic, out_csv):
    """

    :param out_csv:
    :param recommender_result_json_file_dic: key：推荐器名称 val：结果文件
    :return:
    """
    recommender_result_json_dic = {}
    for recommender in recommender_result_json_file_dic.keys():
        recommender_result_json_dic[recommender] = read_json_file(recommender_result_json_file_dic[recommender])
    # 这个字典需要增量更新
    repo_package_row_dic = defaultdict(dict)
    for recommender, reco_result_json_dic in recommender_result_json_dic.items():
        # for repo_i, record_list in reco_result_json_dic.items():
        for repo_i, record_dic in reco_result_json_dic.items():
            # for record in record_list:
            for record in record_dic['package']:
                # package_j = record['nameWithManager']
                package_j = record['key']
                if repo_package_row_dic[repo_i].get(package_j) is None:
                    repo_package_row_dic[repo_i][package_j] = defaultdict(dict)
                    # 默认所有 entity 的 score为 ?
                    for key in recommender_result_json_file_dic.keys():
                        repo_package_row_dic[repo_i][package_j][key] = np.NAN  # '?'
                repo_package_row_dic[repo_i][package_j][recommender] = record['score']
                if record.get('repoDegree') is None:
                    print(1)
                repo_package_row_dic[repo_i][package_j]['repoDegree'] = record['repoDegree']
                repo_package_row_dic[repo_i][package_j]['hit'] = record['hit']
    # 把嵌套字典展平
    res_row_list = []
    for repo_i, val1 in repo_package_row_dic.items():
        for package_j, val2 in val1.items():
            # repo 和 package 列
            res_row_dic = {'repo_i': repo_i, 'package_j': package_j}
            # 其他属性列
            for key in val2.keys():
                res_row_dic[key] = val2[key]
            res_row_list.append(res_row_dic)
    pd.DataFrame(res_row_list).to_csv(out_csv, index=False)
    pass


def add_recommender_attr_and_label(reco_result_json_file, package_degree_csv, **kwargs):
    """
    数据集全作为测试集
    :param package_degree_csv:
    :param reco_result_json_file:
    :param kwargs:
    :return:
    """
    # if len(kwargs.get('test_ks')) != kwargs.get('split_M'):
    #     print('数据集应全作为测试集')
    #     exit(0)
    if kwargs.get('topN') < 1000:
        print('topN param error')
        exit(0)
    # train_row_list, test_row_list, train_dic, test_dic = split_train_test_dic(**kwargs)
    # total_test_repo_set = test_dic.keys()
    # payload_repo_set = sorted(total_test_repo_set)
    init_train_test_dic = split_portrait_train_test_by_count(**kwargs)
    vali_repo_list = init_train_test_dic.keys()
    payload_repo_set = set()
    for nameWithOwner in vali_repo_list:
        # 有训练集 且 有测试集的仓库取交集，也就是保留有测试集的仓库
        if len(init_train_test_dic[nameWithOwner]['test']['package']) > 0:
            payload_repo_set.add(nameWithOwner)
    payload_repo_set = sorted(payload_repo_set)
    reco_result_json_dic = read_json_file(reco_result_json_file)
    package_degree_records = pd.read_csv(package_degree_csv).to_dict(orient='records')
    package_degree_dic = {}
    for dic in package_degree_records:
        package_degree_dic[dic['nameWithManager']] = dic['repoDegree']
    add_label_result_json_dic = OrderedDict()
    # 对测试集中每个repo核对推荐结果
    for index, nameWithOwner in enumerate(payload_repo_set):
        print('-' * 100)
        print("index: " + str(index + 1) + "/" + str(len(payload_repo_set)) + ", repo: " + str(nameWithOwner))
        # copy一份原结果，在copy结果上修改、增加
        add_label_result_json_dic[nameWithOwner] = reco_result_json_dic[nameWithOwner]
        # test_package_set = set(test_dic[nameWithOwner])
        test_package_set = set(init_train_test_dic[nameWithOwner]['test']['package'])
        train_package_set = set([dic['key'] for dic in init_train_test_dic[nameWithOwner]['train']['package']])
        reco_package_set = set()
        # 依次检查 test_package_set 每个 package 是否命中
        # reco_result_package_record_list = reco_result_json_dic[nameWithOwner]
        reco_package_record_list = add_label_result_json_dic[nameWithOwner]['package']
        for record in reco_package_record_list:
            # nameWithManager = record['nameWithManager']
            nameWithManager = record['key']
            reco_package_set.add(nameWithManager)
            # 标记流行度
            record['repoDegree'] = package_degree_dic[nameWithManager]
            # 如果命中（测试集里有），则标记hit为1，没命中则标记为0（是否hit正负例完全取决于测试集里有没有）
            if nameWithManager in test_package_set:
                # 可以做到修改原字典
                record['hit'] = 1
            else:
                record['hit'] = 0
        # 对于推荐列表中没有而在测试集中有的，新增记录，score记录为？，hit为 1
        # 所以：最终标记数据集是 推荐列表（精度）和 测试集（召回）的并集
        # not_in_reco_but_in_test_package_set = test_package_set - reco_package_set
        not_in_reco_and_train_but_in_test_package_set = test_package_set - train_package_set - reco_package_set
        # for package in not_in_reco_but_in_test_package_set:
        for package in not_in_reco_and_train_but_in_test_package_set:
            # reco_result_json_dic[nameWithOwner].append({'score': '?',
            reco_package_record_list.append({'score': '?',
                                             # 'nameWithManager': package,
                                             'key': package,
                                             'repoDegree': package_degree_dic[package],
                                             'hit': 1})

    # dump 结果
    to_reco_csv_file_name = kwargs.get('to_reco_csv_file').split('\\')[-1:][0]
    # split_dir_name = 'split_' + str(kwargs.get('split_M')) + '_test_' + "_".join(map(lambda x: str(x), kwargs.get('test_ks')))
    out_dir = os.path.join(kwargs.get('out_dir'), os.path.splitext(to_reco_csv_file_name)[0], 'init_' + str(kwargs.get('init_input_count')), 'iter_' + str(
        kwargs.get('interaction_iter_count')), 'top' + str(kwargs.get('topN')), 'label')
    dump_file_name = os.path.splitext(reco_result_json_file.split('\\')[-1:][0])[0] + '_label_' + str(uuid1()) + '.json'
    write_json_file(out_dir, os.path.join(out_dir, dump_file_name), add_label_result_json_dic)
    pass


def split_portrait_train_test_by_count(kwargs):
    # 获取要服务的用户
    df_to_reco_csv = pd.read_csv(kwargs.get('to_reco_csv_file'))
    relation_row_list = df_to_reco_csv.iloc[:, 0:2].values.tolist()
    payload_repo_dic = defaultdict(list)
    for row in relation_row_list:
        payload_repo_dic[row[0]].append(row[1])
    payload_repo_set = payload_repo_dic.keys()
    # 读取用户画像信息（实验用）
    total_repo_portrait_json_dic = read_json_file(kwargs.get('repo_portrait_json_file'))
    # 保存训练集和测试集，便于后面计算指标
    repo_portrait_train_test_dic = defaultdict(dict)
    for i, nameWithOwner in enumerate(payload_repo_set):
        # 划分训练集测试集，固定选择个数法
        nameWithOwner_portrait_info = total_repo_portrait_json_dic[nameWithOwner]
        repo_portrait_train_test_dic[nameWithOwner] = defaultdict(dict)
        repo_portrait_train_test_dic[nameWithOwner]['train'] = defaultdict(list)
        for entity in kwargs.get('total_entities'):
            # 画像数据中不存在该实体数据默认为空列表
            entity_list = nameWithOwner_portrait_info.get(entity, [])
            # 希望初始化 key:[]，不然list为空，不能用defaultdict(list)因为默认为空
            repo_portrait_train_test_dic[nameWithOwner]['train'][entity] = []
            repo_portrait_train_test_dic[nameWithOwner]['test'][entity] = []
            # 如果该实体不需要作为训练集，则全当做测试集，否则划分之
            if entity not in kwargs.get('payload_entities'):
                repo_portrait_train_test_dic[nameWithOwner]['test'][entity] = entity_list
                continue
            # 随机选 init_input_count 个对象作为训练集，不足返回空列表
            # 这里target_obj_list的来源是固定repo_portrait_json文件，所以也可以不sorted
            train_entity_list, test_entity_list = split_list_by_sub_count(sorted(entity_list), kwargs.get('init_input_count'), kwargs.get('split_seed'))
            # 测试集保留list就行了
            repo_portrait_train_test_dic[nameWithOwner]['test'][entity] = test_entity_list
            # 训练集用dic_list，保存target的一些属性，比如tf
            entity_portrait_dic_list = []
            for entityKey in train_entity_list:
                train_target_dic = {'key': entityKey, 'tf': 1 / len(train_entity_list)}
                entity_portrait_dic_list.append(train_target_dic)
            repo_portrait_train_test_dic[nameWithOwner]['train'][entity] = entity_portrait_dic_list
    # dump 训练集测试集
    # to_reco_csv_file_name = kwargs.get('to_reco_csv_file').split('\\')[-1:][0]
    # out_dir = os.path.join(kwargs.get('out_dir'), os.path.splitext(to_reco_csv_file_name)[0], 'init_' + str(kwargs.get('init_input_count')))
    # dump_repo_train_test_path = os.path.join(out_dir, 'repo_portrait_init_train_test' + '_' + str(uuid1()) + '.json')
    # write_json_file(out_dir, dump_repo_train_test_path, repo_portrait_train_test_dic)
    return repo_portrait_train_test_dic


def dump_model_validation_set(reco_dir, **kwargs):
    validation_dic, validation_row_list, model_dic, model_row_list = split_model_validation_repo_dic(**kwargs)
    df_validation = pd.DataFrame(data=validation_row_list, columns=['nameWithOwner', 'nameWithManager'])
    df_model = pd.DataFrame(data=model_row_list, columns=['nameWithOwner', 'nameWithManager'])
    dump_file_validation = 'repo_has_model-package_split_' + str(kwargs.get('user_M')) + '_validation_' + "_".join(map(lambda x: str(x), kwargs.get(
        'user_ks'))) + '_' + str(uuid1()) + '.csv'
    dump_file_model = 'repo_has_model-package_split_' + str(kwargs.get('user_M')) + '_validation_' + "_".join(map(lambda x: str(x), kwargs.get(
        'user_ks'))) + '_model_' + str(uuid1()) + '.csv'
    df_validation.to_csv(os.path.join(reco_dir, dump_file_validation), index=False, encoding='utf8')
    df_model.to_csv(os.path.join(reco_dir, dump_file_model), index=False, encoding='utf8')
    pass


def split_model_validation_repo_dic(**kwargs):
    df = pd.read_csv(kwargs.get('to_reco_csv_file'))
    relation_row_list = df.iloc[:, 0:2].values.tolist()
    # 先按repo为键组织
    data_dic = defaultdict(list)
    for row in relation_row_list:
        data_dic[row[0]].append(row[1])
    data_repo_set = data_dic.keys()  # sorted能直接返回list
    model_repo_list, validation_repo_list = split_data_list(sorted(data_repo_set), kwargs.get('user_M'), kwargs.get('user_ks'), kwargs.get('user_seed'))
    # 再划分成模型集和验证集
    validation_repo_set = set(validation_repo_list)
    model_dic = {}
    validation_dic = {}
    for key, val in data_dic.items():
        if key in validation_repo_set:
            validation_dic[key] = val
        else:
            model_dic[key] = val
    validation_row_list = []
    for repo, package_list in validation_dic.items():
        for package in package_list:
            validation_row_list.append([repo, package])
    model_row_list = []
    for repo, package_list in model_dic.items():
        for package in package_list:
            model_row_list.append([repo, package])
    return validation_dic, validation_row_list, model_dic, model_row_list


def split_train_test_dic_by_relation(**kwargs):
    # 数据来源：所有依赖关系 -> 去掉repo的依赖数不足的
    df = pd.read_csv(kwargs.get('to_reco_csv_file'))
    relation_row_list = df.iloc[:, 0:2].values.tolist()
    # 划分关系
    train_row_list, test_row_list = split_data_list(relation_row_list, kwargs.get('split_M'), kwargs.get('test_ks'), kwargs.get('split_seed'))
    # 分别建立test和train的字典
    train_dic = defaultdict(list)
    test_dic = defaultdict(list)
    for row in test_row_list:
        test_dic[row[0]].append(row[1])
    for row in train_row_list:
        train_dic[row[0]].append(row[1])
    return train_row_list, test_row_list, train_dic, test_dic


def rename_file_name_not_match_nameWithOwner(repo_dir):
    target_file_tuple = get_file_name_not_match_nameWithOwner(repo_dir)
    for item in target_file_tuple:
        os.rename(os.path.join(repo_dir, item[0]), os.path.join(repo_dir, item[1]))
    print('finish rename!')


def move_needless_repo(src_dir, dup_dir, no_dependency_dir, target_file_list, min_dependency_count):
    dup_repo_set, no_dependency_set = get_needless_repo_set(src_dir, target_file_list, min_dependency_count)
    for repo in dup_repo_set:
        move_file(src_dir, dup_dir, repo)
    for repo in no_dependency_set:
        move_file(src_dir, no_dependency_dir, repo)


def repo_property():
    json_dic = read_json_file(r"C:\Disk_Data\Small_Data\Neo4j\tensorflow-$-tensorflow.json")
    # count = jsonpath.jsonpath(json_dic, "$.data.repository.dependencyGraphManifests.totalCount")[0]
    packageName_list = jsonpath.jsonpath(json_dic,
                                         "$.data.repository.dependencyGraphManifests.nodes[*].dependencies.nodes[*].packageName")
    # tensorflow有两个"repository": null
    repository_list = jsonpath.jsonpath(json_dic,
                                        "$.data.repository.dependencyGraphManifests.nodes[*].dependencies.nodes[*].repository.nameWithOwner")
    topic_list = jsonpath.jsonpath(json_dic, "$.data.repository.repositoryTopics.nodes[*].topic.name")
    packageName_set = set(packageName_list)

    repository_set = set(repository_list)


def handle_raw_json():
    json_dic = read_json_file(r"C:\Disk_Data\Small_Data\Neo4j\tensorflow-$-tensorflow.json")
    language_edges = jsonpath.jsonpath(json_dic, "$.data.repository.languages.edges[*]")
    language_nodes = jsonpath.jsonpath(json_dic, "$.data.repository.languages.nodes[*]")
    for i in range(len(language_nodes)):
        language_nodes[i]["size"] = language_edges[i]["size"]
    json_dic["data"]["repository"]["languages"]["nodes"] = language_nodes
    json_str = json.dumps(json_dic)
    pass


def input_password():
    # 1. 提示用户输入密码
    pwd = input("请输入密码：")
    # 2. 判断密码长度，如果长度 >= 8，返回用户输入的密码
    if len(pwd) >= 8:
        return pwd
    # 3. 密码长度不够，需要抛出异常
    # 1> 创建异常对象 - 使用异常的错误信息字符串作为参数
    ex = Exception("密码长度不够")
    # 2> 抛出异常对象
    raise ex

    # try:
    #     user_pwd = input_password()
    # print(user_pwd)
    # except Exception as result:
    # print("发现错误：%s" % result)


def jsonpath_not_exist_key():
    json_dic = read_json_file(r"C:\Disk_Data\Small_Data\Neo4j\tensorflow-$-tensorflow.json")
    error = jsonpath.jsonpath(json_dic, "$.errors")
    return error


def load_pwc_json(dir_path):
    evaluation_tables = read_json_file(os.path.join(dir_path, 'evaluation-tables.json'))
    links_between_papers_and_code = read_json_file(os.path.join(dir_path, 'links-between-papers-and-code.json'))
    methods = read_json_file(os.path.join(dir_path, 'methods.json'))
    papers_with_abstracts = read_json_file(os.path.join(dir_path, 'papers-with-abstracts.json'))
    datasets = None
    if os.path.exists(os.path.join(dir_path, 'datasets.json')):
        datasets = read_json_file(os.path.join(dir_path, 'datasets.json'))
    return {"evaluation-tables": evaluation_tables,
            "links_between_papers_and_code": links_between_papers_and_code,
            "methods": methods,
            "papers_with_abstracts": papers_with_abstracts,
            'datasets': datasets
            }



def load_repo_dir_jsons(repo_dir_path):
    res = OrderedDict()
    for repo_index, repo_file in enumerate(os.listdir(repo_dir_path)):
        if os.path.splitext(repo_file)[1] != ".json":
            continue
        json = read_json_file(repo_dir_path + "/" + repo_file)
        res[repo_file] = json
    return res
