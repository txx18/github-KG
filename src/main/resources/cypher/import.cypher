//CREATE database kgschema;

MATCH (:Repo)-[r]-(:Repo)
DELETE r;

MATCH (:Repo)-[r]-(:Owner)
DELETE r;

MATCH (:Repo)-[r]-(:Language)
DELETE r;

MATCH (:Repo)-[r]-(:Topic)
DELETE r;

MERGE (repo:Repo {nameWithOwner: 'tensorflow/tensorflow'})
  ON CREATE SET repo.createdAt = '2015-11-07T01:19:20Z'
MERGE (owner:Owner {login: 'tensorflow'})
MERGE (repo)-[:BELONGS_TO]-(owner)
MERGE (lang:Language {name: 'Python'})
MERGE (repo)-[:USES]->(lang)
MERGE (topic:Topic {name: 'deep-learning'})
MERGE (repo2:Repo {nameWithOwner: 'keras-team/keras-applicationsl'})
MERGE (repo)-[:UNDER]-(topic)
MERGE (repo)-[:DEPENDS_ON]-(repo2)
MERGE (package:Package {packageName: 'keras-applications'})
MERGE (repo)-[:DEPENDS_ON]-(package)
MERGE (repo2)-[:DEVELOPS]-(package)
;

/*// 来一个repo，插入/更新流程
// 判断repo是否已经存在
MATCH (repo{nameWithOwner: 'tensorflow/tensorflow'})
RETURN repo
// 如果该节点不存在，创建节点

// 如果该节点存在，删除所有出关系
MATCH ({nameWithOwner: value.nameWithOwner})-[repo_out_relation]->()
DELETE repo_out_relation*/



// 创建、增量更新
WITH
  'file:///C:/Disk_Data/Small_Data/Neo4j/PaddlePaddle-$-Paddle.json' AS url
CALL apoc.load.json(url, '$.data.repository') YIELD value
//return size(value.languages.nodes), value.languages.nodes[10].name, value.languages.edges[10].size
// repo的标量属性
MERGE (repo:Repo {nameWithOwner: value.nameWithOwner})
// 仅第一次创建时保存创建时间
  ON CREATE SET repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
// （这个SET不属于上一个MERGE）已存在repo，比如更新数据时，则需要重新设置其属性
SET repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT'),
repo.createdAt = value.createdAt,
repo.description = value.description,
repo.forkCount = value.forkCount,
repo.homepageUrl = value.homepageUrl,
repo.isDisabled = value.isDisabled,
repo.isEmpty = value.isEmpty,
repo.isFork = value.isFork,
repo.isInOrganization = value.isInOrganization,
repo.isLocked = value.isLocked,
repo.isMirror = value.isMirror,
repo.isPrivate = value.isPrivate,
repo.isTemplate = value.isTemplate,
repo.issueCount = value.issues.totalCount,
repo.isUserConfigurationRepository = value.isUserConfigurationRepository,
repo.licenseInfoName = value.licenseInfo.name,
repo.name = value.name,
repo.primaryLanguageName = value.primaryLanguage.name,
repo.pullRequestCount = value.pullRequests.totalCount,
repo.pushedAt = value.pushedAt,
repo.stargazerCount = value.stargazerCount,
repo.updatedAt = value.updatedAt,
repo.url = value.url
// 与owner的关系
MERGE (owner:Owner {login: value.owner.login})
  ON CREATE SET owner.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT'),
  owner.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (repo)-[belong:BELONGS_TO]->(owner)
  ON CREATE SET belong.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET belong.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
// 与topic的关系
// value.repositoryTopics.nodes是1row的[{}]，unwind之后的nodes是多rows的{}
FOREACH (node IN value.repositoryTopics.nodes |
  MERGE (topic:Topic {name: node.topic.name})
    ON CREATE SET topic.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT'),
    topic.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
  MERGE (repo)-[under:UNDER]->(topic)
    ON CREATE SET under.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
  SET under.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
)
// repo与package的关系 repo与repo的关系 关系
FOREACH (manifest_node IN value.dependencyGraphManifests.nodes |
  FOREACH (dependency_node IN manifest_node.dependencies.nodes |
  // repo与package的DEPENDS_ON关系
    MERGE(package:Package {packageName: dependency_node.packageManager + '/' + dependency_node.packageName})
      ON CREATE SET package.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT'),
      package.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
    MERGE (repo)-[depends_package:DEPENDS_ON]->(package)
      ON CREATE SET depends_package.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
    SET depends_package.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT'),
    depends_package.requirements = dependency_node.requirements
  // todo case 想实现的是如果依赖的repo为空，则不添加这个dst_repo节点
  // repo与repo的DEPENDS_ON关系，dst_repo作为repo的附属品不作为主要的repo更新手段，由于可能存在自己依赖自己的情况，所以属性基本是ON CREATE SET
    MERGE
      (dst_repo:Repo {nameWithOwner: CASE WHEN exists(dependency_node.repository.nameWithOwner) THEN dependency_node.repository.nameWithOwner
                                       ELSE 'unknown-$-unknown'
                                       END})
      ON CREATE SET dst_repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT'),
      dst_repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
    MERGE (repo)-[depends_repo:DEPENDS_ON]->(dst_repo)
      ON CREATE SET depends_repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
    SET depends_repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
  // repo与package的DEVELOPS关系
    MERGE (dst_repo)-[develops:DEVELOPS]->(package)
      ON CREATE SET develops.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
    SET develops.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
  )
)
// 与language的关系（同时遍历两个list好像插件不识别）
FOREACH (i IN range(0, size(value.languages.nodes) - 1) |
MERGE (lang:Language {name:value.languages.nodes[i].name})
ON CREATE SET lang.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT'),
lang.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (repo)- [uses:USES {size:value.languages.edges[i].size}]- >(lang)
ON CREATE SET uses.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET uses.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
)








