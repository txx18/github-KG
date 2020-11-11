import hashlib
import logging
import random
import time

import requests

from src import queries
from src.statistic import *
from util.FileUtils import *


def read_token():
    token_file = open("../config/token_list.txt", 'r')
    tokens = token_file.readlines()
    return tokens


# create logger
logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
# create console handler and set level to debug
ch = logging.FileHandler(os.path.join(os.getcwd(), "..", "crawler.log"), mode='a', encoding=None, delay=False)
ch.setLevel(logging.DEBUG)
# create formatter
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
# add formatter to ch
ch.setFormatter(formatter)
# add ch to logger
logger.addHandler(ch)


class GithubAPIv3(object):

    def __init__(self):
        self.api = "https://api.github.com/"

    def search_topic_page_repos(self, q, sort, order, per_page, out_dir_path):
        print("begin to search: " + q)
        one_topic = q.split(":")[1]
        pageNo = 1
        while True:
            print("crawling page " + str(pageNo) + "...")
            tokens = read_token()
            token = tokens[random.randint(0, len(tokens) - 1)].strip()
            headers = {"Authorization": "token %s" % token,
                       "Accept": "application/vnd.github.mercy-preview+json"}
            url = self.api + "search/repositories?q=%s&sort=%s&order=%s&page=%s&per_page=%s" % (
                q, sort, order, pageNo, per_page)
            try:
                # 获取第pageNo页
                response = requests.get(url=url, headers=headers)
                response_json = response.json()
                # 有时候被拒绝请求会返回message，抛出异常，请求重试
                print("response.status_code: " + str(response.status_code))
                if response.status_code != 200:
                    raise Exception("request error with search " + q)
                # 测试API限制
                print(headers)
                print("X-RateLimit-Limit: " + str(response.headers.get("X-RateLimit-Limit")))
                print("X-RateLimit-Remaining: " + str(response.headers.get("X-RateLimit-Remaining")))
                print('X-RateLimit-Reset: ' + str(response.headers.get("X-RateLimit-Reset")))
                # 写入文件
                write_json_file(out_dir_path + "/" + one_topic,
                                out_dir_path + "/" + one_topic + "/page" + str(pageNo) + ".json",
                                response_json)
                print("write to file: " + out_dir_path + "/" + one_topic + "/page" + str(pageNo) + ".json")
                # 如果有下一页，循环
                if response.links.__contains__("next"):
                    pageNo += 1
                else:
                    break
            except Exception as e:
                print(e)
                print("error at page" + str(pageNo) + "...retrying...")
                continue
        print("search finished")


class GithubAPIv4(object):

    def __init__(self):
        self.api = "https://api.github.com/graphql"
        self.retry_times = 0

    def get_relate_topics(self, topic, out_dir_path):
        while True:
            tokens = read_token()
            token = tokens[random.randint(0, len(tokens) - 1)].strip()
            headers = {"Authorization": "Bearer %s" % token}
            query = queries.topic_query % topic
            try:
                response = requests.post(url=self.api, headers=headers, json={"query": query})
                response_json = response.json()
                print("response.status_code: " + str(response.status_code))
                if response.status_code != 200:
                    logger.info("request error at: " + str(topic))
                # 写入文件
                write_json_file(out_dir_path, out_dir_path + "/" + topic + ".json", response_json)
                break
            except Exception as e:
                print(e)
                print("other exception at: " + topic)
                logger.error("other exception at: " + topic)
                continue
        print("get_relate_topics finished")

    def get_repo(self, repo_full_name, out_dir_path):
        owner, repoName = repo_full_name.split("/")
        query = queries.repos_query % (owner, repoName)
        # manifest_pageNo = 1
        # dependency_pageNo = 1
        # language_pageNo = 1
        # topic_pageNo = 1
        while True:
            print("begin to get repo: " + owner + "/" + repoName)
            tokens = read_token()
            token = tokens[random.randint(0, len(tokens) - 1)].strip()
            headers = {"Authorization": "Bearer %s" % token,
                       "Accept": "application/vnd.github.hawkgirl-preview+json"}
            try:
                response = requests.post(url=self.api, headers=headers, json={"query": query})
                response_json = response.json()
                # 处理errors
                errors_status = self.handle_errors(response_json)
                if errors_status is None:
                    pass
                elif errors_status == 'RATE_LIMITED':
                    raise Exception(errors_status + ": " + str(headers))
                else:
                    raise Exception(errors_status)
                # 如果过滤仓库时出现异常，视情况而定
                try:
                    exclude = self.filter_repo(response_json)
                except Exception as e:
                    print(e)
                    # null repo异常 记录exclude仓库，跳过
                    if str(e) == "null repo" or str(e) == "null filter property":
                        log = "exception at filter, pass {}, message: {}".format(repo_full_name, e)
                        print(log)
                        logger.exception(log)
                        write_file_line_append(out_dir_path, out_dir_path + "/exclude_repo.txt", repo_full_name)
                        break
                    # error异常 重试
                    elif str(e) == "errors":
                        raise Exception(e)
                    else:
                        raise Exception("unknown filter exception")
                if exclude is True:
                    logger.info("exclude " + owner + "/" + repoName)
                    # 记录exclude仓库，跳过
                    write_file_line_append(out_dir_path, out_dir_path + "/exclude_repo.txt", repo_full_name)
                    break
                print("status_code: \033[32m" + str(response.status_code) + "\033[0m")
                # 不是200异常重试
                if response.status_code != 200:
                    logger.info("request error at: " + owner + "/" + repoName)
                    raise Exception("status code is not 200! ")
                # TODO 内部有4个字段可能有多页dependencyGraphManifests（嵌套dependencies），languages, repositoryTopics，但是一般都不会超过100个，所以这里统统不考虑了
                # 写入文件
                write_json_file(out_dir_path, os.path.join(out_dir_path, owner + "-$-" + repoName + ".json"),
                                response_json)
                print("write to file: " + str(os.path.join(out_dir_path, owner + "-$-" + repoName + ".json")))
                break
            # 捕获需要重试的异常
            except Exception as e:
                self.retry_times += 1
                if self.retry_times >= 10:
                    self.retry_times = 0
                    break
                else:
                    print(e)
                    print("exception at: " + owner + "/" + repoName + ", retrying...")
                    continue

    def handle_errors(self, response_json):
        # 检测 RATE_LIMIT
        errors = response_json.get('errors')
        if errors is None:
            return None
        for error in errors:
            if error.get('type') == 'RATE_LIMITED':
                return 'RATE_LIMITED'
            else:
                return "other error type"

    def filter_repo(self, response_json):
        # repo = jsonpath.jsonpath(response_json, "$.data.repository")
        data = response_json.get("data")
        if data is None:
            raise Exception("null repo")
        repo = data.get("repository")
        if repo is None:
            raise Exception("null repo")
        # errors = jsonpath.jsonpath(response_json, "$.errors")
        # errors = response_json.get("errors")
        # if errors is not None:
        #     raise Exception("errors")
        # 排除这些属性任何一个为true的，也有可能出现异常
        try:
            exclude = repo["isEmpty"] or repo["isFork"] or repo["isLocked"] or repo["isPrivate"]
        except Exception as e:
            raise Exception("null filter property")
        return exclude


