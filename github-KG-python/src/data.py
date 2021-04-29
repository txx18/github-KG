from github import *
from statistic import *
from util.FileUtils import *


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
        for repo_i, record_list in reco_result_json_dic.items():
            for record in record_list:
                package_j = record['nameWithManager']
                if repo_package_row_dic[repo_i].get(package_j) is None:
                    repo_package_row_dic[repo_i][package_j] = defaultdict()
                    # 默认所有recommender的score为0
                    for key in recommender_result_json_file_dic.keys():
                        repo_package_row_dic[repo_i][package_j][key] = 0.0
                repo_package_row_dic[repo_i][package_j][recommender] = record['score']
                repo_package_row_dic[repo_i][package_j]['hit'] = record['hit']
    # 把嵌套字典展平
    res_row_list = []
    for repo_i, val1 in repo_package_row_dic.items():
        for package_j, val2 in val1.items():
            res_row_dic = {'repo_i': repo_i, 'package_j': package_j}
            for key in val2.keys():
                res_row_dic[key] = val2[key]
            res_row_list.append(res_row_dic)
    df = pd.DataFrame(res_row_list)
    df.to_csv(out_csv, index=False)
    pass


def add_recommender_label(reco_result_json_file, **kwargs):
    """
    数据集全作为测试集
    :param reco_result_json_file:
    :param kwargs:
    :return:
    """
    train_row_list, test_row_list, train_dic, test_dic = split_train_test_dic(**kwargs)
    total_test_repo_set = test_dic.keys()
    payload_repo_set = set()
    # 如果是检验内容推荐器，在计算指标时训练集和测试集都是全集
    if kwargs.get('all_testset'):
        payload_repo_set = sorted(total_test_repo_set)
    else:
        print('数据集全作为测试集')
        exit(0)
    reco_result_json_dic = read_json_file(reco_result_json_file)
    # 对测试集中每个repo检查推荐结果
    for index, nameWithOwner in enumerate(payload_repo_set):
        print('-------------------------------------------------------------')
        print("index: " + str(index + 1) + "/" + str(len(payload_repo_set)) + ", repo: " + str(nameWithOwner))
        test_package_set = set(test_dic[nameWithOwner])
        reco_package_set = set()
        # 依次检查 test_package_set 每个 package 是否命中
        for record in reco_result_json_dic[nameWithOwner]:
            reco_package_set.add(record['nameWithManager'])
            # 如果命中，则标记为1，没命中则标记为0
            if record['nameWithManager'] in test_package_set:
                # 可以做到修改原字典
                record['hit'] = 1
            else:
                record['hit'] = 0
        # 对于推荐列表中没有而在测试集中有的，新增记录，score为 0，hit为 1
        not_in_reco_package_set = test_package_set - reco_package_set
        for package in not_in_reco_package_set:
            reco_result_json_dic[nameWithOwner].append({'score': 0.0, 'nameWithManager': package, 'hit': 1})
    # 序列化结果
    to_reco_csv_file = kwargs.get('to_reco_csv_file').split('\\')[-1:][0]
    split_dir_name = 'split_' + str(kwargs.get('split_M')) + '_test_' + "_".join(map(lambda x: str(x), kwargs.get(
        'test_ks')))
    out_dir = kwargs.get('out_dir') + '/' + os.path.join(os.path.splitext(to_reco_csv_file)[0], split_dir_name,
                                                         'top' + str(kwargs.get('topN')), 'label')
    dump_file = os.path.splitext(reco_result_json_file.split('\\')[-1:][0])[0] + '_label_' + str(uuid1()) + '.json'
    dump_file_path = out_dir + '/' + dump_file
    write_json_file(out_dir, dump_file_path, reco_result_json_dic)
    pass


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
