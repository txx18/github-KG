package io.github.txx18.githubKG.mapper;

import cn.hutool.json.JSONObject;
import io.github.txx18.githubKG.exception.DAOException;

import java.util.List;
import java.util.Map;

public interface GithubMapper {

    int mergeRepo(JSONObject repository) throws DAOException;

    int mergeRepoOwner(JSONObject repository) throws DAOException;

    int mergeRepoTopic(JSONObject repository, JSONObject topicNode) throws DAOException;

    int mergeRepoLanguage(JSONObject repository, JSONObject languageNode, JSONObject languageEdge) throws DAOException;

    int mergeRepoDependsOnPackage(JSONObject repository, JSONObject dependencyGraphManifestNode, JSONObject dependencyNode) throws DAOException;

    int mergeRepoDependsOnRepo(JSONObject repository, JSONObject dependencyNode) throws DAOException;

    int mergeRepoDevelopsPackage(JSONObject dependencyNode) throws DAOException;

    int mergePackageDependsOnPackage(JSONObject dependencyNode) throws DAOException;

    List<String> matchRepoDependsOnPackages(String nameWithOwner) throws DAOException;

    String refactorPackageCoOccur(String nameWithOwner) throws DAOException;

    List<String> recommendPackages(List<String> dependencyNameList, List<Map<String, Object>> packageMapList, int pageNum, int pageSize) throws DAOException;

    String updatePackageIDF(String nameWithManager) throws DAOException;

    List<Map<String, Object>> recommendPackagesExperimentICF(List<String> dependencyNameList, List<Map<String, Object>> dependencyMapList, int topN) throws DAOException;

    String updateRepoIDF(String nameWithOwner) throws DAOException;

    String mergeRepoDependsOnPackage(String nameWithOwner, String nameWithManager, String requirements) throws DAOException;

    String deleteRepoDependsOnPackage(String nameWithOwner, String nameWithManager) throws DAOException;

    String refactorRepoCoPackageRepo(String nameWithManager) throws DAOException;

    List<Map<String, Object>> recommendPackagesExperimentUCF(List<String> dependencyNameList, List<Map<String, Object>> dependencyMapList, int topN);

    List<Map<String, Object>> recommendPackagesExperimentPopular(int topN) throws DAOException;
}
