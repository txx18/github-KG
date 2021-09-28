package io.github.txx18.githubKG.service.neo4j;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.github.txx18.githubKG.exception.DAOException;
import io.github.txx18.githubKG.mapper.GraphMapper;
import io.github.txx18.githubKG.model.DependencyPackage;
import io.github.txx18.githubKG.model.Page;
import io.github.txx18.githubKG.model.RecommendRecord;
import io.github.txx18.githubKG.service.GraphService;
import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author ShaneTang
 * @create 2021-05-09 15:50
 */
@Service
public class GraphServiceImpl implements GraphService {

    // Language	Task Method	Dataset	degree
//    Double[] MODEL_SET_MEAN = new Double[]{1.459577, 1.199983, 1.291075, 1.223486, 117.233913};
//    Double[] MODEL_SET_STD = {1.292335, 0.783329, 0.971895, 0.734483, 197.777437};


    Map<String, Double> modelSetMeanMap;

    Map<String, Double> modelSetStdMap;

    Double NOT_IN_RECO_LIST_SCORE = 0.0;

    @Value("${model.filePath}")
    String recommenderWeightModelFile;

    Evaluator evaluator;

    private final GraphMapper graphMapper;

    public GraphServiceImpl(GraphMapper graphMapper) throws JAXBException, IOException, SAXException {
        this.graphMapper = graphMapper;
        modelSetMeanMap = new HashMap<>();
        //        1.173501	0.279589	0.157405	0.280117	117.251887
        modelSetMeanMap.put("language", 1.173501);
        modelSetMeanMap.put("task", 0.279589);
        modelSetMeanMap.put("method", 0.157405);
        modelSetMeanMap.put("dataset", 0.280117);
        modelSetMeanMap.put("repoDegree", 117.251887);
        //        1.298134	0.634492	0.541739	0.625491	197.926418
        modelSetStdMap = new HashMap<>();
        modelSetStdMap.put("language", 1.298134);
        modelSetStdMap.put("task", 0.634492);
        modelSetStdMap.put("method", 0.541739);
        modelSetStdMap.put("dataset", 0.625491);
        modelSetStdMap.put("repoDegree", 197.926418);
    }

    @Override
    public Map<String, List<Map<String, Object>>> recommendEntitiesExperimentInteractive(String inputRepoPortraitJsonStr, String kwargsJsonStr) throws Exception {
        Map<String, List<Map<String, Object>>> res = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> inputRepoPortraitMap = objectMapper.readValue(inputRepoPortraitJsonStr, Map.class);
        Map<String, Object> kwargsMap = objectMapper.readValue(kwargsJsonStr, Map.class);
//        String nameWithOwner = (String) inputRepoPortraitMap.get("nameWithOwner");
        List<String> totalEntityList = (List<String>) kwargsMap.get("total_entities");
        List<String> payloadIterEntityList = (List<String>) kwargsMap.get("payload_iter_entities");
        List<String> recommenderList = (List<String>) kwargsMap.get("package_recommender_entities");
        Map<String, List<String>> entityKeyListMap = new HashMap<>();
        for (String entity : totalEntityList) {
            List<Map<String, Object>> entityMapList = (List<Map<String, Object>>) inputRepoPortraitMap.getOrDefault(entity, new ArrayList<Map<String, Object>>());
            List<String> entityList = new ArrayList<>();
            for (Map<String, Object> map : entityMapList) {
                entityList.add(((String) map.get("key")).replaceAll("\\s*", ""));
            }
            entityKeyListMap.put(entity, entityList);
        }
        // 目前场景推荐依赖包是肯定的：使用 package_recommender_entities 中列举的实体推荐器推荐依赖包
        res.put("package", recommendPackageExperimentInteractive(recommenderList, inputRepoPortraitMap, entityKeyListMap, kwargsMap));
        // 可选：交互的实体
        if (payloadIterEntityList.contains("language")) {
            res.put("language", recommendLanguageExperimentInteractive(inputRepoPortraitMap, entityKeyListMap, kwargsMap));
        }
        if (payloadIterEntityList.contains("task")) {
            res.put("task", recommendTaskExperimentInteractive(inputRepoPortraitMap, entityKeyListMap, kwargsMap));
        }
        if (payloadIterEntityList.contains("method")) {
            res.put("method", recommendMethodExperimentInteractive(inputRepoPortraitMap, entityKeyListMap, kwargsMap));
        }
        if (payloadIterEntityList.contains("dataset")) {
            res.put("dataset", recommendDatasetExperimentInteractive(inputRepoPortraitMap, entityKeyListMap, kwargsMap));
        }
        return res;
    }


