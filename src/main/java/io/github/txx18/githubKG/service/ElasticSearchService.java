package io.github.txx18.githubKG.service;

import java.io.IOException;

public interface ElasticSearchService {

    String importRepo(String jsonStr) throws IOException;
}
