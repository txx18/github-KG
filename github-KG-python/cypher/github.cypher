// Graph
// Graph_cosine_P_R
UNWIND $dependencyMapList AS map
MATCH p = (package_i:Package {nameWithManager: map.nameWithManager})<-[:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)
  -[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend,
       sum((1 / (sqrt(package_i.dependedDegree * package_j.dependedDegree * repo_j.dependDegree)))) AS score
  ORDER BY score DESC
  LIMIT $topN
// Graph_tfidf_P
UNWIND $dependencyMapList AS map
MATCH (package_i:Package {nameWithManager: map.nameWithManager})-[co:PACKAGE_CO_OCCUR_PACKAGE]-(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend, map.dependedTF * sum(co.coOccurrenceCount * package_i.dependedIDF
* package_j.dependedIDF) AS score
  ORDER BY score DESC
  LIMIT $topN
// Graph_cosine_P_tfidf_P
UNWIND $dependencyMapList AS map
MATCH (package_i:Package {nameWithManager: map.nameWithManager})-[co:PACKAGE_CO_OCCUR_PACKAGE]-(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend,
       sum(co.coOccurrenceCount * package_i.dependedIDF * package_j.dependedIDF / sqrt(package_i.
         dependedDegree * package_j.dependedDegree)) AS score
  ORDER BY score DESC
  LIMIT $topN
// Graph_cosine_P_tfidf_P_R
UNWIND $dependencyMapList AS map
MATCH p = (package_i:Package {nameWithManager: map.nameWithManager})<-[:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)
  -[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend,
       sum((1 * package_i.dependedIDF * package_j.dependedIDF * repo_j.dependIDF / sqrt(package_i.
         dependedDegree * package_j.dependedDegree))) AS score
  ORDER BY score DESC
  LIMIT $topN
// Graph_cosine_P_R_tfidf_P_R
UNWIND $dependencyMapList AS map
MATCH p = (package_i:Package {nameWithManager: map.nameWithManager})<-[:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)
  -[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend,
       sum((1 * package_i.dependedIDF * package_j.dependedIDF * repo_j.dependIDF / sqrt(package_i.
         dependedDegree * package_j.dependedDegree * repo_j.dependDegree))) AS score
  ORDER BY score DESC
  LIMIT $topN


// UCF
UNWIND $dependencyMapList AS map
MATCH (package_i:Package {nameWithManager: map.nameWithManager})<-[:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)
        -[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend, sum(1 / (sqrt(size($dependencyNameList) * repo_j.dependDegree))) AS score
  ORDER BY score DESC
  LIMIT $topN
// UCF_IIF
UNWIND $dependencyMapList AS map
MATCH (package_i:Package {nameWithManager: map.nameWithManager})<-[:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)
        -[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend,
       sum(1 * (1 / log(1 + package_i.dependedDegree)) / (sqrt(size($dependencyNameList) * repo_j.dependDegree))) AS
       score
  ORDER BY score DESC
  LIMIT $topN

// ICF
UNWIND $dependencyMapList AS map
MATCH (package_i:Package {nameWithManager: map.nameWithManager})-[co:PACKAGE_CO_OCCUR_PACKAGE]-(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend,
       sum(co.coOccurrenceCount / sqrt(package_i.dependedDegree * package_j.dependedDegree)) AS score
  ORDER BY score DESC
  LIMIT $topN
// ICF_IUF
UNWIND $dependencyMapList AS map
MATCH p = (package_i:Package {nameWithManager: map.nameWithManager})<-[:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)
  -[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend,
       sum((1 * (1 / log(1 + repo_j.dependDegree)) / sqrt(package_i.dependedDegree * package_j.dependedDegree)))
       AS score
  ORDER BY score DESC
  LIMIT $topN


// Popular 热门推荐
MATCH (package:Package)<-[:REPO_DEPENDS_ON_PACKAGE]-(repo:Repository)
  WHERE NOT package.nameWithManager IN $dependencyNameList
WITH package.nameWithManager AS nameWithManager, count(repo) AS dependedDegree
RETURN nameWithManager AS recommend, dependedDegree AS score
  ORDER BY dependedDegree DESC
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
SET repo.dependDegree = degree
SET repo.dependIDF = log(be_depended_package_count * 1.0 / (1 + degree))

// 更新package的度数和idf值，且针对有被依赖的（否则应该使用OPTIONAL MATCH）
MATCH (has_dependency_repo:Repository)
  WHERE exists((has_dependency_repo)-[:REPO_DEPENDS_ON_PACKAGE]->(:Package))
WITH count(has_dependency_repo) AS has_dependency_repo_count
MATCH (package:Package {nameWithManager: $nameWithManager})<-[depend:REPO_DEPENDS_ON_PACKAGE]-(repo:Repository)
WITH count(repo) AS degree, package, has_dependency_repo_count
SET package.dependedDegree = degree
SET package.dependedIDF = log(has_dependency_repo_count * 1.0 / (1 + degree))


// 遍历一个Repo的Package
MATCH (repo:Repository {nameWithOwner: $nameWithOwner})-[:REPO_DEPENDS_ON_PACKAGE]->(pack:Package)
RETURN pack.nameWithManager AS nameWithManager
  ORDER BY nameWithManager


// Repo - REPO_CO_PACKAGE_REPO - Repo
// 一个Packge可能被上万个repo依赖，握手握不动。。
MATCH (package:Package {nameWithManager: $nameWithManager})<-[:REPO_DEPENDS_ON_PACKAGE]-(repo:Repository)
WITH package, count(repo) AS dependedDegree
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