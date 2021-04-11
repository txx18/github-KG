import logging
import re
import time

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
    """
    在训练模型之前，只应该保有 row_train_list 中的依赖关系，其他关系删除，再重新训练模型
    :param kwargs: 数据集和划分方式必须和最后推荐测试时相同
    :return:
    """
    train_row_list, test_row_list, train_dic, test_dic = split_train_test_dic(**kwargs)
    # 对 测试集 与 训练集 的repo交集中 每个repo做推荐
    test_set_repos = test_dic.keys()
    train_set_repos = sorted(train_dic.keys())
    inter_set_repos = sorted(train_set_repos & test_set_repos)
    # 推荐结果
    reco_result_dic = OrderedDict()
    # 时间指标
    start_time = time.time()
    # 开始遍历交集中的repo进行推荐
    for i, nameWithOwner in enumerate(inter_set_repos):  # inter_set_repos train_set_repos
        print('-------------------------------------------------------------')
        print("index: " + str(i + 1) + "/" + str(len(inter_set_repos)) + ", repo: " + str(nameWithOwner))
        # 找到 trainset 和 testset 中的package
        train_package_set = set(train_dic[nameWithOwner])
        test_package_set = set(test_dic[nameWithOwner])
        # 统计依赖次数 depended_count_dic，作为TF值的依据，注意现在只包括训练集中的package，测试集是未知的
        # depended_count_dic, train_package_depended_count = cal_dependency_count_dic(repo_dir, nameWithOwner,
        #                                                                           train_package_set)
        # 计算train_set中每个package的tf值，dependedTF相当于r_ui，对于隐反馈数据，可以都设为1，目前都设为1
        dependency_dic_list = []
        for package in train_package_set:
            dependency_dic = {}
            dependency_dic['nameWithManager'] = package
            # dependency_dic['dependedTF'] = float(depended_count_dic[package] / train_package_depended_count)
            dependency_dic['dependedTF'] = 1
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
        recoRecordList = response_json.get('data')
        reco_package_list = []
        for record in recoRecordList:
            reco_package_list.append(record.get('nameWithManager'))
        # 保存推荐结果
        reco_result_dic[nameWithOwner] = reco_package_list
        reco_package_set = set(reco_package_list)
        # print("train_package_set: " + str(train_package_set))
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
    out_dir = kwargs.get('out_dir') + '/' + os.path.join(os.path.splitext(to_reco_csv_file)[0], 'train' +
                                                         str(kwargs.get('train_k')), 'top' + str(kwargs.get('topN')))
    dump_file = kwargs.get('reco_method') + '.json'
    dump_file_path = out_dir + '/' + dump_file
    write_json_file(out_dir, dump_file_path, reco_result_dic)
    return


def split_train_test_dic(**kwargs):
    # 数据来源：所有依赖关系 -> 去掉repo的依赖数不足的
    df = pd.read_csv(kwargs.get('to_reco_csv_file'))
    relation_row_list = df.iloc[:, 0:2].values.tolist()
    # 划分关系
    train_row_list, test_row_list = split_data_list(relation_row_list, kwargs.get('train_M'),
                                                    kwargs.get('train_k'),
                                                    kwargs.get('train_seed'))
    # 分别建立test和train的字典
    train_dic = defaultdict(list)
    test_dic = defaultdict(list)
    for row in test_row_list:
        test_dic[row[0]].append(row[1])
    for row in train_row_list:
        train_dic[row[0]].append(row[1])
    return train_row_list, test_row_list, train_dic, test_dic


def cal_dependency_count_dic(repo_dir, nameWithOwner, train_package_set):
    repo_file = nameWithOwner.replace('/', '-$-') + '.json'
    file_path = os.path.join(repo_dir, repo_file)
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
    return depended_count_dic, train_package_depended_count


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


def delete_testset_dependency_batch(**kwargs):
    """

    :param kwargs: 数据集和划分方式必须和最后推荐测试时相同
    :return:
    """
    # 数据来源：to_reco_csv_file
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
