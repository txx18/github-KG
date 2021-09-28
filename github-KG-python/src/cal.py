import logging
import math
from collections import defaultdict, OrderedDict
from uuid import uuid1

import numpy as np
import pandas as pd

from data import split_portrait_train_test_by_count, get_split_corpus
from util.FileUtils import *

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
ch = logging.FileHandler(os.path.join(os.getcwd(), "..", 'log', "cal.py.log"), mode='a', encoding=None, delay=False)
ch.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
ch.setFormatter(formatter)
logger.addHandler(ch)


def cal_index_experiment(cal_kwargs, reco_kwargs, nlp_kwargs):
    # result_json_file 一种推荐算法的推荐结果 （保存top50）
    end_reco_result_json_dic = read_json_file(cal_kwargs.get('result_json_file'))
    # 最热门的package top100 表
    df_popular_package = pd.read_csv(cal_kwargs.get('top_popular_repo_csv'))
    top_popular_package_list = df_popular_package.to_dict('list')['nameWithManager']
    # 按整体计算（也就是以init为基准） (cal_kwargs.get('init_train_test_json_file'))
    # 在迭代次数为 1 时，init_train_test_dic 和 end_train_test_dic 是一样的
    init_train_test_dic = split_portrait_train_test_by_count(reco_kwargs)
    end_train_test_dic = read_json_file(cal_kwargs.get('cal_train_test_json_file'))
    # 如果要过滤仓库的 exclude_popular_N 热门依赖
    if cal_kwargs.get('exclude_popular_N') != 0:
        exclude_package_set = set(top_popular_package_list[:cal_kwargs.get('exclude_popular_N')])
        # 推荐列表在过滤时必须按序过滤
        for nameWithOwner in end_reco_result_json_dic.keys():
            end_reco_package_list = end_reco_result_json_dic[nameWithOwner]['package']
            filter_end_reco_package_list = [item for item in end_reco_package_list if item not in exclude_package_set]
            end_reco_result_json_dic[nameWithOwner]['package'] = filter_end_reco_package_list
        for nameWithOwner in init_train_test_dic.keys():
            init_test_package_set = set(init_train_test_dic[nameWithOwner]['test']['package'])
            end_test_package_set = set(end_train_test_dic[nameWithOwner]['test']['package'])
            init_train_test_dic[nameWithOwner]['test']['package'] = init_test_package_set - exclude_package_set
            end_train_test_dic[nameWithOwner]['test']['package'] = end_test_package_set - exclude_package_set
    keep_has_test_set_repo_set = set()
    # （理论上都需要过滤，不然没法算召回率）保留有训练集 且 有测试集的仓库，也就是去掉无测试集的仓库
    for nameWithOwner in end_train_test_dic.keys():
        if nameWithOwner not in init_train_test_dic.keys():
            print('nameWithOwner in end_train_test_dic but not in init_train_test_dic')
            exit(0)
        if len(init_train_test_dic[nameWithOwner]['test']['package']) > 0:
            keep_has_test_set_repo_set.add(nameWithOwner)
        else:
            print(nameWithOwner)
    payload_repo_set = keep_has_test_set_repo_set
    # 如果要过滤空文本输入的仓库
    if cal_kwargs.get('filter_empty_content'):
        filter_empty_content_repo_set = set()
        repo_model_content_dic, repo_vali_content_dic, repo_model_content_record_list, repo_vali_content_record_list = get_split_corpus(nlp_kwargs)
        for repo in keep_has_test_set_repo_set:
            if repo_vali_content_dic[repo] != '':
                filter_empty_content_repo_set.add(repo)
        payload_repo_set = filter_empty_content_repo_set
    # 最后排序，遍历按名称顺序
    payload_repo_set = sorted(payload_repo_set)
    # 精确率、召回率、命中率指标，我自己加了一个【非热门召回率】指标
    total_TP = 0
    total_Tu = 0
    total_Ru = 0
    total_hit_repo_count = 0
    exclude_popular_TP = defaultdict(int)
    exclude_popular_Tu = defaultdict(int)
    exclude_popular_Ru = defaultdict(int)
    end_total_TP = 0
    end_total_Tu = 0
    end_total_Ru = 0
    # 覆盖率指标、基尼系数、新颖度指标，核心是构建 train_set reco_set的 package 反向索引
    # 推荐列表的 package出现次数分布，用于计算各种覆盖率
    total_reco_package_appear_count_dic = defaultdict(int)
    # 命中列表的 package出现次数分布
    total_hit_package_appear_count_dic = defaultdict(int)
    # 可以用 流行度 度量（反相关） 新颖度指标。流行度越高，新颖度越低
    # 构造流行度表 ，用于计算流行度值。度量方法：出现次数，也就是入度，归一化
    total_package_popularity_dic = get_package_popularity_dic(cal_kwargs.get('package_degree_csv'), True)
    # 训练集中package的平均流行度
    total_package_avg_popularity = sum(total_package_popularity_dic.values()) / len(total_package_popularity_dic.keys())
    # 我分为：推荐列表的流行度 & 命中列表的流行度
    total_reco_popularity = 0
    total_hit_popularity = 0
    # 对每个repo计算指标
    for index, nameWithOwner in enumerate(payload_repo_set):
        print('-' * 100)
        print("index: " + str(index + 1) + "/" + str(len(payload_repo_set)) + ", repo: " + str(nameWithOwner))
        init_train_package_set = set([dic['key'] for dic in init_train_test_dic[nameWithOwner]['train']['package']])
        end_train_dic = end_train_test_dic[nameWithOwner]['train']
        end_train_package_set = set([dic['key'] for dic in end_train_dic['package']])
        # 找出每个repo的 train_set test_set reco_set hit_set
        init_test_package_set = set(init_train_test_dic[nameWithOwner]['test']['package'])
        end_test_package_set = set(end_train_test_dic[nameWithOwner]['test']['package'])
        # 截取 end_reco_package_list 的 cal_topN，python列表截取是如果不足则全部取出，符合需求
        end_reco_package_list = end_reco_result_json_dic[nameWithOwner]['package'][0: cal_kwargs.get('cal_topN')]
        if len(end_reco_package_list) == 0:
            print('no end_reco_package_list: ' + nameWithOwner)
        end_reco_package_set = set(end_reco_package_list)
        # 纯基于内容推荐 add_train_package_set 始终为空
        add_train_package_set = end_train_package_set - init_train_package_set
        # 整体计算 all_reco_package_set 算精确率的核心是
        # 1、把【全部推荐列表】定义为 【每次迭代新加入的】 | 【最后一次的推荐列表】
        # 2、把【全部命中列表】定义为 【全部推荐列表】 在 【初始测试集】 中的命中列表，这样随着迭代命中列表是不会减少的
        # 在用户选择GroundTruth的场景下 add_train_package_set肯定hit，自动选择则不一定hit，但 all_reco_package_set 的算法不变
        all_reco_package_set = add_train_package_set | end_reco_package_set
        all_hit_package_set = all_reco_package_set & init_test_package_set
        end_hit_package_set = end_reco_package_set & end_test_package_set
        # 更新命中HR分子
        if len(all_hit_package_set) != 0:
            total_hit_repo_count += 1
        print('init_train_package_set(' + str(len(init_train_package_set)) + '): ' + str(init_train_package_set))
        print('end_train_package_set(' + str(len(end_train_package_set)) + '): ' + str(end_train_package_set))
        print('init_test_package_set(' + str(len(init_test_package_set)) + '): ' + str(init_test_package_set))
        print('end_test_package_set(' + str(len(end_test_package_set)) + '): ' + str(end_test_package_set))
        print('end_reco_package_list(' + str(len(end_reco_package_list)) + '): ' + str(end_reco_package_list))
        print('all_reco_package_set(' + str(len(all_reco_package_set)) + '): ' + str(all_reco_package_set))
        print('end_hit_package_set(' + str(len(end_hit_package_set)) + '): ' + str(end_hit_package_set))
        print('all_hit_package_set(' + str(len(all_hit_package_set)) + '): ' + str(all_hit_package_set))
        print('all_single_precision: ' + str(len(all_hit_package_set)) + '/' + str(len(all_reco_package_set)))
        print('all_single_recall: ' + str(len(all_hit_package_set)) + '/' + str(len(init_test_package_set)))
        total_TP += len(all_hit_package_set)
        total_Ru += len(all_reco_package_set)
        total_Tu += len(init_test_package_set)
        end_total_TP += len(end_hit_package_set)
        end_total_Ru += len(end_reco_package_list)
        end_total_Tu += len(end_test_package_set)
        # 非热门召回率，指定 cal_topN，不算召回中 top [0, 50] 热门的 TP Tu Ru
        for i in range(0, 55, 5):
            top_i_popular_package_set = set(top_popular_package_list[:i])
            exclude_popular_TP['top' + str(i)] += len(all_hit_package_set - top_i_popular_package_set)
            exclude_popular_Tu['top' + str(i)] += len(init_test_package_set - top_i_popular_package_set)
            exclude_popular_Ru['top' + str(i)] += len(all_reco_package_set - top_i_popular_package_set)
        # 指定 不算 top_not_popular_N 热门的， top[0, 50]的 TP Tu Ru
        # 更新推荐列表覆盖分布
        for reco_package in all_reco_package_set:
            total_reco_package_appear_count_dic[reco_package] += 1
        # 更新命中列表覆盖分布
        for reco_package in all_hit_package_set:
            total_hit_package_appear_count_dic[reco_package] += 1
        # 更新推荐列表的 流行 贡献
        for reco_package in all_reco_package_set:
            total_reco_popularity += total_package_popularity_dic[reco_package]
        # 更新命中列表的 流行 贡献
        for hit_package in all_hit_package_set:
            total_hit_popularity += total_package_popularity_dic[hit_package]
    # 计算指标
    # （固定topN，从0~50非热门）召回率Recall、精确率Precision、F值
    avg_recall = total_TP / total_Tu
    avg_precision = total_TP / total_Ru
    avg_end_recall = end_total_TP / end_total_Tu
    avg_end_precision = end_total_TP / end_total_Ru
    avg_exclude_popular_recall_dic = OrderedDict()
    avg_exclude_popular_precision_dic = OrderedDict()
    avg_exclude_popular_F_dic = OrderedDict()
    for i in range(0, 55, 5):
        avg_exclude_popular_recall_dic[str(i)] = exclude_popular_TP['top' + str(i)] / exclude_popular_Tu['top' + str(i)]
        if exclude_popular_Ru['top' + str(i)] != 0:
            avg_exclude_popular_precision_dic[str(i)] = exclude_popular_TP['top' + str(i)] / exclude_popular_Ru['top' + str(i)]
        else:
            avg_exclude_popular_precision_dic[str(i)] = 0
    for f in [0.5, 1, 2]:
        avg_exclude_popular_F_dic['F_' + str(f)] = OrderedDict()
        for i in range(0, 55, 5):
            avg_recall_i = avg_exclude_popular_recall_dic[str(i)]
            avg_precision_i = avg_exclude_popular_precision_dic[str(i)]
            if avg_recall_i != 0:
                avg_exclude_popular_F_dic['F_' + str(f)][str(i)] = (1 + f ** 2) * avg_precision_i * avg_recall_i / ((f ** 2) * avg_precision_i + avg_recall_i)
            else:
                avg_exclude_popular_F_dic['F_' + str(f)][str(i)] = 0
    avg_F1 = avg_exclude_popular_F_dic['F_1']['0']
    # HR命中率
    HR = total_hit_repo_count / len(payload_repo_set)
    # 推荐列表覆盖率
    total_reco_package_set = total_reco_package_appear_count_dic.keys()
    # 检验覆盖（不可能推荐出在模型集中没有关系的package）
    total_model_package_set = total_package_popularity_dic.keys()
    not_in_train_but_in_model_package_set = total_reco_package_set - total_model_package_set
    if len(not_in_train_but_in_model_package_set) != 0:
        print('not_in_train_but_in_model_package_set: ' + str(not_in_train_but_in_model_package_set))
        exit(0)
    # 简单覆盖率
    simple_coverage = len(total_reco_package_set) / len(total_model_package_set)
    # 推荐列表信息熵
    reco_package_appear_count_sum = sum(total_reco_package_appear_count_dic.values())
    reco_information_entropy = 0
    for package, appear_count in total_reco_package_appear_count_dic.items():
        pi = appear_count / reco_package_appear_count_sum
        reco_information_entropy += - pi * math.log(pi)
    # 命中列表信息熵
    hit_package_appear_count_sum = sum(total_hit_package_appear_count_dic.values())
    hit_information_entropy = 0
    for package, appear_count in total_hit_package_appear_count_dic.items():
        pi = appear_count / hit_package_appear_count_sum
        hit_information_entropy += - pi * math.log(pi)
    # 基尼系数
    # 把分布展开，即出现多次的按多个物品算
    reco_popularity_distribution_dic = {}
    for package, appear_count in total_reco_package_appear_count_dic.items():
        for i in range(0, appear_count):
            if package in reco_popularity_distribution_dic.keys():
                new_package_name = package + '_' + str(uuid1())
            else:
                new_package_name = package
            reco_popularity_distribution_dic[new_package_name] = total_package_popularity_dic[package]
    gini_index = gini(np.array(list(reco_popularity_distribution_dic.values()), dtype=np.float))
    # 推荐列表的流行度 & 命中列表的流行度
    avg_reco_popularity = total_reco_popularity / total_Ru
    avg_hit_popularity = total_hit_popularity / total_TP
    print('-' * 50 + 'info' + '-' * 50)
    print("\nnot in model_set but in reco_set: " + str(not_in_train_but_in_model_package_set))
    print('\ntotal_TP:' + str(total_TP))
    print('\ntotal_Ru:' + str(total_Ru))
    print('\ntotal_Tu:' + str(total_Tu))
    print('\nfull_avg_recall_upper_bound: ' + str(len(payload_repo_set) * cal_kwargs.get('cal_topN') / total_Tu))
    print('\navg_recall_upper_bound: ' + str(total_Ru / total_Tu))
    print('\ntotal_package_avg_popularity: ' + str(total_package_avg_popularity))
    print('-' * 50 + 'avg_index' + '-' * 50)
    print('\navg_recall: ' + str(avg_recall))
    print('\navg_precision: ' + str(avg_precision))
    print('\navg_F1: ' + str(avg_F1))
    print('\navg_end_recall: ' + str(avg_end_recall))
    print('\navg_end_precision: ' + str(avg_end_precision))
    print('\navg_exclude_popular_recall_dic: ' + str(avg_exclude_popular_recall_dic))
    print('\navg_exclude_popular_precision_dic: ' + str(avg_exclude_popular_precision_dic))
    print('\navg_exclude_popular_F_dic: ' + str(avg_exclude_popular_F_dic))
    print('\nHR: ' + str(HR))
    print('\nreco_information_entropy: ' + str(reco_information_entropy))
    print('\nhit_information_entropy: ' + str(hit_information_entropy))
    print('\nsimple_coverage: ' + str(simple_coverage))
    print('\ngini_index: ' + str(gini_index))
    print('\navg_reco_popularity: ' + str(avg_reco_popularity))
    print('\navg_hit_popularity: ' + str(avg_hit_popularity))
    print('--------------------------------------')
    # dump csv
    if not os.path.exists(cal_kwargs.get('cal_out_dir')):
        os.mkdir(cal_kwargs.get('cal_out_dir'))
    # dump not_popular指标
    exclude_popular_cal_index_dic = {'recall': avg_exclude_popular_recall_dic, 'precision': avg_exclude_popular_precision_dic,
                                     'F1': avg_exclude_popular_F_dic['F_1']}
    if cal_kwargs.get('dump_exclude_popular_index'):
        dump_exclude_popular_cal_index_csv(exclude_popular_cal_index_dic, cal_kwargs)
    # 指标-流行度
    # dump_index_popularity_csv(index_popularity_dic_list, cal_out_dir, lt_avg_popularity=True)
    # 返回cal_topN的重要指标
    cal_topN_index = {'recall': avg_recall,
                      'precision': avg_precision,
                      'F1': avg_F1,
                      'HR': HR,
                      'reco_info_entropy': reco_information_entropy,
                      'hit_info_entropy': hit_information_entropy,
                      'simple_coverage': simple_coverage,
                      'gini_index': gini_index,
                      'avg_reco_popularity': avg_reco_popularity,
                      'avg_hit_popularity': avg_hit_popularity
                      }
    return cal_topN_index


