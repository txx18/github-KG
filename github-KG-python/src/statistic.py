import os

from util.FileUtils import read_json_file
import jsonpath
import shutil


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


def get_data_repo_set(repo_dir_path):
    res_list = []
    for repo_index, repo_file in enumerate(os.listdir(repo_dir_path)):
        if os.path.splitext(repo_file)[1] != ".json":
            continue
        owner, repoName = os.path.splitext(repo_file)[0].split("-$-")
        res_list.append(owner + "/" + repoName)
    return set(res_list)
