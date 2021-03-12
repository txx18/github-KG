import random

import pandas as pd


class CF(object):

    def read_data(self, file_path):
        data = pd.read_csv(file_path)
        return data

    def split_data(self, data, M, k, seed):
        test = []
        train = []
        random.seed(seed)
        for user, item in data:
            if random.randint(0, M) == k:
                test.append([user, item])
            else:
                train.append([user, item])
        return train, test

    def UserSimilarity(self, train):
        item_users = {}
        for u, items in train.items():
            pass