    @Override
    public List<Map<String, Object>> recommendPackageExperimentInteractive(List<String> recommenderList,
                                                                           Map<String, Object> inputRepoPortraitMap,
                                                                           Map<String, List<String>> entityKeyListMap,
                                                                           Map<String, Object> kwargsMap) throws Exception {
        String entity = "package";
        List<String> packageKeyList = entityKeyListMap.get(entity);
        int topN = (int) kwargsMap.get("topN");
        String useWeightType = ((String) kwargsMap.get("use_weight_type"));
        String recoMethod = (String) kwargsMap.get("reco_method");
        List<Map<String, Object>> res = null;
        switch (recoMethod) {
            case "ICF":
                res = graphMapper.recommendPackagesExperimentICF(packageKeyList, inputRepoPortraitMap, topN, useWeightType);
                return res;
            case "UCF":
                int UCF_KNN = (int) kwargsMap.get("UCF_KNN");
                String fieldWeightType = (String) kwargsMap.get("field_weight_type");
                List<String> payloadFieldList = (List<String>) kwargsMap.get("payload_field");
                res = recommendPackagesExperimentUCF(recommenderList, inputRepoPortraitMap, packageKeyList, topN, UCF_KNN, useWeightType,
                        fieldWeightType, payloadFieldList);
                return res;
            case "Popular":
                res = graphMapper.recommendPackagesExperimentPopular(packageKeyList, topN);
                return res;
            case "Random":
                res = graphMapper.recommendPackagesExperimentRandom(packageKeyList, topN);
                return res;
            default:
                throw new Exception("没有选择推荐方法！");
        }
    }

