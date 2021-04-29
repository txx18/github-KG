package io.github.txx18.githubKG.service.neo4j;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.github.txx18.githubKG.exception.DAOException;
import io.github.txx18.githubKG.mapper.GithubMapper;
import io.github.txx18.githubKG.mapper.GraphMapper;
import io.github.txx18.githubKG.model.DependencyPackage;
import io.github.txx18.githubKG.model.Page;
import io.github.txx18.githubKG.service.GithubService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ShaneTang
 * @create 2021-03-23 19:17
 */
@Service
public class GithubServiceImpl implements GithubService {

    private final GithubMapper githubMapper;

    private final GraphMapper graphMapper;

    public GithubServiceImpl(GithubMapper githubMapper, GraphMapper graphMapper) {
        this.githubMapper = githubMapper;
        this.graphMapper = graphMapper;
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
        Map<String, Object> kwargsMap = objectMapper.readValue(kwargsJsonStr, Map.class);
        String recoMethod = (String) kwargsMap.get("reco_method");
        int topN = (int) kwargsMap.get("topN");
        int UCF_KNN = (int) kwargsMap.get("UCF_KNN");
        List<Map<String, Object>> res;
        switch (recoMethod) {
            case "ICF":
                res = graphMapper.recommendPackagesExperimentICF(nameWithOwner, dependencyNameList, dependencyMapList
                        , topN);
                return res;
            case "UCF":
                res = graphMapper.recommendPackagesExperimentUCF(nameWithOwner, dependencyNameList, dependencyMapList
                        , topN, UCF_KNN);
                return res;
            case "Popular":
                res = graphMapper.recommendPackagesExperimentPopular(dependencyNameList, topN);
                return res;
            case "Graph":
                // 对于不在图中的用户，先需要计算出自己的 xxxRepoIDFQuadraticSum，也就需要先找到图中的那些画像的xxx
                // 目前验证集没有package的关联，但其他关联还保留，方便实验，所以目前只用找package
                res = graphMapper.recommendPackagesExperimentGraph(nameWithOwner, dependencyNameList, dependencyMapList, topN, UCF_KNN);
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
        List<String> res = githubMapper.recommendPackages(dependencyNameList, dependencyMapList, pageNum, pageSize);
        return page;
    }

    @Override
    public String refactorRepoCoPackageRepo(String nameWithManager) throws DAOException {
        System.out.println("start to refactor: " + nameWithManager);
        return githubMapper.refactorRepoCoPackageRepo(nameWithManager.replaceAll("\\s*", ""));
    }

    @Override
    public String updateRepoIDF(String nameWithOwner) throws DAOException {
        System.out.println("start to update: " + nameWithOwner);
        return githubMapper.updateRepoIDF(nameWithOwner.replaceAll("\\s*", ""));
    }

    @Override
    public String createRepoDependsOnPackage(String nameWithOwner, String nameWithManager, String requirements) throws DAOException {
        return githubMapper.mergeRepoDependsOnPackage(nameWithOwner.replaceAll("\\s*", ""),
                nameWithManager.replaceAll("\\s*", ""), requirements);
    }

    @Override
    public String deleteRepoDependsOnPackage(String nameWithOwner, String nameWithManager) throws DAOException {
        return githubMapper.deleteRepoDependsOnPackage(nameWithOwner.replaceAll("\\s*", ""),
                nameWithManager.replaceAll("\\s*", ""));
    }


    @Override
    public String updatePackageIDF(String nameWithManager) throws DAOException {
        System.out.println("start to update: " + nameWithManager);
        return githubMapper.updatePackageIDF(nameWithManager.replaceAll("\\s*", ""));
    }

    @Override
    public String refactorPackageCoOccur(String nameWithOwner) throws DAOException {
        System.out.println("start to refactor: " + nameWithOwner);
        return githubMapper.refactorPackageCoOccur(nameWithOwner.replaceAll("\\s*", ""));
    }

    /**
     * 对主键属性要特别关照
     *
     * @param filePath
     * @return
     * @throws Exception
     */
    @Override
    public int insertRepoByJsonFile(String filePath) throws Exception {
        // 目前的尝试，这种写法是解析JSONObject自动忽略null值的
        JSONObject jsonObject = JSONUtil.parseObj(JSONUtil.toJsonStr(JSONUtil.readJSONObject(new File(filePath),
                StandardCharsets.UTF_8)), true);
        JSONObject repository = ((JSONObject) jsonObject.getByPath("data.repository"));
        // nameWithOwner不为空才执行，这个get的确可能为null，但用getOrDefault
        String nameWithOwner = ((String) repository.getOrDefault("nameWithOwner", "")).replaceAll("\\s*", "");
        System.out.println("inserting repo: " + nameWithOwner);
        if (!StrUtil.isBlankIfStr(nameWithOwner)) {
            // merger Repository
            int resRepo = mergeRepo(repository);
        } else {
            return 1;
        }
        // owner.login不为空才执行
        String login = ((String) ((JSONObject) repository.getOrDefault("owner", JSONUtil.createObj())).getOrDefault(
                "login", "")).replaceAll("\\s*", "");
        if (!StrUtil.isBlankIfStr(login)) {
            // Repository - REPO_BELONGS_TO_OWNER -> Owner
            int resRepoOwner = githubMapper.mergeRepoOwner(repository);
        }
        JSONArray topicNodes =
                ((JSONArray) ((JSONObject) repository.getOrDefault("repositoryTopics", JSONUtil.createObj())).getOrDefault(
                        "nodes", JSONUtil.createArray()));
        for (int i = 0; i < topicNodes.size(); i++) {
            JSONObject topicNode = (JSONObject) topicNodes.get(i);
            // topic.name不为空才执行
            String topicName =
                    ((String) ((JSONObject) topicNode.getOrDefault("topic", JSONUtil.createObj())).getOrDefault(
                            "name", "")).trim();
            if (!StrUtil.isBlankIfStr(topicName)) {
                // Repository - REPO_UNDER_TOPIC -> Topic
                int resTopicRepo = githubMapper.mergeRepoTopic(repository, topicNode);
            }
        }
        JSONArray languageNodes = (JSONArray) ((JSONObject) repository.getOrDefault("languages",
                JSONUtil.createObj())).getOrDefault(
                "nodes", JSONUtil.createArray());
        JSONArray languageEdges =
                (JSONArray) ((JSONObject) repository.getOrDefault("languages", JSONUtil.createObj())).getOrDefault(
                        "edges", JSONUtil.createArray());
        for (int i = 0; i < languageNodes.size(); i++) {
            JSONObject languageNode = (JSONObject) languageNodes.get(i);
            String languageName = ((String) languageNode.getOrDefault("name", "")).trim();
            // languageName为空则跳过
            if (!StrUtil.isBlankIfStr(languageName)) {
                JSONObject languageEdge = (JSONObject) languageEdges.get(i);
                // Repository - REPO_USES_LANGUAGE -> Language
                int resRepoLanguage = githubMapper.mergeRepoLanguage(repository, languageNode, languageEdge);
            }
        }
        JSONArray dependencyGraphManifestNodes =
                (JSONArray) ((JSONObject) repository.getOrDefault("dependencyGraphManifests", JSONUtil.createObj())).getOrDefault(
                        "nodes", JSONUtil.createArray());
        // 已经表达了if (!dependencyGraphManifestNodes.isEmpty())的意思，有dependencyGraphManifests依赖才执行
        for (int i = 0; i < dependencyGraphManifestNodes.size(); i++) {
            JSONObject dependencyGraphManifestNode = (JSONObject) dependencyGraphManifestNodes.get(i);
            // 化boolean为整型
            Object exceedsMaxSize = dependencyGraphManifestNode.getOrDefault("exceedsMaxSize", "");
            dependencyGraphManifestNode.set("exceedsMaxSize", exceedsMaxSize == "" ? -1 : ((boolean) (exceedsMaxSize) ? 1 : 0));
            Object parseable = dependencyGraphManifestNode.getOrDefault("parseable", "");
            dependencyGraphManifestNode.set("parseable", exceedsMaxSize == "" ? -1 : ((boolean) (parseable) ? 1 : 0));
            JSONArray dependencyNodes =
                    (JSONArray) ((JSONObject) dependencyGraphManifestNode.getOrDefault("dependencies",
                            JSONUtil.createObj())).getOrDefault(
                            "nodes", JSONUtil.createArray());
            // 已经表达了有dependencyNodes才执行
            for (int j = 0; j < dependencyNodes.size(); j++) {
                JSONObject dependencyNode = (JSONObject) dependencyNodes.get(j);
                String packageName = ((String) dependencyNode.getOrDefault("packageName", "")).replaceAll("\\s*", "");
                String packageManager = ((String) dependencyNode.getOrDefault("packageManager", "")).replaceAll("\\s*", "");
                // packageName不为空才执行。这里是真正实体粒度的检查
                if (!StrUtil.isBlankIfStr(packageName) && !StrUtil.isBlankIfStr(packageManager)) {
                    // Repository - REPO_DEPENDS_ON_PACKAGE -> Package
                    int resRepoPackage = githubMapper.mergeRepoDependsOnPackage(repository, dependencyGraphManifestNode, dependencyNode);
                }
                // desRepo不为null（Hutool为JSONNull对象）才执行
                String devRepoNameWithOwner =
                        ((String) ((JSONObject) dependencyNode.getOrDefault("repository", JSONUtil.createObj())).getOrDefault(
                                "nameWithOwner", "")).replaceAll("\\s*", "");
                // dstRepoNameWithOwner不为空才执行
                if (!StrUtil.isBlankIfStr(devRepoNameWithOwner)) {
                    // Repository - REPO_DEVELOPS_PACKAGE -> Package
                    int resDstRepoPackage = githubMapper.mergeRepoDevelopsPackage(dependencyNode);
                    // todo mergeRepoDependsOnRepo mergePackageDependsOnPackage 这两种关系有待商榷
/*                    // Repository - REPO_DEPENDS_ON_REPO -> Repository
                    int resRepoRepo = githubMapper.mergeRepoDependsOnRepo(repository, dependencyNode);
                    // merge了dst_repo之后，查询dst_repo的依赖，以便
                    // Package - PACKAGE_DEPENDS_ON_PACKAGE -> Package
                    int resPackageDependsOnPackage = githubMapper.mergePackageDependsOnPackage(dependencyNode);*/
                }
            }
        }
        return 1;
    }


    /**
     * @param repository
     * @return
     * @throws DAOException
     */
    private int mergeRepo(JSONObject repository) throws DAOException {
        // 替换字段中的boolean值，默认值为-1，真为1，假为0
        Object deleteBranchOnMerge = repository.getOrDefault("deleteBranchOnMerge", "");
        repository.set("deleteBranchOnMerge", deleteBranchOnMerge == "" ? -1 : ((boolean) (deleteBranchOnMerge) ? 1 : 0));
        Object hasIssuesEnabled = repository.getOrDefault("hasIssuesEnabled", "");
        repository.set("hasIssuesEnabled", hasIssuesEnabled == "" ? -1 : ((boolean) (hasIssuesEnabled) ? 1 : 0));
        Object hasProjectsEnabled = repository.getOrDefault("hasProjectsEnabled", "");
        repository.set("hasProjectsEnabled", hasProjectsEnabled == "" ? -1 : ((boolean) (hasProjectsEnabled) ? 1 : 0));
        Object hasWikiEnabled = repository.getOrDefault("hasWikiEnabled", "");
        repository.set("hasWikiEnabled", hasWikiEnabled == "" ? -1 : ((boolean) (hasWikiEnabled) ? 1 : 0));
        Object isArchived = repository.getOrDefault("isArchived", "");
        repository.set("isArchived", isArchived == "" ? -1 : ((boolean) (isArchived) ? 1 : 0));
        Object isBlankIssuesEnabled = repository.getOrDefault("isBlankIssuesEnabled", "");
        repository.set("isBlankIssuesEnabled", isBlankIssuesEnabled == "" ? -1 : ((boolean) (isBlankIssuesEnabled) ? 1 : 0));
        Object isDisabled = repository.getOrDefault("isDisabled", "");
        repository.set("isDisabled", isDisabled == "" ? -1 : ((boolean) (isDisabled) ? 1 : 0));
        Object isEmpty = repository.getOrDefault("isEmpty", "");
        repository.set("isEmpty", isEmpty == "" ? -1 : ((boolean) (isEmpty) ? 1 : 0));
        Object isFork = repository.getOrDefault("isFork", "");
        repository.set("isFork", isFork == "" ? -1 : ((boolean) (isFork) ? 1 : 0));
        Object isInOrganization = repository.getOrDefault("isInOrganization", "");
        repository.set("isInOrganization", isInOrganization == "" ? -1 : ((boolean) (isInOrganization) ? 1 : 0));
        Object isLocked = repository.getOrDefault("isLocked", "");
        repository.set("isLocked", isLocked == "" ? -1 : ((boolean) (isLocked) ? 1 : 0));
        Object isMirror = repository.getOrDefault("isMirror", "");
        repository.set("isMirror", isMirror == "" ? -1 : ((boolean) (isMirror) ? 1 : 0));
        Object isPrivate = repository.getOrDefault("isPrivate", "");
        repository.set("isPrivate", isPrivate == "" ? -1 : ((boolean) (isPrivate) ? 1 : 0));
        Object isSecurityPolicyEnabled = repository.getOrDefault("isSecurityPolicyEnabled", "");
        repository.set("isSecurityPolicyEnabled", isSecurityPolicyEnabled == "" ? -1 : ((boolean) (isSecurityPolicyEnabled) ? 1 : 0));
        Object isTemplate = repository.getOrDefault("isTemplate", "");
        repository.set("isTemplate", isTemplate == "" ? -1 : ((boolean) (isTemplate) ? 1 : 0));
        Object isUserConfigurationRepository = repository.getOrDefault("isUserConfigurationRepository", "");
        repository.set("isUserConfigurationRepository", isUserConfigurationRepository == "" ? -1 : ((boolean) (isUserConfigurationRepository) ? 1 : 0));
        Object mergeCommitAllowed = repository.getOrDefault("mergeCommitAllowed", "");
        repository.set("mergeCommitAllowed", mergeCommitAllowed == "" ? -1 : ((boolean) (mergeCommitAllowed) ? 1 : 0));
        Object rebaseMergeAllowed = repository.getOrDefault("rebaseMergeAllowed", "");
        repository.set("rebaseMergeAllowed", rebaseMergeAllowed == "" ? -1 : ((boolean) (rebaseMergeAllowed) ? 1 : 0));
        Object squashMergeAllowed = repository.getOrDefault("squashMergeAllowed", "");
        repository.set("squashMergeAllowed", squashMergeAllowed == "" ? -1 : ((boolean) (squashMergeAllowed) ? 1 : 0));
        int resRepo = githubMapper.mergeRepo(repository);
        return 1;
    }


}
