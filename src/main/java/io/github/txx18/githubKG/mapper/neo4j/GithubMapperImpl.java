package io.github.txx18.githubKG.mapper.neo4j;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.txx18.githubKG.exception.DAOException;
import io.github.txx18.githubKG.mapper.GithubMapper;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static org.neo4j.driver.Values.parameters;

/**
 * @author ShaneTang
 * @create 2021-03-23 19:20
 */
@Repository
public class GithubMapperImpl implements GithubMapper {

    private final Driver driver;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public GithubMapperImpl(Driver driver) {
        this.driver = driver;
    }


    @Override
    public String updateRepoIDF(String nameWithOwner) throws DAOException {
        String query = "// 更新repo的度数和idf值，且针对有依赖的（否则应该使用OPTIONAL MATCH）\n" +
                "MATCH (be_depended_package:Package)\n" +
                "  WHERE exists((be_depended_package)<-[:REPO_DEPENDS_ON_PACKAGE]-(:Repository))\n" +
                "WITH count(be_depended_package) AS be_depended_package_count\n" +
                "MATCH (repo:Repository {nameWithOwner: $nameWithOwner})-[depend:REPO_DEPENDS_ON_PACKAGE]->(package:Package)\n" +
                "WITH count(package) AS degree, repo, be_depended_package_count\n" +
                "SET repo.packageDegree = degree\n" +
                "SET repo.packageIDF = log(be_depended_package_count * 1.0 / (1 + degree))";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> tx.run(query, parameters("nameWithOwner", nameWithOwner)));
        } catch (Exception e) {
            String log = "updatePackageIDF failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
        return "ok";
    }


    @Override
    public String updatePackageIDF(String nameWithManager) throws DAOException {
        String query = "// 更新package的度数和idf值，且针对有被依赖的（否则应该使用OPTIONAL MATCH）\n" +
                "MATCH (has_dependency_repo:Repository)\n" +
                "  WHERE exists((has_dependency_repo)-[:REPO_DEPENDS_ON_PACKAGE]->(:Package))\n" +
                "WITH count(has_dependency_repo) AS has_dependency_repo_count\n" +
                "MATCH (package:Package {nameWithManager: $nameWithManager})<-[depend:REPO_DEPENDS_ON_PACKAGE]-(repo:Repository)\n" +
                "WITH count(repo) AS degree, package, has_dependency_repo_count\n" +
                "SET package.repoDegree = degree\n" +
                "SET package.repoIDF = log(has_dependency_repo_count * 1.0 / (1 + degree))";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> tx.run(query, parameters("nameWithManager", nameWithManager)));
        } catch (Exception e) {
            String log = "updatePackageIDF failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
        return "ok";
    }

    @Override
    public String refactorRepoCoPackageRepo(String nameWithManager) throws DAOException {
        String query = "// Repo - REPO_CO_PACKAGE_REPO - Repo\n" +
                "MATCH (package:Package {nameWithManager: $nameWithManager})<-[:REPO_DEPENDS_ON_PACKAGE]-(repo:Repository)\n" +
                "WITH collect(repo.nameWithOwner) AS repoNames\n" +
                "UNWIND range(0, size(repoNames) - 1) AS i\n" +
                "UNWIND range(i + 1, size(repoNames) - 1) AS j\n" +
                "MATCH (repo1:Repository {nameWithOwner: repoNames[i]})\n" +
                "MATCH (repo2:Repository {nameWithOwner: repoNames[j]})\n" +
                "MERGE (repo1)-[co:REPO_CO_PACKAGE_REPO]-(repo2)\n" +
                "  ON CREATE SET co.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "  ON CREATE SET co.coPackageCount = 1\n" +
                "  ON MATCH SET co.coPackageCount = (co.coPackageCount + 1)\n" +
                "SET co.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> tx.run(query, parameters("nameWithManager", nameWithManager)));
        } catch (Exception e) {
            String log = "refactorPackageCoOccur failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
        return "ok";
    }

    @Override
    public String refactorPackageCoOccur(String nameWithOwner) throws DAOException {
        String query = "MATCH (repo:Repository {nameWithOwner: $nameWithOwner})-[:REPO_DEPENDS_ON_PACKAGE]->(pack:Package)\n" +
                "WITH collect(pack.nameWithManager) AS packageNames\n" +
                "UNWIND range(0, size(packageNames) - 1) AS i\n" +
                "UNWIND range(i + 1, size(packageNames) - 1)  AS  j\n" +
                "MATCH (pack1:Package {nameWithManager: packageNames[i]})\n" +
                "MATCH (pack2:Package {nameWithManager: packageNames[j]})\n" +
                "MERGE (pack1)-[co1:PACKAGE_CO_OCCUR_PACKAGE]-(pack2) // 注意这里不能带箭头\n" +
                "  ON CREATE SET co1.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "  ON CREATE SET co1.coOccurrenceCount = 1\n" +
                "  ON MATCH SET co1.coOccurrenceCount = (co1.coOccurrenceCount + 1)\n" +
                "SET co1.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> tx.run(query, parameters("nameWithOwner", nameWithOwner)));
        } catch (Exception e) {
            String log = "refactorPackageCoOccur failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
        return "ok";
    }

    @Override
    public List<String> matchRepoDependsOnPackages(String nameWithOwner) throws DAOException {
        String query = "MATCH (repo:Repository {nameWithOwner: $nameWithOwner})-[r:REPO_DEPENDS_ON_PACKAGE]->" +
                "(package:Package)\n" +
                "RETURN package.nameWithManager AS nameWithManager";
        try (Session session = driver.session()) {
            return session.readTransaction(tx -> {
                List<String> res = new ArrayList<>();
                Result result = tx.run(query, parameters("nameWithOwner", nameWithOwner));
                while (result.hasNext()) {
                    Record record = result.next();
                    res.add(record.get("nameWithManager").asString());
                }
                return res;
            });
        } catch (Exception e) {
            String log = "matchRepoDependsOnPackages failed!";
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
                        "nameWithOwner", ((String) repository.getOrDefault("nameWithOwner", "")).replaceAll("\\s*", ""),
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
        String query = "// 【关系】 Repository - REPO_BELONGS_TO_OWNER -> Owner\n" +
                "MERGE (repo:Repository {nameWithOwner: $nameWithOwner})\n" +
                "  ON CREATE SET repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (owner:Owner {login: $login})\n" +
                "  ON CREATE SET owner.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET owner.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (repo)-[belong:REPO_BELONGS_TO_OWNER]->(owner)\n" +
                "  ON CREATE SET belong.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET belong.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "nameWithOwner", ((String) repository.get("nameWithOwner")).replaceAll("\\s*", ""),
                        "login", ((String) ((JSONObject) repository.get("owner")).get("login")).replaceAll("\\s*", "")
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
    public String mergeRepoTopic(String nameWithOwner, String topicName) throws DAOException {
        String query = "// 【关系】 Repository - REPO_UNDER_TOPIC -> Topic\n" +
                "MERGE (repo:Repository {nameWithOwner: $nameWithOwner})\n" +
                "  ON CREATE SET repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (topic:Topic {name: $topicName})\n" +
                "  ON CREATE SET topic.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET topic.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (repo)-[under:REPO_UNDER_TOPIC]->(topic)\n" +
                "  ON CREATE SET under.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET under.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "nameWithOwner", nameWithOwner,
                        "topicName", topicName
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeRepoTopic failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
        return "success";
    }

    @Override
    public int mergeRepoLanguage(JSONObject repository, JSONObject languageNode, JSONObject languageEdge) throws DAOException {
        String query = "// 【关系】 Repository - REPO_USES_LANGUAGE -> Language\n" +
                "MERGE (repo:Repository {nameWithOwner: $nameWithOwner})\n" +
                "  ON CREATE SET repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
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
                        "nameWithOwner", ((String) repository.get("nameWithOwner")).replaceAll("\\s*", ""),
                        "languageName", ((String) languageNode.get("name")).trim(),
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
    public String mergeRepoDependsOnPackage(String nameWithOwner, String nameWithManager, String requirements) throws DAOException {
        String query = "// 创建依赖关系 Repository - REPO_DEPENDS_ON_PACKAGE -> Package\n" +
                "MATCH (repo:Repository {nameWithOwner: $nameWithOwner})\n" +
                "MATCH (package:Package {nameWithManager: $nameWithManager})\n" +
                "MERGE (repo)-[depends_package:REPO_DEPENDS_ON_PACKAGE]->(package)\n" +
                "  ON CREATE SET depends_package.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET depends_package.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET depends_package.requirements = $requirements";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "nameWithOwner", nameWithOwner,
                        "nameWithManager", nameWithManager,
                        "requirements", requirements
                ));
                return "ok";
            });
        } catch (Exception e) {
            String log = "mergeRepoPackage failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
        return "ok";
    }

    @Override
    public String deleteRepoDependsOnPackage(String nameWithOwner, String nameWithManager) throws DAOException {
        String query = "// 删除依赖关系 Repository - REPO_DEPENDS_ON_PACKAGE -> Package\n" +
                "MATCH(:Repository {nameWithOwner: $nameWithOwner})\n" +
                "       -[r:REPO_DEPENDS_ON_PACKAGE]-(:Package {nameWithManager: $nameWithManager})\n" +
                "DELETE r";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "nameWithOwner", nameWithOwner,
                        "nameWithManager", nameWithManager
                ));
                return "ok";
            });
        } catch (Exception e) {
            String log = "mergeRepoPackage failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
        return "ok";
    }


    @Override
    public int mergeRepoDependsOnPackage(JSONObject repository, JSONObject dependencyGraphManifestNode, JSONObject dependencyNode) throws DAOException {
        String query = "// 【关系】 Repository - REPO_DEPENDS_ON_PACKAGE -> Package\n" +
                "MERGE (repo:Repository {nameWithOwner: $nameWithOwner})\n" +
                "  ON CREATE SET repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
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
                        "nameWithOwner", ((String) repository.get("nameWithOwner")).replaceAll("\\s*", ""),
                        "packageNameWithManager", ((String) (dependencyNode.get("packageManager") + "/" + dependencyNode.get(
                                "packageName"))).replaceAll("\\s*", ""),
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

    @Deprecated
    @Override
    public int mergeRepoDependsOnRepo(JSONObject repository, JSONObject dependencyNode) throws DAOException {
        String query = "// Repository - REPO_DEPENDS_ON_REPO -> Repository\n" +
                "MATCH (repo:Repository {nameWithOwner: $nameWithOwner})\n" +
                "MERGE(dst_repo:Repository {nameWithOwner: $dstRepoNameWithOwner})\n" +
                "  ON CREATE SET dst_repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET dst_repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (repo)-[depends_repo:REPO_DEPENDS_ON_REPO]->(dst_repo)\n" +
                "  ON CREATE SET depends_repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET depends_repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET depends_repo.requirements = $requirements";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "nameWithOwner", ((String) repository.get("nameWithOwner")).replaceAll("\\s*", ""),
                        "dstRepoNameWithOwner", ((String) ((JSONObject) dependencyNode.get("repository")).get("nameWithOwner")).replaceAll("\\s*", ""),
                        "requirements", dependencyNode.getOrDefault("requirements", "")
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
        String query = "// 【关系】 Repository - REPO_DEVELOPS_PACKAGE -> Package\n" +
                "MERGE (package:Package {nameWithManager: $packageNameWithManager})\n" +
                "  ON CREATE SET package.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET package.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (dev_repo:Repository {nameWithOwner: $devRepoNameWithOwner})\n" +
                "  ON CREATE SET dev_repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET dev_repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (dev_repo)-[develops:REPO_DEVELOPS_PACKAGE]->(package)\n" +
                "  ON CREATE SET develops.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET develops.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "devRepoNameWithOwner", ((String) ((JSONObject) dependencyNode.get("repository")).get(
                                "nameWithOwner")).replaceAll("\\s*", ""),
                        "packageNameWithManager", ((String) (dependencyNode.get("packageManager") + "/" + dependencyNode.get(
                                "packageName"))).replaceAll("\\s*", "")
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

    @Deprecated
    @Override
    public int mergePackageDependsOnPackage(JSONObject dependencyNode) throws DAOException {
        String query = "// Package - PACKAGE_DEPENDS_ON_PACKAGE -> Package\n" +
                "// 可以实时插入时运行，但是保证了实时就不能保证完整，即在merge主repo时，当时dst_repo并没有依赖数据，但是以后爬取到它它有了，以前它DEVELOPS的PackA并不能与packB建立关系\n" +
                "MATCH\n" +
                "  p = (packB:Package)<-[repo_pack:REPO_DEPENDS_ON_PACKAGE]-(dst_repo:Repository {nameWithOwner: $dstRepoNameWithOwner})\n" +
                "    -[:REPO_DEVELOPS_PACKAGE]->(packA:Package)\n" +
                "MERGE (packA)-[pack_pack:PACKAGE_DEPENDS_ON_PACKAGE]->(packB)\n" +
                "  ON CREATE SET pack_pack.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET pack_pack.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET pack_pack.requirements = repo_pack.requirements";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "dstRepoNameWithOwner", ((String) ((JSONObject) dependencyNode.get("repository")).get(
                                "nameWithOwner")).replaceAll("\\s*", "")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeRepoRepo failed! dstRepoNameWithOwner: " + ((JSONObject) dependencyNode.get("repository")).get("nameWithOwner");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

}