    private List<Map<String, Object>> recommendPackagesExperimentUCF(List<String> recommenderList, Map<String, Object> inputRepoPortraitMap,
                                                                     List<String> packageKeyList, int topN, int UCF_KNN, String useWeightType,
                                                                     String fieldWeightType, List<String> payloadFieldList) throws DAOException, JAXBException, IOException, SAXException {
        List<Map<String, Object>> res = new ArrayList<>();
        Map<String, List<RecommendRecord>> recommenderResMap = new HashMap<>();
        // 评分权重
        switch (useWeightType) {
            case "cosine":
                // 遍历各实体推荐器
                for (String recommender : recommenderList) {
                    List<RecommendRecord> recommenderRes = graphMapper.recommendPackagesByEntityExperimentUCF(recommender, inputRepoPortraitMap, packageKeyList, UCF_KNN);
                    recommenderResMap.put(recommender, recommenderRes);
                }
                break;
            case "cosine_TfIdf":
                for (String recommender : recommenderList) {
                    List<RecommendRecord> recommenderRes = graphMapper.recommendPackagesByEntityExperimentUCFTfIdf(recommender, inputRepoPortraitMap, packageKeyList,
                            UCF_KNN);
                    recommenderResMap.put(recommender, recommenderRes);
                }
                break;
            case "PathSim":
                for (String recommender : recommenderList) {
                    List<RecommendRecord> recommenderRes = graphMapper.recommendPackageByEntityUCFPathSim(recommender, inputRepoPortraitMap, packageKeyList,
                            UCF_KNN);
                    recommenderResMap.put(recommender, recommenderRes);
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + useWeightType);
        }
        // 还原成嵌套Map
        Map<String, Map<String, Object>> nestMapRes = new HashMap<>();
        // todo 并制作一个 repoDegreeMap，其实可以从外部读取不用查询
        Map<String, Double> repoDegreeMap = new HashMap<>();
        for (String recommender : recommenderResMap.keySet()) {
            List<RecommendRecord> recommenderRecordList = recommenderResMap.get(recommender);
            for (RecommendRecord record : recommenderRecordList) {
                String nameWithManager = record.getKey();
                Double score = record.getScore();
                // 可能为null
                Double repoDegree = record.getRepoDegree();
                repoDegreeMap.put(nameWithManager, repoDegree);
                if (nestMapRes.get(nameWithManager) == null) {
                    nestMapRes.put(nameWithManager, new HashMap<>());
                }
                nestMapRes.get(nameWithManager).put(recommender, score);
                nestMapRes.get(nameWithManager).put("repoDegree", repoDegreeMap.get(nameWithManager));
            }
        }
        Map<String, Double> resScoreMap = new HashMap<>();
        // 考虑元路径权重的话，需要填充推荐器默认打分、标准化
        if (!"default".equals(fieldWeightType)) {
            // 构造完整的各推荐器结果，单推荐器有而其他推荐器没有的默认为得分0
            for (String nameWithOwner : nestMapRes.keySet()) {
                for (String recommender : modelSetMeanMap.keySet()) {
                    if (!nestMapRes.get(nameWithOwner).containsKey(recommender)) {
                        nestMapRes.get(nameWithOwner).put(recommender, NOT_IN_RECO_LIST_SCORE);
                    }
                }
            }
            // 标准化 score 目前使用 model_set中训练集 的 mean & std
            for (String nameWithManager : nestMapRes.keySet()) {
                for (String recommender : nestMapRes.get(nameWithManager).keySet()) {
                    Double score = (Double) nestMapRes.get(nameWithManager).get(recommender);
                    Double normalScore = (score - modelSetMeanMap.get(recommender)) / modelSetStdMap.get(recommender);
                    nestMapRes.get(nameWithManager).put(recommender, normalScore);
                }
            }
            // 加权各推荐器结果
            // 1）手动赋权重
//        Map<String, Double> resScoreMap = giveRecommenderWeightManual(fieldWeightType, nestMapRes);
            // 2）PMML调用模型
            resScoreMap = callPmmlModel(nestMapRes);
            // 否则 default权重 得分直接相加
        } else {
            for (String nameWithManager : nestMapRes.keySet()) {
                double weightedScore = 0.0;
                for (String field : payloadFieldList) {
                    if (!nestMapRes.get(nameWithManager).containsKey(field)) {
                        continue;
                    }
                    Double score = (Double) nestMapRes.get(nameWithManager).get(field);
                    weightedScore += score;
                }
                resScoreMap.put(nameWithManager, weightedScore);
            }
        }
        //转换成 mapList
        for (String nameWithOwner : resScoreMap.keySet()) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("key", nameWithOwner);
            map.put("score", resScoreMap.get(nameWithOwner));
            res.add(map);
        }
        // 按 score & key 降序
        res.sort((map1, map2) -> {
            if (map1 == null || map2 == null) {
                throw new NullPointerException();
            }
            int c = ((Double) map2.get("score")).compareTo((Double) map1.get("score"));
            if (c != 0) {
                return c;
            }
            c = ((String) map2.get("key")).compareTo(((String) map1.get("key")));
            return c;
        });
        // 截取topN
        int limit = Math.min(topN, res.size());
        res = res.subList(0, limit);
        return res;
    }

    private Map<String, Double> callPmmlModel(Map<String, Map<String, Object>> nestMapRes) throws IOException, JAXBException, SAXException {
        evaluator = new LoadingModelEvaluatorBuilder().load(new File(recommenderWeightModelFile)).build();
        evaluator.verify();
        List<InputField> inputFields = evaluator.getInputFields();
        Map<String, Double> res = new HashMap<>();
        Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();
        for (String nameWithManager : nestMapRes.keySet()) {
            Map<String, Object> inputRecord = nestMapRes.get(nameWithManager);
            for (InputField inputField : inputFields) {
                FieldName fieldName = inputField.getName();
                Object rawValue = inputRecord.get(fieldName.getValue());
                FieldValue fieldValue = inputField.prepare(rawValue);
                arguments.put(fieldName, fieldValue);
            }
            Map<FieldName, ?> results = evaluator.evaluate(arguments);
            Map<String, ?> resultRecord = EvaluatorUtil.decodeAll(results);
            res.put(nameWithManager, ((Double) resultRecord.get("probability(1)")));
        }
        return res;
    }


    private List<Map<String, Object>> recommendLanguageExperimentInteractive(Map<String, Object> inputRepoPortraitMap,
                                                                             Map<String, List<String>> entityKeyListMap,
                                                                             Map<String, Object> kwargsMap) throws Exception {
        List<String> targetKeyList = entityKeyListMap.get("language");
        int topN = (int) kwargsMap.get("topN");
        String useWeight = ((String) kwargsMap.get("use_weight_type"));
        List<Map<String, Object>> res = null;
        res = graphMapper.recommendLanguageExperimentICF(inputRepoPortraitMap, targetKeyList, topN, useWeight);
        return res;
    }

    private List<Map<String, Object>> recommendTaskExperimentInteractive(Map<String, Object> inputRepoPortraitMap,
                                                                         Map<String, List<String>> entityKeyListMap,
                                                                         Map<String, Object> kwargsMap) throws Exception {
        List<String> targetKeyList = entityKeyListMap.get("task");
        int topN = (int) kwargsMap.get("topN");
        String useWeight = ((String) kwargsMap.get("use_weight_type"));
        List<Map<String, Object>> res = null;
        res = graphMapper.recommendTaskExperimentICF(inputRepoPortraitMap, targetKeyList, topN, useWeight);
        return res;
    }

    private List<Map<String, Object>> recommendMethodExperimentInteractive(Map<String, Object> inputRepoPortraitMap,
                                                                           Map<String, List<String>> entityKeyListMap,
                                                                           Map<String, Object> kwargsMap) throws Exception {
        List<String> targetKeyList = entityKeyListMap.get("method");
        int topN = (int) kwargsMap.get("topN");
        String useWeight = ((String) kwargsMap.get("use_weight_type"));
        List<Map<String, Object>> res = null;
        res = graphMapper.recommendMethodExperimentICF(inputRepoPortraitMap, targetKeyList, topN, useWeight);
        return res;
    }

    private List<Map<String, Object>> recommendDatasetExperimentInteractive(Map<String, Object> inputRepoPortraitMap,
                                                                            Map<String, List<String>> entityKeyListMap,
                                                                            Map<String, Object> kwargsMap) throws Exception {
        List<String> targetKeyList = entityKeyListMap.get("dataset");
        int topN = (int) kwargsMap.get("topN");
        String useWeight = ((String) kwargsMap.get("use_weight_type"));
        List<Map<String, Object>> res = null;
        res = graphMapper.recommendDatasetExperimentICF(inputRepoPortraitMap, targetKeyList, topN, useWeight);
        return res;
    }

    @Override
    public String deleteRepoAllRelation(String nameWithOwner) throws DAOException {
        return graphMapper.deleteRepoAllRelation(nameWithOwner);
    }


    @Override
    public List<Map<String, Object>> recommendPackagesExperiment(String repoPortraitJsonStr, String kwargsJsonStr) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> repoPortraitMap = objectMapper.readValue(repoPortraitJsonStr, Map.class);
        String nameWithOwner = (String) repoPortraitMap.get("nameWithOwner");
        List<Map<String, Object>> dependencyMapList = (List<Map<String, Object>>) repoPortraitMap.get("dependency_dic_list");
