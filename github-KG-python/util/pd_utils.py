import operator
from functools import reduce


def get_df_inst(df, **kwargs):
    cond = pop_none_value(kwargs)
    if cond:
        cond = reduce(
            operator.and_, [
                df[k].isin(v) if isinstance(v, list) else df[k] == v
                for k, v in cond.items()
            ])
        return df.loc[cond]
    else:
        return df.loc[:]


def pop_none_value(kwargs):
    for k, v in kwargs.items():
        if v is None:
            kwargs.pop(k)
    return kwargs


def get_one_col_set(df, col_name):
    return set(df[col_name])


def trans_df_two_col_to_dic_many_val(df, col_name_key, col_name_val):
    """
    多个值转化为去重后的list
    """
    return df.groupby(col_name_key)[col_name_val].apply(set).apply(list).to_dict()
