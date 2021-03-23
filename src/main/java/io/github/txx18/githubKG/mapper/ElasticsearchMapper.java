package io.github.txx18.githubKG.mapper;

import java.io.IOException;

public interface ElasticsearchMapper {

    String importRepo(String jsonStr) throws IOException;
}
