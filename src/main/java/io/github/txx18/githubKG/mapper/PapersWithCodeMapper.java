package io.github.txx18.githubKG.mapper;

import io.github.txx18.githubKG.exception.DAOException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface PapersWithCodeMapper {
    int mergeCategoryTask(Map<String, Object> params) throws DAOException;

    int mergeTaskSubtask(Map<String, Object> params) throws DAOException;

    int mergeTaskDataset(Map<String, Object> params) throws DAOException;

    int createModelMetricDataset(HashMap<String, Object> params) throws DAOException;

    int mergeModelPaper(HashMap<String, Object> params) throws DAOException;

    int mergeTaskModel(HashMap<String, Object> params) throws DAOException;

    int mergeModelRepo(Map<String, Object> params) throws DAOException;

    int mergePaperRepo(Map<String, Object> params) throws DAOException;

    int mergeMethodPaperNotExist(Map<String, Object> params) throws DAOException;

    int mergeTask(Map<String, Object> params) throws DAOException;

    int mergePaperLBPACJson(Map<String, Object> params) throws DAOException;

    int mergeMethodMethodsJson(Map<String, Object> params) throws DAOException;

    int mergeCollectionMethod(Map<String, Object> params) throws DAOException;

    int mergeAreaCollection(Map<String, Object> params) throws DAOException;

    int mergePaperPWAJson(Map<String, Object> params) throws DAOException;

    int mergePaperAuthor(Map<String, Object> params) throws DAOException;

    int mergeTaskPaper(Map<String, Object> params) throws DAOException;

    int mergeMethodPWAJson(Map<String, Object> params) throws DAOException;

    int mergeMethodMainCollection(Map<String, Object> params) throws DAOException;

    List<String> matchPaperStartWith(String firstToken) throws DAOException;

    int mergeMethodPaperExist(Map<String, Object> params) throws DAOException;

    int mergePaperUsesMethod(Map<String, Object> params) throws DAOException;
}