def dump_exclude_popular_cal_index_csv(not_popular_index_dic, cal_kwargs):
    # df_not_popular_recall = pd.DataFrame.from_dict(avg_top_not_popular_recall_dic, orient='index')
    # df_not_popular_precision = pd.DataFrame.from_dict(avg_top_not_popular_precision_dic, orient='index')
    # df_not_popular_F1 = pd.DataFrame.from_dict(avg_top_not_popular_F_dic['F_1'], orient='index')
    # df_not_popular_recall.to_csv(cal_kwargs.get('cal_out_dir') + '/' + 'not_popular_recall.csv', sep=',')
    # df_not_popular_precision.to_csv(cal_kwargs.get('cal_out_dir') + '/' + 'not_popular_precision.csv', sep=',')
    # df_not_popular_F1.to_csv(cal_kwargs.get('cal_out_dir') + '/' + 'not_popular_F1.csv', sep=',')
    # dic_list
    recall_dic_list = []
    i = 0
    for key, val in not_popular_index_dic['recall'].items():
        if i < 2:
            i += 1
            continue
        recall_dic_list.append({'method': cal_kwargs.get('method'), 'topK': key, 'value': val})
    precision_dic_list = []
    for key, val in not_popular_index_dic['precision'].items():
        precision_dic_list.append({'method': cal_kwargs.get('method'), 'topK': key, 'value': val})
    F1_dic_list = []
    for key, val in not_popular_index_dic['precision'].items():
        F1_dic_list.append({'method': cal_kwargs.get('method'), 'topK': key, 'value': val})
    pd.DataFrame(recall_dic_list).to_csv(os.path.join(cal_kwargs.get('not_popular_index_result_dir'), 'not_popular_recall_result.csv'), mode='a', header=False,
                                         index=False)
    # pd.DataFrame(precision_dic_list).to_csv(os.path.join(cal_kwargs.get('not_popular_index_result_dir'), 'not_popular_precision_result.csv'), mode='a',
    #                                         header=False,
    #                                         index=False)
    # pd.DataFrame(F1_dic_list).to_csv(os.path.join(cal_kwargs.get('not_popular_index_result_dir'), 'not_popular_F1_result.csv'), mode='a', header=False,
    #                                  index=False)


