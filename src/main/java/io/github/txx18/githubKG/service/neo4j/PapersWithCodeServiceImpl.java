package io.github.txx18.githubKG.service.neo4j;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.txx18.githubKG.exception.DAOException;
import io.github.txx18.githubKG.mapper.PapersWithCodeMapper;
import io.github.txx18.githubKG.service.PapersWithCodeService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * @author ShaneTang
 * @create 2020-12-12 17:34
 */
@Service
public class PapersWithCodeServiceImpl implements PapersWithCodeService {

    private final PapersWithCodeMapper papersWithCodeMapper;

    public PapersWithCodeServiceImpl(PapersWithCodeMapper papersWithCodeMapper) {
        this.papersWithCodeMapper = papersWithCodeMapper;
    }


    /**
     * 导入evaluation-tables.json
     * <p>
     * 这次采取另一种解析思路，在程序里分步解析，因为Cypher太难用了，比如判空逻辑
     * 但是，一个Cypher搞定的好处是，开销小效率高，因为每次 开session transaction是很慢的
     * <p>
     * 调用树：平级则并列，嵌套则嵌套调用
     *
     * @param filePath
     * @return
     */
    @Override
    public int importEvaluationTablesJson(String filePath) throws Exception {
        JSONArray jsonArray = (JSONArray) JSONUtil.readJSON(new File(filePath), StandardCharsets.UTF_8);
        return mergeTaskUnion(jsonArray);
    }

