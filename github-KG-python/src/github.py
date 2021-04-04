import logging
import re

import requests

from data import *
from statistic import *
from util.FileUtils import *

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
ch = logging.FileHandler(os.path.join(os.getcwd(), "..", "github.py.log"), mode='a', encoding=None, delay=False)
ch.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
ch.setFormatter(formatter)
logger.addHandler(ch)


def reco_packages_batch_experiment_split_relation(**kwargs):
    # 数据来源：所有依赖关系 -> 去掉repo的依赖数不足的
    df = pd.read_csv(kwargs.get('to_reco_csv_file'))
    relation_row_list = df.iloc[:, 0:2].values.tolist()
    # 划分关系
    train_row_list, test_row_list = split_data_list(relation_row_list, kwargs.get('train_M'),
                                                    kwargs.get('train_k'),
                                                    kwargs.get('train_seed'))
    # 在训练模型之前，只应该保有 row_train_list 中的依赖关系，其他关系删除，再重新训练模型

    # 总train_package_set
    # 分别建立test和train的字典
    train_dic = defaultdict(list)
    test_dic = defaultdict(list)
    for row in test_row_list:
        test_dic[row[0]].append(row[1])
    for row in train_row_list:
        train_dic[row[0]].append(row[1])
    # 对 测试集 与 训练集 的repo交集中 每个repo做推荐
    test_set_repos = test_dic.keys()
    train_set_repos = train_dic.keys()
    inter_set_repos = train_set_repos & test_set_repos
    # 准确率、召回率指标
    TP = 0
    Ru = 0
    Tu = 0
    # 覆盖率指标
    total_train_package_set = set()
    total_reco_package_set = set()
    # 新颖度指标，先计算package的新颖度表，流行度越高，新颖度越低
    package_popularity_dic = defaultdict(int)
    for row in train_row_list:
        package_popularity_dic[row[1]] += 1
    train_avg_popularity = len(train_row_list) / len(package_popularity_dic.keys())
    reco_package_popular_count = 0
    # 开始遍历交集中的repo进行推荐
    for i, nameWithOwner in enumerate(inter_set_repos):
        print('-------------------------------------------------------------')
        print("index: " + str(i + 1) + "/" + str(len(inter_set_repos)) + ", repo: " + str(nameWithOwner))
        # 找到 trainset 和 testset 中的依赖
        train_package_set = set(train_dic[nameWithOwner])
        test_package_set = set(test_dic[nameWithOwner])
        # 统计依赖次数 depended_count_dic，作为TF值的依据，注意现在只包括训练集中的package，测试集是未知的
        repo_file = nameWithOwner.replace('/', '-$-') + '.json'
        file_path = os.path.join(kwargs.get('dir'), repo_file)
        json_dic = read_json_file(file_path)
        ground_truth_dependency_nodes_list = jsonpath.jsonpath(json_dic,
                                                               "$.data.repository.dependencyGraphManifests.nodes[*].dependencies.nodes[*]")
        depended_count_dic = defaultdict(int)
        train_package_depended_count = 0
        for node in ground_truth_dependency_nodes_list:
            nameWithManager = re.sub(re.compile(r'\s+'), '', node.get("packageManager") + "/" + node.get("packageName"))
            if nameWithManager in train_package_set:
                train_package_depended_count += 1
                depended_count_dic[nameWithManager] += 1
        # 计算train_set中每个package的tf值
        dependency_dic_list = []
        for package in train_package_set:
            dependency_dic = {}
            dependency_dic['nameWithManager'] = package
            dependency_dic['dependedTF'] = float(depended_count_dic[package] / train_package_depended_count)
            dependency_dic_list.append(dependency_dic)
        repo_portrait_dic = {}
        repo_portrait_dic['nameWithOwner'] = nameWithOwner
        repo_portrait_dic['dependency_dic_list'] = dependency_dic_list
        # 输入用户画像，进行推荐
        response = reco_packages_experiment(json.dumps(repo_portrait_dic),
                                            kwargs.get('reco_method'),
                                            kwargs.get('topN'))
        response_json = json.loads(response)
        if json.loads(response).get("status") != "success":
            print(json.loads(response))
            break
        reco_package_set = set(response_json.get('data'))
        print("train_package_set: " + str(train_package_set))
        print("reco_package_set: " + str(reco_package_set))
        print("test_package_set: " + str(test_package_set))
        # 计算准确率召回率
        single_intersection_count = len(reco_package_set & test_package_set)
        # single_precision = single_intersection_count / kwargs.get('topN')
        # single_recall = single_intersection_count / len(test_set)
        print('single_precision: ' + str(single_intersection_count) + '/' + str(len(reco_package_set)))
        print('single_recall: ' + str(single_intersection_count) + '/' + str(len(test_package_set)))
        TP += single_intersection_count
        Ru += len(reco_package_set)
        Tu += len(test_package_set)
        print('avg_precision: ' + str(TP) + '/' + str(Ru))
        print('avg_recall: ' + str(TP) + '/' + str(Tu))
        # 计算覆盖率
        # total_train_package_set 并集 每个trainset
        total_train_package_set = total_train_package_set | train_package_set
        # total_reco_package_set 并集 每个reco_set
        total_reco_package_set = total_reco_package_set | reco_package_set
        print('avg_coverage: ' + str(len(total_reco_package_set)) + '/' + str(len(total_train_package_set)))
        # 计算新颖度
        for reco_package in reco_package_set:
            reco_package_popular_count += package_popularity_dic[reco_package]
        print('avg_popularity: ' + str(reco_package_popular_count) + '/' + str(Ru))
    avg_precision = TP / Ru
    avg_recall = TP / Tu
    coverage = len(total_reco_package_set) / len(total_train_package_set)
    avg_popularity = reco_package_popular_count / Ru
    print('avg_precision: ' + str(avg_precision))
    print('avg_recall: ' + str(avg_recall))
    print('avg_coverage: ' + str(coverage))
    print('avg_popularity: ' + str(avg_popularity) + ', train_avg_popularity: ' + str(train_avg_popularity))
    logger.info('\n' + 'kwargs: ' + json.dumps(kwargs, indent=4) + '\n' + 'avg_precision: ' + str(avg_precision) +
                '\n' + 'avg_recall: ' + str(avg_recall) + '\n' + 'avg_coverage: ' + str(coverage) + '\n' +
                'avg_popularity: ' + str(avg_popularity) + ', train_avg_popularity: ' + str(train_avg_popularity) +
                '\n')
    return avg_precision, avg_recall, coverage, avg_popularity


