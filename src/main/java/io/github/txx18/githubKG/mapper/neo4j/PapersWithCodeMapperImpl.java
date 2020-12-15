package io.github.txx18.githubKG.mapper.neo4j;

import io.github.txx18.githubKG.exception.DAOException;
import io.github.txx18.githubKG.mapper.PapersWithCodeMapper;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
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
    public int mergeTaskCategory(Map<String, Object> params) throws DAOException {
        String query = "MERGE (task:Task {name: $taskName})\n" +
                "  ON CREATE SET task.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET task.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET task.description = $description\n" +
                "MERGE (category:Category {name: $category})\n" +
                "  ON CREATE SET category.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET category.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (task)-[under:TASK_UNDER_CATEGORY]->(category)\n" +
                "  ON CREATE SET under.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET under.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        String taskName = (String) params.get("taskName");
        String category = (String) params.get("category");
        String description = (String) params.get("description");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters("taskName", taskName, "category", category, "description", description));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeTaskCategory failed! task: " + taskName + " - category: " + category;
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeTaskSubtask(Map<String, Object> params) throws DAOException {
        String query = "MATCH (task:Task {name: $taskName})\n" +
                "MERGE (subtask:Task {name: $subtaskName})\n" +
                "  ON CREATE SET subtask.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET subtask.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET subtask.description = $description\n" +
                "MERGE (task)-[has_subtask:TASK_HAS_SUBTASK]->(subtask)\n" +
                "  ON CREATE SET has_subtask.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET has_subtask.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        String taskName = (String) params.get("taskName");
        String subtaskName = (String) params.get("subtaskName");
        String description = (String) params.get("description");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters("taskName", taskName, "subtaskName", subtaskName, "description", description));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeTaskSubtask failed! task: " + taskName + "- subtask: " + subtaskName;
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeTaskDataset(Map<String, Object> params) throws DAOException {
        String query = "// Task 和 Dataset的关系\n" +
                "MATCH (task:Task {name: $taskName})\n" +
                "MERGE (dataset:Dataset {name: $datasetName})\n" +
                "  ON CREATE SET dataset.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET dataset.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET dataset.description = $description\n" +
                "MERGE (task)-[has_dataset:TASK_HAS_DATASET]->(dataset)\n" +
                "  ON CREATE SET has_dataset.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET has_dataset.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        String taskName = (String) params.get("taskName");
        String datasetName = (String) params.get("datasetName");
        String description = (String) params.get("description");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters("taskName", taskName, "datasetName", datasetName, "description", description));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeTaskDataset failed! task: " + taskName + " - dataset: " + datasetName;
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int createModelDataset(HashMap<String, Object> params) throws DAOException {
        String query = "MATCH (dataset:Dataset {name: $datasetName})\n" +
                "MERGE (model:Model {name: $modelName})\n" +
                "MERGE (model)-[model_on_dataset:MODEL_ON_DATASET {metricName: $metricName}]->(dataset)\n" +
                "  ON CREATE SET model_on_dataset.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET model_on_dataset.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET model_on_dataset.metricValue = $metricValue";
        String datasetName = (String) params.get("datasetName");
        String modelName = (String) params.get("modelName");
        String metricName = (String) params.get("metricName");
        String metricValue = (String) params.get("metricValue");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters("modelName", modelName, "datasetName", datasetName,
                        "metricName", metricName, "metricValue", metricValue));
                return 1;
            });
        } catch (Exception e) {
            String log =
                    "mergeModelDataset failed! model: " + modelName + " - dataset: " + datasetName;
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeModelPaper(HashMap<String, Object> params) throws DAOException {
        String query = "MATCH (model:Model {name: $modelName})\n" +
                "MERGE (paper:Paper {paperUrl: $paperUrl})\n" +
                "  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.paperTitle = $paperTitle\n" +
                "SET paper.paperDate = $paperDate\n" +
                "MERGE (model)-[model_in_paper:MODEL_IN_PAPER]->(paper)\n" +
                "  ON CREATE SET model_in_paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET model_in_paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        String modelName = (String) params.get("modelName");
        String paperUrl = (String) params.get("paperUrl");
        String paperTitle = (String) params.get("paperTitle");
        String paperDate = (String) params.get("paperDate");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters("modelName", modelName, "paperUrl", paperUrl, "paperTitle", paperTitle,
                        "paperDate", paperDate));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeModelPaper failed! model: " + modelName + " - paper: " + paperUrl;
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }


    @Override
    public int mergeTaskModel(HashMap<String, Object> params) throws DAOException {
        String query = "//Task & Model 的关系\n" +
                "MATCH (task:Task {name: $taskName})\n" +
                "MERGE (model:Model {name: $modelName})\n" +
                "  ON CREATE SET model.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET model.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (task)-[task_has_model:TASK_HAS_MODEL]->(model)\n" +
                "  ON CREATE SET task_has_model.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET task_has_model.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        String taskName = (String) params.get("taskName");
        String modelName = (String) params.get("modelName");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters("taskName", taskName, "modelName", modelName));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeTaskModel failed! task: " + taskName + "- model: " + modelName;
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeModelRepo(Map<String, Object> params) throws DAOException {
        String query = "// Model - MODEL_IMPLEMENTS_BY_REPO - Repo\n" +
                "MATCH (model:Model {name: $modelName})\n" +
                "MERGE (repo:Repo {nameWithOwner: $nameWithOwner})\n" +
                "  ON CREATE SET repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (model)-[implements:MODEL_IMPLEMENTS_BY_REPO]->(repo)\n" +
                "  ON CREATE SET implements.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET implements.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        String modelName = (String) params.get("modelName");
        String nameWithOwner = (String) params.get("nameWithOwner");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters("modelName", modelName, "nameWithOwner", nameWithOwner));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeModelRepo failed! modelName: " + modelName + "- nameWithOwner: " + nameWithOwner;
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergePaperRepo(Map<String, Object> params) throws DAOException {
        String query = "//Paper - PAPER_LINKS_TO_REPO - Repo\n" +
                "MERGE (paper:Paper {paperUrl: $paperUrlAbs})\n" +
                "  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.paperTitle = $paperTitle\n" +
                "SET paper.paperswithcodeUrl = $paperswithcodeUrl\n" +
                "SET paper.paperArxivId = $paperArxivId\n" +
                "SET paper.paperUrlAbs = $paperUrlAbs\n" +
                "SET paper.paperUrlPdf = $paperUrlPdf\n" +
                "MERGE (repo:Repo {nameWithOwner: $nameWithOwner})\n" +
                "  ON CREATE SET repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (paper)-[link:PAPER_LINKS_TO_REPO]->(repo)\n" +
                "  ON CREATE SET link.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET link.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET link.mentionedInPaper = $mentionedInPaper\n" +
                "SET link.mentionedInGithub = $mentionedInGithub\n" +
                "SET link.framework = $framework";
        String paperswithcodeUrl = (String) params.get("paperswithcodeUrl");
        String paperTitle = (String) params.get("paperTitle");
        String paperArxivId = (String) params.get("paperArxivId");
        String paperUrlAbs = (String) params.get("paperUrlAbs");
        String paperUrlPdf = (String) params.get("paperUrlPdf");
        String nameWithOwner = (String) params.get("nameWithOwner");
        String mentionedInPaper = (String) params.get("mentionedInPaper");
        String mentionedInGithub = (String) params.get("mentionedInGithub");
        String framework = (String) params.get("framework");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "paperswithcodeUrl", paperswithcodeUrl,
                        "paperTitle", paperTitle,
                        "paperTitle", paperTitle,
                        "paperArxivId", paperArxivId,
                        "paperUrlAbs", paperUrlAbs,
                        "paperUrlPdf", paperUrlPdf,
                        "nameWithOwner", nameWithOwner,
                        "mentionedInPaper", mentionedInPaper,
                        "mentionedInGithub", mentionedInGithub,
                        "framework", framework
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergePaperRepo failed! paper: " + paperUrlAbs + "- repo: " + nameWithOwner;
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }
}
