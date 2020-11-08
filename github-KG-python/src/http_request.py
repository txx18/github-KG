import requests

from util.FileUtils import *
import jsonpath


def createRepoByJsonFile_batch(dir_path):
    insert_repo_count = 0
    for i, file in enumerate(os.listdir(dir_path)):
        # if i < 23520:
        #     continue
        json_dic = read_json_file(os.path.join(dir_path, file))
        nameWithOwner = jsonpath.jsonpath(json_dic, "$.data.repository.nameWithOwner")[0]
        dgm_count = jsonpath.jsonpath(json_dic, "$.data.repository.dependencyGraphManifests.totalCount")[0]
        # 过滤没有依赖的repo
        if dgm_count == 0:
            continue
        createRepoByJsonFile(dir_path + "/" + file)
        insert_repo_count += 1
        print("all: " + str(i) + ", insert: " + str(insert_repo_count) + ", repo: " + str(nameWithOwner))


def createRepoByJsonFile(file_path):
    url = "http://localhost:8080/repo/createRepo"
    payload = {"filePath": file_path}
    response = requests.post(url=url, data=payload).content.decode("utf-8")
    return response


if __name__ == "__main__":
    # createRepoByJsonFile(r"C:/Disk_Data/Small_Data/Neo4j/PaddlePaddle-$-Paddle.json")
    createRepoByJsonFile_batch(r"C:/Disk_Dev/Repository/github-KG/github-KG-python/tx_data/repo")
