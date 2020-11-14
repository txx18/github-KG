import requests

from util.FileUtils import *
import jsonpath


def createRepoByJsonFile_batch(dir_path):
    insert_repo_count = 0
    for i, repo_file in enumerate(os.listdir(dir_path)):
        if os.path.splitext(repo_file)[1] != ".json":
            continue
        # if i < 13810:
        #     continue
        json_dic = read_json_file(os.path.join(dir_path, repo_file))
        nameWithOwner = jsonpath.jsonpath(json_dic, "$.data.repository.nameWithOwner")[0]
        dgm_count = jsonpath.jsonpath(json_dic, "$.data.repository.dependencyGraphManifests.totalCount")[0]
        # 过滤没有依赖的repo
        if dgm_count == 0:
            continue
        response = createRepoByJsonFile(dir_path + "/" + repo_file)
        if json.loads(response).get("status") == "success":
            insert_repo_count += 1
            print("index: " + str(i) + ", insert: " + str(insert_repo_count) + ", repo: " + str(nameWithOwner))
        else:
            print(json.loads(response))


def createRepoByJsonFile(file_path):
    url = "http://localhost:8080/repo/createRepoByJsonFile"
    payload = {"filePath": file_path}
    response = requests.post(url=url, data=payload).content.decode("utf-8")
    return response
