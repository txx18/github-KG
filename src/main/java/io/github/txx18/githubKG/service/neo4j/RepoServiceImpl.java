package io.github.txx18.githubKG.service.neo4j;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.txx18.githubKG.exception.DAOException;
import io.github.txx18.githubKG.mapper.RepoMapper;
import io.github.txx18.githubKG.service.RepoService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RepoServiceImpl implements RepoService {

    private final RepoMapper repoMapper;

    public RepoServiceImpl(RepoMapper repoMapper) {
        this.repoMapper = repoMapper;

    }

    /**
     * Repository
     * Repository - REPO_BELONGS_TO_OWNER -> Owner
     * Repository - REPO_UNDER_TOPIC -> Topic
     * Repository - REPO_USES_LANGUAGE -> Language
     * Repository - REPO_DEPENDS_ON_PACKAGE -> Package
     * Repository - REPO_DEPENDS_ON_REPO -> Repository
     * Repository - REPO_DEVELOPS_PACKAGE -> Package
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
        String nameWithOwner = ((String) repository.getOrDefault("nameWithOwner", ""));
        if (!StrUtil.isBlankIfStr(nameWithOwner)) {
            int resRepo = mergeRepo(repository);
        } else {
            return 1;
        }
        // owner.login不为空才执行
        String login = ((String) ((JSONObject) repository.getOrDefault("owner", JSONUtil.createObj())).getOrDefault(
                "login", ""));
        if (!StrUtil.isBlankIfStr(login)) {
            int resRepoOwner = repoMapper.mergeRepoOwner(repository);
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
                int resTopicRepo = repoMapper.mergeRepoTopic(repository, topicNode);
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
                int resRepoLanguage = repoMapper.mergeRepoLanguage(repository, languageNode, languageEdge);
            }
        }
        JSONArray dependencyGraphManifestNodes =
                (JSONArray) ((JSONObject) repository.getOrDefault("dependencyGraphManifests", JSONUtil.createObj())).getOrDefault(
                        "nodes", JSONUtil.createArray());
        // 已经表达了if (!dependencyGraphManifestNodes.isEmpty())的意思，有dependencyGraphManifests依赖才执行
        for (int i = 0; i < dependencyGraphManifestNodes.size(); i++) {
            JSONObject dependencyGraphManifestNode = (JSONObject) dependencyGraphManifestNodes.get(i);
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
                    int resRepoPackage = repoMapper.mergeRepoDependsOnPackage(repository, dependencyGraphManifestNode,
                            dependencyNode);
                }
                // desRepo不为null（Hutool为JSONNull对象）才执行
                String dstRepoNameWithOwner =
                        ((String) ((JSONObject) dependencyNode.getOrDefault("repository", JSONUtil.createObj())).getOrDefault(
                                "nameWithOwner", ""));
                // dstRepoNameWithOwner不为空才执行
                if (!StrUtil.isBlankIfStr(dstRepoNameWithOwner)) {
                    int resRepoRepo = repoMapper.mergeRepoDependsOnRepo(repository, dependencyNode);
                    int resDstRepoPackage = repoMapper.mergeRepoDevelopsPackage(dependencyNode);
                }
            }
        }
        return 1;
    }

    /**
     *
     * @param repository
     * @return
     * @throws DAOException
     */
    private int mergeRepo(JSONObject repository) throws DAOException {
        Map<String, Object> params = new HashMap<>();
        params.put("createdAt", repository.getOrDefault("createdAt", ""));
        params.put("description", repository.getOrDefault("description", ""));
        params.put("forkCount", repository.getOrDefault("forkCount", ""));
        params.put("homepageUrl", repository.getOrDefault("homepageUrl", ""));
        params.put("isDisabled", repository.getOrDefault("isDisabled", ""));
        params.put("isEmpty", repository.getOrDefault("isEmpty", ""));
        params.put("isFork", repository.getOrDefault("isFork", ""));
        params.put("isInOrganization", repository.getOrDefault("isInOrganization", ""));
        params.put("isLocked", repository.getOrDefault("isLocked", ""));
        params.put("isMirror", repository.getOrDefault("isMirror", ""));
        params.put("isPrivate", repository.getOrDefault("isPrivate", ""));
        params.put("isTemplate", repository.getOrDefault("isTemplate", ""));
        params.put("issueCount", ((JSONObject) repository.getOrDefault("issues", JSONUtil.createObj())).getOrDefault(
                "totalCount", ""));
        params.put("isUserConfigurationRepository", repository.getOrDefault("isUserConfigurationRepository", ""));
        params.put("licenseInfoName",
                ((JSONObject) repository.getOrDefault("licenseInfo", JSONUtil.createObj())).getOrDefault("name", ""));
        params.put("name", repository.getOrDefault("name", ""));
        params.put("nameWithOwner", repository.getOrDefault("nameWithOwner", ""));
        params.put("primaryLanguageName",
                ((JSONObject) repository.getOrDefault("primaryLanguage", JSONUtil.createObj())).getOrDefault(
                "name", ""));
        params.put("pullRequestCount",
                ((JSONObject) repository.getOrDefault("pullRequests", JSONUtil.createObj())).getOrDefault("totalCount", ""));
        params.put("pushedAt", repository.getOrDefault("pushedAt", ""));
        params.put("stargazerCount", repository.getOrDefault("stargazerCount", ""));
        params.put("updatedAt", repository.getOrDefault("updatedAt", ""));
        params.put("url", repository.getOrDefault("url", ""));
        // 去除Map中值为null的键值对
        MapUtil.removeNullValue(params);
        int resRepo = repoMapper.mergeRepo(params);
        return 1;
    }

    @Override
    public int updateTfIdf(String ownerWithName) throws DAOException {
        // 查询repo总数
        int repoTotalCount = 0;
        repoTotalCount = repoMapper.countRepoTotalCount();
        if (repoTotalCount < 0) {
            return 0;
        }
        // 查询指定repo所有的path
        List<String> underPaths = repoMapper.listUnderPaths(ownerWithName);

        return 1;
    }
}
