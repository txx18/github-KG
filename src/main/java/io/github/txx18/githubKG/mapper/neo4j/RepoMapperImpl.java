package io.github.txx18.githubKG.mapper.neo4j;

import io.github.txx18.githubKG.exception.DAOException;
import io.github.txx18.githubKG.mapper.RepoMapper;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RepoMapperImpl implements RepoMapper {

    private final Driver driver;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public RepoMapperImpl(Driver driver) {
        this.driver = driver;
    }

    @Override
    public int insertRepoByJsonFile(String filePath) {
        String query = "// 创建、增量更新\n" +
                "WITH\n" +
                "  'file:///" + filePath + "' AS url\n" +
                "CALL apoc.load.json(url, '$.data.repository') YIELD value\n" +
                "//return size(value.languages.nodes), value.languages.nodes[10].name, value.languages.edges[10].size\n" +
                "// repo的标量属性\n" +
                "MERGE (repo:Repo {nameWithOwner: value.nameWithOwner})\n" +
                "// 仅第一次创建时保存创建时间\n" +
                "  ON CREATE SET repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "// （这个SET不属于上一个MERGE）已存在repo，比如更新数据时，则需要重新设置其属性\n" +
                "SET repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT'),\n" +
                "repo.createdAt = value.createdAt,\n" +
                "repo.description = value.description,\n" +
                "repo.forkCount = value.forkCount,\n" +
                "repo.homepageUrl = value.homepageUrl,\n" +
                "repo.isDisabled = value.isDisabled,\n" +
                "repo.isEmpty = value.isEmpty,\n" +
                "repo.isFork = value.isFork,\n" +
                "repo.isInOrganization = value.isInOrganization,\n" +
                "repo.isLocked = value.isLocked,\n" +
                "repo.isMirror = value.isMirror,\n" +
                "repo.isPrivate = value.isPrivate,\n" +
                "repo.isTemplate = value.isTemplate,\n" +
                "repo.issueCount = value.issues.totalCount,\n" +
                "repo.isUserConfigurationRepository = value.isUserConfigurationRepository,\n" +
                "repo.licenseInfoName = value.licenseInfo.name,\n" +
                "repo.name = value.name,\n" +
                "repo.primaryLanguageName = value.primaryLanguage.name,\n" +
                "repo.pullRequestCount = value.pullRequests.totalCount,\n" +
                "repo.pushedAt = value.pushedAt,\n" +
                "repo.stargazerCount = value.stargazerCount,\n" +
                "repo.updatedAt = value.updatedAt,\n" +
                "repo.url = value.url\n" +
                "// 与owner的关系\n" +
                "MERGE (owner:Owner {login: value.owner.login})\n" +
                "  ON CREATE SET owner.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT'),\n" +
                "  owner.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (repo)-[belong:BELONGS_TO]->(owner)\n" +
                "  ON CREATE SET belong.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET belong.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "// 与topic的关系\n" +
                "// value.repositoryTopics.nodes是1row的[{}]，unwind之后的nodes是多rows的{}\n" +
                "FOREACH (node IN value.repositoryTopics.nodes |\n" +
                "  MERGE (topic:Topic {name: node.topic.name})\n" +
                "    ON CREATE SET topic.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT'),\n" +
                "    topic.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "  MERGE (repo)-[under:UNDER]->(topic)\n" +
                "    ON CREATE SET under.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "  SET under.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                ")\n" +
                "// repo与package的关系 repo与repo的关系 关系\n" +
                "FOREACH (manifest_node IN value.dependencyGraphManifests.nodes |\n" +
                "  FOREACH (dependency_node IN manifest_node.dependencies.nodes |\n" +
                "  // repo与package的DEPENDS_ON关系\n" +
                "    MERGE(package:Package {packageName: dependency_node.packageManager + '/' + dependency_node.packageName})\n" +
                "      ON CREATE SET package.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT'),\n" +
                "      package.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "    MERGE (repo)-[depends_package:DEPENDS_ON]->(package)\n" +
                "      ON CREATE SET depends_package.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "    SET depends_package.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT'),\n" +
                "    depends_package.requirements = dependency_node.requirements\n" +
                "  // todo case 想实现的是如果依赖的repo为空，则不添加这个dst_repo节点\n" +
                "  // repo与repo的DEPENDS_ON关系，dst_repo作为repo的附属品不作为主要的repo更新手段，由于可能存在自己依赖自己的情况，所以属性基本是ON CREATE SET\n" +
                "    MERGE\n" +
                "      (dst_repo:Repo {nameWithOwner: CASE WHEN exists(dependency_node.repository.nameWithOwner) THEN dependency_node.repository.nameWithOwner\n" +
                "                                       ELSE 'unknown-$-unknown'\n" +
                "                                       END})\n" +
                "      ON CREATE SET dst_repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT'),\n" +
                "      dst_repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "    MERGE (repo)-[depends_repo:DEPENDS_ON]->(dst_repo)\n" +
                "      ON CREATE SET depends_repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "    SET depends_repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "  // repo与package的DEVELOPS关系\n" +
                "    MERGE (dst_repo)-[develops:DEVELOPS]->(package)\n" +
                "      ON CREATE SET develops.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "    SET develops.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "  )\n" +
                ")\n" +
                "// 与language的关系（同时遍历两个list好像插件不识别）\n" +
                "FOREACH (i IN range(0, size(value.languages.nodes) - 1) |\n" +
                "MERGE (lang:Language {name:value.languages.nodes[i].name})\n" +
                "ON CREATE SET lang.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT'),\n" +
                "lang.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (repo)- [uses:USES {size:value.languages.edges[i].size}]- >(lang)\n" +
                "ON CREATE SET uses.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET uses.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                ")\n" +
                "return 1";
        try (Session session = driver.session()) {
            Result result = session.writeTransaction(tx -> tx.run(query));
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("createRepoByJsonFile failed", e);
            return 0;
        }
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
        }catch (Exception e) {
            e.printStackTrace();
            String log = "failed";
            logger.error(log, e);
            throw new DAOException(log);
        }
    }

    @Override
    public List<Object> listUnderPaths(String ownerWithName) throws DAOException {
        String query = "MATCH (repo:Repo {nameWithOwner: 'tensorflow/tensorflow'})-[under:UNDER]->(topic:Topic)\n" +
                "RETURN collect(under) AS under_list, collect(topic) AS topic_list";
        Record record = null;
        try (Session session = driver.session()) {
            Result result = session.run(query);
            while (result.hasNext()) {
                record = result.next();
            }
            if (record == null) {
                return null;
            }
            List<Object> under_list = record.get("under_list").asList();
            List<Object> topic_list = record.get("topic_list").asList();
            return topic_list;
        }catch (Exception e) {
            e.printStackTrace();
            String log = "failed";
            logger.error(log, e);
            throw new DAOException(log);
        }
    }
}
