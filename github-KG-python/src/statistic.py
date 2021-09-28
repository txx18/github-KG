import os
import re
import shutil
from collections import defaultdict

import jsonpath
import pandas as pd

from util.FileUtils import read_json_file


def stat_dependency_count_dic(repo_dir, nameWithOwner, train_package_set):
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


def get_file_name_not_match_nameWithOwner(repo_dir):
    res = []
    should_file_name_list = []
    for i, repo_file in enumerate(os.listdir(repo_dir)):
        if os.path.splitext(repo_file)[1] != '.json':
            continue
        json_dic = read_json_file(os.path.join(repo_dir, repo_file))
        nameWithOwner = jsonpath.jsonpath(json_dic, '$.data.repository.nameWithOwner')[0]
        should_file_name = nameWithOwner.replace('/', '-$-') + '.json'
        if should_file_name != repo_file:
            res.append((repo_file, should_file_name))
    #         should_file_name_list.append(should_file_name)
    # should_file_name_set = set(should_file_name_list)
    return res


def get_needless_repo_set(repo_dir, raw_file_list, min_dependency_count):
    '''
    1、不同名的仓库，内容完全一样，相当于就换了个名字，对于这种重复只保留一份repo
    2、没有依赖的repo，这样的独立节点对推荐没用

    :param min_dependency_count:
    :param repo_dir:
    :return:
    '''
    res_dic = defaultdict(list)
    dup_dic = defaultdict(list)
    no_dup_nwo_set = set()
    # res_set = set()
    dup_set = set()
    no_dependency_set = set()
    less_dependency_set = set()
    for i, repo_file in enumerate(raw_file_list):
        if os.path.splitext(repo_file)[1] != ".json":
            continue
        json_dic = read_json_file(os.path.join(repo_dir, repo_file))
        nameWithOwner = jsonpath.jsonpath(json_dic, "$.data.repository.nameWithOwner")[0]
        if nameWithOwner in no_dup_nwo_set:
            dup_set.add(repo_file)
            continue
        no_dup_nwo_set.add(nameWithOwner)
        # 过滤没有依赖的repo
        dgm_count = jsonpath.jsonpath(json_dic, "$.data.repository.dependencyGraphManifests.totalCount")[0]
        if dgm_count == 0:
            no_dependency_set.add(repo_file)
            continue
        packageName_list = jsonpath.jsonpath(json_dic, "$.data.repository.dependencyGraphManifests.nodes["
                                                       "*].dependencies.nodes[*].packageName")
        if packageName_list == False:
            no_dependency_set.add(repo_file)
            continue
        # 过滤依赖数量不够的repo
        if len(set(packageName_list)) < min_dependency_count:
            less_dependency_set.add(repo_file)
            continue
    # res_set.add(repo_file)
    #         res_dic[nameWithOwner].append(repo_file)
    #     for key, val in res_dic.items():
    #         if len(val) > 1:
    #             dup_dic[key] = val
    #     res = list(res_dic.keys())
    #     return res
    return dup_set, no_dependency_set, less_dependency_set


def get_exist_repo_list_by_info(repo_dir_path):
    res_list = []
    for repo_index, repo_file in enumerate(os.listdir(repo_dir_path)):
        ext = os.path.splitext(repo_file)[1]
        if ext == ".json" or ext == '.md':
            json_dic = read_json_file(os.path.join(repo_dir_path, repo_file))
            nameWithOwner = jsonpath.jsonpath(json_dic, "$.data.repository.nameWithOwner")[0]
            res_list.append(nameWithOwner)
    return res_list


def get_exist_repo_file_list(repo_dir):
    res_list = []
    for repo_index, repo_file in enumerate(os.listdir(repo_dir)):
        ext = os.path.splitext(repo_file)[1]
        if ext == ".json" or ext == '.md':
            res_list.append(repo_file)
    return res_list


def get_exist_repo_list_by_fileName(repo_dir):
    res_list = []
    for repo_index, repo_file in enumerate(os.listdir(repo_dir)):
        ext = os.path.splitext(repo_file)[1]
        if ext == ".json" or ext == '.md':
            file_name = os.path.splitext(repo_file)[0]
            nameWithOwner = file_name.replace('-$-', '/')
            res_list.append(nameWithOwner)
    return res_list


def stat_topic_set(repo_dir_path):
    topic_list = []
    for repo_index, repo_file in enumerate(os.listdir(repo_dir_path)):
        if os.path.splitext(repo_file)[1] != ".json":
            continue
        json_dic = read_json_file(repo_dir_path + "/" + repo_file)
        dependencyGraphManifests_count = jsonpath.jsonpath(json_dic,
                                                           "$.data.repository.dependencyGraphManifests.totalCount")[0]
        if dependencyGraphManifests_count == 0:
            continue
        repo_topic_list = jsonpath.jsonpath(json_dic, "$.data.repository.repositoryTopics.nodes[*].topic.name")
        if repo_topic_list is False:
            continue
        topic_list.extend(repo_topic_list)
    topic_set = set(topic_list)
    sorted_topic_set = list(topic_set)
    sorted_topic_set.sort()
    df = pd.DataFrame(columns=["topic"], data=sorted_topic_set)
    df.to_csv(
        r"C:\Disk_Dev\Repository\github-KG\github-KG-python\tx_data\resource\paperswithcode\sorted_topic_set_hasdfm.csv",
        encoding='utf-8')
    return sorted_topic_set


def move_errors_repos(repo_dir_path, move_dir_path):
    error_repo_dic = check_errors_repos(repo_dir_path)
    for repo in error_repo_dic.keys():
        shutil.move(repo_dir_path + "/" + repo, move_dir_path)


