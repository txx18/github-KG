import json
import os
import shutil
import traceback


def move_file(src_dir, dst_dir, file):
    try:
        # cmd = 'chmod -R +x ' + src_path
        # os.popen(cmd)
        f_src = os.path.join(src_dir, file)
        f_dst = os.path.join(dst_dir, file)
        shutil.move(f_src, f_dst)
    except Exception as e:
        print('move_file ERROR: ', e)
        traceback.print_exc()


def read_file_lines(file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        return f.readlines()


def write_file_line_append(file_dir_path, file_path, data):
    if not os.path.exists(file_dir_path):
        os.makedirs(file_dir_path)
    with open(file_path, 'a', encoding='utf-8') as f:
        f.write("\n" + data)


def read_file(file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        return f.read()


def write_file(file_dir_path, file_path, data):
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
        outfile.write(data)


def write_file_append(file_dir_path, file, data):
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


def read_json_file(file_path):
    """
    json file to python
    :param file_path:
    :return:
    """
    with open(file_path, 'r', encoding="utf-8") as f:
        return json.load(f)


def read_json_file_str(file_path):
    with open(file_path, 'r', encoding="utf-8") as f:
        return json.dumps(json.load(f))
