package io.github.txx18.githubKG.service.elasticsearch;

import io.github.txx18.githubKG.mapper.elasticsearch.ElasticsearchMapperImpl;
import io.github.txx18.githubKG.service.ElasticSearchService;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @author ShaneTang
 * @create 2021-03-21 15:57
 */
@Service
public class ElasticsearchServiceImpl implements ElasticSearchService {

    private final ElasticsearchMapperImpl packageMapper;


    public ElasticsearchServiceImpl(ElasticsearchMapperImpl packageMapper) {
        this.packageMapper = packageMapper;
    }

    @Override
    public String importRepo(String jsonStr) throws IOException {
        packageMapper.importRepo(jsonStr);
        return "ok";
    }
}
