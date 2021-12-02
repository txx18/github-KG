import base64
import hashlib
import logging
import random
import time

import requests
from lxml import etree

from src import queries
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

    def get_repo_readme(self, repo_full_name, out_dir_path):
        owner, repoName = repo_full_name.split("/")
        print("\nbegin to get: " + repo_full_name)
        while True:
            tokens = read_token()
            token = tokens[random.randint(0, len(tokens) - 1)].strip()
            # accept	header	Setting to application/vnd.github.v3+json is recommended.
            headers = {"Authorization": "token %s" % token,
                       "Accept": "application/vnd.github.v3+json"}
            url = self.api + "repos/%s/%s/readme" % (owner, repoName)
            try:
                # 设置timeout?
                response = requests.get(url=url,
                                        headers=headers,
                                        # timeout=10
                                        )
                response_json = response.json()
                print("response.status_code: " + str(response.status_code))
                if response.status_code != 200:
                    if response.status_code == 404:
                        write_file_line_append(out_dir_path, out_dir_path + "/exclude/exclude_repo.txt", repo_full_name)
                        log = 'request error 404 at:  \033[31m' + owner + '/' + repoName + '\033[0m'
                        print(log)
                        break
                    raise Exception("request error")
                print(headers)
                print("X-RateLimit-Limit: " + str(response.headers.get("X-RateLimit-Limit")))
                print("X-RateLimit-Remaining: " + str(response.headers.get("X-RateLimit-Remaining")))
                print('X-RateLimit-Reset: ' + str(response.headers.get("X-RateLimit-Reset")))
                readme = response_json.get('content')
                readme = base64.urlsafe_b64decode(readme).decode('utf-8')
                out_file = out_dir_path + '/' + owner + "-$-" + repoName + ".md"
                write_file(out_dir_path, out_file, readme)
                print("write to file: " + out_file)
                # 全部完成 break
                self.fail_times = 0
                break
            except Exception as e:
                # 连续10次重试10次，认为连续失败，比如断网了，停止程序
                if self.fail_times >= 10:
                    print("\033[33m failed over 10 repos... stop! \033[0m")
                    return "poor network"
                self.retry_times += 1
                # 重试超过10次，记录并跳过（break完成跳过）
                if self.retry_times >= 10:
                    write_file_line_append(out_dir_path, out_dir_path + "/exclude/retry_over_repo.txt", repo_full_name)
                    self.retry_times = 0
                    self.fail_times += 1
                    break
                # 否则重试（continue完成重试）
                # 已知异常：1、DGM在loading的异常； 2、HTTP连接异常
                else:
                    time.sleep(5)
                    logger.info(e)
                    print("exception at: \033[31m" + owner + "/" + repoName + "\033[0m, message: \033[31m" + str(
                        e) + "\033[0m" + ", \033[33m retrying...\033[0m")
                    continue


v3 = GithubAPIv3()


