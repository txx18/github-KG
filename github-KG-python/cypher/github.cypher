// Graph
// Graph_MetaPath折叠元路径 主要用于cosine系列
// 【直接package】每个推荐器找到自己的KNN Repo, 分别推荐package
// language
OPTIONAL MATCH(repo_i:Repository {nameWithOwner: $nameWithOwner})-[r_i:REPO_USES_LANGUAGE]->(language:Language)<-[r_j:REPO_USES_LANGUAGE]-(repo_j:Repository)
WITH repo_j, sum(1.0 * r_i.TfIdf * r_j.TfIdf / sqrt(repo_i.languageTfIdfQuadraticSum * repo_j.languageTfIdfQuadraticSum)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
OPTIONAL MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE (NOT package_j.nameWithManager IN $dependencyNameList)
WITH package_j.nameWithManager AS package_j_nameWithManager, sum(1.0 * score) AS score
WITH (collect({nameWithManager: package_j_nameWithManager, score: score})) AS rows
// Paper
OPTIONAL MATCH (repo_i:Repository {nameWithOwner: $nameWithOwner})<-[r_i:PAPER_IMPLEMENTED_BY_REPO]-(paper:Paper)-[r_j:PAPER_IMPLEMENTED_BY_REPO]->
(repo_j:Repository)
WITH rows, repo_j, sum(1.0 * r_i.TfIdf * r_j.TfIdf / sqrt(repo_i.paperTfIdfQuadraticSum * repo_j.paperTfIdfQuadraticSum)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
OPTIONAL MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE (NOT package_j.nameWithManager IN $dependencyNameList)
WITH rows, package_j.nameWithManager AS package_j_nameWithManager, sum(1.0 * score) AS score
WITH (rows + collect({nameWithManager: package_j_nameWithManager, score: score})) AS rows
// Task
OPTIONAL MATCH (repo_i:Repository {nameWithOwner: $nameWithOwner})-[r_i:REPO_IMPLEMENTS_PAPER_UNDER_TASK]->(task:Task)
                 <-[r_j:REPO_IMPLEMENTS_PAPER_UNDER_TASK]-(repo_j:Repository)
WITH rows, repo_j, sum(1.0 * r_i.TfIdf * r_j.TfIdf / sqrt(repo_i.taskTfIdfQuadraticSum * repo_j.taskTfIdfQuadraticSum)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
OPTIONAL MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE (NOT package_j.nameWithManager IN $dependencyNameList)
WITH rows, package_j.nameWithManager AS package_j_nameWithManager, sum(1.0 * score) AS score
WITH (rows + collect({nameWithManager: package_j_nameWithManager, score: score})) AS rows
// Method
OPTIONAL MATCH (repo_i:Repository {nameWithOwner: $nameWithOwner})-[r_i:REPO_IMPLEMENTS_PAPER_USES_METHOD]->(method:Method)
                 <-[r_j:REPO_IMPLEMENTS_PAPER_USES_METHOD]-(repo_j:Repository)
WITH rows, repo_j, sum(1.0 * r_i.TfIdf * r_j.TfIdf / sqrt(repo_i.methodTfIdfQuadraticSum * repo_j.methodTfIdfQuadraticSum)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
OPTIONAL MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE (NOT package_j.nameWithManager IN $dependencyNameList)
WITH rows, package_j.nameWithManager AS package_j_nameWithManager, sum(1.0 * score) AS score
WITH (rows + collect({nameWithManager: package_j_nameWithManager, score: score})) AS rows
// Dataset
OPTIONAL MATCH (repo_i:Repository {nameWithOwner: $nameWithOwner})-[r_i:REPO_IMPLEMENTS_MODEL_ON_DATASET]->(dataset:Dataset)
                 <-[r_j:REPO_IMPLEMENTS_MODEL_ON_DATASET]-(repo_j:Repository)
WITH rows, repo_j, sum(1.0 * r_i.TfIdf * r_j.TfIdf / sqrt(repo_i.datasetTfIdfQuadraticSum * repo_j.datasetTfIdfQuadraticSum)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
OPTIONAL MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE (NOT package_j.nameWithManager IN $dependencyNameList)
WITH rows, package_j.nameWithManager AS package_j_nameWithManager, sum(1.0 * score) AS score
WITH (rows + collect({nameWithManager: package_j_nameWithManager, score: score})) AS rows
// 汇总
UNWIND rows AS row
WITH row.nameWithManager AS recommend, sum(row.score) AS score
  WHERE recommend IS NOT NULL
RETURN recommend, score
  ORDER BY score DESC, recommend DESC
  LIMIT $topN
// Package
/*OPTIONAL MATCH(repo_i:Repository {nameWithOwner: $nameWithOwner})-[:REPO_DEPENDS_ON_PACKAGE]->(:Package)
                <-[:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)
WITH rows, repo_j, sum(1.0 / sqrt(repo_i.packageDegree * repo_j.packageDegree)) AS score*/
UNWIND $dependencyMapList AS map
MATCH (package_i:Package {nameWithManager: map.nameWithManager})<-[r_j:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)
WITH rows, repo_j, sum(1.0 * map.packageTfIdf * r_j.TfIdf / sqrt($packageTfIdfQuadraticSum * repo_j.packageTfIdfQuadraticSum)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
OPTIONAL MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
WITH rows, package_j.nameWithManager AS package_j_nameWithManager, sum(1.0 * score) AS score
WITH (rows + collect({nameWithManager: package_j_nameWithManager, score: score})) AS rows
// 汇总
UNWIND rows AS row
WITH row.nameWithManager AS recommend, sum(row.score) AS score
  WHERE recommend IS NOT NULL
RETURN recommend, score
  ORDER BY score DESC, recommend DESC
  LIMIT $topN

// UCF_cosine_Pac
// 1 不在图中的新用户写法 UCF_cosine_Package_userNotInGraph
UNWIND $dependencyMapList AS map
MATCH (package_i:Package {nameWithManager: map.nameWithManager})<-[r_i:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)
WITH repo_j, sum(1.0 * map.packageTfIdf * r_i.TfIdf / (sqrt($packageTfIdfQuadraticSum * repo_j.packageTfIdfQuadraticSum))) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[r_j:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score
  ORDER BY score DESC, package_j.nameWithManager DESC
  LIMIT $topN
// 2 图中用户写法 UCF_cosine_Package_userInGraph
MATCH(repo_i:Repository {nameWithOwner: $nameWithOwner})-[:REPO_DEPENDS_ON_PACKAGE]->(package_i:Package)
       <-[:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)
WITH repo_j, sum(1.0 * package_i.repoIDF ^ 2 / (sqrt(repo_i.packageRepoIDFQuadraticSum * repo_j.packageRepoIDFQuadraticSum))) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score
  ORDER BY score DESC, package_j.nameWithManager DESC
  LIMIT $topN
// Jaccard
UNWIND $dependencyMapList AS map
MATCH (package_i:Package {nameWithManager: map.nameWithManager})<-[r_i:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)
WITH repo_j, sum(1.0 / (repo_j.packageDegree)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[r_j:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score
  ORDER BY score DESC, package_j.nameWithManager DESC
  LIMIT $topN
// UCF_cosine_Language
MATCH(repo_i:Repository {nameWithOwner: $nameWithOwner})-[:REPO_USES_LANGUAGE]->(language:Language)
       <-[:REPO_USES_LANGUAGE]-(repo_j:Repository)
WITH repo_j, sum(1.0 * language.repoIDF ^ 2 / (sqrt(repo_i.languageRepoIDFQuadraticSum * repo_j.languageRepoIDFQuadraticSum))) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score
  ORDER BY score DESC, package_j.nameWithManager DESC
  LIMIT $topN
// UCF_cosine_Paper
MATCH (repo_i:Repository {nameWithOwner: $nameWithOwner})<-[:PAPER_IMPLEMENTED_BY_REPO]-(paper:Paper)
        -[:PAPER_IMPLEMENTED_BY_REPO]->(repo_j:Repository)
WITH repo_j, sum(1.0 * paper.repoIDF ^ 2 / sqrt(repo_i.paperRepoIDFQuadraticSum * repo_j.paperRepoIDFQuadraticSum)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score
  ORDER BY score DESC, package_j.nameWithManager DESC
  LIMIT $topN
// UCF_cosine_Task
MATCH (repo_i:Repository {nameWithOwner: $nameWithOwner})-[:REPO_IMPLEMENTS_PAPER_UNDER_TASK]->(task:Task)
        <-[:REPO_IMPLEMENTS_PAPER_UNDER_TASK]-(repo_j:Repository)
WITH repo_j, sum(1.0 * task.repoIDF ^ 2 / sqrt(repo_i.taskRepoIDFQuadraticSum * repo_j.taskRepoIDFQuadraticSum)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score
  ORDER BY score DESC, package_j.nameWithManager DESC
  LIMIT $topN
// UCF_cosine_Method
MATCH (repo_i:Repository {nameWithOwner: $nameWithOwner})-[:REPO_IMPLEMENTS_PAPER_USES_METHOD]->(method:Method)
        <-[:REPO_IMPLEMENTS_PAPER_USES_METHOD]-(repo_j:Repository)
WITH repo_j, sum(1.0 * method.repoIDF ^ 2 / sqrt(repo_i.methodRepoIDFQuadraticSum * repo_j.methodRepoIDFQuadraticSum)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score
  ORDER BY score DESC, package_j.nameWithManager DESC
  LIMIT $topN
// UCF_cosine_Dataset
MATCH (repo_i:Repository {nameWithOwner: $nameWithOwner})-[:REPO_IMPLEMENTS_MODEL_ON_DATASET]->(dataset:Dataset)
        <-[:REPO_IMPLEMENTS_MODEL_ON_DATASET]-(repo_j:Repository)
WITH repo_j, sum(1.0 * dataset.repoIDF ^ 2 / sqrt(repo_i.datasetRepoIDFQuadraticSum * repo_j.datasetRepoIDFQuadraticSum)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score
  ORDER BY score DESC, package_j.nameWithManager DESC
  LIMIT $topN
// UCF_PathSim_Pac
UNWIND $dependencyMapList AS map
MATCH (package_i:Package {nameWithManager: map.nameWithManager})<-[:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)
WITH repo_j, sum(1.0 * 2 / (size($dependencyNameList) + repo_j.packageDegree)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score
  ORDER BY score DESC, package_j.nameWithManager DESC
  LIMIT $topN
// UCF_IIF_cosine_Pac
UNWIND $dependencyMapList AS map
MATCH (package_i:Package {nameWithManager: map.nameWithManager})<-[:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)
WITH repo_j, sum(1.0 * (1 / log(1 + package_i.repoDegree)) / (sqrt(size($dependencyNameList) * repo_j.packageDegree)
)) AS score
  ORDER BY score DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score
  ORDER BY score DESC
  LIMIT $topN


// ICF_cosine_Pac_tfidf_Pac
// 不在图中的新用户写法
UNWIND $dependencyMapList AS map
MATCH (package_i:Package {nameWithManager: map.nameWithManager})<-[r_i:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)
        -[r_j:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend,
       sum(1.0 * r_i.TfIdf * r_j.TfIdf / sqrt(package_i.repoTfIdfQuadraticSum * package_j.repoTfIdfQuadraticSum)) AS score
  ORDER BY score DESC
  LIMIT $topN
// 图中用户写法
MATCH(repo_i:Repository {nameWithOwner: $nameWithOwner})-[:REPO_DEPENDS_ON_PACKAGE]->(package_i:Package)
       <-[:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend,
       sum(1.0 / sqrt(package_i.repoDegree * package_j.repoDegree)) AS score
  ORDER BY score DESC
  LIMIT $topN
// ICF_PathSim_Pac
UNWIND $dependencyMapList AS map
MATCH (package_i:Package {nameWithManager: map.nameWithManager})<-[:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)
        -[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend,
       sum(1.0 * 2 / (package_i.repoDegree + package_j.repoDegree)) AS score
  ORDER BY score DESC
  LIMIT $topN
// ICF_IUF_cosine_Pac
UNWIND $dependencyMapList AS map
MATCH (package_i:Package {nameWithManager: map.nameWithManager})<-[:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)
        -[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend,
       sum((1.0 * (1 / log(1 + repo_j.packageDegree))) / sqrt(package_i.repoDegree * package_j.repoDegree))
       AS score
  ORDER BY score DESC
  LIMIT $topN


// Popular 热门推荐
MATCH (package:Package)<-[:REPO_DEPENDS_ON_PACKAGE]-(repo:Repository)
  WHERE NOT package.nameWithManager IN $dependencyNameList
WITH package.nameWithManager AS nameWithManager, count(repo) AS repoDegree
RETURN nameWithManager AS recommend, repoDegree AS score
  ORDER BY repoDegree DESC
  LIMIT $topN

// 随机推荐
MATCH (package:Package)
  WHERE exists((package)<-[:REPO_DEPENDS_ON_PACKAGE]-(:Repository))
  AND NOT package.nameWithManager IN $dependencyNameList
RETURN DISTINCT package.nameWithManager AS recommend, rand() AS score
  ORDER BY score
  LIMIT $topN

// 删除依赖关系 Repository - REPO_DEPENDS_ON_PACKAGE -> Package
MATCH(:Repository {nameWithOwner: $nameWithOwner})
       -[r:REPO_DEPENDS_ON_PACKAGE]-(:Package {nameWithManager: $nameWithManager})
DELETE r

// 创建依赖关系 Repository - REPO_DEPENDS_ON_PACKAGE -> Package
MATCH (repo:Repository {nameWithOwner: $nameWithOwner})
MATCH (package:Package {nameWithManager: $nameWithManager})
MERGE (repo)-[depends_package:REPO_DEPENDS_ON_PACKAGE]->(package)
  ON CREATE SET depends_package.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET depends_package.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET depends_package.requirements = $requirements


// 更新repo的度数和idf值，且针对有依赖的（否则应该使用OPTIONAL MATCH）
MATCH (be_depended_package:Package)
  WHERE exists((be_depended_package)<-[:REPO_DEPENDS_ON_PACKAGE]-(:Repository))
WITH count(be_depended_package) AS be_depended_package_count
MATCH (repo:Repository {nameWithOwner: $nameWithOwner})-[depend:REPO_DEPENDS_ON_PACKAGE]->(package:Package)
WITH count(package) AS degree, repo, be_depended_package_count
SET repo.packageDegree = degree
SET repo.packageIDF = log(be_depended_package_count * 1.0 / (1 + degree))

// 更新package的度数和idf值，且针对有被依赖的（否则应该使用OPTIONAL MATCH）
MATCH (has_dependency_repo:Repository)
  WHERE exists((has_dependency_repo)-[:REPO_DEPENDS_ON_PACKAGE]->(:Package))
WITH count(has_dependency_repo) AS has_dependency_repo_count
MATCH (package:Package {nameWithManager: $nameWithManager})<-[depend:REPO_DEPENDS_ON_PACKAGE]-(repo:Repository)
WITH count(repo) AS degree, package, has_dependency_repo_count
SET package.repoDegree = degree
SET package.repoIDF = log(has_dependency_repo_count * 1.0 / (1 + degree))


// 遍历一个Repo的Package
MATCH (repo:Repository {nameWithOwner: $nameWithOwner})-[:REPO_DEPENDS_ON_PACKAGE]->(pack:Package)
RETURN pack.nameWithManager AS nameWithManager
  ORDER BY nameWithManager


// Repo - REPO_CO_PACKAGE_REPO - Repo
// 一个Packge可能被上万个repo依赖，握手握不动。。
MATCH (package:Package {nameWithManager: $nameWithManager})<-[:REPO_DEPENDS_ON_PACKAGE]-(repo:Repository)
WITH package, count(repo) AS repoDegree
WITH collect(repo.nameWithOwner) AS repoNames
UNWIND range(0, size(repoNames) - 1) AS i
UNWIND range(i + 1, size(repoNames) - 1) AS j
MATCH (repo1:Repository {nameWithOwner: repoNames[i]})
MATCH (repo2:Repository {nameWithOwner: repoNames[j]})
MERGE (repo1)-[co:REPO_CO_PACKAGE_REPO]-(repo2)
  ON CREATE SET co.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
  ON CREATE SET co.coPackageCount = 1
  ON MATCH SET co.coPackageCount = (co.coPackageCount + 1)
SET co.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// Package - PACKAGE_CO_OCCUR_PACKAGE - Package
// 第一次同现关系ON CREATE SET, 非第一次同现关系 ON MATCH SET
MATCH (repo:Repository {nameWithOwner: $nameWithOwner})-[:REPO_DEPENDS_ON_PACKAGE]->(pack:Package)
WITH collect(pack.nameWithManager) AS packageNames
UNWIND range(0, size(packageNames) - 1) AS i
UNWIND range(i + 1, size(packageNames) - 1)  AS  j
MATCH (pack1:Package {nameWithManager: packageNames[i]})
MATCH (pack2:Package {nameWithManager: packageNames[j]})
MERGE (pack1)-[co1:PACKAGE_CO_OCCUR_PACKAGE]-(pack2) // 注意这里不能带箭头，下次merge双向有一条就算有
  ON CREATE SET co1.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
  ON CREATE SET co1.coOccurrenceCount = 1
  ON MATCH SET co1.coOccurrenceCount = (co1.coOccurrenceCount + 1)
SET co1.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// Repository
MERGE (repo:Repository {nameWithOwner: $nameWithOwner})
  ON CREATE SET repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET repo.assignableUserTotalCount = $assignableUserTotalCount
SET repo.commitCommentTotalCount = $commitCommentTotalCount
SET repo.createdAt = $createdAt
SET repo.databaseId = $databaseId
SET repo.deleteBranchOnMerge = $deleteBranchOnMerge
SET repo.dependencyGraphManifestTotalCount = $dependencyGraphManifestTotalCount
SET repo.description = $description
SET repo.diskUsage = $diskUsage
SET repo.forkCount = $forkCount
SET repo.forkTotalCount = $forkTotalCount
SET repo.hasIssuesEnabled = $hasIssuesEnabled
SET repo.hasProjectsEnabled = $hasProjectsEnabled
SET repo.hasWikiEnabled = $hasWikiEnabled
SET repo.homepageUrl = $homepageUrl
SET repo.isArchived = $isArchived
SET repo.isBlankIssuesEnabled = $isBlankIssuesEnabled
SET repo.isDisabled = $isDisabled
SET repo.isEmpty = $isEmpty
SET repo.isFork = $isFork
SET repo.isInOrganization = $isInOrganization
SET repo.isLocked = $isLocked
SET repo.isMirror = $isMirror
SET repo.isPrivate = $isPrivate
SET repo.isSecurityPolicyEnabled = $isSecurityPolicyEnabled
SET repo.isTemplate = $isTemplate
SET repo.issueTotalCount = $issueTotalCount
SET repo.isUserConfigurationRepository = $isUserConfigurationRepository
SET repo.labelTotalCount = $labelTotalCount
SET repo.languageTotalCount = $languageTotalCount
SET repo.languageTotalSize = $languageTotalSize
SET repo.licenseInfoName = $licenseInfoName
SET repo.mentionableUserTotalCount = $mentionableUserTotalCount
SET repo.mergeCommitAllowed = $mergeCommitAllowed
SET repo.milestoneTotalCount = $milestoneTotalCount
SET repo.mirrorUrl = $mirrorUrl
SET repo.name = $name
SET repo.openGraphImageUrl = $openGraphImageUrl
SET repo.packageTotalCount = $packageTotalCount
SET repo.parentNameWithOwner = $parentNameWithOwner
SET repo.primaryLanguageName = $primaryLanguageName
SET repo.projectTotalCount = $projectTotalCount
SET repo.projectsResourcePath = $projectsResourcePath
SET repo.projectsUrl = $projectsUrl
SET repo.pullRequestTotalCount = $pullRequestTotalCount
SET repo.pushedAt = $pushedAt
SET repo.rebaseMergeAllowed = $rebaseMergeAllowed
SET repo.releaseTotalCount = $releaseTotalCount
SET repo.resourcePath = $resourcePath
SET repo.securityPolicyUrl = $securityPolicyUrl
SET repo.squashMergeAllowed = $squashMergeAllowed
SET repo.sshUrl = $sshUrl
SET repo.stargazerCount = $stargazerCount
SET repo.stargazerTotalCount = $stargazerTotalCount
SET repo.submoduleTotalCount = $submoduleTotalCount
SET repo.templateRepositoryNameWithOwner = $templateRepositoryNameWithOwner
SET repo.updatedAt = $updatedAt
SET repo.url = $url
SET repo.vulnerabilityAlertTotalCount = $vulnerabilityAlertTotalCount
SET repo.watcherTotalCount = $watcherTotalCount

// Repository - REPO_BELONGS_TO_OWNER -> Owner
MATCH (repo:Repository {nameWithOwner: $nameWithOwner})
MERGE (owner:Owner {login: $login})
  ON CREATE SET owner.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET owner.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (repo)-[belong:REPO_BELONGS_TO_OWNER]->(owner)
  ON CREATE SET belong.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET belong.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// Repository - REPO_UNDER_TOPIC -> Topic
MATCH (repo:Repository {nameWithOwner: $nameWithOwner})
MERGE (topic:Topic {name: $topicName})
  ON CREATE SET topic.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET topic.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (repo)-[under:REPO_UNDER_TOPIC]->(topic)
  ON CREATE SET under.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET under.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// Repository - REPO_USES_LANGUAGE -> Language
MATCH (repo:Repository {nameWithOwner: $nameWithOwner})
MERGE (lang:Language {name: $languageName})
  ON CREATE SET lang.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET lang.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (repo)-[uses:REPO_USES_LANGUAGE]->(lang)
  ON CREATE SET uses.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET uses.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET uses.size = $size

// dependency的一些关系要注意merge的先后次序

// Repository - REPO_DEPENDS_ON_PACKAGE -> Package
// 其实中间还有个dependencyGraphManifests层级，但是作简略处理
MATCH (repo:Repository {nameWithOwner: $nameWithOwner})
MERGE(package:Package {nameWithManager: $packageNameWithManager})
  ON CREATE SET package.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET package.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET package.name = $packageName
SET package.manager = $packageManager
MERGE (repo)-[depends_package:REPO_DEPENDS_ON_PACKAGE]->(package)
  ON CREATE SET depends_package.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET depends_package.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET depends_package.blobPath = $blobPath
SET depends_package.exceedsMaxSize = $exceedsMaxSize
SET depends_package.filename = $filename
SET depends_package.parseable = $parseable
SET depends_package.requirements = $requirements

// Repository - REPO_DEVELOPS_PACKAGE -> Package
/*
// 这种方式在插入repo时遗漏了dev_repo
MATCH (dst_repo:Repository {nameWithOwner: $dstRepoNameWithOwner})
MATCH (package:Package {nameWithManager: $packageNameWithManager})
MERGE (dst_repo)-[develops:REPO_DEVELOPS_PACKAGE]->(package)
  ON CREATE SET develops.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET develops.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')*/
MATCH (package:Package {nameWithManager: $packageNameWithManager})
MERGE (dev_repo:Repository {nameWithOwner: $devRepoNameWithOwner})
  ON CREATE SET dev_repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET dev_repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (dev_repo)-[develops:REPO_DEVELOPS_PACKAGE]->(package)
  ON CREATE SET develops.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET develops.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// todo Repository - REPO_DEPENDS_ON_REPO -> Repository
MATCH (repo:Repository {nameWithOwner: $nameWithOwner})
MATCH (dev_repo:Repository {nameWithOwner: $dstRepoNameWithOwner})
SET dev_repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (repo)-[depends_repo:REPO_DEPENDS_ON_REPO]->(dev_repo)
  ON CREATE SET depends_repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET depends_repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET depends_repo.requirements = $requirements

// todo Package - PACKAGE_DEPENDS_ON_PACKAGE -> Package
// 可以实时插入时运行，但是保证了实时就不能保证完整，即在merge主repo时，当时dst_repo并没有依赖数据，但是以后爬取到它它有了，以前它DEVELOPS的PackA并不能与packB建立关系
MATCH
  p = (packB:Package)<-[repo_pack:REPO_DEPENDS_ON_PACKAGE]-(dst_repo:Repository {nameWithOwner: $dstRepoNameWithOwner})
    -[:REPO_DEVELOPS_PACKAGE]->(packA:Package)
MERGE (packA)-[pack_pack:PACKAGE_DEPENDS_ON_PACKAGE]->(packB)
  ON CREATE SET pack_pack.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET pack_pack.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET pack_pack.requirements = repo_pack.requirements
// 离线更新 Package的同质依赖
MATCH p = (packB:Package)<-[repo_pack:REPO_DEPENDS_ON_PACKAGE]-(:Repository)-[:REPO_DEVELOPS_PACKAGE]->(packA:Package)
MERGE (packA)-[pack_pack:PACKAGE_DEPENDS_ON_PACKAGE]->(packB)
  ON CREATE SET pack_pack.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET pack_pack.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET pack_pack.requirements = repo_pack.requirements