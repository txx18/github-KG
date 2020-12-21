package io.github.txx18.githubKG.service.neo4j;

import io.github.txx18.githubKG.mapper.GithubMapper;
import io.github.txx18.githubKG.service.GithubService;
import org.springframework.stereotype.Service;

/**
 * @author ShaneTang
 * @create 2020-12-21 9:39
 */
@Service
public class GithubServiceImpl implements GithubService {

    private final GithubMapper githubMapper;

    public GithubServiceImpl(GithubMapper githubMapper) {
        this.githubMapper = githubMapper;
    }

    @Override
    public int transCoOccurrenceNetworkNoRequirements() {
        int res = githubMapper.transCoOccurrenceNetworkNoRequirements();
        return 1;
    }

    @Override
    public int transCoOccurrenceNetwork() {
        return 1;
    }

}
