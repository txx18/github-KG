package io.github.txx18.githubKG.mapper;

import cn.hutool.json.JSONObject;
import io.github.txx18.githubKG.exception.DAOException;

import java.util.List;

public interface RepoMapper {

    int countRepoTotalCount() throws DAOException;

    List<String> listUnderPaths(String ownerWithName) throws DAOException;

    int mergeRepo(JSONObject repository) throws DAOException;

    int mergeRepoOwner(JSONObject repository) throws DAOException;

    int mergeRepoTopic(JSONObject repository, JSONObject topicNode) throws DAOException;

    int mergeRepoLanguage(JSONObject repository, JSONObject languageNode, JSONObject languageEdge) throws DAOException;

    int mergeRepoDependsOnPackage(JSONObject repository, JSONObject dependencyGraphManifestNode, JSONObject dependencyNode) throws DAOException;

    int mergeRepoDependsOnRepo(JSONObject repository, JSONObject dependencyNode) throws DAOException;

    int mergeRepoDevelopsPackage(JSONObject dependencyNode) throws DAOException;
}
