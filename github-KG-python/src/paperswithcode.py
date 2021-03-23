import requests

from util.FileUtils import *


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