    /**
     * Paper - PAPER_IMPLEMENTED_BY_REPO -> Repo
     *
     * @param jsonArray
     * @return
     */
    @Override
    public int importLinksBetweenPapersAndCodeJson(String filePath) throws DAOException {
        JSONArray jsonArray = (JSONArray) JSONUtil.readJSON(new File(filePath), StandardCharsets.UTF_8);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            String paperTitle = (String) jsonObject.get("paper_title");
            // paperTitle为空则跳过，否则先merge paper
            if (StrUtil.isBlankIfStr(paperTitle)) {
                continue;
            }
            String paperswithcodeUrl = (String) jsonObject.get("paper_url");
            String paperArxivId = (String) jsonObject.get("paper_arxiv_id");
            String paperUrlAbs = (String) jsonObject.get("paper_url_abs");
            String paperUrlPdf = (String) jsonObject.get("paper_url_pdf");
            HashMap<String, Object> params = new HashMap<>();
            params.put("paperswithcodeUrl", paperswithcodeUrl);
            params.put("paperTitle", paperTitle);
            params.put("paperArxivId", paperArxivId);
            params.put("paperUrlAbs", paperUrlAbs);
            params.put("paperUrlPdf", paperUrlPdf);
            int resPaper = papersWithCodeMapper.mergePaperLBPACJson(params);
            System.out.println("mergePaper: " + i + "/" + jsonArray.size());
            String repoUrl = (String) jsonObject.get("repo_url");
            // repoUrl为空则跳过
            if (StrUtil.isBlankIfStr(repoUrl)) {
                continue;
            }
            String[] tokens = StrUtil.split(repoUrl, "/");
            String nameWithOwner = tokens[3] + "/" + tokens[4];
            boolean mentionedInPaper = (boolean) jsonObject.get("mentioned_in_paper");
            boolean mentionedInGithub = (boolean) jsonObject.get("mentioned_in_github");
            String framework = (String) jsonObject.get("framework");
            params.put("nameWithOwner", nameWithOwner);
            params.put("mentionedInPaper", mentionedInPaper);
            params.put("mentionedInGithub", mentionedInGithub);
            params.put("framework", framework);
            int res = papersWithCodeMapper.mergePaperRepo(params);
            System.out.println("mergePaperRepo: " + i + "/" + jsonArray.size());
        }
        return 1;
    }

    /**
     * Method
     * Method - METHOD_INTRODUCED_IN_PAPER -> Paper
     * Collection - COLLECTION_HAS_METHOD -> Method
     * Area - AREA_HAS_COLLECTION -> Collection
     *
     * @param filePath
     * @return
     * @throws DAOException
     */
    @Override
    public int importMethodsJson(String filePath) throws DAOException {
        JSONArray jsonArray = (JSONArray) JSONUtil.readJSON(new File(filePath), StandardCharsets.UTF_8);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            String name = (String) jsonObject.get("name");
            if (StrUtil.isBlankIfStr(name)) {
                continue;
            }
            String paperswithcodeUrl = (String) jsonObject.get("url");
            String fullName = (String) jsonObject.get("full_name");
            String description = (String) jsonObject.get("description");
            String codeSnippetUrl = (String) jsonObject.get("code_snippet_url");
            String introducedYear = jsonObject.get("introduced_year").toString();
            Map<String, Object> params = new HashMap<>();
            params.put("paperswithcodeUrl", paperswithcodeUrl);
            params.put("name", name);
            params.put("fullName", fullName);
            params.put("description", description);
            params.put("codeSnippetUrl", codeSnippetUrl);
            params.put("introducedYear", introducedYear);
            int resMethod = papersWithCodeMapper.mergeMethodMethodsJson(params);
            String paperTitle = (String) jsonObject.get("paper");
            // paperTitle不为空才执行
            if (!StrUtil.isBlankIfStr(paperTitle)) {
                params.put("paperTitle", paperTitle);
                int resMethodPaper = papersWithCodeMapper.mergeMethodPaper(params);
            }
            JSONArray collections = ((JSONArray) jsonObject.get("collections"));
            for (int j = 0; j < collections.size(); j++) {
                JSONObject collection = (JSONObject) collections.get(j);
                String collectionName = (String) collection.get("collection");
                // collectionName不为空才执行
                if (!StrUtil.isBlankIfStr(collectionName)) {
                    params.put("collectionName", collectionName);
                    int resCollectionMethod = papersWithCodeMapper.mergeCollectionMethod(params);
                    String areaName = (String) collection.get("area");
                    // area不为空才执行
                    if (!StrUtil.isBlankIfStr(areaName)) {
                        String areaId = (String) collection.get("area_id");
                        params.put("areaId", areaId);
                        params.put("areaName", areaName);
                        int resAreaColleciton = papersWithCodeMapper.mergeAreaCollection(params);
                    }
                }
            }
            System.out.println("importMethodsJson: " + i + "/" + jsonArray.size());
        }
        return 1;
    }

    /**
     * Paper （PWAJson）
     * Paper - PAPER_WRITTEN_BY_AUTHOR -> Author
     * Task - TASK_HAS_PAPER -> Paper
     * Paper - Paper_USES_METHOD -> Method
     * Method - Method_MAIN_UNDER_COLLECTION -> Collection
     * Area - AREA_HAS_COLLECTION -> Collection
     *
     * @param filePath
     * @return
     */
    @Override
    public int importPapersWithAbstractJson(String filePath) throws DAOException {
        JSONArray jsonArray = (JSONArray) JSONUtil.readJSON(new File(filePath), StandardCharsets.UTF_8);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            String paperTitle = (String) jsonObject.get("title");
            // paperTitle为空则跳过
            if (StrUtil.isBlankIfStr(paperTitle)) {
                continue;
            }
            String paperswithcodeUrl = (String) jsonObject.get("paper_url");
            String arxivId = (String) jsonObject.get("arxiv_id");
            String paperAbstract = (String) jsonObject.get("abstract");
            String urlAbs = (String) jsonObject.get("url_abs");
            String urlPdf = (String) jsonObject.get("url_pdf");
            String proceeding = (String) jsonObject.get("proceeding");
            String date = (String) jsonObject.get("date");
            HashMap<String, Object> params = new HashMap<>();
            params.put("paperswithcodeUrl", paperswithcodeUrl);
            params.put("arxivId", arxivId);
            params.put("paperTitle", paperTitle);
            params.put("abstract", paperAbstract);
            params.put("urlAbs", urlAbs);
            params.put("urlPdf", urlPdf);
            params.put("proceeding", proceeding);
            params.put("date", date);
            int resPaper = papersWithCodeMapper.mergePaperPWAJson(params);
            JSONArray authors = (JSONArray) jsonObject.get("authors");
            for (int j = 0; j < authors.size(); j++) {
                String authorName = (String) authors.get(j);
                // authorName 为空则跳过
                if (StrUtil.isBlankIfStr(authorName)) {
                    continue;
                }
                params.put("authorName", authorName);
                int resPaperAuthor = papersWithCodeMapper.mergePaperAuthor(params);
            }
            JSONArray tasks = (JSONArray) jsonObject.get("tasks");
            for (int j = 0; j < tasks.size(); j++) {
                String taskName = (String) tasks.get(j);
                // taskName 为空则跳过
                if (StrUtil.isBlankIfStr(taskName)) {
                    continue;
                }
                params.put("taskName", taskName);
                int resTaskPaper = papersWithCodeMapper.mergeTaskPaper(params);
            }
            JSONArray methods = (JSONArray) jsonObject.get("methods");
            for (int j = 0; j < methods.size(); j++) {
                JSONObject method = (JSONObject) methods.get(j);
                String name = (String) jsonObject.get("name");
                if (StrUtil.isBlankIfStr(name)) {
                    continue;
                }
                String fullName = (String) jsonObject.get("full_name");
                String description = (String) jsonObject.get("description");
                String codeSnippetUrl = (String) jsonObject.get("code_snippet_url");
                String introducedYear = jsonObject.get("introduced_year").toString();
                params.put("name", name);
                params.put("fullName", fullName);
                params.put("description", description);
                params.put("codeSnippetUrl", codeSnippetUrl);
                params.put("introducedYear", introducedYear);
                int resMethod = papersWithCodeMapper.mergeMethodPWAJson(params);
                JSONObject collection = (JSONObject) method.get("main_collection");
                String collectionName = (String) collection.get("name");
                // collectionName不为空才执行
                if (!StrUtil.isBlankIfStr(collectionName)) {
                    String collectionDescription = (String) collection.get("description");
                    params.put("collectionName", collectionName);
                    params.put("collectionDescription", collectionDescription);
                    int resMethodMainCollection = papersWithCodeMapper.mergeMethodMainCollection(params);
                    String areaName = (String) collection.get("area");
                    // area不为空才执行
                    if (!StrUtil.isBlankIfStr(areaName)) {
                        params.put("areaName", areaName);
                        int resAreaColleciton = papersWithCodeMapper.mergeAreaCollection(params);
                    }
                }
            }
            System.out.println("importPapersWithAbstractJson: " + i + "/" + jsonArray.size());
        }
        return 1;
    }


    /**
     * JSON最外层是以Task出发的
     *
     * @param jsonArray
     * @return
     * @throws Exception
     */
    private int mergeTaskUnion(JSONArray jsonArray) throws Exception {
        // 第一层JSONArray
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            // Task 和 Category的关系
            int resTaskCategory = mergeCategoryTask(jsonObject);
            if (resTaskCategory == 0) {
                continue;
            }
            // Task & Dataset & Model & Paper & Metric & Repo的关系
            int resDataset = mergeDatasetUnion(jsonObject);
            // Task 和 Subtasks的关系 subtask是task的递归嵌套
            int resSubtask = mergeSubtaskUnion(jsonObject);
            System.out.println("mergeTask: " + i + "/" + jsonArray.size());
        }
        return 1;
    }


    /**
     * Task - TASK_HAS_MODEL -> Model
     * 'model_links' 都为[]
     *
     * @param jsonObject
     * @param dataset
     * @return
     * @throws DAOException
     */
    private int mergeModelPaperRepoUnion(JSONObject jsonObject, JSONObject dataset) throws DAOException {
        String taskName = (String) jsonObject.get("task");
        JSONArray rows = (JSONArray) ((JSONObject) dataset.get("sota")).get("rows");
        String datasetName = (String) dataset.get("dataset");
        for (int i = 0; i < rows.size(); i++) {
            JSONObject model = (JSONObject) rows.get(i);
            String modelName = (String) model.get("model_name");
            // modelName为空则跳过
            if (StrUtil.isBlankIfStr(modelName)) {
                continue;
            }
            HashMap<String, Object> params = new HashMap<>();
            params.put("taskName", taskName);
            params.put("modelName", modelName);
            int res1 = papersWithCodeMapper.mergeTaskModel(params);
            JSONObject metrics = (JSONObject) model.get("metrics");
            Set<Map.Entry<String, Object>> metricEntries = metrics.entrySet();
            params.put("datasetName", datasetName);
            for (Map.Entry<String, Object> metricEntry : metricEntries) {
                String metricName = metricEntry.getKey();
                String metricValue = metricEntry.getValue().toString();
                params.put("metricName", metricName);
                params.put("metricValue", metricValue);
                // model&dataset都保证存在，顺便merge了model
                int res2 = papersWithCodeMapper.createModelDataset(params);
            }
            // 即将merge的paper和repo是平级的，不能随便卫语句跳出for循环，不然另一个就忽略了，通过单if语句控制逻辑
            String paperTitle = (String) model.get("paper_title");
            // paper不为空才执行这一段，否则继续执行repo段
            if (!StrUtil.isBlankIfStr(paperTitle)) {
                String paperUrl = (String) model.get("paper_url");
                String paperDate = (String) model.get("paper_date");
                params.put("paperTitle", paperTitle);
                params.put("paperUrl", paperUrl);
                params.put("paperDate", paperDate);
                int res3 = papersWithCodeMapper.mergeModelPaper(params);
            }
            JSONArray codeLinks = (JSONArray) model.get("code_links");
            for (int j = 0; j < codeLinks.size(); j++) {
                JSONObject codeLink = (JSONObject) codeLinks.get(j);
                String nameWithOwner = (String) codeLink.get("title");
                // repo为空则跳过
                if (StrUtil.isBlankIfStr(nameWithOwner)) {
                    continue;
                }
                params.put("nameWithOwner", nameWithOwner);
                int res4 = papersWithCodeMapper.mergeModelRepo(params);
            }
        }
        return 1;
    }


    /**
     * Task - TASK_HAS_DATASET -> Dataset
     * uniom
     * 字段 'dataset_citations' 'dataset_links' 'subdatasets'都为[]
     *
     * @param jsonObject
     * @return
     */
    private int mergeDatasetUnion(JSONObject jsonObject) throws DAOException {
        // task 已经merge过了
        String taskName = (String) jsonObject.get("task");
        JSONArray datasets = (JSONArray) jsonObject.get("datasets");
        for (int i = 0; i < datasets.size(); i++) {
            JSONObject dataset = (JSONObject) datasets.get(i);
            String datasetName = (String) dataset.get("dataset");
            // dataset为空则跳过
            if (StrUtil.isBlankIfStr(datasetName)) {
                continue;
            }
            String description = (String) dataset.get("description");
            Map<String, Object> params = new HashMap<>();
            params.put("taskName", taskName);
            params.put("datasetName", datasetName);
            params.put("description", description);
            int res = papersWithCodeMapper.mergeTaskDataset(params);
            // 嵌套关系，如果要分方法写的话，也嵌套调用
            int resModel = mergeModelPaperRepoUnion(jsonObject, dataset);
        }
        return 1;
    }


    /**
     * Task - TASK_HAS_SUBTASK -> Subtask
     * subtask是task的递归嵌套
     * <p>
     * 字段'source_link'都为null
     *
     * @param jsonObject
     * @return
     * @throws DAOException
     */
    private int mergeSubtaskUnion(JSONObject jsonObject) throws Exception {
        String taskName = (String) jsonObject.get("task");
        JSONArray subtasks = (JSONArray) jsonObject.get("subtasks");
        // 没有subtasks则递归结束
        if (subtasks.isEmpty()) {
            return 1;
        }
        for (int j = 0; j < subtasks.size(); j++) {
            JSONObject subtask = (JSONObject) subtasks.get(j);
            String subtaskName = ((String) subtask.get("task"));
            // subtaskName为空则跳过
            if (StrUtil.isBlankIfStr(subtaskName)) {
                continue;
            }
            String description = (String) jsonObject.get("description");
            HashMap<String, Object> params = new HashMap<>();
            params.put("taskName", taskName);
            params.put("subtaskName", subtaskName);
            params.put("description", description);
            int res = papersWithCodeMapper.mergeTaskSubtask(params);
        }
        // 递归嵌套
        mergeTaskUnion(subtasks);
        return 1;
    }

    /**
     * Category - CATEGORY_HAS_TASK -> Task
     *
     * @param jsonObject
     * @return
     * @throws Exception
     */
    private int mergeCategoryTask(JSONObject jsonObject) throws Exception {
        String taskName = (String) jsonObject.get("task");
        String description = (String) jsonObject.get("description");
        if (StrUtil.isBlankIfStr(taskName)) {
            return 0;
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("taskName", taskName);
        params.put("description", description);
        int resTask = papersWithCodeMapper.mergeTask(params);
        JSONArray categories = (JSONArray) jsonObject.get("categories");
        // 对于subtask，categories字段是[]，Jackson解析为size为0的JSONArray
        for (int j = 0; j < categories.size(); j++) {
            String category = (String) categories.get(j);
            if (StrUtil.isBlankIfStr(category)) {
                continue;
            }
            params.put("category", category);
            int resCategoryTask = papersWithCodeMapper.mergeCategoryTask(params);
        }
        return 1;
    }
}
