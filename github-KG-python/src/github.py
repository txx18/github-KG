import logging
import time
from collections import OrderedDict
from uuid import uuid1

import pandas as pd
import requests

from util.FileUtils import *
from util.utils import *

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
ch = logging.FileHandler(os.path.join(os.getcwd(), "..", 'log', "github.py.log"), mode='a', encoding=None, delay=False)
ch.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
ch.setFormatter(formatter)
logger.addHandler(ch)


def reco_packages_batch_experiment_split_relation(**kwargs):
    """
    在训练模型之前，只应该保有 row_train_list 中的依赖关系，其他关系删除，再重新训练模型
    :param kwargs: 数据集和划分方式必须和最后推荐测试时相同
    :return:
    """
    train_row_list, test_row_list, train_dic, test_dic = split_train_test_dic(**kwargs)
    total_test_repo_set = test_dic.keys()
    # 如果是检验内容推荐器，【在推荐时】只有测试集没有训练集
    if kwargs.get('all_testset'):
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
        # 找到 trainset 和 testset 中的package
        train_package_set = set(train_dic[nameWithOwner])
        test_package_set = set(test_dic[nameWithOwner])
        # 计算 trainset 中每个package的tf值，TfIdf = packageTF * packageRepoIDF
        dependency_dic_list = []
        for package in train_package_set:
            dependency_dic = {}
            dependency_dic['nameWithManager'] = package
            dependency_dic['packageTf'] = 1 / len(train_package_set)
            # dependency_dic['dependedTF'] = 1
            dependency_dic_list.append(dependency_dic)
        repo_portrait_dic = {}
        repo_portrait_dic['nameWithOwner'] = nameWithOwner
        repo_portrait_dic['dependency_dic_list'] = dependency_dic_list
        # 输入用户画像，进行推荐
        response = reco_packages_experiment(json.dumps(repo_portrait_dic),
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
    logger.info('\nkwargs: ' + json.dumps(kwargs, indent=4) + '\ncost_time: ' + str(cost_time))
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


def dump_model_validation_set(reco_dir, **kwargs):
    validation_dic, validation_row_list, model_dic, model_row_list = split_model_validation_dic(**kwargs)
    df_validation = pd.DataFrame(data=validation_row_list, columns=['nameWithOwner', 'nameWithManager'])
    df_model = pd.DataFrame(data=model_row_list, columns=['nameWithOwner', 'nameWithManager'])
    dump_file_validation = 'repo_has_model-package_split_' + str(kwargs.get('user_M')) + '_validation_' + "_".join(map(lambda x: str(x), kwargs.get(
        'user_ks'))) + '_' + str(uuid1()) + '.csv'
    dump_file_model = 'repo_has_model-package_split_' + str(kwargs.get('user_M')) + '_validation_' + "_".join(map(lambda x: str(x), kwargs.get(
        'user_ks'))) + '_model_' + str(uuid1()) + '.csv'
    df_validation.to_csv(os.path.join(reco_dir, dump_file_validation), index=False, encoding='utf8')
    df_model.to_csv(os.path.join(reco_dir, dump_file_model), index=False, encoding='utf8')
    pass


def split_model_validation_dic(**kwargs):
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


def split_train_test_dic(**kwargs):
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


def reco_packages_experiment(repo_portrait_dic, kwargs):
    """

    :param kwargs:
    :param repo_portrait_dic:
    :return:
    """
    url = "http://localhost:8080/github/exp/recommend/package"
    payload = {"repo_portrait_dic": repo_portrait_dic, 'kwargs': kwargs}
    # header_dict = {"Content-Type": "application/json; charset=utf8"}
    return requests.post(url=url, data=payload).content.decode("utf-8")


def cal_precision_recall(TP, Tu, Ru):
    recall = TP / Tu
    precision = TP / Ru
    return precision, recall


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


def delete_validationset_dependency_batch(**kwargs):
    validation_dic, validation_row_list, model_dic, model_row_list = split_model_validation_dic(**kwargs)
    for i, row in enumerate(validation_row_list):
        if i < 27495:
            continue
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
