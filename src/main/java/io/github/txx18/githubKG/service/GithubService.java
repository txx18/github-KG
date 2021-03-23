package io.github.txx18.githubKG.service;

import io.github.txx18.githubKG.exception.DAOException;

public interface GithubService {

    int insertRepoByJsonFile(String filePath) throws Exception;

    String transCoOccurrenceNetworkNoRequirements();

    int transCoOccurrenceNetwork();

    int updateTfIdf(String ownerWithName) throws DAOException;
}
