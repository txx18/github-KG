package io.github.txx18.githubKG.mapper.neo4j;

import cn.hutool.json.JSONObject;
import io.github.txx18.githubKG.exception.DAOException;
import io.github.txx18.githubKG.mapper.RepoMapper;
import org.neo4j.driver.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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
    public int mergeRepo(Map<String, Object> params) throws DAOException {
        String query;
        query = "// Repository\n" +
                "MERGE (repo:Repository {nameWithOwner: $nameWithOwner})\n" +
                "  ON CREATE SET repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET repo.createdAt = $createdAt\n" +
                "SET repo.description = $description\n" +
                "SET repo.forkCount = $forkCount\n" +
                "SET repo.homepageUrl = $homepageUrl\n" +
                "SET repo.isDisabled = $isDisabled\n" +
                "SET repo.isEmpty = $isEmpty\n" +
                "SET repo.isFork = $isFork\n" +
                "SET repo.isInOrganization = $isInOrganization\n" +
                "SET repo.isLocked = $isLocked\n" +
                "SET repo.isMirror = $isMirror\n" +
                "SET repo.isPrivate = $isPrivate\n" +
                "SET repo.isTemplate = $isTemplate\n" +
                "SET repo.issueCount = $issueCount\n" +
                "SET repo.isUserConfigurationRepository = $isUserConfigurationRepository\n" +
                "SET repo.licenseInfoName = $licenseInfoName\n" +
                "SET repo.name = $name\n" +
                "SET repo.primaryLanguageName = $primaryLanguageName\n" +
                "SET repo.pullRequestCount = $pullRequestCount\n" +
                "SET repo.pushedAt = $pushedAt\n" +
                "SET repo.stargazerCount = $stargazerCount\n" +
                "SET repo.updatedAt = $updatedAt\n" +
                "SET repo.url = $url";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "nameWithOwner", params.get("nameWithOwner"),
                        "createdAt", params.get("createdAt"),
                        "description", params.get("description"),
                        "forkCount", params.get("forkCount"),
                        "homepageUrl", params.get("homepageUrl"),
                        "isDisabled", params.get("isDisabled"),
                        "isEmpty", params.get("isEmpty"),
                        "isFork", params.get("isFork"),
                        "isInOrganization", params.get("isInOrganization"),
                        "isLocked", params.get("isLocked"),
                        "isMirror", params.get("isMirror"),
                        "isPrivate", params.get("isPrivate"),
                        "isTemplate", params.get("isTemplate"),
                        "issueCount", params.get("issueCount"),
                        "isUserConfigurationRepository", params.get("isUserConfigurationRepository"),
                        "licenseInfoName", params.get("licenseInfoName"),
                        "name", params.get("name"),
                        "nameWithOwner", params.get("nameWithOwner"),
                        "primaryLanguageName", params.get("primaryLanguageName"),
                        "pullRequestCount", params.get("pullRequestCount"),
                        "pushedAt", params.get("pushedAt"),
                        "stargazerCount", params.get("stargazerCount"),
                        "updatedAt", params.get("updatedAt"),
                        "url", params.get("url")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeRepo failed! repo: " + params.get("nameWithOwner");
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
                "MERGE (repo)-[uses:REPO_USES_LANGUAGE {size: $size}]->(lang)\n" +
                "  ON CREATE SET uses.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET uses.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "nameWithOwner", repository.get("nameWithOwner"),
                        "languageName", languageNode.get("name"),
                        "size", languageEdge.getOrDefault("size", "")
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
                "SET depends_package.requirements = $requirements";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "nameWithOwner", repository.get("nameWithOwner"),
                        "packageNameWithManager", dependencyNode.get("packageManager") + "/" + dependencyNode.get("packageName"),
                        "packageName", dependencyNode.get("packageName"),
                        "packageManager", dependencyNode.get("packageManager"),
                        "blobPath", dependencyGraphManifestNode.getOrDefault("blobPath", ""),
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
