package io.github.txx18.githubKG.service;

import io.github.txx18.githubKG.exception.DAOException;
import io.github.txx18.githubKG.model.DependencyPackage;
import io.github.txx18.githubKG.model.Page;

import java.util.List;
import java.util.Map;

public interface GithubService {

    int insertRepoByJsonFile(String filePath) throws Exception;

    String refactorPackageCoOccur(String nameWithOwner) throws DAOException;

    Page<DependencyPackage> recommendPackages(String jsonStr, int pageNum, int pageSize) throws DAOException;

    String updatePackageIDF(String nameWithManager) throws DAOException;

    List<Map<String, Object>> recommendPackagesExperiment(String repoPortraitJsonStr, String kwargsJsonStr) throws Exception;

    String updateRepoIDF(String nameWithOwner) throws DAOException;

    String createRepoDependsOnPackage(String nameWithOwner, String nameWithManager, String requirements) throws DAOException;

    String deleteRepoDependsOnPackage(String nameWithOwner, String nameWithManager) throws DAOException;

    String refactorRepoCoPackageRepo(String nameWithManager) throws DAOException;
}
