import jsonpath
import requests

from util.FileUtils import *


def transCoOccurrenceNetworkNoRequirements():
    url = "http://localhost:8080/github/transCoOccurrenceNetworkNoRequirements"
    response = requests.post(url=url).content.decode("utf-8")
    if json.loads(response).get("status") != "success":
        print(json.loads(response))
        return


def importPapersWithAbstractJson(file_path):
    url = "http://localhost:8080/paperswithcode/importPapersWithAbstractJson"
    payload = {"filePath": file_path}
    response = requests.post(url=url, data=payload).content.decode("utf-8")
    if json.loads(response).get("status") != "success":
        print(json.loads(response))
        return


def importMethodsJson(file_path):
    url = "http://localhost:8080/paperswithcode/importMethodsJson"
    payload = {"filePath": file_path}
    response = requests.post(url=url, data=payload).content.decode("utf-8")
    if json.loads(response).get("status") != "success":
        print(json.loads(response))
        return


def importLinksBetweenPapersAndCodeJson(file_path):
    url = "http://localhost:8080/paperswithcode/importLinksBetweenPapersAndCodeJson"
    payload = {"filePath": file_path}
    response = requests.post(url=url, data=payload).content.decode("utf-8")
    if json.loads(response).get("status") != "success":
        print(json.loads(response))
        return


def importEvaluationTablesJson(file_path):
    url = "http://localhost:8080/paperswithcode/importEvaluationTablesJson"
    payload = {"filePath": file_path}
    response = requests.post(url=url, data=payload).content.decode("utf-8")
    if json.loads(response).get("status") != "success":
        print(json.loads(response))
        return


def updateTfIdf(ownerWithName):
    url = "http://localhost:8080/repo/updateTfIdf"
    payload = {"ownerWithName": ownerWithName}
    response = requests.post(url=url, data=payload).content.decode("utf-8")
    return response


def createRepoByJsonFile_batch(dir_path):
    insert_repo_count = 0
    for i, repo_file in enumerate(os.listdir(dir_path)):
        if os.path.splitext(repo_file)[1] != ".json":
            continue
        # if i <= 336:
        #     continue
        json_dic = read_json_file(os.path.join(dir_path, repo_file))
        nameWithOwner = jsonpath.jsonpath(json_dic, "$.data.repository.nameWithOwner")[0]
        print("file_index: " + str(i) + ", inserting: " + str(insert_repo_count) + ", repo: " + str(nameWithOwner))
        dgm_count = jsonpath.jsonpath(json_dic, "$.data.repository.dependencyGraphManifests.totalCount")[0]
        # 过滤没有依赖的repo
        if dgm_count == 0:
            continue
        response = createRepoByJsonFile(dir_path + "/" + repo_file)
        if json.loads(response).get("status") == "success":
            insert_repo_count += 1
        else:
            print(json.loads(response))
            break


def createRepoByJsonFile(file_path):
    url = "http://localhost:8080/repo/createRepoByJsonFile"
    payload = {"filePath": file_path}
    response = requests.post(url=url, data=payload).content.decode("utf-8")
    return response