def get_repo_readme_batch(**kwargs):
    target = kwargs.get('target')
    data_file = kwargs.get('target_file').get(target)
    out_dir = kwargs.get('out_dir').get(target)
    target_repo_list = get_target_repo_list(target, data_file)
    payload_repo_set = get_payload_repo_set(target_repo_list, out_dir)
    payload_repo_set = sorted(payload_repo_set)
    for index, nameWithOwner in enumerate(payload_repo_set):
        print()
        print('\033[31m' + 'index: ' + str(index + 1) + "/" + str(len(payload_repo_set)) + ", repo: " + str(nameWithOwner) + '-' * 100 + '\033[0m')
        v3.get_repo_readme(nameWithOwner, out_dir)
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

    def get_repo(self, target, raw_nwo, out_dir_path, no_dup_repo_set):
        pattern = re.compile(r'\s+')
        print('\n')
        owner, repoName = raw_nwo.split("/")
        query = queries.repos_query % (owner, repoName)
        # manifest_pageNo = 1
        # dependency_pageNo = 1
        # language_pageNo = 1
        # topic_pageNo = 1
        while True:
            print("begin to get repo: " + raw_nwo)
            tokens = read_token()
            token = tokens[random.randint(0, len(tokens) - 1)].strip()
            headers = {"Authorization": "Bearer %s" % token,
                       "Accept": "application/vnd.github.hawkgirl-preview+json"}
            try:
                response = requests.post(url=self.api,
                                         headers=headers,
                                         json={"query": query},
                                         # timeout=30
                                         )
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
                        write_file_line_append(out_dir_path, out_dir_path + "/exclude/exclude_repo.txt", raw_nwo)
                        log = 'error_type: ' + error_type + ", error_message: " + error_message + str(raw_nwo)
                        print("\033[31m" + log + "\033[0m, exclude it in ./exclude/exclude_repo.txt")
                        logger.info(log)
                        break
                    else:
                        log = str(errors)
                        raise Exception(log)
                # 如果过滤仓库时出现异常，分情况处理
                try:
                    exclude = self.filter_repo(target, response_json)
                except Exception as e:
                    print(e)
                    # null repo异常 记录exclude仓库，跳过
                    if str(e) == "null repo":
                        log = "exception at filter, pass {}, message: {}".format(raw_nwo, e)
                        print("\033[31m" + log + "\033[0m")
                        logger.exception(log)
                        write_file_line_append(out_dir_path, out_dir_path + "/exclude/exclude_repo.txt", raw_nwo)
                        break
                    # 其他异常应该抛出重试
                    else:
                        raise Exception(e)
                # 记录exclude仓库，跳过
                if exclude is True:
                    log = "exclude " + raw_nwo
                    print("\033[31m" + log + "\033[0m")
                    logger.info("exclude " + raw_nwo)
                    write_file_line_append(out_dir_path, out_dir_path + "/exclude/exclude_repo.txt", raw_nwo)
                    break
                print("status_code: \033[32m" + str(response.status_code) + "\033[0m")
                # 不是200异常重试
                if response.status_code != 200:
                    raise Exception("status code is not 200! ")
                # TODO 内部有4个字段可能有多页dependencyGraphManifests（嵌套dependencies），languages, repositoryTopics，但是一般都不会超过100个，所以这里统统不考虑了
                # 写入文件，文件名为 owner + "-$-" + repoName + ".json"
                # todo 统一以数据里的 nameWithOwner 字段为准，如果已经爬过这个nwo了就下一个，这样也就顺便解决了 同nwo不同文件名的问题
                real_nwo = re.sub(pattern, '', response_json['nameWithOwner'])
                if real_nwo in no_dup_repo_set:
                    write_file_line_append(out_dir_path, out_dir_path + "/exclude/dup_repo.txt", raw_nwo)
                    break
                repo_file_name = real_nwo.replace('/', '-$-') + '.json'
                write_json_file(out_dir_path, os.path.join(out_dir_path, repo_file_name), response_json)
                print("write to file: " + str(os.path.join(out_dir_path, repo_file_name)))
                # 全部完成，加入已爬取set break return?
                self.fail_times = 0
                no_dup_repo_set.add(real_nwo)
                return real_nwo
            # 捕获需要重试的异常
            except Exception as e:
                # 连续10次重试10次，认为连续失败，比如断网了，停止程序
                if self.fail_times >= 10:
                    print("\033[33m failed over 10 repos... stop! \033[0m")
                    return "poor network"
                self.retry_times += 1
                # 重试超过10次，记录并跳过（break完成跳过）
                if self.retry_times >= 5:
                    write_file_line_append(out_dir_path, out_dir_path + "/exclude/retry_over_repo.txt", raw_nwo)
                    self.retry_times = 0
                    self.fail_times += 1
                    break
                # 否则重试（continue完成重试）
                # 已知异常：1、DGM在loading的异常； 2、HTTP连接异常
                else:
                    time.sleep(5)
                    logger.info(e)
                    print("exception at: \033[31m" + raw_nwo + "\033[0m, message: \033[31m" + str(
                        e) + "\033[0m" + ", \033[33m retrying...\033[0m")
                    continue

    def filter_repo(self, target, response_json):
        """

        既有过滤自定义过滤repo的作用，也通过验证部分属性验证数据是否有效
        :param target:
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
        exclude = False
        # 注意,jsonpath无法区分 没有key 和 有key但value为布尔值的
        # 这些key任何一个值为true的，跳过
        try:
            # todo fork的仓库要不要呢
            is_empty = repo["isEmpty"]
            is_fork = repo["isFork"]
            is_locked = repo["isLocked"]
            is_private = repo["isPrivate"]
            # 认为dev_repo的fork不应该排除
            if target == 2:
                exclude = is_empty or is_locked or is_private
        except Exception as e:
            # 这些key取 value时异常，则抛出
            raise Exception("no property key exception")
        return exclude


v4 = GithubAPIv4()


def get_repos_batch(target, **kwargs):
    out_dir_path = kwargs.get('out_dir')
    target_repo_set = get_target_repo_list(target, **kwargs)
    # 除去 exclude的、重试过多的、nwo重复的
    payload_repo_set = get_payload_repo_set(target_repo_set, out_dir_path)
    # 如果要多机分派
    # payload_repo_list = crawl_repo_on_two_machine(not_exist_repo_set)
    crawled_count = len(target_repo_set) - len(payload_repo_set)
    minute_crawl_count = 0
    start_time = time.time()
    no_dup_repo_set = set()
    for raw_nwo in payload_repo_set:
        # 单个repo爬取
        res = v4.get_repo(target, raw_nwo, out_dir_path, no_dup_repo_set)
        if res == 'poor network':
            break
        crawled_count += 1
        minute_crawl_count += 1
        cur_time = time.time()
        if cur_time >= start_time + 60:
            start_time = time.time()
            print("\033[1;35m" + "rate: " + str(minute_crawl_count) + " repos/min \033[0m")
            minute_crawl_count = 0
        print("has crawled: " + str(crawled_count) + "/" + str(len(target_repo_set)))


def get_target_repo_list(target, data_file):
    pattern = re.compile(r'\s+')
    if target == 'links-between-papers-and-code':
        # 从 2 links-between-papers-and-code 获取 repo_url
        # json_file_path = kwargs.get("json_file_path")
        json_dic = read_json_file(data_file)
        repo_url_list = jsonpath.jsonpath(json_dic, "$..repo_url")
        nameWithOwner_list = []
        # 从url提取 ownerWithName, 这里有个坑比如有的url是带分支，所以应该取github.com/后面的 XXX/XXX
        for url in repo_url_list:
            tokens = url.split("/")
            nameWithOwner = re.sub(pattern, '', tokens[3] + "/" + tokens[4])
            nameWithOwner_list.append(nameWithOwner)
        return nameWithOwner_list
    elif target == 'dev_repos_csv' or target == 'repo_has_model':
        dic_list = pd.read_csv(data_file).to_dict(orient='records')
        repo_dic = defaultdict(list)
        for record in dic_list:
            repo_dic[record['nameWithOwner']].append(record['nameWithManager'])
        return list(repo_dic.keys())
    elif target == '1785_dataset':
        df_data = pd.read_csv(data_file)
        return list(df_data['repo'].apply(lambda x: x[len('Repository_'):]))


def get_payload_repo_set(target_repo_list, out_dir_path):
    """
    断点续传
    :param target_repo_list:
    :param out_dir_path:
    :return:
    """
    pattern = re.compile(r'\s+')
    exist_repo_list = get_exist_repo_list_by_fileName(out_dir_path)
    try:
        dup_repo_list = read_file_lines(out_dir_path + "/exclude/dup_repo.txt")
    except FileNotFoundError as e:
        dup_repo_list = []
    try:
        exclude_repo_list = read_file_lines(out_dir_path + "/exclude/exclude_repo.txt")
    except FileNotFoundError as e:
        exclude_repo_list = []
    try:
        # 重试次数过多的，先不爬取，有时候因为断网导致被记录的，可以清空之后就能对这些重新爬取
        retry_over_repo_list = read_file_lines(out_dir_path + "/exclude/retry_over_repo.txt")
    except FileNotFoundError as e:
        retry_over_repo_list = []
    for i in range(len(exist_repo_list)):
        exist_repo_list[i] = re.sub(pattern, '', exist_repo_list[i])
    for i in range(len(exclude_repo_list)):
        exclude_repo_list[i] = re.sub(pattern, '', exclude_repo_list[i])
    for i in range(len(retry_over_repo_list)):
        retry_over_repo_list[i] = re.sub(pattern, '', retry_over_repo_list[i])
    target_repo_set = set(target_repo_list)
    exist_repo_set = set(exist_repo_list)
    exclude_repo_set = set(exclude_repo_list)
    retry_over_repo_set = set(retry_over_repo_list)
    dup_repo_set = set(dup_repo_list)
    return target_repo_set - exist_repo_set - exclude_repo_set - retry_over_repo_set - dup_repo_set


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
    data_repo_set = get_exist_repo_list_by_info(out_dir_path)
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
    data_repo_set = get_exist_repo_list_by_info(out_dir_path)
    payload_repo_set = data_topic_repo_set - data_repo_set
    for repo in payload_repo_set:
        v4.get_repo(repo, out_dir_path)
    print("get repo batch finished")
