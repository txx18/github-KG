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

    int mergePaperRepoFromLBPACJson(Map<String, Object> params) throws DAOException;

    int mergeMethodPaperNotExist(Map<String, Object> params) throws DAOException;

    int mergeTask(Map<String, Object> params) throws DAOException;

    int mergePaperFromLBPACJson(Map<String, Object> params) throws DAOException;

    int mergeMethodFromMethodsJson(Map<String, Object> params) throws DAOException;

    int mergeCollectionMethod(Map<String, Object> params) throws DAOException;

    int mergeAreaCollection(Map<String, Object> params) throws DAOException;

    int mergePaperFromPWAJson(Map<String, Object> params) throws DAOException;

    int mergeTaskPaperFromPWAJson(Map<String, Object> params) throws DAOException;

    int mergeMethodFromPWAJson(Map<String, Object> params) throws DAOException;

    int mergeMethodMainCollection(Map<String, Object> params) throws DAOException;

    List<String> matchPaperStartWith(String firstToken) throws DAOException;

    int mergeMethodIntroInPaperExist(Map<String, Object> params) throws DAOException;

    int mergePaperUsesMethod(Map<String, Object> params) throws DAOException;

    String mergeTaskPaperFromETJson(String taskName, String paperTitle) throws DAOException;

    String mergePaperRepoFromETJson(String paperTitle, String nameWithOwner, String githubUrl) throws DAOException;
}
