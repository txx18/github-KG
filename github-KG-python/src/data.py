import json
import os
from collections import OrderedDict

import jsonpath

from util.FileUtils import read_json_file


class Paperswithcode(object):
    def get_arxivId_paperTitle_dic(self):
        json = read_json_file(
            r"C:\Disk_Dev\Repository\github-KG\github-KG-python\tx_data\resource\paperswithcode\papers-with-abstracts.json")
        res = {}
        for item in json:
            res[item["arxiv_id"]] = item["title"]
        return res



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
