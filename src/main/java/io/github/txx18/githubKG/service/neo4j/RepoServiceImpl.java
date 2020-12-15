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
    public int insertRepoByJsonFile(String filePath) throws Exception {
        // 过滤 repo为空的仓库，没有依赖的repo
        FileReader fileReader = new FileReader(filePath, "UTF-8");
        String jsonStr = fileReader.readString();
        Object repo = JsonPath.read(jsonStr, "$.data.repository");
        if (repo == null) {
            throw new Exception("null repo");
        }
        int dgmCount = JsonPath.read(jsonStr, "$.data.repository.dependencyGraphManifests.totalCount");
        if (dgmCount == 0) {
            throw new Exception("dependencyGraphManifests.totalCount == 0");
        }
        // 其余执行插入
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
        List<String> underPaths = repoMapper.listUnderPaths(ownerWithName);

        return 1;
    }
}