def reco_packages_experiment(repo_portrait_dic, reco_method, topN):
    """

    :param repo_portrait_dic:
    :param topN:
    :return:
    """
    url = "http://localhost:8080/github/exp/recommend/package"
    payload = {"repo_portrait_dic": repo_portrait_dic, 'reco_method': reco_method, "topN": topN}
    # header_dict = {"Content-Type": "application/json; charset=utf8"}
    return requests.post(url=url, data=payload).content.decode("utf-8")


def cal_precision_recall(TP, Tu, Ru):
    recall = TP / Tu
    precision = TP / Ru
    return precision, recall


def refactorRepoCoPackageRepo_batch(**kwargs):
    # 1）划分数据
    # df = pd.read_csv(kwargs.get('csv_file'))
    # relation_row_list = df.iloc[:, 0:2].values.tolist()
    # row_train_list, row_test_list = split_data_list(relation_row_list, kwargs.get('model_M'),
    #                                                 kwargs.get('model_k'),
    #                                                 kwargs.get('model_seed'))

    # train_dic = defaultdict(list)
    # for row in row_train_list:
    #     train_dic[row[0]].append(row[1])
    # need_set = train_dic.keys()
    # 2）全部数据
    df = pd.read_csv(kwargs.get('csv_file'))
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


def update_repo_IDF_batch(repo_csv_file):
    count = 0
    data = pd.read_csv(repo_csv_file)
    repo_row_list = sorted(data.iloc[:, 0:1].values.tolist())
    for i, nameWithOwner in enumerate(repo_row_list):
        # if i <= 336:
        #     continue
        print(
            "index: " + str(i + 1) + ", updated: " + str(count) + ", repo: " + str(nameWithOwner[0]))
        response = update_repo_IDF(nameWithOwner[0])
        if json.loads(response).get("status") == "success":
            count += 1
        else:
            print(json.loads(response))
            break


