package io.github.txx18.githubKG.mapper;

import io.github.txx18.githubKG.exception.DAOException;
import io.github.txx18.githubKG.model.RecommendRecord;

import java.util.List;
import java.util.Map;

public interface GraphMapper {

    List<Map<String, Object>> recommendPackagesExperimentInGraphUCF(String payloadEntity, String nameWithOwner, List<String> dependencyNameList,
                                                                    List<Map<String, Object>> dependencyMapList, int topN, int UCF_KNN) throws DAOException;

    List<Map<String, Object>> recommendPackagesExperimentInGraphICF(String nameWithOwner, List<String> dependencyNameList,
                                                                    List<Map<String, Object>> dependencyMapList, int topN) throws DAOException;

    List<Map<String, Object>> recommendPackagesExperimentPopular(List<String> dependencyNameList, int topN) throws DAOException;


    List<Map<String, Object>> recommendPackagesExperimentRandom(List<String> dependencyNameList, int topN) throws DAOException;

    List<Map<String, Object>> recommendPackagesExperimentInGraphGraph(String nameWithOwner, List<String> dependencyNameList,
                                                                      List<Map<String, Object>> dependencyMapList, int topN, int UCF_KNN) throws DAOException;

    String deleteRepoAllRelation(String nameWithOwner) throws DAOException;

    List<String> recommendPackages(List<String> dependencyNameList, List<Map<String, Object>> packageMapList, int pageNum, int pageSize) throws DAOException;

    List<Map<String, Object>> recommendPackagesExperimentICF(List<String> dependencyNameList, Map<String, Object> inputRepoPortraitMap, int topN, String useWeight) throws DAOException;

    List<Map<String, Object>> recommendLanguageExperimentICF(Map<String, Object> inputRepoPortraitMap, List<String> packageKeyList, int topN, String useWeight) throws DAOException;

    List<Map<String, Object>> recommendTaskExperimentICF(Map<String, Object> inputRepoPortraitMap, List<String> targetKeyList, int topN, String useWeight) throws DAOException;

    List<Map<String, Object>> recommendMethodExperimentICF(Map<String, Object> inputRepoPortraitMap, List<String> targetKeyList, int topN, String useWeight) throws DAOException;

    List<Map<String, Object>> recommendDatasetExperimentICF(Map<String, Object> inputRepoPortraitMap, List<String> targetKeyList, int topN, String useWeight) throws DAOException;

    List<RecommendRecord> recommendPackagesByEntityExperimentUCF(String entity, Map<String, Object> inputRepoPortraitMap, List<String> packageKeyList,
                                                                 int ucf_knn) throws DAOException;

    List<RecommendRecord> recommendPackagesByEntityExperimentUCFTfIdf(String entity, Map<String, Object> inputRepoPortraitMap, List<String> packageKeyList,
                                                                      int ucf_knn) throws DAOException;

    List<RecommendRecord> recommendPackageByEntityUCFPathSim(String entity, Map<String, Object> inputRepoPortraitMap, List<String> packageKeyList, int UCF_KNN) throws DAOException;
}