def check_errors_repos(repo_path):
    error_repo_dic = {}
    # error_repo_list = []
    for repo_index, repo_file in enumerate(os.listdir(repo_path)):
        if os.path.splitext(repo_file)[1] != ".json":
            continue
        json_dic = read_json_file(os.path.join(repo_path, repo_file))
        errors = jsonpath.jsonpath(json_dic, "$.errors")
        if errors is not False:
            error_repo_dic[repo_file] = errors
            # error_repo_list.append(repo_file)
    # return error_repo_list
    return error_repo_dic


def stat_dependencyGraphManifests(repo_path):
    res = {
        "has_dfm": 0,
        "no_dfm": 0,
    }
    for repo_index, repo_file in enumerate(os.listdir(repo_path)):
        if os.path.splitext(repo_file)[1] != ".json":
            continue
        json_dic = read_json_file(os.path.join(repo_path, repo_file))
        dependencyGraphManifests_count = jsonpath.jsonpath(json_dic,
                                                           "$.data.repository.dependencyGraphManifests.totalCount")[0]
        if dependencyGraphManifests_count == 0:
            res["no_dfm"] += 1
        else:
            res["has_dfm"] += 1
    return res


def get_data_topic_set(out_dir_path):
    res = set()
    for i, topic_file in enumerate(os.listdir(out_dir_path)):
        if os.path.splitext(topic_file)[1] != ".json":
            continue
        topic = os.path.splitext(topic_file)[0]
        res.add(topic)
    return res


def check_property_null(repo_path):
    null_property_dic = {
        "hasDependencies": set(),
        "packageManager": set(),
        "packageName": set(),
        "repository": set(),
        "requirements": set(),
    }
    for repo_index, repo_file in enumerate(os.listdir(repo_path)):
        if os.path.splitext(repo_file)[1] != ".json":
            continue
        json = read_json_file(os.path.join(repo_path, repo_file))
        try:
            dependencyGraphManifest_nodes = json["data"]["repository"]["dependencyGraphManifests"]["nodes"]
            for node in dependencyGraphManifest_nodes:
                dependency_nodes = node["dependencies"]["nodes"]
                for node2 in dependency_nodes:
                    for key, val in node2.items():
                        if val is None:
                            null_property_dic[key].add(json["data"]["repository"]["nameWithOwner"])
        except Exception as e:
            print(e)
            print("exception at: " + repo_path + repo_file)
    return null_property_dic


def check_multi_page_100(repo_path):
    over_100_dic = {
        "dependencyGraphManifests_count": set(),
        "max_dependency_count": set(),
        "language_count": set(),
        "topic_count": set()
    }
    for repo_index, repo_file in enumerate(os.listdir(repo_path)):
        if os.path.splitext(repo_file)[1] != ".json":
            continue
        json = read_json_file(os.path.join(repo_path, repo_file))
        try:
            repo = json["data"]["repository"]
            repoName = repo["nameWithOwner"]
            exclude = repo["isEmpty"] or repo["isFork"] or repo["isLocked"] or repo["isPrivate"]
            if exclude:
                print("should be excluded: " + repo_path + repo_file)
            dependencyGraphManifests_count = repo["dependencyGraphManifests"]["totalCount"]
            if dependencyGraphManifests_count > 100:
                over_100_dic["dependencyGraphManifests_count"].add(repoName)
            max_dependency_count = 0
            for node in repo["dependencyGraphManifests"]["nodes"]:
                dependency_count = node["dependencies"]["totalCount"]
                if dependency_count > max_dependency_count:
                    max_dependency_count = dependency_count
            if max_dependency_count > 100:
                over_100_dic["max_dependency_count"].add(repoName)
            language_count = repo["languages"]["totalCount"]
            if language_count > 100:
                over_100_dic["language_count"].add(repoName)
            topic_count = repo["repositoryTopics"]["totalCount"]
            if topic_count > 100:
                over_100_dic["topic_count"].add(repoName)
        except Exception as e:
            print(e)
            print("exception at: " + repo_path + repo_file)
    return over_100_dic


def get_data_one_topic_repo_set(topic_path):
    res = set()
    for page_index, page_file in enumerate(os.listdir(topic_path)):
        if os.path.splitext(page_file)[1] != ".json":
            continue
        json = read_json_file(os.path.join(topic_path, page_file))
        # 预处理，忽略fork的仓库 private仓库
        for item in json["items"]:
            exclude = item["size"] == 0 or item["fork"] or item["private"]
            if exclude is True:
                continue
            res.add(item["full_name"])
    return res


def get_data_topic_repo_set(topic_repo_path):
    res_list = []
    for topic_index, topic_dir in enumerate(os.listdir(topic_repo_path)):
        for page_index, page_file in enumerate(os.listdir(os.path.join(topic_repo_path, topic_dir))):
            if os.path.splitext(page_file)[1] != ".json":
                continue
            json = read_json_file(os.path.join(topic_repo_path, topic_dir, page_file))
            # 预处理，忽略fork的仓库 private仓库
            for item in json["items"]:
                exclude = item["size"] == 0 or item["fork"] or item["private"]
                if exclude is True:
                    continue
                res_list.append(item["full_name"])
    return set(res_list)


class Paperswithcode(object):
    def get_arxivId_paperTitle_dic(self):
        json = read_json_file(
            r"C:\Disk_Dev\Repository\github-KG\github-KG-python\tx_data\resource\paperswithcode\papers-with-abstracts.json")
        res = {}
        for item in json:
            res[item["arxiv_id"]] = item["title"]
        return res