def update_repo_IDF(nameWithOwner):
    url = "http://localhost:8080/github/update/repo/IDF"
    payload = {"nameWithOwner": nameWithOwner}
    response = requests.post(url=url, data=payload).content.decode("utf-8")
    return response


def delete_testset_dependency_batch(**kwargs):
    # 数据来源：所有依赖关系 -> 去掉repo的依赖数不足的
    df = pd.read_csv(kwargs.get('to_reco_csv_file'))
    relation_row_list = df.iloc[:, 0:2].values.tolist()
    # 划分关系
    row_train_list, row_test_list = split_data_list(relation_row_list, kwargs.get('train_M'),
                                                    kwargs.get('train_k'),
                                                    kwargs.get('train_seed'))
    for i, row in enumerate(row_test_list):
        # if i < 42758:
        #     continue
        print(
            "index: " + str(i + 1) + '/' + str(len(row_test_list)) + ", deleting: " + str(row[0]) + " - " + str(row[1]))
        response = delete_dependency(row[0], row[1])
        if json.loads(response).get("status") != "success":
            print(json.loads(response))
            break


def delete_dependency(nameWithOwner, nameWithManager):
    url = "http://localhost:8080/github/delete/depends_on"
    # header_dict = {"Content-Type": "application/json; charset=utf8"}
    payload = {"nameWithOwner": nameWithOwner, "nameWithManager": nameWithManager}
    return requests.post(url=url, data=payload).content.decode("utf-8")


def update_package_depended_degree_IDF_batch(**kwargs):
    data = pd.read_csv(kwargs.get('package_csv_file'))
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


def refactorPackageCoOccurrence_batch(**kwargs):
    # 1）划分
    # df = pd.read_csv(kwargs.get('csv_file'))
    # relation_row_list = df.iloc[:, 0:2].values.tolist()
    # row_train_list, row_test_list = split_data_list(relation_row_list, kwargs.get('model_M'),
    #                                                 kwargs.get('model_k'),
    #                                                 kwargs.get('model_seed'))
    # 总train_package_set
    # 分别建立test和train的字典
    # train_dic = defaultdict(list)
    # for row in row_train_list:
    #     train_dic[row[0]].append(row[1])
    # need_set = train_dic.keys()
    # 2）不划分
    df = pd.read_csv(kwargs.get('import_csv_file'))
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


def createRepoByJsonFile_batch(**kwargs):
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
    df = pd.read_csv(kwargs.get('import_csv_file'))
    need_set = df.iloc[:, 0:1].values.tolist()
    # 3）测试
    # need_set = ['densechen/AReLU', 'kenkai21/Image_Captioning', 'microsoft/IRNet',
    #             'muchafel/judgmentPrediction']
    for i, nameWithOwner in enumerate(need_set):
        # if i <= 336:
        #     continue
        print("index: " + str(i + 1) + '/' + str(len(need_set)) + ", repo: " + str(nameWithOwner[0]))
        repo_file = nameWithOwner[0].replace('/', '-$-') + '.json'
        file_path = os.path.join(kwargs.get('dir'), repo_file)
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
    # 数据来源：所有依赖关系 -> 去掉repo的依赖数不足的
    df = pd.read_csv(kwargs.get('csv_file'))
    relation_row_list = df.iloc[:, 0:3].values.tolist()
    # 划分关系
    row_train_list, row_test_list = split_data_list(relation_row_list, kwargs.get('model_M'),
                                                    kwargs.get('model_k'),
                                                    kwargs.get('model_seed'))
    for i, row in enumerate(row_train_list):
        if i < 97819:
            continue
        print("index: " + str(i) + ", merging: " + str(row[0]) + " - " + str(row[1]))
        response = create_dependency(row[0], row[1], row[2])
        if json.loads(response).get("status") != "success":
            print(json.loads(response))
            break


def create_dependency(nameWithOwner, nameWithManager, requirements):
    url = "http://localhost:8080/github/create/depends_on"
    # header_dict = {"Content-Type": "application/json; charset=utf8"}
    payload = {"nameWithOwner": nameWithOwner, "nameWithManager": nameWithManager, "requirements": requirements}
    return requests.post(url=url, data=payload).content.decode("utf-8")


