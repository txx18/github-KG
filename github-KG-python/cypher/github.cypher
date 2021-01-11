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
MATCH (dst_repo:Repository {nameWithOwner: $dstRepoNameWithOwner})
MATCH (package:Package {nameWithManager: $packageNameWithManager})
MERGE (dst_repo)-[develops:REPO_DEVELOPS_PACKAGE]->(package)
  ON CREATE SET develops.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET develops.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// Repository - REPO_DEPENDS_ON_REPO -> Repository
MATCH (repo:Repository {nameWithOwner: $nameWithOwner})
MERGE(dst_repo:Repository {nameWithOwner: $dstRepoNameWithOwner})
  ON CREATE SET dst_repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET dst_repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (repo)-[depends_repo:REPO_DEPENDS_ON_REPO]->(dst_repo)
  ON CREATE SET depends_repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET depends_repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET depends_repo.requirements = $requirements

// Package - PACKAGE_DEPENDS_ON_PACKAGE -> Package
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