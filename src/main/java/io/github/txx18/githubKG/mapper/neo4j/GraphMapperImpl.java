package io.github.txx18.githubKG.mapper.neo4j;

import io.github.txx18.githubKG.exception.DAOException;
import io.github.txx18.githubKG.mapper.GraphMapper;
import io.github.txx18.githubKG.model.RecommendRecord;
import org.neo4j.driver.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.neo4j.driver.Values.parameters;

/**
 * @author ShaneTang
 * @create 2021-04-28 17:16
 */
@Repository
public class GraphMapperImpl implements GraphMapper {

    private final Driver driver;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public GraphMapperImpl(Driver driver) {
        this.driver = driver;
    }

    @Override
    public List<Map<String, Object>> recommendPackagesExperimentICF(List<String> dependencyNameList,
                                                                    Map<String, Object> inputRepoPortraitMap,
                                                                    int topN,
                                                                    String useWeight) throws DAOException {
        String query = "";
        switch (useWeight) {
            case "none":
                query = "// package\n" +
                        "UNWIND $targetMapList AS map\n" +
                        "MATCH (tar_i:Package {nameWithManager: map.key})<-[:REPO_DEPENDS_ON_PACKAGE]-(:Repository)-[:REPO_DEPENDS_ON_PACKAGE]->(tar_j:Package)\n" +
                        "  WHERE NOT tar_j.nameWithManager IN $targetKeyList\n" +
                        "RETURN tar_j.nameWithManager AS key, sum(1.0 / sqrt(tar_i.repoDegree * tar_j.repoDegree)) AS score\n" +
                        "  ORDER BY score DESC, key DESC\n" +
                        "  LIMIT $topN";
                break;
            case "cosine_TfIdf":
                query = "// package\n" +
                        "UNWIND $targetMapList AS map\n" +
                        "MATCH (tar_i:Package {nameWithManager: map.key})<-[r_i:REPO_DEPENDS_ON_PACKAGE]-(:Repository)-[r_j:REPO_DEPENDS_ON_PACKAGE]->(tar_j:Package)\n" +
                        "  WHERE NOT tar_j.nameWithManager IN $targetKeyList\n" +
                        "RETURN tar_j.nameWithManager AS key, sum(1.0 * r_i.packageTfIdf * r_j.packageTfIdf / sqrt(tar_i.repoTfIdfQuadraticSum * tar_j.repoTfIdfQuadraticSum)) AS score\n" +
                        "  ORDER BY score DESC, key DESC\n" +
                        "  LIMIT $topN";
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + useWeight);
        }
        try (Session session = driver.session()) {
            String finalQuery = query;
            return session.readTransaction(tx -> {
                List<Map<String, Object>> res = new ArrayList<>();
                Result result = tx.run(finalQuery, parameters(
                        "targetMapList", inputRepoPortraitMap.get("package"),
                        "targetKeyList", dependencyNameList,
                        "topN", topN));
                while (result.hasNext()) {
                    Record record = result.next();
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("key", record.get("key").asString());
                    resultMap.put("score", record.get("score").asDouble());
                    res.add(resultMap);
                }
                return res;
            });
        } catch (Exception e) {
            String log = "recommendPackages failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
    }

    @Override
    public List<Map<String, Object>> recommendLanguageExperimentICF(Map<String, Object> inputRepoPortraitMap,
                                                                    List<String> targetKeyList,
                                                                    int topN,
                                                                    String useWeight) throws DAOException {
        String query = "";
        switch (useWeight) {
            case "none":
                query = "// language\n" +
                        "UNWIND $targetMapList AS map\n" +
                        "MATCH (tar_i:Language {name: map.key})<-[r_j:REPO_USES_LANGUAGE]-(:Repository)-[:REPO_USES_LANGUAGE]->(tar_j:Language)\n" +
                        "  WHERE NOT tar_j.name IN $targetKeyList\n" +
                        "RETURN tar_j.name AS key, sum(1.0 / sqrt(tar_i.repoDegree * tar_j.repoDegree)) AS score\n" +
                        "  ORDER BY score DESC, key DESC\n" +
                        "  LIMIT $topN";
                break;
            case "cosine_TfIdf":
                query = "// language\n" +
                        "UNWIND $targetMapList AS map\n" +
                        "MATCH (tar_i:Language {name: map.key})<-[r_i:REPO_USES_LANGUAGE]-(:Repository)-[r_j:REPO_USES_LANGUAGE]->(tar_j:Language)\n" +
                        "  WHERE NOT tar_j.name IN $targetKeyList\n" +
                        "RETURN tar_j.name AS key, sum(1.0 * r_i.languageTfIdf * r_j.languageTfIdf / sqrt(tar_i.repoTfIdfQuadraticSum * tar_j.repoTfIdfQuadraticSum)) AS score\n" +
                        "  ORDER BY score DESC, key DESC\n" +
                        "  LIMIT $topN";
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + useWeight);
        }
        try (Session session = driver.session()) {
            String finalQuery = query;
            return session.readTransaction(tx -> {
                List<Map<String, Object>> res = new ArrayList<>();
                Result result = tx.run(finalQuery, parameters(
                        "targetMapList", inputRepoPortraitMap.get("language"),
                        "targetKeyList", targetKeyList,
                        "topN", topN));
                while (result.hasNext()) {
                    Record record = result.next();
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("key", record.get("key").asString());
                    resultMap.put("score", record.get("score").asDouble());
                    res.add(resultMap);
                }
                return res;
            });
        } catch (Exception e) {
            String log = "recommend language failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
    }