//        List<Map<String, Object>> dependencyMapList = JsonPath.read(jsonStr, "$.*"); // dependedTF被转成了BigDecimal
        List<String> dependencyNameList = new ArrayList<>();
        for (Map<String, Object> map : dependencyMapList) {
            dependencyNameList.add(((String) map.get("nameWithManager")).replaceAll("\\s*", ""));
        }
        // 其他参数
        Map<String, Object> kwargsMap = objectMapper.readValue(kwargsJsonStr, Map.class);
        List<String> recommenderEntityList = (List<String>) kwargsMap.get("package_recommender_entities");
        String recoMethod = (String) kwargsMap.get("reco_method");
        int topN = (int) kwargsMap.get("topN");
        int UCF_KNN = (int) kwargsMap.get("UCF_KNN");
        List<Map<String, Object>> res;
        switch (recoMethod) {
            case "ICF":
                res = graphMapper.recommendPackagesExperimentInGraphICF(nameWithOwner, dependencyNameList, dependencyMapList, topN);
                return res;
            case "UCF":
                String payloadEntity = recommenderEntityList.get(0);
                res = graphMapper.recommendPackagesExperimentInGraphUCF(payloadEntity, nameWithOwner, dependencyNameList, dependencyMapList, topN, UCF_KNN);
                return res;
            case "Popular":
                res = graphMapper.recommendPackagesExperimentPopular(dependencyNameList, topN);
                return res;
            case "Graph":
                // 对于不在图中的用户，先需要计算出自己的 xxxTfIdfQuadraticSum，也就需要先找到图中的那些画像的xxx
                // 目前验证集没有package的关联，但其他关联还保留，方便实验，所以目前只用找package
                res = graphMapper.recommendPackagesExperimentInGraphGraph(nameWithOwner, dependencyNameList, dependencyMapList, topN, UCF_KNN);
                return res;
            case "Combine":
                if ((double) repoPortraitMap.get("train_package_set_avg_popularity") > 3.1398058252427186) {
                    res = graphMapper.recommendPackagesExperimentInGraphGraph(nameWithOwner, dependencyNameList, dependencyMapList, topN, UCF_KNN);
                } else {
//                    res = graphMapper.recommendPackagesExperimentUCF(nameWithOwner, dependencyNameList, dependencyMapList, topN, UCF_KNN);
                    res = graphMapper.recommendPackagesExperimentInGraphICF(nameWithOwner, dependencyNameList, dependencyMapList, topN);
                }
                return res;
            case "Random":
                res = graphMapper.recommendPackagesExperimentRandom(dependencyNameList, topN);
                return res;
            default:
                throw new Exception("没有选择推荐方法！");
        }
    }

    @Override
    public Page<DependencyPackage> recommendPackages(String jsonStr, int pageNum, int pageSize) throws DAOException {
        Page<DependencyPackage> page = new Page<>();
        List<Object> dependencyNodes = JsonPath.read(jsonStr, "data.repository" +
                ".dependencyGraphManifests.nodes[*].dependencies.nodes[*]");
        double totalDependencyCount = dependencyNodes.size();
        Map<String, Double> dependencyCountMap = new HashMap<>();
        // 完成去重、统计
        for (Object dependencyNode : dependencyNodes) {
            Map<String, Object> map = (Map<String, Object>) dependencyNode;
            String nameWithManager = (map.get("packageManager") + "/" + map.get("packageName")).replaceAll("\\s*", "");
            dependencyCountMap.put(nameWithManager, dependencyCountMap.getOrDefault(nameWithManager, (double) 0) + 1);
        }
        // 计算TF
        dependencyCountMap.replaceAll((k, v) -> dependencyCountMap.get(k) / totalDependencyCount);
        List<String> dependencyNameList = new ArrayList<>(dependencyCountMap.keySet());
        // 换一种mapList的格式存储
        List<Map<String, Object>> dependencyMapList = new ArrayList<>(dependencyCountMap.size());
        for (Map.Entry<String, Double> entry : dependencyCountMap.entrySet()) {
            Map<String, Object> map = new HashMap<>();
            map.put("nameWithManager", entry.getKey());
            map.put("dependedTF", entry.getValue());
            dependencyMapList.add(map);
        }
        List<String> res = graphMapper.recommendPackages(dependencyNameList, dependencyMapList, pageNum, pageSize);
        return page;
    }


}
