package io.github.txx18.githubKG.service.elasticsearch;

import io.github.txx18.githubKG.mapper.elasticsearch.PackageMapperImpl;
import io.github.txx18.githubKG.service.PackageService;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @author ShaneTang
 * @create 2021-03-21 15:57
 */
@Service
public class PackageServiceImpl implements PackageService {


    private final PackageMapperImpl packageMapper;


    public PackageServiceImpl(PackageMapperImpl packageMapper) {
        this.packageMapper = packageMapper;
    }

    @Override
    public String importRepo(String jsonStr) throws IOException {
        packageMapper.importRepo(jsonStr);
        return "ok";
    }
}