    @Override
    public List<Map<String, Object>> recommendTaskExperimentICF(Map<String, Object> inputRepoPortraitMap,
                                                                List<String> targetKeyList,
                                                                int topN,
                                                                String useWeight) throws DAOException {
        String query = "";
        switch (useWeight) {
            case "none":
                query = "// task\n" +
                        "UNWIND $targetMapList AS map\n" +
                        "MATCH (tar_i:Task {name: map.key})<-[:REPO_IMPLEMENTS_PAPER_UNDER_TASK]-(:Repository)-[:REPO_IMPLEMENTS_PAPER_UNDER_TASK]->(tar_j:Task)\n" +
                        "  WHERE NOT tar_j.name IN $targetKeyList\n" +
                        "RETURN tar_j.name AS key, sum(1.0 / sqrt(tar_i.repoDegree * tar_j.repoDegree)) AS score\n" +
                        "  ORDER BY score DESC, key DESC\n" +
                        "  LIMIT $topN";
                break;
            case "cosine_TfIdf":
                query = "// task\n" +
                        "UNWIND $targetMapList AS map\n" +
                        "MATCH (tar_i:Task {name: map.key})<-[r_i:REPO_IMPLEMENTS_PAPER_UNDER_TASK]-(:Repository)-[r_j:REPO_IMPLEMENTS_PAPER_UNDER_TASK]->(tar_j:Task)\n" +
                        "  WHERE NOT tar_j.name IN $targetKeyList\n" +
                        "RETURN tar_j.name AS key, sum(1.0 * r_i.taskTfIdf * r_j.taskTfIdf / sqrt(tar_i.repoTfIdfQuadraticSum * tar_j.repoTfIdfQuadraticSum)) AS score\n" +
                        "  ORDER BY score DESC, key DESC\n" +
                        "  LIMIT $topN";
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + useWeight);
        }
        try (Session session = driver.session()) {
            String finalQuery = query;
            return session.readTransaction(tx -> {
                List<Map<String, Object>> res = new ArrayList<>();
                Result result = tx.run(finalQuery, parameters(
                        "targetMapList", inputRepoPortraitMap.get("task"),
                        "targetKeyList", targetKeyList,
                        "topN", topN));
                while (result.hasNext()) {
                    Record record = result.next();
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("key", record.get("key").asString());
                    resultMap.put("score", record.get("score").asDouble());
                    res.add(resultMap);
                }
                return res;
            });
        } catch (Exception e) {
            String log = "recommend language failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
    }

    @Override
    public List<Map<String, Object>> recommendMethodExperimentICF(Map<String, Object> inputRepoPortraitMap,
                                                                  List<String> targetKeyList,
                                                                  int topN,
                                                                  String useWeight) throws DAOException {
        String query = "";
        switch (useWeight) {
            case "none":
                query = "// method\n" +
                        "UNWIND $targetMapList AS map\n" +
                        "MATCH (tar_i:Method {name: map.key})<-[:REPO_IMPLEMENTS_PAPER_USES_METHOD]-(:Repository)-[:REPO_IMPLEMENTS_PAPER_USES_METHOD]->(tar_j:Method)\n" +
                        "  WHERE NOT tar_j.name IN $targetKeyList\n" +
                        "RETURN tar_j.name AS key, sum(1.0 / sqrt(tar_i.repoDegree * tar_j.repoDegree)) AS score\n" +
                        "  ORDER BY score DESC, key DESC\n" +
                        "  LIMIT $topN";
                break;
            case "cosine_TfIdf":
                query = "// method\n" +
                        "UNWIND $targetMapList AS map\n" +
                        "MATCH (tar_i:Method {name: map.key})<-[r_i:REPO_IMPLEMENTS_PAPER_USES_METHOD]-(:Repository)-[r_j:REPO_IMPLEMENTS_PAPER_USES_METHOD]->(tar_j:Method)\n" +
                        "  WHERE NOT tar_j.name IN $targetKeyList\n" +
                        "RETURN tar_j.name AS key, sum(1.0 * r_i.methodTfIdf * r_j.methodTfIdf / sqrt(tar_i.repoTfIdfQuadraticSum * tar_j.repoTfIdfQuadraticSum)) AS score\n" +
                        "  ORDER BY score DESC, key DESC\n" +
                        "  LIMIT $topN";
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + useWeight);
        }
        try (Session session = driver.session()) {
            String finalQuery = query;
            return session.readTransaction(tx -> {
                List<Map<String, Object>> res = new ArrayList<>();
                Result result = tx.run(finalQuery, parameters(
                        "targetMapList", inputRepoPortraitMap.get("method"),
                        "targetKeyList", targetKeyList,
                        "topN", topN));
                while (result.hasNext()) {
                    Record record = result.next();
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("key", record.get("key").asString());
                    resultMap.put("score", record.get("score").asDouble());
                    res.add(resultMap);
                }
                return res;
            });
        } catch (Exception e) {
            String log = "recommend language failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
    }

    @Override
    public List<Map<String, Object>> recommendDatasetExperimentICF(Map<String, Object> inputRepoPortraitMap,
                                                                   List<String> targetKeyList,
                                                                   int topN,
                                                                   String useWeight) throws DAOException {
        String query = "";
        switch (useWeight) {
            case "none":
                query = "// dataset\n" +
                        "UNWIND $targetMapList AS map\n" +
                        "MATCH (tar_i:Dataset {name: map.key})<-[:REPO_IMPLEMENTS_MODEL_ON_DATASET]-(:Repository)-[:REPO_IMPLEMENTS_MODEL_ON_DATASET]->(tar_j:Dataset)\n" +
                        "  WHERE NOT tar_j.name IN $targetKeyList\n" +
                        "RETURN tar_j.name AS key, sum(1.0 / sqrt(tar_i.repoDegree * tar_j.repoDegree)) AS score\n" +
                        "  ORDER BY score DESC, key DESC\n" +
                        "  LIMIT $topN";
                break;
            case "cosine_TfIdf":
                query = "// dataset\n" +
                        "UNWIND $targetMapList AS map\n" +
                        "MATCH (tar_i:Dataset {name: map.key})<-[r_i:REPO_IMPLEMENTS_MODEL_ON_DATASET]-(:Repository)-[r_j:REPO_IMPLEMENTS_MODEL_ON_DATASET]->(tar_j:Dataset)\n" +
                        "  WHERE NOT tar_j.name IN $targetKeyList\n" +
                        "RETURN tar_j.name AS key, sum(1.0 * r_i.datasetTfIdf * r_j.datasetTfIdf / sqrt(tar_i.repoTfIdfQuadraticSum * tar_j.repoTfIdfQuadraticSum)) AS score\n" +
                        "  ORDER BY score DESC, key DESC\n" +
                        "  LIMIT $topN";
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + useWeight);
        }
        try (Session session = driver.session()) {
            String finalQuery = query;
            return session.readTransaction(tx -> {
                List<Map<String, Object>> res = new ArrayList<>();
                Result result = tx.run(finalQuery, parameters(
                        "targetMapList", inputRepoPortraitMap.get("dataset"),
                        "targetKeyList", targetKeyList,
                        "topN", topN));
                while (result.hasNext()) {
                    Record record = result.next();
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("key", record.get("key").asString());
                    resultMap.put("score", record.get("score").asDouble());
                    res.add(resultMap);
                }
                return res;
            });
        } catch (Exception e) {
            String log = "recommend language failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
    }

