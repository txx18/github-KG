package io.github.txx18.githubKG.mapper;

import io.github.txx18.githubKG.exception.DAOException;

import java.util.HashMap;
import java.util.Map;

public interface PapersWithCodeMapper {
    int mergeCategoryTask(Map<String, Object> params) throws DAOException;

    int mergeTaskSubtask(Map<String, Object> params) throws DAOException;

    int mergeTaskDataset(Map<String, Object> params) throws DAOException;

    int createModelDataset(HashMap<String, Object> params) throws DAOException;

    int mergeModelPaper(HashMap<String, Object> params) throws DAOException;

    int mergeTaskModel(HashMap<String, Object> params) throws DAOException;

    int mergeModelRepo(Map<String, Object> params) throws DAOException;

    int mergePaperRepo(Map<String, Object> params) throws DAOException;

    int mergeMethodPaper(Map<String, Object> params) throws DAOException;

    int mergeTask(Map<String, Object> params) throws DAOException;

    int mergePaper(Map<String, Object> params) throws DAOException;

    int mergeMethod(Map<String, Object> params) throws DAOException;

    int mergeCollectionMethod(Map<String, Object> params) throws DAOException;

    int mergeAreaCollection(Map<String, Object> params) throws DAOException;
}
