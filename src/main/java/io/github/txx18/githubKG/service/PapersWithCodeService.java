package io.github.txx18.githubKG.service;

import io.github.txx18.githubKG.exception.DAOException;

public interface PapersWithCodeService {
    int importEvaluationTablesJson(String filePath) throws Exception;
}
