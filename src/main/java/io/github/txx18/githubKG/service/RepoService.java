package io.github.txx18.githubKG.service;


import io.github.txx18.githubKG.exception.DAOException;

public interface RepoService {
    int insertRepoByJsonFile(String filePath) throws Exception;

    int updateTfIdf(String ownerWithName) throws DAOException;
}
