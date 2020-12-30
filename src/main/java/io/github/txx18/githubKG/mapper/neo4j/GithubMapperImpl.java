package io.github.txx18.githubKG.mapper.neo4j;

import io.github.txx18.githubKG.mapper.GithubMapper;
import org.neo4j.driver.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.neo4j.driver.Values.parameters;

/**
 * @author ShaneTang
 * @create 2020-12-21 13:00
 */
@Component
public class GithubMapperImpl implements GithubMapper {

    private final Driver driver;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public GithubMapperImpl(Driver driver) {
        this.driver = driver;
    }

    @Override
    public int transCoOccurrenceNetworkNoRequirements() {
        try (Session session = driver.session()) {
            int repoCount = 0;
            List<Record> repoNameWithOwners = session.readTransaction(new TransactionWork<List<Record>>() { // 匿名内部类写法
                @Override
                public List<Record> execute(Transaction tx) {
                    return matchHasDependenciesRepos(tx);
                }
            });
            // 遍历Repo
            for (final Record nameWithOwner : repoNameWithOwners) {
                // 遍历Package
                List<Record> nameWithManagers = session.readTransaction(new TransactionWork<List<Record>>() {
                    @Override
                    public List<Record> execute(Transaction tx) {
                        return matchPackagesByRepo(tx, nameWithOwner);
                    }
                });
                // 握手问题，只需遍历到倒数第二个小朋友
                for (int i = 0; i < nameWithManagers.size() - 1; i++) {
                    int packageCount = 0;
                    Record nameWithManager1 = nameWithManagers.get(i);
                    for (int j = i + 1; j < nameWithManagers.size(); j++) {
                        Record nameWithManager2 = nameWithManagers.get(j);
                        packageCount += session.writeTransaction(tx -> mergePackageCoOccurrencePackage(tx,
                                nameWithManager1, nameWithManager2)); // lambda写法
                    }
                    System.out.println("package1: " + nameWithManager1 + "index: " + packageCount);
                }
                repoCount++;
                System.out.println("repo: " + nameWithOwner + "index: " + repoCount);
            }
            return repoCount;
        }
    }

    private Integer mergePackageCoOccurrencePackage(Transaction tx, Record nameWithManager1, Record nameWithManager2) {
        String query = "// Package - PACKAGE_CO_OCCURRENCE_PACKAGE - Package\n" +
                "// 第一次同现关系ON CREATE SET, 非第一次同现关系 ON MATCH SET\n" +
                "MATCH (pack1:Package {nameWithManager: $nameWithManager1})\n" +
                "MATCH (pack2:Package {nameWithManager: $nameWithManager2})\n" +
                "MERGE (pack1)-[co1:PACKAGE_CO_OCCURRENCE_PACKAGE]->(pack2)\n" +
                "  ON CREATE SET co1.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "  ON CREATE SET co1.coOccurrenceCount = 1\n" +
                "  ON MATCH SET co1.coOccurrenceCount = (co1.coOccurrenceCount + 1)\n" +
                "SET co1.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (pack2)-[co2:PACKAGE_CO_OCCURRENCE_PACKAGE]->(pack1)\n" +
                "  ON CREATE SET co2.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "  ON CREATE SET co2.coOccurrenceCount = 1\n" +
                "  ON MATCH SET co2.coOccurrenceCount = (co2.coOccurrenceCount + 1)\n" +
                "SET co2.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        tx.run(query, parameters(
                "nameWithManager1", nameWithManager1.get("nameWithManager").asString(),
                "nameWithManager2", nameWithManager2.get("nameWithManager").asString()
        ));
        return 1;
    }

    private List<Record> matchPackagesByRepo(Transaction tx, Record nameWithOwner) {
        String query = "// 遍历一个Repo的Package\n" +
                "MATCH (repo:Repository {nameWithOwner: $nameWithOwner})-[:REPO_DEPENDS_ON_PACKAGE]->(pack:Package)\n" +
                "RETURN pack.nameWithManager as nameWithManager\n" +
                "  ORDER BY nameWithManager";
        return tx.run(query, parameters("nameWithOwner", nameWithOwner.get("nameWithOwner").asString())).list();
    }

    private List<Record> matchHasDependenciesRepos(Transaction tx) {
        String query = "MATCH (repo:Repository)\n" +
                "WHERE EXISTS {\n" +
                "  MATCH (repo)-[:REPO_DEPENDS_ON_PACKAGE]->(pack:Package)\n" +
                "}\n" +
                "//RETURN COUNT(repo.nameWithOwner)\n" +
                "RETURN repo.nameWithOwner as nameWithOwner\n" +
                "  ORDER BY nameWithOwner";
        return tx.run(query).list();
    }
}
