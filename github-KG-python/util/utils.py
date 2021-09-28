import os
import random
from collections import defaultdict


def get_fileName_no_ext(file_path):
    (path, filename) = os.path.split(file_path)
    return os.path.splitext(filename)[0]


def get_fileName_with_ext(file_path):
    (path, filename) = os.path.split(file_path)
    return filename


def split_list_by_sub_count(raw_list, count, seed):
    """
    raw_list保证有序的情况下，则能保证每次得的train_list, test_list的元素是相同的，但是它们内部顺序是不保证的
    :param raw_list:
    :param count:
    :param seed:
    :return: train_list, test_list
    """
    if len(raw_list) <= count:
        return raw_list, []
    random.seed(seed)
    train_list = random.sample(raw_list, count)
    test_list = list(set(raw_list) - set(train_list))
    return train_list, test_list


def split_data_list(raw_lst, split_M, test_ks, seed):
    """
    按照大致比例划分，余数归入训练集
    :param raw_lst: 要想保证每次输出都是相同，输入要自己保证是固定的顺序；此函数只保证划分list，如果要去重要自己输入list(set)
    :param split_M:
    :param test_ks:
    :param seed:
    :return: 不转为set，
    """

    N = len(raw_lst)
    index_lst = [i for i in range(0, N)]
    index_dic = defaultdict(list)
    offset = int(N / split_M)
    for i in range(0, split_M):
        random.seed(seed)
        # 从lst中采样offset个元素
        index_dic['set_' + str(i)] = random.sample(index_lst, offset)
        # for repo in dic['set_' + str(i)]:
        #     lst.remove(repo)
        index_lst = list(set(index_lst) - set(index_dic['set_' + str(i)]))
    # index_lst还有剩余（余数），依次加入前几个set_
    for i in range(len(index_lst)):
        index_dic['set_' + str(i)].append(index_lst[i])
    item_dic = defaultdict(list)
    for i in range(0, split_M):
        for index in index_dic['set_' + str(i)]:
            item_dic[i].append(raw_lst[index])
    train_list = []
    test_list = []
    # 从test_ks选其中k份作为测试，其余训练模型
    for key, val in item_dic.items():
        if key not in test_ks:
            train_list.extend(val)
        else:
            test_list.extend(val)
    return train_list, test_list