    @Override
    public List<RecommendRecord> recommendPackagesByEntityExperimentUCFTfIdf(String entity, Map<String, Object> inputRepoPortraitMap,
                                                                             List<String> packageKeyList, int UCF_KNN) throws DAOException {
        String query = "";
        switch (entity) {
            case "package":
                query = "// package\n" +
                        "UNWIND $entityMapList AS map\n" +
                        "MATCH (package_i:Package {nameWithManager: map.key})<-[r_j:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)\n" +
                        "  WHERE exists {\n" +
                        "    MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]-(package_j:Package)\n" +
                        "      WHERE package_j <> package_i\n" +
                        "    }\n" +
                        "WITH repo_j, sum(1.0 * map.TfIdf * r_j.TfIdf / sqrt($entityTfIdfQuadraticSum * repo_j.packageTfIdfQuadraticSum)) AS score\n" +
                        "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                        "  LIMIT $UCF_KNN\n" +
                        "MATCH (repo_j)-[r_j:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                        "  WHERE NOT package_j.nameWithManager IN $packageKeyList\n" +
                        "RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score, package_j.repoDegree AS repoDegree";
                break;
            case "language":
                query = "// Language\n" +
                        "UNWIND $entityMapList AS map\n" +
                        "MATCH (language:Language {name: map.key})<-[r_j:REPO_USES_LANGUAGE]-(repo_j:Repository)\n" +
                        "WITH repo_j, sum(1.0 * map.TfIdf * r_j.TfIdf / sqrt($entityTfIdfQuadraticSum * repo_j.languageTfIdfQuadraticSum)) AS score\n" +
                        "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                        "  LIMIT $UCF_KNN\n" +
                        "MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                        "  WHERE NOT package_j.nameWithManager IN $packageKeyList\n" +
                        "RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score, package_j.repoDegree AS repoDegree";
                break;
            case "task":
                query = "// Task\n" +
                        "UNWIND $entityMapList AS map\n" +
                        "MATCH (task:Task {name: map.key})<-[r_j:REPO_IMPLEMENTS_PAPER_UNDER_TASK]-(repo_j:Repository)\n" +
                        "WITH repo_j, sum(1.0 * map.TfIdf * r_j.TfIdf / sqrt($entityTfIdfQuadraticSum * repo_j.taskTfIdfQuadraticSum)) AS score\n" +
                        "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                        "  LIMIT $UCF_KNN\n" +
                        "MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                        "  WHERE NOT package_j.nameWithManager IN $packageKeyList\n" +
                        "RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score, package_j.repoDegree AS repoDegree";
                break;
            case "method":
                query = "// Method\n" +
                        "UNWIND $entityMapList AS map\n" +
                        "MATCH (method:Method {name: map.key})<-[r_j:REPO_IMPLEMENTS_PAPER_USES_METHOD]-(repo_j:Repository)\n" +
                        "WITH repo_j, sum(1.0 * map.TfIdf * r_j.TfIdf / sqrt($entityTfIdfQuadraticSum * repo_j.methodTfIdfQuadraticSum)) AS score\n" +
                        "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                        "  LIMIT $UCF_KNN\n" +
                        "MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                        "  WHERE NOT package_j.nameWithManager IN $packageKeyList\n" +
                        "RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score, package_j.repoDegree AS repoDegree";
                break;
            case "dataset":
                query = "// Dataset\n" +
                        "UNWIND $entityMapList AS map\n" +
                        "MATCH (dataset:Dataset {name: map.key})<-[r_j:REPO_IMPLEMENTS_MODEL_ON_DATASET]-(repo_j:Repository)\n" +
                        "WITH repo_j, sum(1.0 * map.TfIdf * r_j.TfIdf / sqrt($entityTfIdfQuadraticSum * repo_j.datasetTfIdfQuadraticSum)) AS score\n" +
                        "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                        "  LIMIT $UCF_KNN\n" +
                        "MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                        "  WHERE NOT package_j.nameWithManager IN $packageKeyList\n" +
                        "RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score, package_j.repoDegree AS repoDegree";
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + entity);
        }
        try (Session session = driver.session()) {
            String finalQuery = query;
            return session.readTransaction(tx -> {
                // 找出实体画像 language task method dataset
                List<Map<String, Object>> entityMapList = (List<Map<String, Object>>) inputRepoPortraitMap.get(entity);
                List<Map<String, Object>> portraitEntities = getPortraitEntitiesIdf(tx, entity, entityMapList);
                // 计算 xxxTfIdfQuadraticSum
                addEntityTfIdf(entityMapList, portraitEntities);
                double entityTfIdfQuadraticSum = calEntityTfIdfQuadraticSum(entityMapList);
                List<RecommendRecord> res = new ArrayList<>();
                Result result = tx.run(finalQuery, parameters(
                        "entityMapList", entityMapList,
                        "packageKeyList", packageKeyList,
                        "UCF_KNN", UCF_KNN,
                        "entityTfIdfQuadraticSum", entityTfIdfQuadraticSum
                ));
                while (result.hasNext()) {
                    Record record = result.next();
                    RecommendRecord recommendRecord = new RecommendRecord();
                    recommendRecord.setKey(record.get("recommend").asString());
                    recommendRecord.setScore(record.get("score").asDouble());
                    recommendRecord.setRepoDegree(record.get("repoDegree").asDouble());
                    res.add(recommendRecord);
                }
                return res;
            });
        } catch (Exception e) {
            String log = "recommendPackages failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
    }

