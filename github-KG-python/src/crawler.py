import base64
import hashlib
import logging
import random
import time

import requests
from lxml import etree

from src import queries
from src.data import Paperswithcode
from src.statistic import *
from util.FileUtils import *


class GoogleScholar(object):
    def __init__(self):
        # self.api = "https://scholar.google.com/scholar?hl=en&as_sdt=0%2C5&q={}"
        # self.api = "https://xueshu.baidu.com/s?wd={}&tn=SE_baiduxueshu_c1gjeupa&cl=3&ie=utf-8&bs=arcface-additive-angular-margin-loss-for-deep&f=8&rsv_bp=1&rsv_sug2=0&sc_f_para=sc_tasktype%3D%7BfirstSimpleSearch%7D&rsv_spt=3&rsv_n=2"
        self.api = "https://xueshu.baidu.com/s?wd={}&tn=SE_baiduxueshu_c1gjeupa&ie=utf-8&sc_hit=1"
        self.retry_times = 0
        # self.header = get_header('scholar.google.com')
        self.header = get_header('xueshu.baidu.com')
        self.paperswithcode = Paperswithcode()

    # FIXME 搜索引擎往往区分了红字蓝字，很难分离
    def get_paper_title_batch(self, json_file_path, out_dir_path):
        json = read_json_file(json_file_path)
        dic = self.paperswithcode.get_arxivId_paperTitle_dic()
        dash_titles = jsonpath.jsonpath(json, "$..paper")
        for i, dash_title in enumerate(dash_titles):
            # if i < 157:
            #     continue
            if dash_title is None or dash_title == "":
                continue
            if dash_title.isdigit():
                str_list = list(dash_title)
                str_list.insert(4, ".")
                arxivId = "".join(str_list)
                title = dic.get(arxivId, "unknown")
                write_file_line_append(out_dir_path, out_dir_path + "/paper_titles.csv", dash_title + ", " + title)
                continue
            try:
                time.sleep(random.randint(2, 5))
                # search_title = dash_title.replace("-", " ")
                title = self.get_paper_title(dash_title, out_dir_path)
            except Exception as e:
                print(e)
                print("other exception")
                break
            print("get_paper_title finished: index: " + str(i) + ", title: " + dash_title)

    def get_paper_title(self, search_title, out_dir_path):
        url = self.api.format(search_title)
        response = requests.get(url=url, headers=self.header)
        content = response.text
        # content = str(response.text)
        html = etree.HTML(content)
        # gs_rt = html.xpath("//*[@class='gs_rt']/a[1]")
        try:
            search_tokens = html.xpath("//*[@id='1']/div[1]/h3/a")[0].text
            other_tokens = html.xpath("//*[@id='1']/div[1]/h3/a/text()")
            merge_tokens = []
            for i in range(len(other_tokens)):
                if other_tokens[i] == " ":
                    merge_tokens.append(search_tokens[i])
                else:
                    merge_tokens.append(other_tokens[i])
            title = " ".join(merge_tokens)
            if title is None or title == "":
                title = "unknown"
            # title = html.xpath("//*[@id='dtl_l']/div[1]/h3/a/text()")
        except Exception as e:
            print(e)
            title = "unknown"
        # soup = BeautifulSoup(content, "lxml")
        # title = soup.html.body.div[2].div[10].div[2].div[2].div[2].div[1].div[2].h3.a[1]
        # try:
        #     title = soup.select("h3[class='gs_rt']")[0].text
        # except Exception as e:
        #     title = "unknown"

        print("response.status_code: " + str(response.status_code))
        if response.status_code != 200:
            logger.info("request error at: " + str(search_title))
        # 写入文件
        write_file_line_append(out_dir_path, out_dir_path + "/paper_titles.csv", search_title + ", " + title)
        return title