def dump_index_popularity_csv(index_popularity_dic_list, cal_out_dir, lt_avg_popularity):
    payload_list = []
    for dic in index_popularity_dic_list:
        if dic['single_avg_popularity'] <= 1:
            continue
        if lt_avg_popularity is True and dic['single_avg_popularity'] > 3.1398058252427186:
            continue
        payload_list.append(dic)
    for dic in payload_list:
        precision = dic['single_precision']
        recall = dic['single_recall']
        if precision != 0:
            dic['single_F1'] = 2 * precision * recall / (precision + recall)
        else:
            dic['single_F1'] = 0
    df_index_popularity = pd.DataFrame(payload_list)
    df_index_popularity.to_csv(cal_out_dir + '/' + 'index_popularity_' + str(uuid1()) + '.csv', index=False)
    pass


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


def get_package_popularity_dic(package_degree_csv, normalize=True):
    df = pd.read_csv(package_degree_csv)
    row_list = df.iloc[:, 0:2].values.tolist()
    package_popularity_dic = {}
    for row in row_list:
        package_popularity_dic[row[0]] = row[1]
    if not normalize:
        return package_popularity_dic
    else:
        # 流行度归一化
        max_popularity = max(package_popularity_dic.values())
        for key, val in package_popularity_dic.items():
            package_popularity_dic[key] = val / max_popularity
        return package_popularity_dic
