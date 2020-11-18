package io.github.txx18.githubKG.mapper;

import io.github.txx18.githubKG.exception.DAOException;

import java.util.List;

public interface RepoMapper {
    int insertRepoByJsonFile(String filePath);

    int countRepoTotalCount() throws DAOException;

    List<Object> listUnderPaths(String ownerWithName) throws DAOException;
}