v4 = GithubAPIv4()


def get_repos_paperswithcode(json_file_path, out_dir_path):
    json_dic = read_json_file(json_file_path)
    repo_url_list = jsonpath.jsonpath(json_dic, "$..repo_url")
    ownerWithName_list = []
    for url in repo_url_list:
        tokens = url.split("/")
        owner_with_name = tokens[-2] + "/" + tokens[-1]
        ownerWithName_list.append(owner_with_name)
    ownerWithName_set = set(ownerWithName_list)
    # 扫描已有的
    data_repo_set = get_data_repo_set(out_dir_path)
    try:
        exclude_repo_list = read_file_lines(out_dir_path + "/exclude_repo.txt")
    except FileExistsError as e:
        exclude_repo_list = []
    for i in range(len(exclude_repo_list)):
        exclude_repo_list[i] = exclude_repo_list[i].strip()
    exclude_repo_set = set(exclude_repo_list)
    not_exist_repo_set = ownerWithName_set - data_repo_set - exclude_repo_set
    payload_repo_list = crawl_repo_on_two_machine(not_exist_repo_set)
    crawled_count = 0
    minute_crawl_count = 0
    start_time = time.time()
    for ownerWithName in payload_repo_list:
        v4.get_repo(ownerWithName, out_dir_path)
        crawled_count += 1
        minute_crawl_count += 1
        cur_time = time.time()
        if cur_time >= start_time + 60:
            start_time = time.time()
            print("\033[1;35m" + "rate: " + str(minute_crawl_count) + " repos/min \033[0m")
            minute_crawl_count = 0
        print("has crawled: " + str(crawled_count) + "/" + str(len(payload_repo_list)))


def crawl_repo_on_two_machine(not_exist_repo_set):
    payload_repo_list = []
    # 分派任务，这里有个大坑，python的hash()对同一字符串 在不同平台or多次运行 结果是不同的
    for repo in not_exist_repo_set:
        if int(hashlib.md5(repo.encode("utf8")).hexdigest(), 16) % 2 == 1:
            payload_repo_list.append(repo)
        else:
            pass
    return payload_repo_list


def get_topic_repos(self, topic_dir_path, out_dir_path):
    data_one_topic_repo_set = get_data_one_topic_repo_set(topic_dir_path)
    # 扫描已有的仓库数据
    data_repo_set = get_data_repo_set(out_dir_path)
    payload_repo_set = data_one_topic_repo_set - data_repo_set
    for repo in payload_repo_set:
        self.get_repo(repo, out_dir_path)
    print("get topic repo batch finished")


def get_relate_topics_from_file_batch(file_path, out_dir_path):
    # 扫描需要爬取的
    topics = read_file(file_path).replace("\n", "").split(",")
    for i in range(len(topics)):
        topics[i] = topics[i].strip()
    topic_set = set(topics)
    # 扫描已有的
    data_topic_set = get_data_topic_set(out_dir_path)
    payload_topic_set = topic_set - data_topic_set
    lower_topic_set = set()
    for topic in payload_topic_set:
        lower_topic_set.add(topic.lower())
    extend_topic_set = (payload_topic_set | lower_topic_set)
    for topic in extend_topic_set:
        v4.get_relate_topics(topic, out_dir_path)


def get_topics_repos_batch(topic_repo_dir_path, out_dir_path):
    # 扫描topic需要爬取的仓库
    data_topic_repo_set = get_data_topic_repo_set(topic_repo_dir_path)
    # 扫描已有的仓库数据
    data_repo_set = get_data_repo_set(out_dir_path)
    payload_repo_set = data_topic_repo_set - data_repo_set
    for repo in payload_repo_set:
        v4.get_repo(repo, out_dir_path)
    print("get repo batch finished")
