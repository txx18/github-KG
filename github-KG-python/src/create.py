import jsonpath
import requests

from util.FileUtils import *


def import_repo_batch(dir_path):
    insert_repo_count = 0
    for i, repo_file in enumerate(os.listdir(dir_path)):
        if os.path.splitext(repo_file)[1] != ".json":
            continue
        # if i <= 336:
        #     continue
        json_dic = read_json_file(os.path.join(dir_path, repo_file))
        repo = jsonpath.jsonpath(json_dic, "$.data.repository")[0]
        json_str = json.dumps(repo)
        nameWithOwner = jsonpath.jsonpath(repo, "$.nameWithOwner")[0]
        print("file_index: " + str(i) + ", inserting: " + str(insert_repo_count) + ", repo: " + str(nameWithOwner))
        url = "http://localhost:8080/package/importRepo"
        payload = {"jsonStr": json_str}
        # header_dict = {"Content-Type": "application/json; charset=utf8"}
        # response = requests.post(url=url, data=json_str, headers=header_dict).content.decode("utf-8")
        response = requests.post(url=url, data=payload).content.decode("utf-8")
        if json.loads(response).get("status") == "success":
            insert_repo_count += 1
        else:
            print(json.loads(response))
            break
