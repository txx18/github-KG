# github-KG
github知识图谱——硕士课题项目
当前基于Neo4j图数据库，主要关注软件依赖关系

## github-KG-python
目前爬虫存到文件以及简单数据处理使用python
### 使用爬虫模块初始配置
请先在github-KG-python目录下新建config/token_list.txt
内部每行放置一个github的private token，格式如:
36c19c77004780355347f0f1d8381a1a58XXXXXX<br>
9322fbadda497df4d426a472bfca4552a2XXXXXX<br>
46990787815998b1ed271244bd708e499aXXXXXX<br>

## Spring Boot
未来考虑实时更新，已经实现了http请求解析文件批量插入节点