def reco_packages_batch_experiment_newset(**kwargs):
    # 建议使用从csv读取need_set 替换成file格式
    data = pd.read_csv(kwargs.get('csv_file'))
    repo_row_list = sorted(data.iloc[:, 0:1].values.tolist())
    repo_file_list = []
    for repo in repo_row_list:
        repo_file_list.append(repo[0].replace('/', '-$-') + '.json')
    # 从文件夹读取
    # repo_file_list = get_exist_repo_file_list(kwargs.get('dir'))
    dup_repo_set, no_dependency_set, less_dependency_set = get_needless_repo_set(kwargs.get('dir'),
                                                                                 repo_file_list,
                                                                                 kwargs.get('min_dependency_count'))
    need_set = set(repo_file_list) - dup_repo_set - no_dependency_set - less_dependency_set
    # 划分模型集 和验证集
    # split_data_list函数要想保证每次输出次序一样需要保证每次输入次序一样，也就是本身不保证每次次序一样
    model_dataset, validation_dataset = split_data_list(sorted(list(need_set)), kwargs.get('model_M'),
                                                        kwargs.get('model_k'),
                                                        kwargs.get('model_seed'))
    count = 0
    TP = 0
    Ru = 0
    Tu = 0
    # 在验证集里做验证
    for i, repo_file in enumerate(validation_dataset):
        # if i <= 336:
        #     continue
        print("file_index: " + str(i + 1) + ", recommended: " + str(count) + ", repo: " + str(repo_file))
        file_path = os.path.join(kwargs.get('dir'), repo_file)
        json_dic = read_json_file(file_path)
        dependency_nodes_list = jsonpath.jsonpath(json_dic,
                                                  "$.data.repository.dependencyGraphManifests.nodes[*].dependencies.nodes[*]")
        # 对每个repo划分 训练集 和 测试集
        # 统计依赖次数 depended_count_dic
        # 首先对 dependency_nodes_list 去重 ，因为对于topN推荐，扔 M-1 份【不同的】关系进去，才是正确的召回率
        nameWithManager_set = set()
        depended_count_dic = {}
        for node in dependency_nodes_list:
            # 去除空白字符
            nameWithManager = re.sub(re.compile(r'\s+'), '', node.get("packageManager") + "/" + node.get("packageName"))
            nameWithManager_set.add(nameWithManager)
            if nameWithManager in depended_count_dic:
                depended_count_dic[nameWithManager] += 1
            else:
                depended_count_dic[nameWithManager] = 1
        train_list, test_list = split_data_list(sorted(list(nameWithManager_set)), kwargs.get('train_M'),
                                                kwargs.get('train_k'),
                                                kwargs.get('train_seed'))
        train_set = set(train_list)
        test_set = set(test_list)
        test_set_len = len(test_set)
        # 计算train_set中每个package的tf值，注意虽然扔进去不同的关系，但是每个关系的权重要在去重前计算
        input_dic_list = []
        for package in train_set:
            input_dic = {}
            input_dic['nameWithManager'] = package
            input_dic['dependedTF'] = float(depended_count_dic[package] / len(dependency_nodes_list))
            input_dic_list.append(input_dic)
        response = reco_packages_experiment(json.dumps(input_dic_list), kwargs.get('topN'))
        response_json = json.loads(response)
        if json.loads(response).get("status") != "success":
            print(json.loads(response))
            break
        else:
            count += 1
        reco_set = set(response_json.get('data'))
        # print('index:' + str(i) + ', ' + repo_file + "的推荐列表: " + str(reco_set))
        single_intersection_count = len(reco_set & test_set)
        single_precision = single_intersection_count / kwargs.get('topN')
        if test_set_len == 0:
            single_recall = 'no test set'
        else:
            single_recall = single_intersection_count / len(test_set)
        print('single_precision: ' + str(single_intersection_count) + '/' + str(kwargs.get('topN')) +
              ', single_recall: ' + str(single_intersection_count) + '/' + str(len(test_set)))
        TP += single_intersection_count
        Ru += kwargs.get('topN')
        Tu += len(test_set)
    precision, recall = cal_precision_recall(TP, Tu, Ru)
    print('avg_precision: ' + str(precision) + ', avg_recall: ' + str(recall))
    return precision, recall
