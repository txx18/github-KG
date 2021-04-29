import logging
import math
from collections import defaultdict, OrderedDict
from uuid import uuid1

import numpy as np
import pandas as pd

from github import split_train_test_dic
from util.FileUtils import *

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
ch = logging.FileHandler(os.path.join(os.getcwd(), "..", 'log', "cal.py.log"), mode='a', encoding=None, delay=False)
ch.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
ch.setFormatter(formatter)
logger.addHandler(ch)


def gini(array):
    """Calculate the Gini coefficient of a numpy array."""
    # based on bottom eq: http://www.statsdirect.com/help/content/image/stat0206_wmf.gif
    # from: http://www.statsdirect.com/help/default.htm#nonparametric_methods/gini.htm
    array = array.flatten()  # all values are treated equally, arrays must be 1d
    if np.amin(array) < 0:
        array -= np.amin(array)  # values cannot be negative
    array += 0.0000001  # values cannot be 0
    array = np.sort(array)  # values must be sorted
    index = np.arange(1, array.shape[0] + 1)  # index per array element
    n = array.shape[0]  # number of array elements
    return (np.sum((2 * index - n - 1) * array)) / (n * np.sum(array))  # Gini coefficient


def cal_index(result_json_file, top_popular_repo_csv, cal_out_dir, **kwargs):
    """

    :param cal_out_dir:
    :param result_json_file: 一种推荐方法的top100推荐结果
    :param top_popular_repo_csv: 在训练集上最流行的 top100 package
    :param kwargs: 预存了结果，所以 验证结果的数据集划分 要与 推荐时的数据集划分 完全一致
    :return:
    """
    train_row_list, test_row_list, train_dic, test_dic = split_train_test_dic(**kwargs)
    total_test_repo_set = test_dic.keys()
    # 如果是检验内容推荐器，【在计算指标时】训练集和测试集都是全集
    if kwargs.get('all_testset'):
        payload_repo_set = sorted(total_test_repo_set)
        train_row_list = test_row_list
        train_dic = test_dic
    else:
        total_train_repo_set = train_dic.keys()
        total_inter_repo_set = total_train_repo_set & total_test_repo_set
        payload_repo_set = sorted(total_inter_repo_set)
    # 一种推荐方法的top100推荐结果
    reco_result_json_dic = read_json_file(result_json_file)
    # 准确率、召回率、命中率指标，我自己加了一个【非热门召回率】指标
    total_TP = 0
    total_Tu = 0
    total_Ru = 0
    total_hit_repo_count = 0
    top_not_popular_TP = defaultdict(int)
    top_not_popular_Tu = defaultdict(int)
    top_not_popular_Ru = defaultdict(int)
    df_popular_package = pd.read_csv(top_popular_repo_csv)
    top_popular_package_set_dic = defaultdict(set)
    for i in range(0, 55, 5):
        top_popular_package_set_dic['top' + str(i)] = set([x[0] for x in df_popular_package.iloc[0:i, 0:1].values.tolist()])
    # 覆盖率指标 基尼系数 新颖度指标，核心是构建trainset recoset的package反向索引
    # 推荐列表的package出现次数分布，用于计算各种覆盖率
    reco_package_appear_count_distribution_dic = defaultdict(int)
    # 新颖度指标，可以用流行度度量（反相关）。流行度越高，新颖度越低
    # 计算 trainset package的流行度表，用于计算流行度值。度量方法：出现次数，也就是入度
    train_package_popularity_dic = defaultdict(int)
    for row in train_row_list:
        train_package_popularity_dic[row[1]] += 1
    # 流行度归一化
    # max_popularity = max(train_package_popularity_dic.values())
    # for key, val in train_package_popularity_dic.items():
    #     train_package_popularity_dic[key] = val / max_popularity
    # 训练集中package的平均流行度（平均出现次数）
    train_avg_popularity = len(train_row_list) / len(train_package_popularity_dic.keys())
    # 我认为，应该分为：推荐列表的流行度 & 命中列表的流行度
    total_reco_popularity = 0
    total_hit_popularity = 0
    # 对每个repo计算指标
    for index, nameWithOwner in enumerate(payload_repo_set):
        # print('-------------------------------------------------------------')
        # print("index: " + str(index + 1) + "/" + str(len(payload_repo_set)) + ", repo: " + str(nameWithOwner))
        # 找出每个repo的 trainset testset recoset hitset
        # train_package_set = set(train_dic[nameWithOwner])
        test_package_set = set(test_dic[nameWithOwner])
        # 截取 cal_topN，如果不足则全部取出
        reco_package_list = reco_result_json_dic[nameWithOwner][0: kwargs.get('cal_topN')]
        # if len(reco_package_list) == 0:
        #     print('no reco_package_list: ' + nameWithOwner)
        #     return
        reco_package_set = set(reco_package_list)
        # print("train_package_set: " + str(train_package_set))
        # print("test_package_set: " + str(test_package_set))
        # print("reco_package_list: " + str(reco_package_list))
        hit_package_set = reco_package_set & test_package_set
        if len(hit_package_set) != 0:
            total_hit_repo_count += 1
        # print('hit_set: ' + str(hit_package_set))
        # print('single_precision: ' + str(len(hit_package_set)) + '/' + str(len(reco_package_set)))
        # print('single_recall: ' + str(len(hit_package_set)) + '/' + str(len(test_package_set)))
        total_TP += len(hit_package_set)
        total_Ru += len(reco_package_set)
        total_Tu += len(test_package_set)
        # 对于非流行召回率，不算召回中 cal_topN 热门的
        for i in range(0, 55, 5):
            top_not_popular_TP['top' + str(i)] += len(hit_package_set - top_popular_package_set_dic['top' + str(i)])
            top_not_popular_Tu['top' + str(i)] += len(test_package_set - top_popular_package_set_dic['top' + str(i)])
            top_not_popular_Ru['top' + str(i)] += len(reco_package_set - top_popular_package_set_dic['top' + str(i)])
        # 更新覆盖分布
        for reco_package in reco_package_set:
            reco_package_appear_count_distribution_dic[reco_package] += 1
        # 更新推荐列表的 流行 贡献
        for reco_package in reco_package_set:
            total_reco_popularity += train_package_popularity_dic[reco_package]
        # 更新命中列表的 流行 贡献
        for hit_package in hit_package_set:
            total_hit_popularity += train_package_popularity_dic[hit_package]
    # 计算指标
    # （从0~50非热门）召回率Recall、精确率Precision、F值
    avg_recall = total_TP / total_Tu
    avg_precision = total_TP / total_Ru
    avg_top_not_popular_recall_dic = OrderedDict()
    avg_top_not_popular_precision_dic = OrderedDict()
    for i in range(0, 55, 5):
        avg_top_not_popular_recall_dic[str(i)] = top_not_popular_TP['top' + str(i)] / top_not_popular_Tu['top' + str(i)]
        if top_not_popular_Ru['top' + str(i)] != 0:
            avg_top_not_popular_precision_dic[str(i)] = top_not_popular_TP['top' + str(i)] / top_not_popular_Ru['top' + str(i)]
        else:
            avg_top_not_popular_precision_dic[str(i)] = 0
    avg_top_not_popular_F_dic = OrderedDict()
    for f in [0.5, 1, 2]:
        avg_top_not_popular_F_dic['F_' + str(f)] = OrderedDict()
        for i in range(0, 55, 5):
            avg_recall_i = avg_top_not_popular_recall_dic[str(i)]
            avg_precision_i = avg_top_not_popular_precision_dic[str(i)]
            if avg_recall_i != 0:
                avg_top_not_popular_F_dic['F_' + str(f)][str(i)] = (1 + f ** 2) * avg_precision_i * avg_recall_i / ((f ** 2) * avg_precision_i + avg_recall_i)
            else:
                avg_top_not_popular_F_dic['F_' + str(f)][str(i)] = 0
    # HR命中率
    HR = total_hit_repo_count / len(payload_repo_set)
    # 覆盖率
    total_reco_package_set = reco_package_appear_count_distribution_dic.keys()
    # 检验覆盖（不可能推荐出不在训练集中的package）
    total_train_package_set = train_package_popularity_dic.keys()
    not_in_train_but_in_reco_package_set = total_reco_package_set - total_train_package_set
    # 简单覆盖率
    simple_coverage = len(total_reco_package_set) / len(total_train_package_set)
    # 信息熵
    reco_package_appear_count_sum = sum(reco_package_appear_count_distribution_dic.values())
    information_entropy = 0
    for package, appear_count in reco_package_appear_count_distribution_dic.items():
        pi = appear_count / reco_package_appear_count_sum
        information_entropy += - pi * math.log(pi)
    # 基尼系数
    # 把分布展开，即出现多次的按多个物品算
    reco_popularity_distribution_dic = {}
    for package, appear_count in reco_package_appear_count_distribution_dic.items():
        for i in range(0, appear_count):
            if package in reco_popularity_distribution_dic.keys():
                new_package_name = package + '_' + str(uuid1())
            else:
                new_package_name = package
            reco_popularity_distribution_dic[new_package_name] = train_package_popularity_dic[package]
    # # 不展开，只是保存推荐列表package的流行度
    # reco_package_popularity_dic = {}
    # for package, appear_count in reco_package_appear_count_distribution_dic.items():
    #     reco_package_popularity_dic[package] = train_package_popularity_dic[package]
    sorted_values = np.array(list(reco_popularity_distribution_dic.values()), dtype=np.float)
    gini_index = gini(sorted_values)
    # 推荐列表的流行度 & 命中列表的流行度
    avg_reco_popularity = total_reco_popularity / total_Ru
    avg_hit_popularity = total_hit_popularity / total_TP
    print('---------------------------------------- info -----------------------------------')
    print("\nnot in trainset but in recoset: " + str(not_in_train_but_in_reco_package_set))
    print('\ntotal_TP:' + str(total_TP))
    print('\ntotal_Ru:' + str(total_Ru))
    print('\ntotal_Tu:' + str(total_Tu))
    print('\nfull_avg_recall_upper_bound: ' + str(len(payload_repo_set) * kwargs.get('cal_topN') / total_Tu))
    print('\navg_recall_upper_bound: ' + str(total_Ru / total_Tu))
    print('\ntrain_avg_popularity: ' + str(train_avg_popularity))
    print('\n---------------------------------------- avg index -----------------------------------')
    print('\navg_recall: ' + str(avg_recall))
    print('\navg_precision: ' + str(avg_precision))
    print('\navg_top_not_popular_recall_dic: ' + str(avg_top_not_popular_recall_dic))
    print('\navg_top_not_popular_precision_dic: ' + str(avg_top_not_popular_precision_dic))
    print('\navg_top_not_popular_F_dic: ' + str(avg_top_not_popular_F_dic))
    print('\nHR: ' + str(HR))
    print('\ninformation_entropy: ' + str(information_entropy))
    print('\nsimple_coverage: ' + str(simple_coverage))
    print('\ngini_index: ' + str(gini_index))
    print('\navg_reco_popularity: ' + str(avg_reco_popularity))
    print('\navg_hit_popularity: ' + str(avg_hit_popularity))
    print('--------------------------------------')
    df_not_popular_recall = pd.DataFrame.from_dict(avg_top_not_popular_recall_dic, orient='index')
    df_not_popular_precision = pd.DataFrame.from_dict(avg_top_not_popular_precision_dic, orient='index')
    df_not_popular_F1 = pd.DataFrame.from_dict(avg_top_not_popular_F_dic['F_1'], orient='index')
    if not os.path.exists(cal_out_dir):
        os.mkdir(cal_out_dir)
    df_not_popular_recall.to_csv(cal_out_dir + '/' + 'not_popular_recall.csv', sep=',')
    df_not_popular_precision.to_csv(cal_out_dir + '/' + 'not_popular_precision.csv', sep=',')
    df_not_popular_F1.to_csv(cal_out_dir + '/' + 'not_popular_F1.csv', sep=',')
    pass