    @Override
    public List<RecommendRecord> recommendPackageByEntityUCFPathSim(String entity, Map<String, Object> inputRepoPortraitMap, List<String> packageKeyList, int UCF_KNN) throws DAOException {
        String query = "";
        switch (entity) {
            case "package":
                query = "// UCF_PathSim_Pac\n" +
                        "UNWIND $entityMapList AS map\n" +
                        "MATCH (:Package {nameWithManager: map.key})<-[r_j:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)\n" +
                        "WITH repo_j, sum(2 * 1.0 / (size($entityMapList) + repo_j.packageDegree)) AS score\n" +
                        "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                        "  LIMIT $UCF_KNN\n" +
                        "MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                        "  WHERE NOT package_j.nameWithManager IN $packageKeyList\n" +
                        "RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score";
                break;
            case "language":
                query = "// UCF_PathSim_La\n" +
                        "UNWIND $entityMapList AS map\n" +
                        "MATCH (:Language {name: map.key})<-[r_j:REPO_USES_LANGUAGE]-(repo_j:Repository)\n" +
                        "WITH repo_j, sum(2 * 1.0 / (size($entityMapList) + repo_j.languageDegree)) AS score\n" +
                        "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                        "  LIMIT $UCF_KNN\n" +
                        "MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                        "  WHERE NOT package_j.nameWithManager IN $packageKeyList\n" +
                        "RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score";
                break;
            case "task":
                query = "// UCF_PathSim_Ta\n" +
                        "UNWIND $entityMapList AS map\n" +
                        "MATCH (:Task {name: map.key})<-[r_j:REPO_IMPLEMENTS_PAPER_UNDER_TASK]-(repo_j:Repository)\n" +
                        "WITH repo_j, sum(2 * 1.0 / (size($entityMapList) + repo_j.taskDegree)) AS score\n" +
                        "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                        "  LIMIT $UCF_KNN\n" +
                        "MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                        "  WHERE NOT package_j.nameWithManager IN $packageKeyList\n" +
                        "RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score";
                break;
            case "method":
                query = "// UCF_PathSim_Me\n" +
                        "UNWIND $entityMapList AS map\n" +
                        "MATCH (:Method {name: map.key})<-[r_j:REPO_IMPLEMENTS_PAPER_USES_METHOD]-(repo_j:Repository)\n" +
                        "WITH repo_j, sum(2 * 1.0 / (size($entityMapList) + repo_j.methodDegree)) AS score\n" +
                        "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                        "  LIMIT $UCF_KNN\n" +
                        "MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                        "  WHERE NOT package_j.nameWithManager IN $packageKeyList\n" +
                        "RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score";
                break;
            case "dataset":
                query = "// UCF_PathSim_Da\n" +
                        "UNWIND $entityMapList AS map\n" +
                        "MATCH (:Dataset {name: map.key})<-[r_j:REPO_IMPLEMENTS_MODEL_ON_DATASET]-(repo_j:Repository)\n" +
                        "WITH repo_j, sum(2 * 1.0 / (size($entityMapList) + repo_j.datasetDegree)) AS score\n" +
                        "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                        "  LIMIT $UCF_KNN\n" +
                        "MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                        "  WHERE NOT package_j.nameWithManager IN $packageKeyList\n" +
                        "RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score";
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + entity);
        }
        try (Session session = driver.session()) {
            String finalQuery = query;
            return session.readTransaction(tx -> {
                List<RecommendRecord> res = new ArrayList<>();
                Result result = tx.run(finalQuery, parameters(
                        "entityMapList", inputRepoPortraitMap.get(entity),
                        "packageKeyList", packageKeyList,
                        "UCF_KNN", UCF_KNN
                ));
                while (result.hasNext()) {
                    Record record = result.next();
                    RecommendRecord recommendRecord = new RecommendRecord();
                    recommendRecord.setKey(record.get("recommend").asString());
                    recommendRecord.setScore(record.get("score").asDouble());
                    res.add(recommendRecord);
                }
                return res;
            });
        } catch (Exception e) {
            String log = "recommendPackages failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
    }


