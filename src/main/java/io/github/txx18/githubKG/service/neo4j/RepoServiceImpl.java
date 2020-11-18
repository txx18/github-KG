package io.github.txx18.githubKG.service.neo4j;

import cn.hutool.core.io.file.FileReader;
import com.jayway.jsonpath.JsonPath;
import io.github.txx18.githubKG.exception.DAOException;
import io.github.txx18.githubKG.mapper.RepoMapper;
import io.github.txx18.githubKG.service.RepoService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RepoServiceImpl implements RepoService {

    private final RepoMapper repoMapper;

    public RepoServiceImpl(RepoMapper repoMapper) {
        this.repoMapper = repoMapper;

    }

    @Override
    public int insertRepoByJsonFile(String filePath) {
        // 过滤 repo为空的仓库，没有依赖的repo
        FileReader fileReader = new FileReader(filePath, "UTF-8");
        String jsonStr = fileReader.readString();
        Object repo = JsonPath.read(jsonStr, "$.data.repository");
        if (repo == null) {
            return 0;
        }
        int dgmCount = JsonPath.read(jsonStr, "$.data.repository.dependencyGraphManifests.totalCount");
        if (dgmCount == 0) {
            return 0;
        }
        return repoMapper.insertRepoByJsonFile(filePath);
    }

    @Override
    public int updateTfIdf(String ownerWithName) throws DAOException {
        // 查询repo总数
        int repoTotalCount = 0;
        repoTotalCount = repoMapper.countRepoTotalCount();
        if (repoTotalCount < 0) {
            return 0;
        }
        // 查询指定repo所有的path
        List<Object> underPaths = repoMapper.listUnderPaths(ownerWithName);

        return 1;
    }
}