def get_header(host):
    header = {
        'Host': host,
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8',
        'Accept-encoding': 'gzip, deflate',
        'Accept-language': 'zh-CN,zh;q=0.8',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36',
        'Connection': 'keep-alive',
        'Cache-Control': 'max-age=0',
        'Upgrade-Insecure-Requests': '1'
    }
    return header


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
        self.retry_times = 0
        self.fail_times = 0

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

    def get_repo_readme(self, owner, repo, out_dir_path):
        print("\nbegin to get: " + owner + "/" + repo)
        while True:
            tokens = read_token()
            token = tokens[random.randint(0, len(tokens) - 1)].strip()
            # accept	header	Setting to application/vnd.github.v3+json is recommended.
            headers = {"Authorization": "token %s" % token,
                       "Accept": "application/vnd.github.v3+json"}
            url = self.api + "repos/%s/%s/readme" % (owner, repo)
            try:
                # 设置timeout?
                response = requests.get(url=url, headers=headers, timeout=10)
                response_json = response.json()
                print("response.status_code: " + str(response.status_code))
                if response.status_code != 200:
                    if response.status_code == 404:
                        write_file_line_append(out_dir_path, out_dir_path + "/exclude/exclude_repo.txt", owner + "/" +
                                               repo)
                        log = 'request error 404 at:  \033[31m' + owner + '/' + repo + '\033[0m'
                        print(log)
                        break
                    raise Exception("request error")
                print(headers)
                print("X-RateLimit-Limit: " + str(response.headers.get("X-RateLimit-Limit")))
                print("X-RateLimit-Remaining: " + str(response.headers.get("X-RateLimit-Remaining")))
                print('X-RateLimit-Reset: ' + str(response.headers.get("X-RateLimit-Reset")))
                readme = response_json.get('content')
                readme = base64.urlsafe_b64decode(readme).decode('utf-8')
                out_file = out_dir_path + '/' + owner + "-$-" + repo + ".md"
                write_file(out_dir_path, out_file, readme)
                print("write to file: " + out_file)
                # 全部完成 break
                self.fail_times = 0
                break
            except Exception as e:
                if self.fail_times >= 10:
                    print("\033[33m failed over 10 repos... stop! \033[0m")
                    return
                self.retry_times += 1
                if self.retry_times >= 10:
                    write_file_line_append(out_dir_path, out_dir_path + "/exclude/retry_over_repo.txt", owner + "/" +
                                           repo)
                    self.retry_times = 0
                    self.fail_times += 1
                    break
                else:
                    time.sleep(5)
                    logger.info(e)
                    print("exception at: \033[31m" + owner + "/" + repo + "\033[0m, message: \033[31m" + str(
                        e) + "\033[0m" + ", \033[33m retrying...\033[0m")
                    continue


v3 = GithubAPIv3()


def get_repo_readme_batch(data_file, out_dir):
    data = pd.read_csv(data_file)
    repo_row_list = data.iloc[:, 0:1].values.tolist()
    repo_list = []
    for repo_row in repo_row_list:
        repo_list.append(repo_row[0])
    payload_repo_list = get_payload_repo_list(set(repo_list), out_dir)
    for repo in payload_repo_list:
        owner, repo = repo.split('/')
        v3.get_repo_readme(owner, repo, out_dir)
    print("get_repo_readme_batch finished!")


class GithubAPIv4(object):

    def __init__(self):
        self.api = "https://api.github.com/graphql"
        self.retry_times = 0
        self.fail_times = 0

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
                # 处理errors，分情况处理
                errors = response_json.get('errors')
                if errors is None:
                    pass
                else:
                    # 已经发现的error类型，RATE_LIMITED, NOT_FOUND, FORBIDDEN
                    # errors还可能是别的数据结构比如 loading, timedout 。此时error_type 为 None
                    error_type = errors[0].get('type')
                    error_message = errors[0].get('message')
                    if error_type == 'RATE_LIMITED':
                        log = 'error_type: ' + error_type + ", error_message: " + error_message + "headers: " + str(
                            headers)
                        print("\033[31m" + log + "\033[0m")
                        raise Exception(log)
                    elif error_type == 'NOT_FOUND':
                        write_file_line_append(out_dir_path, out_dir_path + "/exclude/exclude_repo.txt", repo_full_name)
                        log = 'error_type: ' + error_type + ", error_message: " + error_message + str(repo_full_name)
                        print("\033[31m" + log + "\033[0m, exclude it in ./exclude/exclude_repo.txt")
                        logger.info(log)
                        break
                    else:
                        log = str(errors)
                        raise Exception(log)
                # 如果过滤仓库时出现异常，分情况处理
                try:
                    exclude = self.filter_repo(response_json)
                except Exception as e:
                    print(e)
                    # null repo异常 记录exclude仓库，跳过
                    if str(e) == "null repo":
                        log = "exception at filter, pass {}, message: {}".format(repo_full_name, e)
                        print("\033[31m" + log + "\033[0m")
                        logger.exception(log)
                        write_file_line_append(out_dir_path, out_dir_path + "/exclude/exclude_repo.txt", repo_full_name)
                        break
                    # 其他异常应该抛出重试
                    else:
                        raise Exception(e)
                # 记录exclude仓库，跳过
                if exclude is True:
                    log = "exclude " + owner + "/" + repoName
                    print("\033[31m" + log + "\033[0m")
                    logger.info("exclude " + owner + "/" + repoName)
                    write_file_line_append(out_dir_path, out_dir_path + "/exclude/exclude_repo.txt", repo_full_name)
                    break
                print("status_code: \033[32m" + str(response.status_code) + "\033[0m")
                # 不是200异常重试
                if response.status_code != 200:
                    raise Exception("status code is not 200! ")
                # TODO 内部有4个字段可能有多页dependencyGraphManifests（嵌套dependencies），languages, repositoryTopics，但是一般都不会超过100个，所以这里统统不考虑了
                # 写入文件
                write_json_file(out_dir_path, os.path.join(out_dir_path, owner + "-$-" + repoName + ".json"),
                                response_json)
                print("write to file: " + str(os.path.join(out_dir_path, owner + "-$-" + repoName + ".json")))
                # 全部完成 break
                self.fail_times = 0
                break
            # 捕获需要重试的异常
            except Exception as e:
                # 连续10次重试10次，认为连续失败，比如断网了，停止程序
                if self.fail_times >= 10:
                    print("\033[33m failed over 10 repos... stop! \033[0m")
                    return
                self.retry_times += 1
                # 重试超过10次，记录并跳过（break完成跳过）
                if self.retry_times >= 10:
                    write_file_line_append(out_dir_path, out_dir_path + "/exclude/retry_over_repo.txt", repo_full_name)
                    self.retry_times = 0
                    self.fail_times += 1
                    break
                # 否则重试（continue完成重试）
                else:
                    time.sleep(5)
                    logger.info(e)
                    print("exception at: \033[31m" + owner + "/" + repoName + "\033[0m, message: \033[31m" + str(
                        e) + "\033[0m" + ", \033[33m retrying...\033[0m")
                    continue

    def filter_repo(self, response_json):
        """

        既有过滤自定义过滤repo的作用，也通过验证部分属性验证数据是否有效
        :param response_json:
        :return:
        """
        # 取一个值，可能是没有key，也有可能是有key value为null
        # jsonpath没有key返回 False，有key value为null返回[None]
        # repo = jsonpath.jsonpath(response_json, "$.data.repository")
        try:
            repo = response_json["data"]["repository"]
        except Exception as e:
            # 没有key, 先抛出观察
            raise Exception("no repo key repo exception")
        # 有key value为null的才跳过
        if repo is None:
            raise Exception("null repo")
        else:
            pass
        # 注意,jsonpath无法区分 没有key 和 有key但value为布尔值的
        # 这些key任何一个值为true的，跳过
        try:
            exclude = repo["isEmpty"] or repo["isFork"] or repo["isLocked"] or repo["isPrivate"]
        except Exception as e:
            # 这些key取 value时异常，则抛出
            raise Exception("no property key exception")
        return exclude


