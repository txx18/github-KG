package io.github.txx18.githubKG.mapper.neo4j;

import io.github.txx18.githubKG.exception.DAOException;
import io.github.txx18.githubKG.mapper.PapersWithCodeMapper;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
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
 * @create 2020-12-12 20:02
 */
@Repository
public class PapersWithCodeMapperImpl implements PapersWithCodeMapper {

    private final Driver driver;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public PapersWithCodeMapperImpl(Driver driver) {
        this.driver = driver;
    }

    @Override
    public int mergeCategoryTask(Map<String, Object> params) throws DAOException {
        String query = "// 【关系】 Category - CATEGORY_HAS_TASK -> Task\n" +
                "MERGE (task:Task {name: $taskName})\n" +
                "  ON CREATE SET task.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET task.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (category:Category {name: $category})\n" +
                "  ON CREATE SET category.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET category.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (category)-[has:CATEGORY_HAS_TASK]->(task)\n" +
                "  ON CREATE SET has.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET has.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "taskName", params.get("taskName"),
                        "category", params.get("category")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeCategoryTask failed! category: " + params.get("category") + "task: " + params.get("taskName");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeTaskSubtask(Map<String, Object> params) throws DAOException {
        String query = "// 第二种思路：把 subTask 也看作 Task\n" +
                "// 【关系】 Task - TASK_HAS_SUBTASK -> Subtask\n" +
                "MERGE (task:Task {name: $taskName})\n" +
                "  ON CREATE SET task.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET task.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (subtask:Task {name: $subtaskName})\n" +
                "  ON CREATE SET subtask.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET subtask.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET subtask.description = $description\n" +
                "MERGE (task)-[has_subtask:TASK_HAS_SUBTASK]->(subtask)\n" +
                "  ON CREATE SET has_subtask.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET has_subtask.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "taskName", params.get("taskName"),
                        "subtaskName", params.get("subtaskName"),
                        "description", params.get("description")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeTaskSubtask failed! task: " + params.get("taskName") + "- subtask: " + params.get("subtaskName");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeTaskDataset(Map<String, Object> params) throws DAOException {
        String query = "// 【关系】 Task - TASK_HAS_DATASET -> Dataset\n" +
                "MERGE (task:Task {name: $taskName})\n" +
                "  ON CREATE SET task.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET task.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (dataset:Dataset {name: $datasetName})\n" +
                "  ON CREATE SET dataset.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET dataset.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET dataset.description = $description\n" +
                "MERGE (task)-[has_dataset:TASK_HAS_DATASET]->(dataset)\n" +
                "  ON CREATE SET has_dataset.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET has_dataset.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "taskName", params.get("taskName"),
                        "datasetName", params.get("datasetName"),
                        "description", params.get("description")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeTaskDataset failed! task: " + params.get("taskName") + " - dataset: " + params.get("datasetName");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int createModelMetricDataset(HashMap<String, Object> params) throws DAOException {
        String query = "// 【关系】 Model - MODEL_ON_DATASET -> Dataset\n" +
                "MERGE (dataset:Dataset {name: $datasetName})\n" +
                "  ON CREATE SET dataset.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET dataset.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (model:Model {name: $modelName})\n" +
                "  ON CREATE SET model.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET model.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "// 把metric作为 Dataset & Model之间属性 最后有几个metric就插入几条关系\n" +
                "MERGE (model)-[model_on_dataset:MODEL_ON_DATASET {metricName: $metricName}]->(dataset)\n" +
                "  ON CREATE SET model_on_dataset.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET model_on_dataset.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "// 有的含有%之类的，所以设为string类型\n" +
                "SET model_on_dataset.metricValue = $metricValue";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "modelName", params.get("modelName"),
                        "datasetName", params.get("datasetName"),
                        "metricName", params.getOrDefault("metricName", ""),
                        "metricValue", params.getOrDefault("metricValue", "")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log =
                    "mergeModelDataset failed! model: " + params.get("modelName") + " - dataset: " + params.get("datasetName");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeModelPaper(HashMap<String, Object> params) throws DAOException {
        String query = "// 【实体】 Paper\n" +
                "// 【关系】 Model - MODEL_INTRODUCED_IN_PAPER -> Paper\n" +
                "MERGE (model:Model {name: $modelName})\n" +
                "  ON CREATE SET model.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET model.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (paper:Paper {paperTitle: $paperTitle})\n" +
                "  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.paperUrl = $paperUrl\n" +
                "SET paper.paperDate = $paperDate\n" +
                "MERGE (model)-[model_in_paper:MODEL_INTRODUCED_IN_PAPER]->(paper)\n" +
                "  ON CREATE SET model_in_paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET model_in_paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "modelName", params.get("modelName"),
                        "paperUrl", params.get("paperUrl"),
                        "paperTitle", params.get("paperTitle"),
                        "paperDate", params.get("paperDate")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeModelPaper failed! model: " + params.get("modelName") + " - paper: " + params.get("paperTitle");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }


    @Override
    public int mergeTaskModel(HashMap<String, Object> params) throws DAOException {
        String query = "// 【关系】 Task - TASK_HAS_MODEL -> Model\n" +
                "MERGE (task:Task {name: $taskName})\n" +
                "  ON CREATE SET task.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET task.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (model:Model {name: $modelName})\n" +
                "  ON CREATE SET model.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET model.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (task)-[task_has_model:TASK_HAS_MODEL]->(model)\n" +
                "  ON CREATE SET task_has_model.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET task_has_model.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "taskName", params.get("taskName"),
                        "modelName", params.get("modelName")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeTaskModel failed! task: " + params.get("taskName") + "- model: " + params.get("modelName");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeModelRepo(Map<String, Object> params) throws DAOException {
        String query = "// 【关系】 Model - MODEL_IMPLEMENTED_BY_REPO -> Repo\n" +
                "MERGE (model:Model {name: $modelName})\n" +
                "  ON CREATE SET model.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET model.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (repo:Repository {nameWithOwner: $nameWithOwner})\n" +
                "  ON CREATE SET repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET repo.githubUrl = $githubUrl\n" +
                "MERGE (model)-[implements:MODEL_IMPLEMENTED_BY_REPO]->(repo)\n" +
                "  ON CREATE SET implements.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET implements.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "modelName", params.get("modelName"),
                        "nameWithOwner", ((String) params.get("nameWithOwner")).replaceAll("\\s*", ""),
                        "githubUrl", params.get("githubUrl")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log =
                    "mergeModelRepo failed! modelName: " + params.get("modelName") + " - nameWithOwner: " + params.get("nameWithOwner");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergePaperRepoFromLBPACJson(Map<String, Object> params) throws DAOException {
        String query = "// 【关系】 Paper - PAPER_IMPLEMENTED_BY_REPO -> Repo\n" +
                "// todo 这里有坑，url截取的可能有大小写错误导致match不到\n" +
                "// 可以不区分大小写，所以这里想match repo的话就必须先导入 github\n" +
                "// 这种情况merge repo也不好因为怕大小写不对，后面github merge 不到，只能match\n" +
                "MERGE (paper:Paper {paperTitle: $paperTitle})\n" +
                "  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "WITH paper\n" +
                "MATCH (repo:Repository)\n" +
                "  WHERE repo.nameWithOwner =~ '(?i)' + $nameWithOwner\n" +
                "MERGE (paper)-[link:PAPER_IMPLEMENTED_BY_REPO]->(repo)\n" +
                "  ON CREATE SET link.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET link.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET link.mentionedInPaper = $mentionedInPaper\n" +
                "SET link.mentionedInGithub = $mentionedInGithub\n" +
                "SET link.framework = $framework";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "paperTitle", params.get("paperTitle"),
                        "nameWithOwner", ((String) params.get("nameWithOwner")).replaceAll("\\s*", ""),
                        "mentionedInPaper", params.get("mentionedInPaper"),
                        "mentionedInGithub", params.get("mentionedInGithub"),
                        "framework", params.get("framework")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergePaperRepo failed! paper: " + params.get("paperTitle") + "- repo: " + params.get("nameWithOwner");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeMethodPaperNotExist(Map<String, Object> params) throws DAOException {
        String query = "// if paper not exist\n" +
                "MATCH (method:Method {name: $methodName})\n" +
                "CREATE (new_paper:Paper)\n" +
                "SET new_paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET new_paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET new_paper.dashPaperTitle = $dashPaperTitle\n" +
                "MERGE (method)-[intro:METHOD_INTRODUCED_IN_PAPER]->(new_paper)\n" +
                "  ON CREATE SET intro.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET intro.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "methodName", params.get("methodName"),
                        "dashPaperTitle", params.get("dashPaperTitle")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeMethodPaperNotExist failed! method: " + params.get("methodName") + "- paper: " + params.get("dashPaperTitle");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeTask(Map<String, Object> params) throws DAOException {
        String query = "// Task\n" +
                "MERGE (task:Task {name: $taskName})\n" +
                "  ON CREATE SET task.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET task.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET task.description = $description";
        String taskName = (String) params.get("taskName");
        String description = (String) params.get("description");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters("taskName", taskName, "description", description));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeTask failed! task: " + taskName;
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergePaperFromLBPACJson(Map<String, Object> params) throws DAOException {
        String query = "// 【实体】Paper （links-between-papers-and-code.json）\n" +
                "MERGE (paper:Paper {paperTitle: $paperTitle})\n" +
                "  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.paperswithcodeUrl = $paperswithcodeUrl\n" +
                "SET paper.paperArxivId = $paperArxivId\n" +
                "SET paper.paperUrlAbs = $paperUrlAbs\n" +
                "SET paper.paperUrlPdf = $paperUrlPdf";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "paperswithcodeUrl", params.get("paperswithcodeUrl"),
                        "paperTitle", params.get("paperTitle"),
                        "paperArxivId", params.get("paperArxivId"),
                        "paperUrlAbs", params.get("paperUrlAbs"),
                        "paperUrlPdf", params.get("paperUrlPdf")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergePaper failed! paper: " + params.get("paperTitle");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeMethodFromMethodsJson(Map<String, Object> params) throws DAOException {
        String query = "// 【实体】 Method （methods.json）\n" +
                "MERGE (method:Method {name: $methodName})\n" +
                "  ON CREATE SET method.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET method.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET method.paperswithcodeUrl = $paperswithcodeUrl\n" +
                "SET method.fullName = $fullName\n" +
                "SET method.description = $description\n" +
                "SET method.codeSnippetUrl = $codeSnippetUrl\n" +
                "SET method.introducedYear = $introducedYear";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "paperswithcodeUrl", params.get("paperswithcodeUrl"),
                        "methodName", params.get("methodName"),
                        "fullName", params.get("fullName"),
                        "description", params.get("description"),
                        "codeSnippetUrl", params.get("codeSnippetUrl"),
                        "introducedYear", params.get("introducedYear")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeMethodPaper failed! method: " + params.get("methodName");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeCollectionMethod(Map<String, Object> params) throws DAOException {
        String query = "// 【关系】 Collection - COLLECTION_HAS_METHOD -> Method\n" +
                "MERGE (method:Method {name: $methodName})\n" +
                "  ON CREATE SET method.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET method.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (coll:Collection {name: $collectionName})\n" +
                "  ON CREATE SET coll.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET coll.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (coll)-[has:COLLECTION_HAS_METHOD]->(method)\n" +
                "  ON CREATE SET has.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET has.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "methodName", params.get("methodName"),
                        "collectionName", params.get("collectionName")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeCollectionMethod failed! collection: " + params.get("collectionName") + " - method: " + params.get("methodName");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeAreaCollection(Map<String, Object> params) throws DAOException {
        String query = "// 【关系】 Area - AREA_HAS_COLLECTION -> Collection\n" +
                "MERGE (coll:Collection {name: $collectionName})\n" +
                "  ON CREATE SET coll.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET coll.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (area:Area {area: $areaName})\n" +
                "  ON CREATE SET area.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET area.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET area.areaId = $areaId\n" +
                "MERGE (area)-[has:AREA_HAS_COLLECTION]->(coll)\n" +
                "  ON CREATE SET has.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET has.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "collectionName", params.get("collectionName"),
                        "areaId", params.get("areaId"),
                        "areaName", params.get("areaName")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeAreaCollection failed! area: " + params.get("areaName") + " - collection: " + params.get("collectionName");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergePaperFromPWAJson(Map<String, Object> params) throws DAOException {
        String query = "// 【实体】 Paper （papers-with-abstracts.json）\n" +
                "MERGE (paper:Paper {paperTitle: $paperTitle})\n" +
                "  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.paperswithcodeUrl = $paperswithcodeUrl\n" +
                "SET paper.arxivId = $arxivId\n" +
                "SET paper.abstract = $abstract\n" +
                "SET paper.urlAbs = $urlAbs\n" +
                "SET paper.urlPdf = $urlPdf\n" +
                "SET paper.proceeding = $proceeding\n" +
                "SET paper.date = $date";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "paperswithcodeUrl", params.get("paperswithcodeUrl"),
                        "arxivId", params.get("arxivId"),
                        "paperTitle", params.get("paperTitle"),
                        "abstract", params.get("abstract"),
                        "urlAbs", params.get("urlAbs"),
                        "urlPdf", params.get("urlPdf"),
                        "proceeding", params.get("proceeding"),
                        "date", params.get("date")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergePaperPWAJson failed! paper: " + params.get("paperTitle");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }


    @Override
    public int mergeTaskPaperFromPWAJson(Map<String, Object> params) throws DAOException {
        String query = "// 【关系】【重叠1】Task - TASK_HAS_PAPER -> Paper\n" +
                "MERGE (paper:Paper {paperTitle: $paperTitle})\n" +
                "  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (task:Task {name: $taskName})\n" +
                "  ON CREATE SET task.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET task.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (task)-[has:TASK_HAS_PAPER]->(paper)\n" +
                "  ON CREATE SET has.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET has.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "paperTitle", params.get("paperTitle"),
                        "taskName", params.get("taskName")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeTaskPaper failed! task: " + params.get("taskName") + " - paper: " + params.get("paperTitle");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeMethodFromPWAJson(Map<String, Object> params) throws DAOException {
        String query = "// 【实体】【重叠3】Method （papers-with-abstracts.json）\n" +
                "MERGE (method:Method {name: $methodName})\n" +
                "  ON CREATE SET method.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET method.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET method.fullName = $fullName\n" +
                "SET method.description = $description\n" +
                "SET method.codeSnippetUrl = $codeSnippetUrl\n" +
                "SET method.introducedYear = $introducedYear";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "methodName", params.get("methodName"),
                        "fullName", params.get("fullName"),
                        "description", params.get("description"),
                        "codeSnippetUrl", params.get("codeSnippetUrl"),
                        "introducedYear", params.get("introducedYear")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeMethodPWAJson failed! method: " + params.get("methodName");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeMethodMainCollection(Map<String, Object> params) throws DAOException {
        String query = "// 【关系】 Method - METHOD_MAIN_UNDER_COLLECTION -> Collection\n" +
                "MERGE (method:Method {name: $methodName})\n" +
                "  ON CREATE SET method.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET method.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (coll:Collection {name: $collectionName})\n" +
                "  ON CREATE SET coll.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET coll.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET coll.description = $collectionDescription\n" +
                "MERGE (method)-[main:METHOD_MAIN_UNDER_COLLECTION]->(coll)\n" +
                "  ON CREATE SET main.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET main.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "methodName", params.get("methodName"),
                        "collectionName", params.get("collectionName"),
                        "collectionDescription", params.get("collectionDescription")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeMethodMainCollection failed! method: " + params.get("methodName") + " - collection: " + params.get("collectionName");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public List<String> matchPaperStartWith(String firstToken) throws DAOException {
        String query = "// match paper start with\n" +
                "MATCH (paper:Paper)\n" +
                "  WHERE paper.paperTitle =~ '(?i)" + firstToken + ".*'\n" +
                "RETURN paper.paperTitle";
        try (Session session = driver.session()) {
            return session.readTransaction(tx -> {
                List<String> titles = new ArrayList<>();
                Result result = tx.run(query);
                while (result.hasNext()) {
                    titles.add(result.next().get(0).asString());
                }
                return titles;
            });
        } catch (Exception e) {
            String log = "matchPaperStartWith failed!";
            logger.error(log, e);
            throw new DAOException(log);
        }
    }

    @Override
    public int mergeMethodIntroInPaperExist(Map<String, Object> params) throws DAOException {
        String query = "// if paper exist\n" +
                "MERGE (method:Method {name: $methodName})\n" +
                "  ON CREATE SET method.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET method.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (paper:Paper {paperTitle: $existPaperTitle})\n" +
                "  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.dashPaperTitle = $dashPaperTitle\n" +
                "MERGE (method)-[intro:METHOD_INTRODUCED_IN_PAPER]->(paper)\n" +
                "  ON CREATE SET intro.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET intro.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "methodName", params.get("methodName"),
                        "existPaperTitle", params.get("existPaperTitle"),
                        "dashPaperTitle", params.get("dashPaperTitle")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log =
                    "mergeMethodIntroInPaperExist failed! method: " + params.get("methodName") + " - paper: " + params.get("existPaperTitle");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergePaperUsesMethod(Map<String, Object> params) throws DAOException {
        String query = "// 【关系】 Paper - PAPER_USES_METHOD -> Method\n" +
                "MERGE (paper:Paper {paperTitle: $paperTitle})\n" +
                "  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (method:Method {name: $methodName})\n" +
                "  ON CREATE SET method.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET method.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (paper)-[use:PAPER_USES_METHOD]-(method)\n" +
                "  ON CREATE SET use.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET use.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "paperTitle", params.get("paperTitle"),
                        "methodName", params.get("methodName")
                ));
                return 1;
            });
        } catch (Exception e) {
            String log =
                    "mergePaperUsesMethod failed! paper: " + params.get("paperTitle") + " - method: " + params.get(
                            "methodName");
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public String mergeTaskPaperFromETJson(String taskName, String paperTitle) throws DAOException {
        String query = "//【+关系】 Task - TASK_HAS_PAPER -> Paper\n" +
                "MERGE (task:Task {name: $taskName})\n" +
                "  ON CREATE SET task.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET task.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (paper:Paper {paperTitle: $paperTitle})\n" +
                "  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (task)-[predicate:TASK_HAS_PAPER]->(paper)\n" +
                "  ON CREATE SET predicate.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET predicate.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "taskName", taskName,
                        "paperTitle", paperTitle
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeTaskPaperFromETJson failed! task: " + taskName + " - paperTitle: " + paperTitle;
            logger.error(log, e);
            throw new DAOException(log);
        }
        return "success";
    }

    @Override
    public String mergePaperRepoFromETJson(String paperTitle, String nameWithOwner, String githubUrl) throws DAOException {
        String query = "// 【+关系】 Paper - PAPER_IMPLEMENTED_BY_REPO -> Repo\n" +
                "MERGE (paper:Paper {paperTitle: $paperTitle})\n" +
                "  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (repo:Repository {nameWithOwner: $nameWithOwner})\n" +
                "  ON CREATE SET repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET repo.githubUrl = $githubUrl\n" +
                "MERGE (paper)-[predicate:PAPER_IMPLEMENTED_BY_REPO]->(repo)\n" +
                "  ON CREATE SET predicate.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET predicate.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "paperTitle", paperTitle,
                        "nameWithOwner", nameWithOwner,
                        "githubUrl", githubUrl
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergePaperRepoFromETJson failed! paperTitle: " + paperTitle + " - nameWithOwner: " + nameWithOwner;
            logger.error(log, e);
            throw new DAOException(log);
        }
        return "success";
    }
}
