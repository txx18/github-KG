package io.github.txx18.githubKG.mapper.neo4j;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.txx18.githubKG.exception.DAOException;
import io.github.txx18.githubKG.mapper.RepoMapper;
import org.neo4j.driver.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.neo4j.driver.Values.parameters;

@Component
public class RepoMapperImpl implements RepoMapper {

    private final Driver driver;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public RepoMapperImpl(Driver driver) {
        this.driver = driver;
    }

    @Override
    public int countRepoTotalCount() throws DAOException {
        String query = "MATCH (total_repo:Repo)\n" +
                "RETURN count(total_repo) AS total_repo_count";
        Record record = null;
        try (Session session = driver.session()) {
            Result result = session.run(query);
            while (result.hasNext()) {
                record = result.next();
            }
            if (record == null) {
                return -1;
            }
            return record.get("total_repo_count").asInt();
        } catch (Exception e) {
            String log = "failed";
            logger.error(log, e);
            throw new DAOException(log);
        }
    }

    /**
     * FIXME 这查询当时看的哪儿？？就用Java Driver的吧。。
     *
     * @param ownerWithName
     * @return
     * @throws DAOException
     */
    @Override
    public List<String> listUnderPaths(String ownerWithName) throws DAOException {
        String query = "MATCH (repo:Repo {nameWithOwner: 'tensorflow/tensorflow'})-[under:UNDER]->(topic:Topic)\n" +
                "RETURN collect(under) AS under_list, collect(topic) AS topic_list";
        Record record = null;
        try (Session session = driver.session(SessionConfig.builder().withDefaultAccessMode(AccessMode.READ).build())) {
            List<String> underList = session.run(query).list(r -> r.get("under_list").asString());
/*            while (result.hasNext()) {
                record = result.next();
            }
            if (record == null) {
                return null;
            }
            List<Object> under_list = record.get("under_list").asList();
            List<Object> topic_list = record.get("topic_list").asList();*/
            return underList;
        } catch (Exception e) {
            String log = "failed";
            logger.error(log, e);
            throw new DAOException(log);
        }
    }

