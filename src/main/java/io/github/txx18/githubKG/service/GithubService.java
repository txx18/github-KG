package io.github.txx18.githubKG.service;

import io.github.txx18.githubKG.exception.DAOException;

public interface GithubService {

    int insertRepoByJsonFile(String filePath) throws Exception;

    String refactorPackageCoOccur(String nameWithOwner) throws DAOException;

    String updatePackageIDF(String nameWithManager) throws DAOException;

    String updateRepoIDF(String nameWithOwner) throws DAOException;

    String createRepoDependsOnPackage(String nameWithOwner, String nameWithManager, String requirements) throws DAOException;

    String deleteRepoDependsOnPackage(String nameWithOwner, String nameWithManager) throws DAOException;

    String refactorRepoCoPackageRepo(String nameWithManager) throws DAOException;
}
