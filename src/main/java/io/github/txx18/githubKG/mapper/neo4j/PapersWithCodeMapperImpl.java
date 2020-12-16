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
    public int mergeCategoryTask(Map<String, Object> params) throws DAOException {
        String query = "// Category - CATEGORY_HAS_TASK -> Task\n" +
                "MATCH (task:Task {name: $taskName})\n" +
                "MERGE (category:Category {name: $category})\n" +
                "  ON CREATE SET category.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET category.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (category)-[has:CATEGORY_HAS_TASK]->(task)\n" +
                "  ON CREATE SET has.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET has.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        String taskName = (String) params.get("taskName");
        String category = (String) params.get("category");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters("taskName", taskName, "category", category));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeCategoryTask failed! category: " + category + "task: " + taskName;
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
        String query = "// model - MODEL_INTRODUCED_IN_PAPER -> paper\n" +
                "MATCH (model:Model {name: $modelName})\n" +
                "MERGE (paper:Paper {paperTitle: $paperTitle})\n" +
                "  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.paperUrl = $paperUrl\n" +
                "SET paper.paperDate = $paperDate\n" +
                "MERGE (model)-[model_in_paper:MODEL_INTRODUCED_IN_PAPER]->(paper)\n" +
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
        String query = "// Model - MODEL_IMPLEMENTED_BY_REPO -> Repo\n" +
                "MATCH (model:Model {name: $modelName})\n" +
                "MERGE (repo:Repo {nameWithOwner: $nameWithOwner})\n" +
                "  ON CREATE SET repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (model)-[implements:MODEL_IMPLEMENTED_BY_REPO]->(repo)\n" +
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
        String query = "// Paper - PAPER_IMPLEMENTED_BY_REPO -> Repo\n" +
                "MATCH (paper:Paper {paperTitle: $paperTitle})\n" +
                "MERGE (repo:Repo {nameWithOwner: $nameWithOwner})\n" +
                "  ON CREATE SET repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (paper)-[link:PAPER_IMPLEMENTED_BY_REPO]->(repo)\n" +
                "  ON CREATE SET link.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET link.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET link.mentionedInPaper = $mentionedInPaper\n" +
                "SET link.mentionedInGithub = $mentionedInGithub\n" +
                "SET link.framework = $framework";
        String paperTitle = (String) params.get("paperTitle");
        String nameWithOwner = (String) params.get("nameWithOwner");
        String mentionedInPaper = (String) params.get("mentionedInPaper");
        String mentionedInGithub = (String) params.get("mentionedInGithub");
        String framework = (String) params.get("framework");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "paperTitle", paperTitle,
                        "nameWithOwner", nameWithOwner,
                        "mentionedInPaper", mentionedInPaper,
                        "mentionedInGithub", mentionedInGithub,
                        "framework", framework
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergePaperRepo failed! paper: " + paperTitle + "- repo: " + nameWithOwner;
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeMethodPaper(Map<String, Object> params) throws DAOException {
        String query = "// Method - METHOD_INTRODUCED_IN_PAPER -> Paper\n" +
                "MATCH (method:Method {name: $name})\n" +
                "MERGE (paper:Paper {paperTitle: $paperTitle})\n" +
                "  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (method)-[intro:METHOD_INTRODUCED_IN_PAPER]->(paper)\n" +
                "  ON CREATE SET intro.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET intro.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        String name = (String) params.get("name");
        String paperTitle = (String) params.get("paperTitle");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "name", name,
                        "paperTitle", paperTitle
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeMethodPaper failed! method: " + name + "- paper: " + paperTitle;
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
    public int mergePaperLBPACJson(Map<String, Object> params) throws DAOException {
        String query = "// Paper\n" +
                "MERGE (paper:Paper {paperTitle: $paperTitle})\n" +
                "  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET paper.paperswithcodeUrl = $paperswithcodeUrl\n" +
                "SET paper.paperArxivId = $paperArxivId\n" +
                "SET paper.paperUrlAbs = $paperUrlAbs\n" +
                "SET paper.paperUrlPdf = $paperUrlPdf";
        String paperswithcodeUrl = (String) params.get("paperswithcodeUrl");
        String paperTitle = (String) params.get("paperTitle");
        String paperArxivId = (String) params.get("paperArxivId");
        String paperUrlAbs = (String) params.get("paperUrlAbs");
        String paperUrlPdf = (String) params.get("paperUrlPdf");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "paperswithcodeUrl", paperswithcodeUrl,
                        "paperTitle", paperTitle,
                        "paperArxivId", paperArxivId,
                        "paperUrlAbs", paperUrlAbs,
                        "paperUrlPdf", paperUrlPdf
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergePaper failed! paper: " + paperTitle;
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeMethodMethodsJson(Map<String, Object> params) throws DAOException {
        String query = "// Method\n" +
                "MERGE (method:Method {name: $name})\n" +
                "  ON CREATE SET method.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET method.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET method.paperswithcodeUrl = $paperswithcodeUrl\n" +
                "SET method.fullName = $fullName\n" +
                "SET method.description = $description\n" +
                "SET method.codeSnippetUrl = $codeSnippetUrl\n" +
                "SET method.introducedYear = $introducedYear";
        String paperswithcodeUrl = (String) params.get("paperswithcodeUrl");
        String name = (String) params.get("name");
        String fullName = (String) params.get("fullName");
        String description = (String) params.get("description");
        String codeSnippetUrl = (String) params.get("codeSnippetUrl");
        String introducedYear = (String) params.get("introducedYear");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "paperswithcodeUrl", paperswithcodeUrl,
                        "name", name,
                        "fullName", fullName,
                        "description", description,
                        "codeSnippetUrl", codeSnippetUrl,
                        "introducedYear", introducedYear
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeMethodPaper failed! method: " + name;
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeCollectionMethod(Map<String, Object> params) throws DAOException {
        String query = "// Collection - COLLECTION_HAS_METHOD -> Method\n" +
                "MATCH (method:Method {name: $name})\n" +
                "MERGE (coll:Collection {name: $collectionName})\n" +
                "  ON CREATE SET coll.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET coll.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (coll)-[has:COLLECTION_HAS_METHOD]->(method)\n" +
                "  ON CREATE SET has.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET has.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        String methodName = (String) params.get("name");
        String collectionName = (String) params.get("collectionName");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "name", methodName,
                        "collectionName", collectionName
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeCollectionMethod failed! collection: " + collectionName + " - method: " + methodName;
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeAreaCollection(Map<String, Object> params) throws DAOException {
        String query = "// Area - AREA_HAS_COLLECTION -> Collection\n" +
                "MATCH (coll:Collection {name: $collectionName})\n" +
                "MERGE (area:Area {area: $areaName})\n" +
                "  ON CREATE SET area.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET area.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET area.areaId = $areaId\n" +
                "MERGE (area)-[has:AREA_HAS_COLLECTION]->(coll)\n" +
                "  ON CREATE SET has.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET has.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        String collectionName = (String) params.get("collectionName");
        String areaId = (String) params.get("areaId");
        String areaName = (String) params.get("areaName");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "collectionName", collectionName,
                        "areaId", areaId,
                        "areaName", areaName
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeAreaCollection failed! area: " + areaName + " - collection: " + collectionName;
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergePaperPWAJson(Map<String, Object> params) throws DAOException {
        String query = "// Paper （PWAJson）\n" +
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
        String paperswithcodeUrl = (String) params.get("paperswithcodeUrl");
        String arxivId = (String) params.get("arxivId");
        String paperTitle = (String) params.get("paperTitle");
        String paperAbstract = (String) params.get("abstract");
        String urlAbs = (String) params.get("urlAbs");
        String urlPdf = (String) params.get("urlPdf");
        String proceeding = (String) params.get("proceeding");
        String date = (String) params.get("date");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "paperswithcodeUrl", paperswithcodeUrl,
                        "arxivId", arxivId,
                        "paperTitle", paperTitle,
                        "abstract", paperAbstract,
                        "urlAbs", urlAbs,
                        "urlPdf", urlPdf,
                        "proceeding", proceeding,
                        "date", date
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergePaperPWAJson failed! paper: " + paperTitle;
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergePaperAuthor(Map<String, Object> params) throws DAOException {
        String query = "// Paper - PAPER_WRITTEN_BY_AUTHOR -> Author\n" +
                "MATCH (paper:Paper {paperTitle: $paperTitle})\n" +
                "MERGE (author:Author {name: $authorName})\n" +
                "  ON CREATE SET author.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET author.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (paper)-[written:PAPER_WRITTEN_BY_AUTHOR]->(author)\n" +
                "  ON CREATE SET written.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET written.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        String paperTitle = (String) params.get("paperTitle");
        String authorName = (String) params.get("authorName");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "paperTitle", paperTitle,
                        "authorName", authorName
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergePaperAuthor failed! paper: " + paperTitle + " - author: " + authorName;
            logger.error(log, e);
            throw new DAOException(log)
                    ;
        }
        return 1;
    }

    @Override
    public int mergeTaskPaper(Map<String, Object> params) throws DAOException {
        String query = "// Task - TASK_HAS_PAPER -> Paper\n" +
                "MATCH (paper:Paper {paperTitle: $paperTitle})\n" +
                "MERGE (task:Task {name: $taskName})\n" +
                "  ON CREATE SET task.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET task.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "MERGE (task)-[has:TASK_HAS_PAPER]->(paper)\n" +
                "  ON CREATE SET has.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET has.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        String paperTitle = (String) params.get("paperTitle");
        String taskName = (String) params.get("taskName");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "paperTitle", paperTitle,
                        "taskName", taskName
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeTaskPaper failed! task: " + taskName + " - paper: " + paperTitle;
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeMethodPWAJson(Map<String, Object> params) throws DAOException {
        String query = "// Method （PWAJson）\n" +
                "MERGE (method:Method {name: $name})\n" +
                "  ON CREATE SET method.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET method.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET method.fullName = $fullName\n" +
                "SET method.description = $description\n" +
                "SET method.codeSnippetUrl = $codeSnippetUrl\n" +
                "SET method.introducedYear = $introducedYear";
        String name = (String) params.get("name");
        String fullName = (String) params.get("fullName");
        String description = (String) params.get("description");
        String codeSnippetUrl = (String) params.get("codeSnippetUrl");
        String introducedYear = (String) params.get("introducedYear");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "name", name,
                        "fullName", fullName,
                        "description", description,
                        "codeSnippetUrl", codeSnippetUrl,
                        "introducedYear", introducedYear
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeMethodPWAJson failed! method: " + name;
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }

    @Override
    public int mergeMethodMainCollection(Map<String, Object> params) throws DAOException {
        String query = "// Method - Method_MAIN_UNDER_COLLECTION -> Collection\n" +
                "MATCH (method:Method {name: $name})\n" +
                "MERGE (coll:Collection {name: $collectionName})\n" +
                "  ON CREATE SET coll.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET coll.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET coll.description = $collectionDescription\n" +
                "MERGE (method)-[main:Method_MAIN_UNDER_COLLECTION]->(coll)\n" +
                "  ON CREATE SET main.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')\n" +
                "SET main.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')";
        String name = (String) params.get("name");
        String collectionName = (String) params.get("collectionName");
        String collectionDescription = (String) params.get("collectionDescription");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(query, parameters(
                        "name", name,
                        "collectionName", collectionName,
                        "collectionDescription", collectionDescription
                ));
                return 1;
            });
        } catch (Exception e) {
            String log = "mergeMethodMainCollection failed! method: " + name + " - collection: " + collectionName;
            logger.error(log, e);
            throw new DAOException(log);
        }
        return 1;
    }
}
