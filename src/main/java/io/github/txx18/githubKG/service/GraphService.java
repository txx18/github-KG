package io.github.txx18.githubKG.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.txx18.githubKG.exception.DAOException;
import io.github.txx18.githubKG.model.DependencyPackage;
import io.github.txx18.githubKG.model.Page;

import java.util.List;
import java.util.Map;

public interface GraphService {
    String deleteRepoAllRelation(String nameWithOwner) throws DAOException;

    List<Map<String, Object>> recommendPackagesExperiment(String repoPortraitJsonStr, String kwargsJsonStr) throws Exception;

    Page<DependencyPackage> recommendPackages(String jsonStr, int pageNum, int pageSize) throws DAOException;

    List<Map<String, Object>> recommendPackageExperimentInteractive(List<String> payloadIterEntityList, Map<String, Object> inputRepoPortraitMap, Map<String, List<String>> entityKeyListMap, Map<String, Object> kwargsMap) throws JsonProcessingException, Exception;

    Map<String, List<Map<String, Object>>> recommendEntitiesExperimentInteractive(String inputRepoPortraitJsonStr, String kwargsJsonStr) throws Exception;
}
