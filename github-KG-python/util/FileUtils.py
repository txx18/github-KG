import os
import json


def writeFile(file_dir_path, file_path, data):
    """
    python to file
    :param file_dir_path:
    :param file_path:
    :param data:
    :return:
    """
    if not os.path.exists(file_dir_path):
        os.makedirs(file_dir_path)
    with open(file_path, 'w', encoding="utf-8") as outfile:
        # json.dump(data, outfile)
        outfile.write(data)


def writeFileAppend(file_dir_path, file, data):
    """
    python to file append
    :param file_dir_path:
    :param file:
    :param data:
    :return:
    """
    if not os.path.exists(file_dir_path):
        os.makedirs(file_dir_path)
    with open(file, 'a', encoding="utf-8") as outfile:
        # json.dump(data, outfile)
        outfile.write(data)


def write_json_file(file_dir_path, file_path, data):
    """
    python to json file
    :param file_dir_path:
    :param file_path:
    :param data:
    :return:
    """
    if not os.path.exists(file_dir_path):
        os.makedirs(file_dir_path)
    with open(file_path, 'w', encoding='utf-8') as outfile:
        json.dump(data, outfile, ensure_ascii=False)
        # outfile.write(data)


def read_json_file(file_path):
    """
    json file to python
    :param file_path:
    :return:
    """
    with open(file_path, 'r', encoding="utf-8") as f:
        return json.load(f)
