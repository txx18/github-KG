package io.github.txx18.githubKG.service;

import io.github.txx18.githubKG.exception.DAOException;

public interface PapersWithCodeService {
    int importEvaluationTablesJson(String filePath) throws Exception;

    int importLinksBetweenPapersAndCodeJson(String filePath) throws DAOException;

    int importMethodsJson(String filePath) throws DAOException;

    int importPapersWithAbstractJson(String filePath) throws DAOException;
}