v4 = GithubAPIv4()


def get_repos_batch(json_file_path, out_dir_path):
    json_dic = read_json_file(json_file_path)
    repo_url_list = jsonpath.jsonpath(json_dic, "$..repo_url")
    ownerWithName_list = []
    # 从url提取ownerWithName, 这里有个坑比如有的url是带分支，所以应该取github.com/后面的 XXX/XXX
    for url in repo_url_list:
        tokens = url.split("/")
        owner_with_name = tokens[3] + "/" + tokens[4]
        ownerWithName_list.append(owner_with_name)
    ownerWithName_set = set(ownerWithName_list)
    # 扫描data_repo_set exclude_repo_list retry_over_repo_list
    data_repo_set = get_data_repo_set(out_dir_path)
    try:
        exclude_repo_list = read_file_lines(out_dir_path + "/exclude/exclude_repo.txt")
    except FileNotFoundError as e:
        exclude_repo_list = []
    try:
        # 重试次数过多的，先不爬取，有时候因为断网导致被记录的，可以清空之后就能对这些重新爬取
        retry_over_repo_list = read_file_lines(out_dir_path + "/exclude/retry_over_repo.txt")
    except FileNotFoundError as e:
        retry_over_repo_list = []
    for i in range(len(exclude_repo_list)):
        exclude_repo_list[i] = exclude_repo_list[i].strip()
    for i in range(len(retry_over_repo_list)):
        retry_over_repo_list[i] = retry_over_repo_list[i].strip()
    exclude_repo_set = set(exclude_repo_list)
    retry_over_repo_set = set(retry_over_repo_list)
    not_exist_repo_set = ownerWithName_set - data_repo_set - exclude_repo_set - retry_over_repo_set
    # 多机分派
    # payload_repo_list = crawl_repo_on_two_machine(not_exist_repo_set)
    payload_repo_list = not_exist_repo_set
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

def get_payload_repo_list(raw_repo_set, out_dir_path):
    data_repo_set = get_data_repo_set(out_dir_path)
    try:
        exclude_repo_list = read_file_lines(out_dir_path + "/exclude/exclude_repo.txt")
    except FileNotFoundError as e:
        exclude_repo_list = []
    try:
        # 重试次数过多的，先不爬取，有时候因为断网导致被记录的，可以清空之后就能对这些重新爬取
        retry_over_repo_list = read_file_lines(out_dir_path + "/exclude/retry_over_repo.txt")
    except FileNotFoundError as e:
        retry_over_repo_list = []
    for i in range(len(exclude_repo_list)):
        exclude_repo_list[i] = exclude_repo_list[i].strip()
    for i in range(len(retry_over_repo_list)):
        retry_over_repo_list[i] = retry_over_repo_list[i].strip()
    exclude_repo_set = set(exclude_repo_list)
    retry_over_repo_set = set(retry_over_repo_list)
    return raw_repo_set - data_repo_set - exclude_repo_set - retry_over_repo_set

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
