package io.github.txx18.githubKG.service.neo4j;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.txx18.githubKG.exception.DAOException;
import io.github.txx18.githubKG.mapper.GithubMapper;
import io.github.txx18.githubKG.service.GithubService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author ShaneTang
 * @create 2021-03-23 19:17
 */
@Service
public class GithubServiceImpl implements GithubService {

    private final GithubMapper githubMapper;

    public GithubServiceImpl(GithubMapper githubMapper) {
        this.githubMapper = githubMapper;
    }

    @Override
    public int insertRepoByJsonFile(String filePath) throws Exception {
        // 目前的尝试，这种写法是解析JSONObject自动忽略null值的
        JSONObject jsonObject = JSONUtil.parseObj(JSONUtil.toJsonStr(JSONUtil.readJSONObject(new File(filePath),
                StandardCharsets.UTF_8)), true);
        JSONObject repository = ((JSONObject) jsonObject.getByPath("data.repository"));
        // nameWithOwner不为空才执行，这个get的确可能为null，但用getOrDefault
        String nameWithOwner = ((String) repository.getOrDefault("nameWithOwner", ""));
        System.out.println("inserting repo: " + nameWithOwner);
        if (!StrUtil.isBlankIfStr(nameWithOwner)) {
            // merger Repository
            int resRepo = mergeRepo(repository);
        } else {
            return 1;
        }
        // owner.login不为空才执行
        String login = ((String) ((JSONObject) repository.getOrDefault("owner", JSONUtil.createObj())).getOrDefault(
                "login", ""));
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
            String topicName = (String) ((JSONObject) topicNode.getOrDefault("topic", JSONUtil.createObj())).getOrDefault(
                    "name", "");
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
            String languageName = (String) languageNode.getOrDefault("name", "");
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
                String packageName = (String) dependencyNode.getOrDefault("packageName", "");
                String packageManager = (String) dependencyNode.getOrDefault("packageManager", "");
                // packageName不为空才执行。这里是真正实体粒度的检查
                if (!StrUtil.isBlankIfStr(packageName) && !StrUtil.isBlankIfStr(packageManager)) {
                    // Repository - REPO_DEPENDS_ON_PACKAGE -> Package
                    int resRepoPackage = githubMapper.mergeRepoDependsOnPackage(repository, dependencyGraphManifestNode, dependencyNode);
                }
                // desRepo不为null（Hutool为JSONNull对象）才执行
                String dstRepoNameWithOwner =
                        ((String) ((JSONObject) dependencyNode.getOrDefault("repository", JSONUtil.createObj())).getOrDefault(
                                "nameWithOwner", ""));
                // dstRepoNameWithOwner不为空才执行
                if (!StrUtil.isBlankIfStr(dstRepoNameWithOwner)) {
                    // Repository - REPO_DEVELOPS_PACKAGE -> Package
                    int resDstRepoPackage = githubMapper.mergeRepoDevelopsPackage(dependencyNode);
                    // Repository - REPO_DEPENDS_ON_REPO -> Repository
                    int resRepoRepo = githubMapper.mergeRepoDependsOnRepo(repository, dependencyNode);
                    // merge了dst_repo之后，查询dst_repo的依赖，以便
                    // Package - PACKAGE_DEPENDS_ON_PACKAGE -> Package
                    int resPackageDependsOnPackage = githubMapper.mergePackageDependsOnPackage(dependencyNode);
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

    @Override
    public String transCoOccurrenceNetworkNoRequirements() {
        return githubMapper.transCoOccurrenceNetworkNoRequirements();
    }

    @Override
    public int transCoOccurrenceNetwork() {
        return 1;
    }

    @Override
    public int updateTfIdf(String ownerWithName) throws DAOException {
        // 查询repo总数
        int repoTotalCount = 0;
        repoTotalCount = githubMapper.countRepoTotalCount();
        if (repoTotalCount < 0) {
            return 0;
        }
        // 查询指定repo所有的path
        List<String> underPaths = githubMapper.listUnderPaths(ownerWithName);

        return 1;
    }
}