    @Override
    public int mergeRepo(JSONObject repository) throws DAOException {
        String query;
        query = "// Repository\n" +
                "MERGE (repo:Repository {nameWithOwner: $nameWithOwner})\n" +
                "  ON CREATE SET repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET repo.assignableUserTotalCount = $assignableUserTotalCount\n" +
                "SET repo.commitCommentTotalCount = $commitCommentTotalCount\n" +
                "SET repo.createdAt = $createdAt\n" +
                "SET repo.databaseId = $databaseId\n" +
                "SET repo.deleteBranchOnMerge = $deleteBranchOnMerge\n" +
                "SET repo.dependencyGraphManifestTotalCount = $dependencyGraphManifestTotalCount\n" +
                "SET repo.description = $description\n" +
                "SET repo.diskUsage = $diskUsage\n" +
                "SET repo.forkCount = $forkCount\n" +
                "SET repo.forkTotalCount = $forkTotalCount\n" +
                "SET repo.hasIssuesEnabled = $hasIssuesEnabled\n" +
                "SET repo.hasProjectsEnabled = $hasProjectsEnabled\n" +
                "SET repo.hasWikiEnabled = $hasWikiEnabled\n" +
                "SET repo.homepageUrl = $homepageUrl\n" +
                "SET repo.isArchived = $isArchived\n" +
                "SET repo.isBlankIssuesEnabled = $isBlankIssuesEnabled\n" +
                "SET repo.isDisabled = $isDisabled\n" +
                "SET repo.isEmpty = $isEmpty\n" +
                "SET repo.isFork = $isFork\n" +
                "SET repo.isInOrganization = $isInOrganization\n" +
                "SET repo.isLocked = $isLocked\n" +
                "SET repo.isMirror = $isMirror\n" +
                "SET repo.isPrivate = $isPrivate\n" +
                "SET repo.isSecurityPolicyEnabled = $isSecurityPolicyEnabled\n" +
                "SET repo.isTemplate = $isTemplate\n" +
                "SET repo.issueTotalCount = $issueTotalCount\n" +
                "SET repo.isUserConfigurationRepository = $isUserConfigurationRepository\n" +
                "SET repo.labelTotalCount = $labelTotalCount\n" +
                "SET repo.languageTotalCount = $languageTotalCount\n" +
                "SET repo.languageTotalSize = $languageTotalSize\n" +
                "SET repo.licenseInfoName = $licenseInfoName\n" +
                "SET repo.mentionableUserTotalCount = $mentionableUserTotalCount\n" +
                "SET repo.mergeCommitAllowed = $mergeCommitAllowed\n" +
                "SET repo.milestoneTotalCount = $milestoneTotalCount\n" +
                "SET repo.mirrorUrl = $mirrorUrl\n" +
                "SET repo.name = $name\n" +
                "SET repo.openGraphImageUrl = $openGraphImageUrl\n" +
                "SET repo.packageTotalCount = $packageTotalCount\n" +
                "SET repo.parentNameWithOwner = $parentNameWithOwner\n" +
                "SET repo.primaryLanguageName = $primaryLanguageName\n" +
                "SET repo.projectTotalCount = $projectTotalCount\n" +
                "SET repo.projectsResourcePath = $projectsResourcePath\n" +
                "SET repo.projectsUrl = $projectsUrl\n" +
                "SET repo.pullRequestTotalCount = $pullRequestTotalCount\n" +
                "SET repo.pushedAt = $pushedAt\n" +
                "SET repo.rebaseMergeAllowed = $rebaseMergeAllowed\n" +
                "SET repo.releaseTotalCount = $releaseTotalCount\n" +
                "SET repo.resourcePath = $resourcePath\n" +
                "SET repo.securityPolicyUrl = $securityPolicyUrl\n" +
                "SET repo.squashMergeAllowed = $squashMergeAllowed\n" +
                "SET repo.sshUrl = $sshUrl\n" +
                "SET repo.stargazerCount = $stargazerCount\n" +
                "SET repo.stargazerTotalCount = $stargazerTotalCount\n" +
                "SET repo.submoduleTotalCount = $submoduleTotalCount\n" +
                "SET repo.templateRepositoryNameWithOwner = $templateRepositoryNameWithOwner\n" +
                "SET repo.updatedAt = $updatedAt\n" +
                "SET repo.url = $url\n" +
                "SET repo.vulnerabilityAlertTotalCount = $vulnerabilityAlertTotalCount\n" +
                "SET repo.watcherTotalCount = $watcherTotalCount";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "nameWithOwner", repository.getOrDefault("nameWithOwner", ""),
                        "assignableUserTotalCount", ((JSONObject) repository.getOrDefault("assignableUsers",
                                JSONUtil.createObj())).getOrDefault("totalCount", -1),
                        "commitCommentTotalCount", ((JSONObject) repository.getOrDefault("commitComments",
                                JSONUtil.createObj())).getOrDefault("totalCount", -1),
                        "createdAt", repository.getOrDefault("createdAt", ""),
                        "databaseId", repository.getOrDefault("databaseId", -1),
                        "deleteBranchOnMerge", repository.getOrDefault("deleteBranchOnMerge", -2),
                        "dependencyGraphManifestTotalCount", ((JSONObject) repository.getOrDefault("dependencyGraphManifests",
                                JSONUtil.createObj())).getOrDefault("totalCount", -1),
                        "description", repository.getOrDefault("description", ""),
                        "diskUsage", repository.getOrDefault("diskUsage", -1),
                        "forkCount", repository.getOrDefault("forkCount", -1),
                        "forkTotalCount", ((JSONObject) repository.getOrDefault("forks",
                                JSONUtil.createObj())).getOrDefault("totalCount", -1),
                        "hasIssuesEnabled", repository.getOrDefault("hasIssuesEnabled", -2),
                        "hasProjectsEnabled", repository.getOrDefault("hasProjectsEnabled", -2),
                        "hasWikiEnabled", repository.getOrDefault("hasWikiEnabled", -2),
                        "homepageUrl", repository.getOrDefault("homepageUrl", ""),
                        "isArchived", repository.getOrDefault("isArchived", -2),
                        "isBlankIssuesEnabled", repository.getOrDefault("isBlankIssuesEnabled", -2),
                        "isDisabled", repository.getOrDefault("isDisabled", -2),
                        "isEmpty", repository.getOrDefault("isEmpty", -2),
                        "isFork", repository.getOrDefault("isFork", -2),
                        "isInOrganization", repository.getOrDefault("isInOrganization", -2),
                        "isLocked", repository.getOrDefault("isLocked", -2),
                        "isMirror", repository.getOrDefault("isMirror", -2),
                        "isPrivate", repository.getOrDefault("isPrivate", -2),
                        "isSecurityPolicyEnabled", repository.getOrDefault("isSecurityPolicyEnabled", -2),
                        "isTemplate", repository.getOrDefault("isTemplate", -2),
                        "issueTotalCount",
                        ((JSONObject) repository.getOrDefault("issues", JSONUtil.createObj())).getOrDefault(
                                "totalCount", -1),
                        "isUserConfigurationRepository", repository.getOrDefault("isUserConfigurationRepository", -2),
                        "labelTotalCount", ((JSONObject) repository.getOrDefault("labels",
                                JSONUtil.createObj())).getOrDefault("totalCount", -1),
                        "languageTotalCount", ((JSONObject) repository.getOrDefault("languages",
                                JSONUtil.createObj())).getOrDefault("totalCount", -1),
                        "languageTotalSize", ((JSONObject) repository.getOrDefault("languages",
                                JSONUtil.createObj())).getOrDefault("totalSize", -1),
                        "licenseInfoName", ((JSONObject) repository.getOrDefault("licenseInfo", JSONUtil.createObj())).getOrDefault("name", ""),
                        "mentionableUserTotalCount", ((JSONObject) repository.getOrDefault("mentionableUsers",
                                JSONUtil.createObj())).getOrDefault("totalCount", -1),
                        "mergeCommitAllowed", repository.getOrDefault("mergeCommitAllowed", -2),
                        "milestoneTotalCount", ((JSONObject) repository.getOrDefault("milestones",
                                JSONUtil.createObj())).getOrDefault("totalCount", -1),
                        "mirrorUrl", repository.getOrDefault("mirrorUrl", ""),
                        "name", repository.getOrDefault("name", ""),
                        "openGraphImageUrl", repository.getOrDefault("openGraphImageUrl", ""),
                        "packageTotalCount", ((JSONObject) repository.getOrDefault("packages",
                                JSONUtil.createObj())).getOrDefault("totalCount", -1),
                        "parentNameWithOwner", ((JSONObject) repository.getOrDefault("parent",
                                JSONUtil.createObj())).getOrDefault("nameWithOwner", ""),
                        "primaryLanguageName", ((JSONObject) repository.getOrDefault("primaryLanguage", JSONUtil.createObj())).getOrDefault(
                                "name", ""),
                        "projectTotalCount", ((JSONObject) repository.getOrDefault("projects", JSONUtil.createObj())).getOrDefault("totalCount", -1),
                        "projectsResourcePath", repository.getOrDefault("projectsResourcePath", ""),
                        "projectsUrl", repository.getOrDefault("projectsUrl", ""),
                        "pullRequestTotalCount", ((JSONObject) repository.getOrDefault("pullRequests", JSONUtil.createObj())).getOrDefault("totalCount", -1),
                        "pushedAt", repository.getOrDefault("pushedAt", ""),
                        "rebaseMergeAllowed", repository.getOrDefault("rebaseMergeAllowed", -2),
                        "releaseTotalCount", ((JSONObject) repository.getOrDefault("releases",
                                JSONUtil.createObj())).getOrDefault("totalCount", -1),
                        "resourcePath", repository.getOrDefault("resourcePath", ""),
                        "securityPolicyUrl", repository.getOrDefault("securityPolicyUrl", ""),
                        "squashMergeAllowed", repository.getOrDefault("squashMergeAllowed", -2),
                        "sshUrl", repository.getOrDefault("sshUrl", ""),
                        "stargazerCount", repository.getOrDefault("stargazerCount", -1),
                        "stargazerTotalCount", ((JSONObject) repository.getOrDefault("stargazers", JSONUtil.createObj())).getOrDefault("totalCount", -1),
                        "submoduleTotalCount", ((JSONObject) repository.getOrDefault("submodules", JSONUtil.createObj())).getOrDefault("totalCount", -1),
                        "templateRepositoryNameWithOwner", ((JSONObject) repository.getOrDefault("templateRepository", JSONUtil.createObj())).getOrDefault(
                                "nameWithOwner", ""),
                        "updatedAt", repository.getOrDefault("updatedAt", ""),
                        "url", repository.getOrDefault("url", ""),
                        "vulnerabilityAlertTotalCount", ((JSONObject) repository.getOrDefault("vulnerabilityAlerts", JSONUtil.createObj())).getOrDefault("totalCount", -1),
                        "watcherTotalCount", ((JSONObject) repository.getOrDefault("watchers", JSONUtil.createObj())).getOrDefault("totalCount", -1)
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeRepo failed! repo: " + repository.get("nameWithOwner");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeRepoOwner(JSONObject repository) throws DAOException {
        String query = "// Repository - REPO_BELONGS_TO_OWNER -> Owner\n" +
                "MATCH (repo:Repository {nameWithOwner: $nameWithOwner})\n" +
                "MERGE (owner:Owner {login: $login})\n" +
                "  ON CREATE SET owner.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET owner.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (repo)-[belong:REPO_BELONGS_TO_OWNER]->(owner)\n" +
                "  ON CREATE SET belong.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET belong.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "nameWithOwner", repository.get("nameWithOwner"),
                        "login", ((JSONObject) repository.get("owner")).get("login")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeRepo failed! repo: " + repository.get("nameWithOwner");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeRepoTopic(JSONObject repository, JSONObject topicNode) throws DAOException {
        String query = "// Repository - REPO_UNDER_TOPIC -> Topic\n" +
                "MATCH (repo:Repository {nameWithOwner: $nameWithOwner})\n" +
                "MERGE (topic:Topic {name: $topicName})\n" +
                "  ON CREATE SET topic.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET topic.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (repo)-[under:REPO_UNDER_TOPIC]->(topic)\n" +
                "  ON CREATE SET under.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET under.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "nameWithOwner", repository.get("nameWithOwner"),
                        "topicName", ((JSONObject) topicNode.get("topic")).get("name")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeRepoTopic failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeRepoLanguage(JSONObject repository, JSONObject languageNode, JSONObject languageEdge) throws DAOException {
        String query = "// Repository - REPO_USES_LANGUAGE -> Language\n" +
                "MATCH (repo:Repository {nameWithOwner: $nameWithOwner})\n" +
                "MERGE (lang:Language {name: $languageName})\n" +
                "  ON CREATE SET lang.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET lang.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (repo)-[uses:REPO_USES_LANGUAGE]->(lang)\n" +
                "  ON CREATE SET uses.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET uses.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET uses.size = $size";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "nameWithOwner", repository.get("nameWithOwner"),
                        "languageName", languageNode.get("name"),
                        "size", languageEdge.getOrDefault("size", -1)
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeRepoLanguage failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeRepoDependsOnPackage(JSONObject repository, JSONObject dependencyGraphManifestNode, JSONObject dependencyNode) throws DAOException {
        String query = "// Repository - REPO_DEPENDS_ON_PACKAGE -> Package\n" +
                "// 其实中间还有个dependencyGraphManifests层级，但是作简略处理\n" +
                "MATCH (repo:Repository {nameWithOwner: $nameWithOwner})\n" +
                "MERGE(package:Package {nameWithManager: $packageNameWithManager})\n" +
                "  ON CREATE SET package.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET package.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET package.name = $packageName\n" +
                "SET package.manager = $packageManager\n" +
                "MERGE (repo)-[depends_package:REPO_DEPENDS_ON_PACKAGE]->(package)\n" +
                "  ON CREATE SET depends_package.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET depends_package.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET depends_package.blobPath = $blobPath\n" +
                "SET depends_package.exceedsMaxSize = $exceedsMaxSize\n" +
                "SET depends_package.filename = $filename\n" +
                "SET depends_package.parseable = $parseable\n" +
                "SET depends_package.requirements = $requirements";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "nameWithOwner", repository.get("nameWithOwner"),
                        "packageNameWithManager", dependencyNode.get("packageManager") + "/" + dependencyNode.get("packageName"),
                        "packageName", dependencyNode.get("packageName"),
                        "packageManager", dependencyNode.get("packageManager"),
                        "blobPath", dependencyGraphManifestNode.getOrDefault("blobPath", ""),
                        "exceedsMaxSize", dependencyGraphManifestNode.getOrDefault("exceedsMaxSize", -2),
                        "filename", dependencyGraphManifestNode.getOrDefault("filename", ""),
                        "parseable", dependencyGraphManifestNode.getOrDefault("parseable", -2),
                        "requirements", dependencyNode.getOrDefault("requirements", "")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeRepoPackage failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeRepoDependsOnRepo(JSONObject repository, JSONObject dependencyNode) throws DAOException {
        String query = "// Repository - REPO_DEPENDS_ON_REPO -> Repository\n" +
                "MATCH (repo:Repository {nameWithOwner: $nameWithOwner})\n" +
                "MERGE(dst_repo:Repository {nameWithOwner: $dstRepoNameWithOwner})\n" +
                "  ON CREATE SET dst_repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET dst_repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (repo)-[depends_repo:REPO_DEPENDS_ON_REPO]->(dst_repo)\n" +
                "  ON CREATE SET depends_repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET depends_repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "nameWithOwner", repository.get("nameWithOwner"),
                        "dstRepoNameWithOwner", ((JSONObject) dependencyNode.get("repository")).get("nameWithOwner")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeRepoRepo failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeRepoDevelopsPackage(JSONObject dependencyNode) throws DAOException {
        String query = "// Repository - REPO_DEVELOPS_PACKAGE -> Package\n" +
                "MATCH (dst_repo:Repository {nameWithOwner: $desRepoNameWithOwner})\n" +
                "MATCH (package:Package {nameWithManager: $packageNameWithManager})\n" +
                "MERGE (dst_repo)-[develops:REPO_DEVELOPS_PACKAGE]->(package)\n" +
                "  ON CREATE SET develops.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET develops.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "desRepoNameWithOwner", ((JSONObject) dependencyNode.get("repository")).get("nameWithOwner"),
                        "packageNameWithManager", dependencyNode.get("packageManager") + "/" + dependencyNode.get("packageName")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeRepoRepo failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }
}
