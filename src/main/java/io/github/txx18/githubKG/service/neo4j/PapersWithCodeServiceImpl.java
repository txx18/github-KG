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
     * 判空逻辑：
     * 目标：不管是Null还是size=0，在KG里面都是“不处理（不merge）”，这样查询结果就是“查不到”，用户清楚查不到不代表“没有”
     * 1、实体的主键是 不能为null也不能为“空串” 的，不然没有必要创建它了；而一个()-[]-()中有部分为null需要做一些处理，不能说有一个为null全都不要了
     * 写完mergeTaskCategory和mergeTaskDataset之后，我发现Jackson反序列化时会自动忽略值为null的字段，不像json.load()（看完整版就去看python的），
     * 那其实没必要判null了，但是判空串 还是有必要的
     * 2、对于size=0的集合，自然不会遍历，也就自动“不处理”了
     * <p>
     * 调用树：平级则并列，嵌套则嵌套调用
     *
     * @param filePath
     * @return
     */
    @Override
    public int importEvaluationTablesJson(String filePath) throws Exception {
        JSONArray jsonArray = (JSONArray) JSONUtil.readJSON(new File(filePath), StandardCharsets.UTF_8);
        return mergeTask(jsonArray);
    }

    /**
     * Paper - PAPER_LINKS_TO_REPO - Repo
     *
     * @param jsonArray
     * @return
     */
    @Override
    public int importLinksBetweenPapersAndCodeJson(String filePath) throws DAOException {
        JSONArray jsonArray = (JSONArray) JSONUtil.readJSON(new File(filePath), StandardCharsets.UTF_8);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            String paperswithcodeUrl = (String) jsonObject.get("paper_url");
            String paperTitle = (String) jsonObject.get("paper_title");
            String paperArxivId = (String) jsonObject.get("paper_arxiv_id");
            String paperUrlAbs = (String) jsonObject.get("paper_url_abs");
            String paperUrlPdf = (String) jsonObject.get("paper_url_pdf");
            String repoUrl = (String) jsonObject.get("repo_url");
            String[] tokens = StrUtil.split(repoUrl, "/");
            String nameWithOwner = tokens[3] + "/" + tokens[4];
            String mentionedInPaper = jsonObject.get("mentioned_in_paper").toString();
            String mentionedInGithub = (String) jsonObject.get("mentioned_in_github").toString();
            String framework = (String) jsonObject.get("framework");
            HashMap<String, Object> params = new HashMap<>();
            params.put("paperswithcodeUrl", paperswithcodeUrl);
            params.put("paperTitle", paperTitle);
            params.put("paperArxivId", paperArxivId);
            params.put("paperUrlAbs", paperUrlAbs);
            params.put("paperUrlPdf", paperUrlPdf);
            params.put("nameWithOwner", nameWithOwner);
            params.put("mentionedInPaper", mentionedInPaper);
            params.put("mentionedInGithub", mentionedInGithub);
            params.put("framework", framework);
            int res = papersWithCodeMapper.mergePaperRepo(params);
            System.out.println("mergePaperRepo: " + i + "/" + jsonArray.size());
        }
        return 1;
    }

    @Override
    public int importMethodsJson(String filePath) {
        return 0;
    }


    /**
     * JSON最外层是以Task出发的
     *
     * @param jsonArray
     * @return
     * @throws Exception
     */
    private int mergeTask(JSONArray jsonArray) throws Exception {
        // 第一层JSONArray
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            // Task 和 Category的关系
            int resTaskCategory = mergeTaskCategory(jsonObject);
            if (resTaskCategory == 0) {
                continue;
            }
            // Task & Dataset & Model & Paper & Metric & Repo的关系
            int resDataset = mergeDataset(jsonObject);
            // Task 和 Subtasks的关系 subtask是task的递归嵌套
            int resSubtask = mergeSubtask(jsonObject);
            System.out.println("mergeTask: " + i + "/" + jsonArray.size());
        }
        return 1;
    }


    /**
     * Task & Model 的关系
     * Model - MODEL_HAS_DATASET - Dataset 的关系
     * Model - MODEL_IN_PAPER - Paper的关系
     * Model - MODEL_IMPLEMENTS_BY_REPO - Repo
     * <p>
     * 'model_links' 都为[]
     *
     * @param jsonObject
     * @param dataset
     * @return
     * @throws DAOException
     */
    private int mergeModelPaperRepo(JSONObject jsonObject, JSONObject dataset) throws DAOException {
        String taskName = (String) jsonObject.get("task");
        JSONArray rows = (JSONArray) ((JSONObject) dataset.get("sota")).get("rows");
        String datasetName = (String) dataset.get("dataset");
        for (int i = 0; i < rows.size(); i++) {
            JSONObject model = (JSONObject) rows.get(i);
            String modelName = (String) model.get("model_name");
            if (StrUtil.isBlankIfStr(modelName)) {
                continue;
            }
            // Task & Model 的关系
            HashMap<String, Object> params = new HashMap<>();
            params.put("taskName", taskName);
            params.put("modelName", modelName);
            int res1 = papersWithCodeMapper.mergeTaskModel(params);
//            System.out.println("task: " + taskName + " - model: " + modelName);
            // Model & Dataset 的关系，并且 Dataset & Model之间把metric作为属性
            JSONObject metrics = (JSONObject) model.get("metrics");
            Set<Map.Entry<String, Object>> metricEntries = metrics.entrySet();
            params.put("datasetName", datasetName);
            for (Map.Entry<String, Object> metricEntry : metricEntries) {
                String metricName = metricEntry.getKey();
                String metricValue = metricEntry.getValue().toString();
                params.put("metricName", metricName);
                params.put("metricValue", metricValue);
                int res2 = papersWithCodeMapper.createModelDataset(params);
            }
//            System.out.println("model: " + modelName + " - dataset: " + datasetName);
            // Model & paper 的关系
            String paperUrl = (String) model.get("paper_url");
            if (!StrUtil.isBlankIfStr(paperUrl)) {
                String paperTitle = (String) model.get("paper_title");
                String paperDate = (String) model.get("paper_date");
                params.put("paperTitle", paperTitle);
                params.put("paperUrl", paperUrl);
                params.put("paperDate", paperDate);
                int res3 = papersWithCodeMapper.mergeModelPaper(params);
//                System.out.println("model: " + modelName + " - paper: " + paperUrl);
            }
            // Model - MODEL_IMPLEMENTS_BY_REPO - Repo
            JSONArray codeLinks = (JSONArray) model.get("code_links");
            for (int j = 0; j < codeLinks.size(); j++) {
                JSONObject codeLink = (JSONObject) codeLinks.get(j);
                String nameWithOwner = (String) codeLink.get("title");
                params.put("nameWithOwner", nameWithOwner);
                if (!StrUtil.isBlankIfStr(nameWithOwner)) {
                    int res4 = papersWithCodeMapper.mergeModelRepo(params);
//                    System.out.println("model: " + modelName + " - repo: " + nameWithOwner);
                }
            }
        }
        return 1;
    }


    /**
     * Task & Dataset & Model & Paper & Metric & Repo的关系
     * 字段 'dataset_citations' 'dataset_links' 'subdatasets'都为[]
     *
     * @param jsonObject
     * @return
     */
    private int mergeDataset(JSONObject jsonObject) throws DAOException {
        String taskName = (String) jsonObject.get("task");
        JSONArray datasets = (JSONArray) jsonObject.get("datasets");
        for (int i = 0; i < datasets.size(); i++) {
            JSONObject dataset = (JSONObject) datasets.get(i);
            String datasetName = (String) dataset.get("dataset");
            if (StrUtil.isBlankIfStr(datasetName)) {
                continue;
            }
            String description = (String) dataset.get("description");
            // Task 和 Dataset 的关系
            Map<String, Object> params = new HashMap<>();
            params.put("taskName", taskName);
            params.put("datasetName", datasetName);
            params.put("description", description);
            int res = papersWithCodeMapper.mergeTaskDataset(params);
//            System.out.println("task: " + taskName + " - dataset:" + datasetName);
            // 嵌套关系，如果要分方法写的话，也嵌套调用
            int resModel = mergeModelPaperRepo(jsonObject, dataset);
        }
        return 1;
    }


    /**
     * Task & Subtask的关系 递归嵌套
     * 字段'source_link'都为null
     *
     * @param jsonObject
     * @return
     * @throws DAOException
     */
    private int mergeSubtask(JSONObject jsonObject) throws Exception {
        String taskName = (String) jsonObject.get("task");
        JSONArray subtasks = (JSONArray) jsonObject.get("subtasks");
        // 没有subtasks则递归结束
        if (subtasks.isEmpty()) {
            return 1;
        }
        for (int j = 0; j < subtasks.size(); j++) {
            JSONObject subtask = (JSONObject) subtasks.get(j);
            String subtaskName = ((String) subtask.get("task"));
            if (StrUtil.isBlankIfStr(subtaskName)) {
                continue;
            }
            String description = (String) jsonObject.get("description");
            HashMap<String, Object> params = new HashMap<>();
            params.put("taskName", taskName);
            params.put("subtaskName", subtaskName);
            params.put("description", description);
            // Task & Subtask 的关系
            int res = papersWithCodeMapper.mergeTaskSubtask(params);
        }
        // 递归嵌套
        mergeTask(subtasks);
        return 1;
    }

    /**
     * Task & Category的关系
     *
     * @param jsonObject
     * @return
     * @throws Exception
     */
    private int mergeTaskCategory(JSONObject jsonObject) throws Exception {
        String taskName = (String) jsonObject.get("task");
        if (StrUtil.isBlankIfStr(taskName)) {
            return 0;
        }
        JSONArray categories = (JSONArray) jsonObject.get("categories");
        // 即使同义词是一个列表，但它不重要，也作为属性转为字符串存储
        String description = (String) jsonObject.get("description");
        // 对于subtask，categories字段是[]，Jackson解析为size为0的JSONArray
        for (int j = 0; j < categories.size(); j++) {
            String category = (String) categories.get(j);
            if (StrUtil.isBlankIfStr(category)) {
                continue;
            }
            HashMap<String, Object> params = new HashMap<>();
            params.put("taskName", taskName);
            params.put("category", category);
            params.put("description", description);
            int res = papersWithCodeMapper.mergeTaskCategory(params);
//            System.out.println("task: " + taskName + " - category:" + category);
        }
        return 1;
    }
}
