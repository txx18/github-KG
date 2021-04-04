import random
from collections import OrderedDict

from statistic import *
from util.FileUtils import *


def rename_file_name_not_match_nameWithOwner(repo_dir):
    target_file_tuple = get_file_name_not_match_nameWithOwner(repo_dir)
    for item in target_file_tuple:
        os.rename(os.path.join(repo_dir, item[0]), os.path.join(repo_dir, item[1]))
    print('finish rename!')


def split_data_list(raw_lst, M, k, seed):
    '''
    :param raw_lst: 要想保证每次输出都是相同，输入要自己保证是固定的顺序；此函数只保证划分list，如果要去重要自己输入list(set)
    :param M:
    :param k:
    :param seed:
    :return: 不转为set，
    '''

    N = len(raw_lst)
    index_lst = [i for i in range(0, N)]
    index_dic = defaultdict(list)
    offset = int(N / M)
    for i in range(0, M):
        random.seed(seed)
        # 从lst中采样offset个元素
        index_dic['set_' + str(i)] = random.sample(index_lst, offset)
        # for repo in dic['set_' + str(i)]:
        #     lst.remove(repo)
        index_lst = list(set(index_lst) - set(index_dic['set_' + str(i)]))
    # index_lst还有剩余（余数），依次加入前几个set_
    for i in range(len(index_lst)):
        index_dic['set_' + str(i)].append(index_lst[i])
    item_dic = defaultdict(list)
    for i in range(0, M):
        for index in index_dic['set_' + str(i)]:
            item_dic['set_' + str(i)].append(raw_lst[index])
    train_list = []
    # 选其中一份作为测试，其余训练模型
    for key, val in item_dic.items():
        if key != 'set_' + str(k):
            train_list.extend(val)
    test_list = item_dic.get('set_' + str(k))
    return train_list, test_list


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


def load_pwc_json():
    evaluation_tables = read_json_file(r"C:\Disk_Dev\Repository\github-KG\github-KG-python\tx_data\resource"
                                       r"\paperswithcode\evaluation-tables.json")
    links_between_papers_and_code = read_json_file(
        r"C:\Disk_Dev\Repository\github-KG\github-KG-python\tx_data\resource\paperswithcode\links-between-papers-and-code.json")
    methods = read_json_file(
        r"C:\Disk_Dev\Repository\github-KG\github-KG-python\tx_data\resource\paperswithcode\methods.json")
    papers_with_abstracts = read_json_file(
        r"C:\Disk_Dev\Repository\github-KG\github-KG-python\tx_data\resource\paperswithcode\papers-with-abstracts.json")
    return {"evaluation-tables": evaluation_tables,
            "links_between_papers_and_code": links_between_papers_and_code,
            "methods": methods,
            "papers_with_abstracts": papers_with_abstracts
            }


def load_repo_dir_jsons(repo_dir_path):
    res = OrderedDict()
    for repo_index, repo_file in enumerate(os.listdir(repo_dir_path)):
        if os.path.splitext(repo_file)[1] != ".json":
            continue
        json = read_json_file(repo_dir_path + "/" + repo_file)
        res[repo_file] = json
    return res
