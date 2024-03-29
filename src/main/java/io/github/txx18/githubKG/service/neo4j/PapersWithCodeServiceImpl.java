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
import java.util.List;
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
        // 目前的尝试，读取的如果本身是JSONArray，会自动忽略null值字段
        JSONArray jsonArray = (JSONArray) JSONUtil.readJSON(new File(filePath), StandardCharsets.UTF_8);
        return mergeTaskUnion(jsonArray);
    }

    @Override
    public int importLinksBetweenPapersAndCodeJson(String filePath) throws DAOException {
        JSONArray jsonArray = (JSONArray) JSONUtil.readJSON(new File(filePath), StandardCharsets.UTF_8);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            String paperTitle = ((String) jsonObject.getOrDefault("paper_title", "")).trim();
            // paperTitle为空则跳过，否则先merge paper
            if (StrUtil.isBlankIfStr(paperTitle)) {
                continue;
            }
            String paperswithcodeUrl = (String) jsonObject.getOrDefault("paper_url", "");
            String paperArxivId = (String) jsonObject.getOrDefault("paper_arxiv_id", "");
            String paperUrlAbs = (String) jsonObject.getOrDefault("paper_url_abs", "");
            String paperUrlPdf = (String) jsonObject.getOrDefault("paper_url_pdf", "");
            HashMap<String, Object> params = new HashMap<>();
            params.put("paperswithcodeUrl", paperswithcodeUrl);
            params.put("paperTitle", paperTitle);
            params.put("paperArxivId", paperArxivId);
            params.put("paperUrlAbs", paperUrlAbs);
            params.put("paperUrlPdf", paperUrlPdf);
            // 【实体】 merge Paper （links-between-papers-and-code.json）
            System.out.println("merging paper: " + paperTitle);
            int resPaper = papersWithCodeMapper.mergePaperFromLBPACJson(params);
            String repoUrl = ((String) jsonObject.getOrDefault("repo_url", "")).replaceAll("\\s*", "");
            // repoUrl为空则跳过
            if (StrUtil.isBlankIfStr(repoUrl)) {
                continue;
            }
            String[] tokens = StrUtil.split(repoUrl, "/");
            String nameWithOwner = tokens[3] + "/" + tokens[4];
            String framework = (String) jsonObject.getOrDefault("framework", "");
            params.put("nameWithOwner", nameWithOwner);
            Object mentionedInPaper = jsonObject.getOrDefault("mentioned_in_paper", "");
            params.put("mentionedInPaper", mentionedInPaper == "" ? -1 : ((boolean) (mentionedInPaper) ? 1 : 0));
            Object mentionedInGithub = jsonObject.getOrDefault("mentioned_in_github", "");
            params.put("mentionedInGithub", mentionedInGithub == "" ? -1 : ((boolean) (mentionedInGithub) ? 1 : 0));
            params.put("framework", framework);
            // 【关系】 Paper - PAPER_IMPLEMENTED_BY_REPO -> Repo
            int res = papersWithCodeMapper.mergePaperRepoFromLBPACJson(params);
            System.out.println("importLinksBetweenPapersAndCodeJson: " + i + "/" + jsonArray.size());
        }
        return 1;
    }

    @Override
    public int importMethodsJson(String filePath) throws DAOException {
        JSONArray jsonArray = (JSONArray) JSONUtil.readJSON(new File(filePath), StandardCharsets.UTF_8);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            String methodName = ((String) jsonObject.getOrDefault("name", "")).trim();
            if (StrUtil.isBlankIfStr(methodName)) {
                continue;
            }
            String paperswithcodeUrl = (String) jsonObject.getOrDefault("url", "");
            String fullName = (String) jsonObject.getOrDefault("full_name", "");
            String description = (String) jsonObject.getOrDefault("description", "");
            String codeSnippetUrl = (String) jsonObject.getOrDefault("code_snippet_url", "");
            String introducedYear = jsonObject.getOrDefault("introduced_year", "").toString();
            Map<String, Object> params = new HashMap<>();
            params.put("paperswithcodeUrl", paperswithcodeUrl);
            params.put("methodName", methodName);
            params.put("fullName", fullName);
            params.put("description", description);
            params.put("codeSnippetUrl", codeSnippetUrl);
            params.put("introducedYear", introducedYear);
            // 【实体】 merge Method
            System.out.println("importing method: " + methodName);
            int resMethod = papersWithCodeMapper.mergeMethodFromMethodsJson(params);
            String dashPaperTitle = ((String) jsonObject.getOrDefault("paper", "")).trim();
            // introPaperTitle 不为空才执行
            if (!StrUtil.isBlankIfStr(dashPaperTitle)) {
                params.put("dashPaperTitle", dashPaperTitle);
                //【关系】 Method - METHOD_INTRODUCED_IN_PAPER -> Paper
                int resMethodPaper = this.mergeMethodIntroInPaper(params);
            }
            JSONArray collections = ((JSONArray) jsonObject.getOrDefault("collections", JSONUtil.createArray()));
            for (int j = 0; j < collections.size(); j++) {
                JSONObject collection = (JSONObject) collections.get(j);
                String collectionName = ((String) collection.getOrDefault("collection", "")).trim();
                // collectionName不为空才执行
                if (!StrUtil.isBlankIfStr(collectionName)) {
                    params.put("collectionName", collectionName);
                    // Collection - COLLECTION_HAS_METHOD -> Method
                    int resCollectionMethod = papersWithCodeMapper.mergeCollectionMethod(params);
                    String areaName = ((String) collection.getOrDefault("area", "")).trim();
                    // area不为空才执行
                    if (!StrUtil.isBlankIfStr(areaName)) {
                        String areaId = (String) collection.getOrDefault("area_id", "");
                        params.put("areaId", areaId);
                        params.put("areaName", areaName);
                        // Area - AREA_HAS_COLLECTION -> Collection
                        int resAreaColleciton = papersWithCodeMapper.mergeAreaCollection(params);
                    }
                }
            }
            System.out.println("importMethodsJson: " + i + "/" + jsonArray.size());
        }
        return 1;
    }

    private int mergeMethodIntroInPaper(Map<String, Object> params) throws DAOException {
        String dashPaperTitle = ((String) params.get("dashPaperTitle"));
        // introPaperTitle是连字符连接的，需要分词匹配，如果匹配到了与那个paper连接，没匹配到就新建paper连接
        String[] dashTitleTokens = StrUtil.split(dashPaperTitle, "-");
        if (dashTitleTokens.length <= 1) {
            return 0;
        }
        // 【召回思想】先召回paperTitle START WITH titleTokens[0]的papers
        List<String> titles = papersWithCodeMapper.matchPaperStartWith(dashTitleTokens[0]);
        // 查询是否存在paper
        // 存在判定标准：titleTokens里的tokens与查询到的dashTitleTokens分词的tokens是否顺次相等；二者都去掉空格标点，其中一个是另一个的开头忽略大小写
        boolean isPaperExist = false;
        String existPaperTitle = "";
        // 依次判断召回的titles
        outer:
        for (String title : titles) {
            String[] titleTokens = title.split("\\W+");
            // titleTokens是全名，不能比dash的缩写版短
            if (titleTokens.length < dashTitleTokens.length) {
                continue outer;
            }
            // 顺次比较tokens
            for (int i = 1; i < dashTitleTokens.length; i++) {
                // 如果有一个token不同，则比较下一个title
                if (!StrUtil.equalsIgnoreCase(dashTitleTokens[i], titleTokens[i])) {
                    continue outer;
                }
                // 否则比较下一个token
                continue;
            }
            // token全相同，认为存在paper
            isPaperExist = true;
            existPaperTitle = title;
            break;
        }
        // 暂且先只连接到“存在”的 paper 上，对于不存在的，本来应该merge上去（待完成）
        if (isPaperExist) {
            params.put("existPaperTitle", existPaperTitle);
            int resMethodPaperExist = papersWithCodeMapper.mergeMethodIntroInPaperExist(params);
        } else {
            // todo 如果paperTitle不存在 暂时通过dashTitle获得完整 paperTitle 还没有比较好的方式，而dashTitle作为paperTitle又不好，所以先不merge了
//            int resMethodPaper = papersWithCodeMapper.mergeMethodPaperNotExist(params);
        }
        return 1;
    }

    @Override
    public int importPapersWithAbstractJson(String filePath) throws DAOException {
        JSONArray jsonArray = (JSONArray) JSONUtil.readJSON(new File(filePath), StandardCharsets.UTF_8);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            String paperTitle = ((String) jsonObject.getOrDefault("title", "")).trim();
            // paperTitle为空则跳过
            if (StrUtil.isBlankIfStr(paperTitle)) {
                continue;
            }
            String paperswithcodeUrl = (String) jsonObject.getOrDefault("paper_url", "");
            String arxivId = (String) jsonObject.getOrDefault("arxiv_id", "");
            String paperAbstract = (String) jsonObject.getOrDefault("abstract", "");
            String urlAbs = (String) jsonObject.getOrDefault("url_abs", "");
            String urlPdf = (String) jsonObject.getOrDefault("url_pdf", "");
            String proceeding = (String) jsonObject.getOrDefault("proceeding", "");
            String date = (String) jsonObject.getOrDefault("date", "");
            HashMap<String, Object> params = new HashMap<>();
            params.put("paperswithcodeUrl", paperswithcodeUrl);
            params.put("arxivId", arxivId);
            params.put("paperTitle", paperTitle);
            params.put("abstract", paperAbstract);
            params.put("urlAbs", urlAbs);
            params.put("urlPdf", urlPdf);
            params.put("proceeding", proceeding);
            params.put("date", date);
            // 【实体】 merge Paper （PWAJson）
            System.out.println("merging paper: " + paperTitle);
            int resPaper = papersWithCodeMapper.mergePaperFromPWAJson(params);
            // todo Author的同名如果放任merge的话，是一个比较严重的知识错误，因为简写同名的概率还是挺大的
            /*JSONArray authors = (JSONArray) jsonObject.getOrDefault("authors", JSONUtil.createArray());
            for (int j = 0; j < authors.size(); j++) {
                String authorName = (String) authors.get(j);
                // authorName 为空则跳过
                if (StrUtil.isBlankIfStr(authorName)) {
                    continue;
                }
                params.put("authorName", authorName);
                // Paper - PAPER_WRITTEN_BY_AUTHOR -> Author
                int resPaperAuthor = papersWithCodeMapper.mergePaperAuthor(params);
            }*/
            JSONArray tasks = (JSONArray) jsonObject.getOrDefault("tasks", JSONUtil.createArray());
            for (int j = 0; j < tasks.size(); j++) {
                String taskName = ((String) tasks.get(j)).trim();
                // taskName 为空则跳过
                if (StrUtil.isBlankIfStr(taskName)) {
                    continue;
                }
                params.put("taskName", taskName);
                // 【关系】 Task - TASK_HAS_PAPER -> Paper
                int resTaskPaper = papersWithCodeMapper.mergeTaskPaperFromPWAJson(params);
            }
            JSONArray methods = (JSONArray) jsonObject.getOrDefault("methods", JSONUtil.createArray());
            for (int j = 0; j < methods.size(); j++) {
                JSONObject method = (JSONObject) methods.get(j);
                String methodName = ((String) method.getOrDefault("name", "")).trim();
                if (StrUtil.isBlankIfStr(methodName)) {
                    continue;
                }
                String fullName = (String) method.getOrDefault("full_name", "");
                String description = (String) method.getOrDefault("description", "");
                String codeSnippetUrl = (String) method.getOrDefault("code_snippet_url", "");
                params.put("methodName", methodName);
                params.put("fullName", fullName);
                params.put("description", description);
                params.put("codeSnippetUrl", codeSnippetUrl);
                params.put("introducedYear", jsonObject.getOrDefault("introduced_year", ""));
                // 【实体】 merge Method （papers-with-abstracts.json）
                int resMethod = papersWithCodeMapper.mergeMethodFromPWAJson(params);
                // Paper - Paper_USES_METHOD -> Method
                int resPaperMethod = papersWithCodeMapper.mergePaperUsesMethod(params);
                JSONObject collection = (JSONObject) method.getOrDefault("main_collection", JSONUtil.createObj());
                String collectionName = ((String) collection.getOrDefault("name", "")).trim();
                // collectionName不为空才执行
                if (!StrUtil.isBlankIfStr(collectionName)) {
                    String collectionDescription = (String) collection.getOrDefault("description", "");
                    params.put("collectionName", collectionName);
                    params.put("collectionDescription", collectionDescription);
                    // Method - Method_MAIN_UNDER_COLLECTION -> Collection
                    int resMethodMainCollection = papersWithCodeMapper.mergeMethodMainCollection(params);
                    String areaName = ((String) collection.getOrDefault("area", "")).trim();
                    // area不为空才执行
                    if (!StrUtil.isBlankIfStr(areaName)) {
                        params.put("areaName", areaName);
                        // Area - AREA_HAS_COLLECTION -> Collection
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
     * Task 和 Category的关系
     * Task & Dataset & Model & Paper & Metric & Repo的关系
     * Task 和 Subtasks的关系 subtask是task的递归嵌套
     *
     * @param jsonArray
     * @return
     * @throws Exception
     */
    private int mergeTaskUnion(JSONArray jsonArray) throws Exception {
        // 第一层JSONArray
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            if (JSONUtil.isNull(jsonObject)) {
                continue;
            }
            int resTaskCategory = mergeCategoryTask(jsonObject);
            int resDataset = mergeDatasetUnion(jsonObject);
            // 递归嵌套
            int resSubtask = mergeSubtaskUnion(jsonObject);
            System.out.println("mergeTaskUnion: " + i + "/" + jsonArray.size());
        }
        return 1;
    }

    private int mergeModelPaperRepoUnion(JSONObject jsonObject, JSONObject dataset) throws DAOException {
        String taskName = ((String) jsonObject.getOrDefault("task", "")).trim();
        JSONArray rows = (JSONArray) ((JSONObject) dataset.getOrDefault("sota", JSONUtil.createObj())).getOrDefault(
                "rows", JSONUtil.createArray());
        String datasetName = (String) dataset.getOrDefault("dataset", "");
        for (int i = 0; i < rows.size(); i++) {
            JSONObject row = (JSONObject) rows.get(i);
            String modelName = ((String) row.getOrDefault("model_name", "")).trim();
            // 即将merge的paper和repo是平级的，不能随便卫语句跳出for循环，不然另一个就被忽略了，通过单if语句控制逻辑
            String paperTitle = ((String) row.getOrDefault("paper_title", "")).trim();
            // modelName为空则跳过
            if (StrUtil.isBlankIfStr(modelName)) {
                continue;
            }
            HashMap<String, Object> params = new HashMap<>();
            if (!StrUtil.isBlankIfStr(taskName)) {
                params.put("taskName", taskName);
                params.put("modelName", modelName);
                // 【关系】 Task - TASK_HAS_MODEL -> Model
                int resMergeTaskModel = papersWithCodeMapper.mergeTaskModel(params);
                // 【关系】 Task - TASK_HAS_PAPER -> Paper
                String resMergeTaskPaper = papersWithCodeMapper.mergeTaskPaperFromETJson(taskName, paperTitle);
            }
            JSONObject metrics = (JSONObject) row.getOrDefault("metrics", JSONUtil.createObj());
            Set<Map.Entry<String, Object>> metricEntries = metrics.entrySet();
            params.put("datasetName", datasetName);
            for (Map.Entry<String, Object> metricEntry : metricEntries) {
                String metricName = metricEntry.getKey();
                String metricValue = metricEntry.getValue().toString();
                params.put("metricName", metricName);
                params.put("metricValue", metricValue);
                // model&dataset都保证存在，顺便merge了model
                // 【关系】 Model - MODEL_ON_DATASET -> Dataset
                int res2 = papersWithCodeMapper.createModelMetricDataset(params);
            }
            // paper不为空才执行这一段，否则继续执行repo段
            if (!StrUtil.isBlankIfStr(paperTitle)) {
                String paperUrl = (String) row.getOrDefault("paper_url", "");
                String paperDate = (String) row.getOrDefault("paper_date", "");
                params.put("paperTitle", paperTitle);
                params.put("paperUrl", paperUrl);
                params.put("paperDate", paperDate);
                // 【关系】 Model - MODEL_INTRODUCED_IN_PAPER -> Paper
                int res3 = papersWithCodeMapper.mergeModelPaper(params);
            }
            JSONArray codeLinks = (JSONArray) row.getOrDefault("code_links", JSONUtil.createArray());
            for (int j = 0; j < codeLinks.size(); j++) {
                JSONObject codeLink = (JSONObject) codeLinks.get(j);
                // title字段就是nameWithOwner，还有个url字段和github数据是一样的
                String nameWithOwner = ((String) codeLink.getOrDefault("title", "")).replaceAll("\\s*", "");
                // repo为空则跳过
                if (!StrUtil.isBlankIfStr(nameWithOwner)) {
                    params.put("nameWithOwner", nameWithOwner);
                    String githubUrl = (String) codeLink.getOrDefault("url", "");
                    params.put("githubUrl", githubUrl);
                    // 【关系】 Model - MODEL_IMPLEMENTED_BY_REPO -> Repo
                    int res4 = papersWithCodeMapper.mergeModelRepo(params);
                    // 【+关系】 Paper - PAPER_IMPLEMENTED_BY_REPO -> Repo
                    String mergePaperRepo = papersWithCodeMapper.mergePaperRepoFromETJson(paperTitle, nameWithOwner, githubUrl);
                }
            }
        }
        return 1;
    }


    /**
     * 字段 'dataset_citations' 'dataset_links' 'subdatasets'都为[]
     *
     * @param jsonObject
     * @return
     */
    private int mergeDatasetUnion(JSONObject jsonObject) throws DAOException {
        // task 已经merge过了
        String taskName = ((String) jsonObject.getOrDefault("task", "")).trim();
        JSONArray datasets = (JSONArray) jsonObject.getOrDefault("datasets", JSONUtil.createArray());
        for (int i = 0; i < datasets.size(); i++) {
            JSONObject dataset = (JSONObject) datasets.get(i);
            String datasetName = ((String) dataset.getOrDefault("dataset", "")).trim();
            // dataset为空则跳过
            if (!StrUtil.isBlankIfStr(taskName) && !StrUtil.isBlankIfStr(datasetName)) {
                String description = (String) dataset.getOrDefault("description", "");
                Map<String, Object> params = new HashMap<>();
                params.put("taskName", taskName);
                params.put("datasetName", datasetName);
                params.put("description", description);
                // 【关系】 Task - TASK_HAS_DATASET -> Dataset
                int res = papersWithCodeMapper.mergeTaskDataset(params);
            }
            // 嵌套关系，如果要分方法写的话，也嵌套调用
            int resModel = this.mergeModelPaperRepoUnion(jsonObject, dataset);
        }
        return 1;
    }


    /**
     * subtask是task的递归嵌套
     * <p>
     * 字段'source_link'都为null
     *
     * @param jsonObject
     * @return
     * @throws DAOException
     */
    private int mergeSubtaskUnion(JSONObject jsonObject) throws Exception {
        String taskName = ((String) jsonObject.getOrDefault("task", "")).trim();
        JSONArray subtasks = (JSONArray) jsonObject.getOrDefault("subtasks", JSONUtil.createArray());
        // 没有subtasks则递归结束
        if (subtasks.isEmpty()) {
            return 1;
        }
        for (int j = 0; j < subtasks.size(); j++) {
            JSONObject subtask = (JSONObject) subtasks.get(j);
            String subtaskName = ((String) subtask.getOrDefault("task", "")).trim();
            // subtaskName为空则跳过
            if (StrUtil.isBlankIfStr(taskName) || StrUtil.isBlankIfStr(subtaskName)) {
                continue;
            }
            String description = (String) jsonObject.getOrDefault("description", "");
            HashMap<String, Object> params = new HashMap<>();
            params.put("taskName", taskName);
            params.put("subtaskName", subtaskName);
            params.put("description", description);
            // Task - TASK_HAS_SUBTASK -> Subtask
            int res = papersWithCodeMapper.mergeTaskSubtask(params);
        }
        // 递归嵌套
        mergeTaskUnion(subtasks);
        return 1;
    }

    private int mergeCategoryTask(JSONObject jsonObject) throws Exception {
        String taskName = ((String) jsonObject.getOrDefault("task", "")).trim();
        if (StrUtil.isBlankIfStr(taskName)) {
            return 0;
        }
        String description = (String) jsonObject.getOrDefault("description", "");
        HashMap<String, Object> params = new HashMap<>();
        params.put("taskName", taskName);
        params.put("description", description);
        // 【实体】 Task
        int resTask = papersWithCodeMapper.mergeTask(params);
        JSONArray categories = (JSONArray) jsonObject.getOrDefault("categories", JSONUtil.createArray());
        // 对于subtask，categories字段是[]，Jackson解析为size为0的JSONArray
        for (int j = 0; j < categories.size(); j++) {
            String category = ((String) categories.get(j)).trim();
            if (!StrUtil.isBlankIfStr(category)) {
                params.put("category", category);
                // 【关系】 Category - CATEGORY_HAS_TASK -> Task
                int resCategoryTask = papersWithCodeMapper.mergeCategoryTask(params);
            }
        }
        return 1;
    }
}