    public List<Map<String, Object>> getPortraitEntitiesIdf(Transaction tx, String entity, List<Map<String, Object>> entityMapList) {
        String query = "";
        switch (entity) {
            case "package":
                query = "// package\n" +
                        "UNWIND $entityMapList AS map\n" +
                        "MATCH (entity_i:Package {nameWithManager: map.key})\n" +
                        "RETURN entity_i.nameWithManager AS key, entity_i.repoIDF AS repoIDF";
                break;
            case "language":
                query = "// language\n" +
                        "UNWIND $entityMapList AS map\n" +
                        "MATCH (entity_i:Language {name: map.key})\n" +
                        "RETURN entity_i.name AS key, entity_i.repoIDF AS repoIDF";
                break;
            case "task":
                query = "// task\n" +
                        "UNWIND $entityMapList AS map\n" +
                        "MATCH (entity_i:Task {name: map.key})\n" +
                        "RETURN entity_i.name AS key, entity_i.repoIDF AS repoIDF";
                break;
            case "method":
                query = "// method\n" +
                        "UNWIND $entityMapList AS map\n" +
                        "MATCH (entity_i:Method {name: map.key})\n" +
                        "RETURN entity_i.name AS key, entity_i.repoIDF AS repoIDF";
                break;
            case "dataset":
                query = "// dataset\n" +
                        "UNWIND $entityMapList AS map\n" +
                        "MATCH (entity_i:Dataset {name: map.key})\n" +
                        "RETURN entity_i.name AS key, entity_i.repoIDF AS repoIDF";
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + entity);
        }
        List<Map<String, Object>> res = new ArrayList<>();
        Result result = tx.run(query, parameters("entityMapList", entityMapList));
        while (result.hasNext()) {
            Record record = result.next();
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("key", record.get("key").asString());
            resultMap.put("repoIDF", record.get("repoIDF").asDouble());
            res.add(resultMap);
        }
        return res;
    }

    private List<Map<String, Object>> addEntityTfIdf(List<Map<String, Object>> entityMapList, List<Map<String, Object>> portraitEntities) {
        Map<String, Double> idfMap = new HashMap<>();
        for (Map<String, Object> portraitEntity : portraitEntities) {
            idfMap.put(((String) portraitEntity.get("key")), ((double) portraitEntity.get("repoIDF")));
        }
        for (Map<String, Object> map : entityMapList) {
            String key = (String) map.get("key");
            double tf = (double) map.get("tf");
            double idf = idfMap.getOrDefault(key, 0.0);
            map.put("idf", idf);
            double TfIdf = tf * idf;
            map.put("TfIdf", TfIdf);
        }
        return entityMapList;
    }

    private double calEntityTfIdfQuadraticSum(List<Map<String, Object>> entityMapList) {
        double res = 0;
        for (Map<String, Object> map : entityMapList) {
            res += Math.pow((double) map.get("TfIdf"), 2);
        }
        return res;
    }


    @Override
    public List<RecommendRecord> recommendPackagesByEntityExperimentUCF(String entity, Map<String, Object> inputRepoPortraitMap,
                                                                        List<String> packageKeyList, int UCF_KNN) throws DAOException {
        String query = "";
        switch (entity) {
            case "package":
                query = "UNWIND $targetMapList AS map\n" +
                        "MATCH (package_i:Package {nameWithManager: map.key})<-[r_i:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)\n" +
                        "  WHERE exists {\n" +
                        "    MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]-(package_j:Package)\n" +
                        "      WHERE package_j <> package_i\n" +
                        "    }\n" +
                        "WITH repo_j, sum(1.0 / (sqrt(size($targetMapList) * repo_j.packageDegree))) AS score\n" +
                        "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                        "  LIMIT $UCF_KNN\n" +
                        "MATCH (repo_j)-[r_j:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                        "  WHERE NOT package_j.nameWithManager IN $dependencyNameList\n" +
                        "RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score";
                break;
            case "language":
                query = "// UCF_cosine_Language\n" +
                        "UNWIND $targetMapList AS map\n" +
                        "MATCH (language:Language {name: map.key})<-[r_j:REPO_USES_LANGUAGE]-(repo_j:Repository)\n" +
                        "WITH repo_j, sum(1.0 / sqrt(size($targetMapList) * repo_j.languageDegree)) AS score\n" +
                        "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                        "  LIMIT $UCF_KNN\n" +
                        "MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                        "  WHERE NOT package_j.nameWithManager IN $dependencyNameList\n" +
                        "RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score";
                break;
            case "task":
                query = "// UCF_cosine_Task\n" +
                        "UNWIND $targetMapList AS map\n" +
                        "MATCH (task:Task {name: map.key})<-[r_j:REPO_IMPLEMENTS_PAPER_UNDER_TASK]-(repo_j:Repository)\n" +
                        "WITH repo_j, sum(1.0 / sqrt(size($targetMapList) * repo_j.taskDegree)) AS score\n" +
                        "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                        "  LIMIT $UCF_KNN\n" +
                        "MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                        "  WHERE NOT package_j.nameWithManager IN $dependencyNameList\n" +
                        "RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score";
                break;
            case "method":
                query = "// UCF_cosine_Method\n" +
                        "UNWIND $targetMapList AS map\n" +
                        "MATCH (method:Method {name: map.key})<-[r_j:REPO_IMPLEMENTS_PAPER_USES_METHOD]-(repo_j:Repository)\n" +
                        "WITH repo_j, sum(1.0 / sqrt(size($targetMapList) * repo_j.methodDegree)) AS score\n" +
                        "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                        "  LIMIT $UCF_KNN\n" +
                        "MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                        "  WHERE NOT package_j.nameWithManager IN $dependencyNameList\n" +
                        "RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score";
                break;
            case "dataset":
                query = "// UCF_cosine_Dataset\n" +
                        "UNWIND $targetMapList AS map\n" +
                        "MATCH (dataset:Dataset {name: map.key})<-[r_j:REPO_IMPLEMENTS_MODEL_ON_DATASET]-(repo_j:Repository)\n" +
                        "WITH repo_j, sum(1.0 / sqrt(size($targetMapList) * repo_j.datasetDegree)) AS score\n" +
                        "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                        "  LIMIT $UCF_KNN\n" +
                        "MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                        "  WHERE NOT package_j.nameWithManager IN $dependencyNameList\n" +
                        "RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score";
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + entity);
        }
        try (Session session = driver.session()) {
            String finalQuery = query;
            return session.readTransaction(tx -> {
//                List<Map<String, Object>> res = new ArrayList<>();
                List<RecommendRecord> res = new ArrayList<>();
                Result result = tx.run(finalQuery, parameters(
                        "targetMapList", inputRepoPortraitMap.get(entity),
                        "dependencyNameList", packageKeyList,
                        "UCF_KNN", UCF_KNN
                ));
                while (result.hasNext()) {
                    Record record = result.next();
//                    Map<String, Object> resultMap = new HashMap<>();
//                    resultMap.put("score", record.get("score").asDouble());
//                    res.add(resultMap);
                    RecommendRecord recommendRecord = new RecommendRecord();
                    recommendRecord.setKey(record.get("recommend").asString());
                    recommendRecord.setScore(record.get("score").asDouble());
                    res.add(recommendRecord);
                }
                return res;
            });
        } catch (Exception e) {
            String log = "recommendPackages failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
    }


    @Override
    public String deleteRepoAllRelation(String nameWithOwner) throws DAOException {
        String query = "// 删除repo所有关系\n" +
                "MATCH (repo:Repository {nameWithOwner: $nameWithOwner})-[r]-()\n" +
                "delete r";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "nameWithOwner", nameWithOwner
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
    public List<Map<String, Object>> recommendPackagesExperimentInGraphGraph(String nameWithOwner,
                                                                             List<String> dependencyNameList,
                                                                             List<Map<String, Object>> dependencyMapList,
                                                                             int topN, int UCF_KNN) throws DAOException {
        String Graph_UCF_cosine_PacLaPapTaMeDa_tfidf_PacLaPapTaMeDa = "// language\n" +
                "OPTIONAL MATCH(repo_i:Repository {nameWithOwner: $nameWithOwner})-[r_i:REPO_USES_LANGUAGE]->(language:Language)<-[r_j:REPO_USES_LANGUAGE]-(repo_j:Repository)\n" +
                "WITH repo_j, sum(1.0 * r_i.TfIdf * r_j.TfIdf / sqrt(repo_i.languageTfIdfQuadraticSum * repo_j.languageTfIdfQuadraticSum)) AS score\n" +
                "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                "  LIMIT $UCF_KNN\n" +
                "OPTIONAL MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                "  WHERE (NOT package_j.nameWithManager IN $dependencyNameList)\n" +
                "WITH package_j.nameWithManager AS package_j_nameWithManager, sum(1.0 * score) AS score\n" +
                "WITH (collect({nameWithManager: package_j_nameWithManager, score: score})) AS rows\n" +
                "// Paper\n" +
                "OPTIONAL MATCH (repo_i:Repository {nameWithOwner: $nameWithOwner})<-[r_i:PAPER_IMPLEMENTED_BY_REPO]-(paper:Paper)-[r_j:PAPER_IMPLEMENTED_BY_REPO]->\n" +
                "(repo_j:Repository)\n" +
                "WITH rows, repo_j, sum(1.0 * r_i.TfIdf * r_j.TfIdf / sqrt(repo_i.paperTfIdfQuadraticSum * repo_j.paperTfIdfQuadraticSum)) AS score\n" +
                "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                "  LIMIT $UCF_KNN\n" +
                "OPTIONAL MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                "  WHERE (NOT package_j.nameWithManager IN $dependencyNameList)\n" +
                "WITH rows, package_j.nameWithManager AS package_j_nameWithManager, sum(1.0 * score) AS score\n" +
                "WITH (rows + collect({nameWithManager: package_j_nameWithManager, score: score})) AS rows\n" +
                "// Task\n" +
                "OPTIONAL MATCH (repo_i:Repository {nameWithOwner: $nameWithOwner})-[r_i:REPO_IMPLEMENTS_PAPER_UNDER_TASK]->(task:Task)\n" +
                "                 <-[r_j:REPO_IMPLEMENTS_PAPER_UNDER_TASK]-(repo_j:Repository)\n" +
                "WITH rows, repo_j, sum(1.0 * r_i.TfIdf * r_j.TfIdf / sqrt(repo_i.taskTfIdfQuadraticSum * repo_j.taskTfIdfQuadraticSum)) AS score\n" +
                "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                "  LIMIT $UCF_KNN\n" +
                "OPTIONAL MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                "  WHERE (NOT package_j.nameWithManager IN $dependencyNameList)\n" +
                "WITH rows, package_j.nameWithManager AS package_j_nameWithManager, sum(1.0 * score) AS score\n" +
                "WITH (rows + collect({nameWithManager: package_j_nameWithManager, score: score})) AS rows\n" +
                "// Method\n" +
                "OPTIONAL MATCH (repo_i:Repository {nameWithOwner: $nameWithOwner})-[r_i:REPO_IMPLEMENTS_PAPER_USES_METHOD]->(method:Method)\n" +
                "                 <-[r_j:REPO_IMPLEMENTS_PAPER_USES_METHOD]-(repo_j:Repository)\n" +
                "WITH rows, repo_j, sum(1.0 * r_i.TfIdf * r_j.TfIdf / sqrt(repo_i.methodTfIdfQuadraticSum * repo_j.methodTfIdfQuadraticSum)) AS score\n" +
                "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                "  LIMIT $UCF_KNN\n" +
                "OPTIONAL MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                "  WHERE (NOT package_j.nameWithManager IN $dependencyNameList)\n" +
                "WITH rows, package_j.nameWithManager AS package_j_nameWithManager, sum(1.0 * score) AS score\n" +
                "WITH (rows + collect({nameWithManager: package_j_nameWithManager, score: score})) AS rows\n" +
                "// Dataset\n" +
                "OPTIONAL MATCH (repo_i:Repository {nameWithOwner: $nameWithOwner})-[r_i:REPO_IMPLEMENTS_MODEL_ON_DATASET]->(dataset:Dataset)\n" +
                "                 <-[r_j:REPO_IMPLEMENTS_MODEL_ON_DATASET]-(repo_j:Repository)\n" +
                "WITH rows, repo_j, sum(1.0 * r_i.TfIdf * r_j.TfIdf / sqrt(repo_i.datasetTfIdfQuadraticSum * repo_j.datasetTfIdfQuadraticSum)) AS score\n" +
                "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                "  LIMIT $UCF_KNN\n" +
                "OPTIONAL MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                "  WHERE (NOT package_j.nameWithManager IN $dependencyNameList)\n" +
                "WITH rows, package_j.nameWithManager AS package_j_nameWithManager, sum(1.0 * score) AS score\n" +
                "WITH (rows + collect({nameWithManager: package_j_nameWithManager, score: score})) AS rows\n" +
                "// Package\n" +
                "UNWIND $dependencyMapList AS map\n" +
                "OPTIONAL MATCH (package_i:Package {nameWithManager: map.nameWithManager})<-[r_j:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository) // 注意这个OPTIONAL必须有\n" +
                "WITH rows, repo_j, sum(1.0 * map.packageTfIdf * r_j.TfIdf / sqrt($packageTfIdfQuadraticSum * repo_j.packageTfIdfQuadraticSum)) AS score\n" +
                "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                "  LIMIT $UCF_KNN\n" +
                "OPTIONAL MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                "  WHERE NOT package_j.nameWithManager IN $dependencyNameList\n" +
                "WITH rows, package_j.nameWithManager AS package_j_nameWithManager, sum(1.0 * score) AS score\n" +
                "WITH (rows + collect({nameWithManager: package_j_nameWithManager, score: score})) AS rows\n" +
                "// 汇总\n" +
                "UNWIND rows AS row\n" +
                "WITH row.nameWithManager AS recommend, sum(row.score) AS score\n" +
                "  WHERE recommend IS NOT NULL\n" +
                "RETURN recommend, score\n" +
                "  ORDER BY score DESC, recommend DESC\n" +
                "  LIMIT $topN";
        try (Session session = driver.session()) {
            return session.readTransaction(tx -> {
                // todo 找出用户画像 package language [paper] task method dataset
                List<Map<String, Object>> portraitPackages = getPortraitPackages(tx, dependencyMapList);
//                List<Map<String, Object>> portraitLanguages = getPortraitLanguages(tx, languageMapList);
                // 计算 packageTfIdfQuadraticSum
                addPackageTfIdf(dependencyMapList, portraitPackages);
                double packageTfIdfQuadraticSum = calPackageTfIdfQuadraticSum(dependencyMapList);
                // 推荐
                List<Map<String, Object>> res = new ArrayList<>();
                Result result = tx.run(Graph_UCF_cosine_PacLaPapTaMeDa_tfidf_PacLaPapTaMeDa, parameters(
                        "nameWithOwner", nameWithOwner,
                        "dependencyMapList", dependencyMapList,
                        "dependencyNameList", dependencyNameList,
                        "topN", topN,
                        "UCF_KNN", UCF_KNN,
                        "packageTfIdfQuadraticSum", packageTfIdfQuadraticSum
                ));
/*                Result result = tx.run(Graph_UCF_cosine_PacLaPapTaMeDa, parameters("nameWithOwner", nameWithOwner, "dependencyNameList"
                        , dependencyNameList, "topN", topN, "UCF_KNN", UCF_KNN));*/
                while (result.hasNext()) {
                    Record record = result.next();
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("nameWithManager", record.get("recommend").asString());
                    resultMap.put("score", record.get("score").asDouble());
                    res.add(resultMap);
                }
                return res;
            });
        } catch (Exception e) {
            String log = "recommendPackages failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
    }


    @Override
    public List<Map<String, Object>> recommendPackagesExperimentInGraphUCF(String payloadEntity, String nameWithOwner, List<String> dependencyNameList, List<Map<String,
            Object>> dependencyMapList, int topN, int UCF_KNN) throws DAOException {
        String query = "";
        switch (payloadEntity) {
            case "package":
                query = "// UCF_cosine_Pac\n" +
                        "MATCH(repo_i:Repository {nameWithOwner: $nameWithOwner})-[r_i:REPO_DEPENDS_ON_PACKAGE]->(package_i:Package)<-[r_j:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)\n" +
                        "  WHERE exists {\n" +
                        "    MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]-(package_j:Package)\n" +
                        "      WHERE package_j <> package_i\n" +
                        "    }\n" +
                        "WITH repo_j, sum(1.0 / (sqrt(repo_i.packageDegree * repo_j.packageDegree))) AS score\n" +
                        "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                        "  LIMIT $UCF_KNN\n" +
                        "MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                        "  WHERE NOT package_j.nameWithManager IN $dependencyNameList\n" +
                        "RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score\n" +
                        "  ORDER BY score DESC, recommend DESC\n" +
                        "  LIMIT $topN";
                break;
            case "language":
                query = "// UCF_cosine_Language\n" +
                        "MATCH(repo_i:Repository {nameWithOwner: $nameWithOwner})-[r_i:REPO_USES_LANGUAGE]->(language:Language)<-[r_j:REPO_USES_LANGUAGE]-(repo_j:Repository)\n" +
                        "WITH repo_j, sum(1.0 / sqrt(repo_i.languageDegree * repo_j.languageDegree)) AS score\n" +
                        "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                        "  LIMIT $UCF_KNN\n" +
                        "MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                        "  WHERE NOT package_j.nameWithManager IN $dependencyNameList\n" +
                        "RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score\n" +
                        "  ORDER BY score DESC, recommend DESC\n" +
                        "  LIMIT $topN";
                break;
            case "task":
                query = "// UCF_cosine_Task\n" +
                        "MATCH (repo_i:Repository {nameWithOwner: $nameWithOwner})-[r_i:REPO_IMPLEMENTS_PAPER_UNDER_TASK]->(task:Task)<-[r_j:REPO_IMPLEMENTS_PAPER_UNDER_TASK]-\n" +
                        "(repo_j:Repository)\n" +
                        "WITH repo_j, sum(1.0 / sqrt(repo_i.taskDegree * repo_j.taskDegree)) AS score\n" +
                        "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                        "  LIMIT $UCF_KNN\n" +
                        "MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                        "  WHERE NOT package_j.nameWithManager IN $dependencyNameList\n" +
                        "RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score\n" +
                        "  ORDER BY score DESC, recommend DESC\n" +
                        "  LIMIT $topN";
                break;
            case "method":
                query = "// UCF_cosine_Method\n" +
                        "MATCH(repo_i:Repository {nameWithOwner: $nameWithOwner})-[r_i:REPO_IMPLEMENTS_PAPER_USES_METHOD]->(method:Method)\n" +
                        "       <-[r_j:REPO_IMPLEMENTS_PAPER_USES_METHOD]-(repo_j:Repository)\n" +
                        "WITH repo_j, sum(1.0 / sqrt(repo_i.methodDegree * repo_j.methodDegree)) AS score\n" +
                        "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                        "  LIMIT $UCF_KNN\n" +
                        "MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                        "  WHERE NOT package_j.nameWithManager IN $dependencyNameList\n" +
                        "RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score\n" +
                        "  ORDER BY score DESC, recommend DESC\n" +
                        "  LIMIT $topN";
                break;
            case "dataset":
                query = "// UCF_cosine_Dataset\n" +
                        "MATCH (repo_i:Repository {nameWithOwner: $nameWithOwner})-[r_i:REPO_IMPLEMENTS_MODEL_ON_DATASET]->(dataset:Dataset)\n" +
                        "                 <-[r_j:REPO_IMPLEMENTS_MODEL_ON_DATASET]-(repo_j:Repository)\n" +
                        "WITH repo_j, sum(1.0 / sqrt(repo_i.datasetDegree * repo_j.datasetDegree)) AS score\n" +
                        "  ORDER BY score DESC, repo_j.nameWithOwner DESC\n" +
                        "  LIMIT $UCF_KNN\n" +
                        "MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                        "  WHERE NOT package_j.nameWithManager IN $dependencyNameList\n" +
                        "RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score\n" +
                        "  ORDER BY score DESC, recommend DESC\n" +
                        "  LIMIT $topN";
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + payloadEntity);
        }
        try (Session session = driver.session()) {
            String finalQuery = query;
            return session.readTransaction(tx -> {
                // 找出用户画像
//                List<Map<String, Object>> portraitPackages = getPortraitPackages(tx, dependencyMapList);
                // 计算 packageTfIdfQuadraticSum
//                addPackageTfIdf(dependencyMapList, portraitPackages);
//                double packageTfIdfQuadraticSum = calPackageTfIdfQuadraticSum(dependencyMapList);
                List<Map<String, Object>> res = new ArrayList<>();
                Result result = tx.run(finalQuery, parameters(
                        "nameWithOwner", nameWithOwner,
//                        "dependencyMapList", dependencyMapList,
                        "dependencyNameList", dependencyNameList,
                        "topN", topN,
                        "UCF_KNN", UCF_KNN
//                        "packageTfIdfQuadraticSum", packageTfIdfQuadraticSum
                ));
/*                Result result = tx.run(UCF_cosine_Dataset, parameters(
                        "nameWithOwner", nameWithOwner,
                        "dependencyNameList", dependencyNameList,
                        "topN", topN,
                        "UCF_KNN", UCF_KNN
                ));*/
                while (result.hasNext()) {
                    Record record = result.next();
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("nameWithManager", record.get("recommend").asString());
                    resultMap.put("score", record.get("score").asDouble());
                    res.add(resultMap);
                }
                return res;
            });
        } catch (Exception e) {
            String log = "recommendPackages failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
    }

    @Override
    public List<Map<String, Object>> recommendPackagesExperimentInGraphICF(String nameWithOwner,
                                                                           List<String> dependencyNameList,
                                                                           List<Map<String, Object>> dependencyMapList, int topN) throws DAOException {
        String ICF_cosine_Pac_tfidf_Pac = "// ICF_cosine_Pac_tfidf_Pac\n" +
                "// 不在图中的新用户写法\n" +
                "UNWIND $dependencyMapList AS map\n" +
                "MATCH (package_i:Package {nameWithManager: map.nameWithManager})<-[r_i:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)\n" +
                "        -[r_j:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)\n" +
                "  WHERE NOT package_j.nameWithManager IN $dependencyNameList\n" +
                "RETURN package_j.nameWithManager AS recommend,\n" +
                "       sum(1.0 * r_i.inverseTfIdf * r_j.inverseTfIdf / sqrt(package_i.repoTfIdfQuadraticSum * package_j.repoTfIdfQuadraticSum)) AS score\n" +
                "  ORDER BY score DESC\n" +
                "  LIMIT $topN";
        try (Session session = driver.session()) {
            return session.readTransaction(tx -> {
                List<Map<String, Object>> res = new ArrayList<>();
                Result result = tx.run(ICF_cosine_Pac_tfidf_Pac, parameters("dependencyMapList", dependencyMapList, "dependencyNameList"
                        , dependencyNameList, "topN", topN));
/*                Result result = tx.run(ICF_cosine_Pac_userNotInGraph, parameters("nameWithOwner", nameWithOwner, "dependencyNameList"
                        , dependencyNameList, "topN", topN));*/
                while (result.hasNext()) {
                    Record record = result.next();
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("nameWithManager", record.get("recommend").asString());
                    resultMap.put("score", record.get("score").asDouble());
                    res.add(resultMap);
                }
                return res;
            });
        } catch (Exception e) {
            String log = "recommendPackages failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
    }

    private double calPackageTfIdfQuadraticSum(List<Map<String, Object>> dependencyMapList) {
        double res = 0;
        for (Map<String, Object> packageMap : dependencyMapList) {
            res += Math.pow((double) packageMap.get("packageTfIdf"), 2);
        }
        return res;
    }

    private List<Map<String, Object>> addPackageTfIdf(List<Map<String, Object>> entityMapList, List<Map<String, Object>> portraitPackages) {
        Map<String, Double> idfMap = new HashMap<>();
        for (Map<String, Object> portraitPackage : portraitPackages) {
            idfMap.put(((String) portraitPackage.get("nameWithManager")), ((double) portraitPackage.get("packageRepoIDF")));
        }
        for (Map<String, Object> dependencyMap : entityMapList) {
            String nameWithManager = (String) dependencyMap.get("nameWithManager");
            double packageTf = (double) dependencyMap.get("packageTf");
            double packageIdf = idfMap.getOrDefault(nameWithManager, 0.0);
            dependencyMap.put("packageIdf", packageIdf);
            double packageTfIdf = packageTf * packageIdf;
            dependencyMap.put("packageTfIdf", packageTfIdf);
        }
        return entityMapList;
    }

    public List<Map<String, Object>> getPortraitPackages(Transaction tx, List<Map<String, Object>> dependencyMapList) {
        String query = "// 根据主键List找出 xxxList\n" +
                "UNWIND $dependencyMapList AS map\n" +
                "MATCH (package_i:Package {nameWithManager: map.nameWithManager})\n" +
                "RETURN package_i.nameWithManager AS nameWithManager, package_i.repoIDF AS packageRepoIDF";
        List<Map<String, Object>> res = new ArrayList<>();
        Result result = tx.run(query, parameters("dependencyMapList", dependencyMapList));
        while (result.hasNext()) {
            Record record = result.next();
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("nameWithManager", record.get("nameWithManager").asString());
            resultMap.put("packageRepoIDF", record.get("packageRepoIDF").asDouble());
            res.add(resultMap);
        }
        return res;
    }

    private double calPackageRepoIDFQuadraticSum(List<Map<String, Object>> portraitPackages) {
        double res = 0;
        for (Map<String, Object> portraitPackage : portraitPackages) {
            res += Math.pow((double) portraitPackage.get("packageRepoIDF"), 2);
        }
        return res;
    }


    @Override
    public List<Map<String, Object>> recommendPackagesExperimentPopular(List<String> dependencyNameList, int topN) throws DAOException {
        String query = "// Popular 热门推荐\n" +
                "MATCH (package:Package)<-[:REPO_DEPENDS_ON_PACKAGE]-(repo:Repository)\n" +
                "  WHERE NOT package.nameWithManager IN $dependencyNameList\n" +
                "WITH package.nameWithManager AS nameWithManager, count(repo) AS repoDegree\n" +
                "RETURN nameWithManager AS recommend, repoDegree AS score\n" +
                "  ORDER BY repoDegree DESC\n" +
                "  LIMIT $topN";
        try (Session session = driver.session()) {
            return session.readTransaction(tx -> {
                List<Map<String, Object>> res = new ArrayList<>();
                Result result = tx.run(query, parameters("dependencyNameList", dependencyNameList, "topN", topN));
                while (result.hasNext()) {
                    Record record = result.next();
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("key", record.get("recommend").asString());
                    resultMap.put("score", record.get("score").asDouble());
                    res.add(resultMap);
                }
                return res;
            });
        } catch (Exception e) {
            String log = "recommendPackages failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
    }

    @Override
    public List<Map<String, Object>> recommendPackagesExperimentRandom(List<String> dependencyNameList, int topN) throws DAOException {
        String query = "// 随机推荐\n" +
                "MATCH (package:Package)\n" +
                "  WHERE exists((package)<-[:REPO_DEPENDS_ON_PACKAGE]-(:Repository))\n" +
                "  AND NOT package.nameWithManager IN $dependencyNameList\n" +
                "RETURN DISTINCT package.nameWithManager AS recommend, rand() AS score\n" +
                "  ORDER BY score\n" +
                "  LIMIT $topN";
        try (Session session = driver.session()) {
            return session.readTransaction(tx -> {
                List<Map<String, Object>> res = new ArrayList<>();
                Result result = tx.run(query, parameters("dependencyNameList", dependencyNameList, "topN", topN));
                while (result.hasNext()) {
                    Record record = result.next();
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("nameWithManager", record.get("recommend").asString());
                    resultMap.put("score", record.get("score").asDouble());
                    res.add(resultMap);
                }
                return res;
            });
        } catch (Exception e) {
            String log = "recommendPackages failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
    }

    @Override
    public List<String> recommendPackages(List<String> dependencyNameList, List<Map<String, Object>> dependencyMapList, int pageNum, int pageSize) throws DAOException {
        String query = "// 针对一个package_list做推荐\n" +
                "UNWIND $dependencyMapList AS map\n" +
                "MATCH (package_i:Package {nameWithManager: map.nameWithManager})-[co:PACKAGE_CO_OCCUR_PACKAGE]-(package_j:Package)\n" +
                "  WHERE NOT package_j.nameWithManager IN $dependencyNameList\n" +
                "RETURN package_j.nameWithManager AS recommend, map.dependedTF * sum(co.coOccurrenceCount * package_j.repoIDF) AS\n" +
                "score\n" +
                "  ORDER BY score DESC\n" +
                "  SKIP $pageNum\n" +
                "  LIMIT $pageSize";
        try (Session session = driver.session()) {
            return session.readTransaction(tx -> {
                List<String> res = new ArrayList<>();
                Result result = tx.run(query, parameters("dependencyMapList", dependencyMapList, "dependencyNameList", dependencyNameList, "pageNum",
                        pageNum, "pageSize", pageSize));
                while (result.hasNext()) {
                    res.add(result.next().get("recommend").asString());
                }
                return res;
            });
        } catch (Exception e) {
            String log = "recommendPackages failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
    }


}

