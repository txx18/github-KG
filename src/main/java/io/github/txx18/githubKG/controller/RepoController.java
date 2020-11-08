package io.github.txx18.githubKG.controller;

import cn.hutool.core.util.StrUtil;
import io.github.txx18.githubKG.model.ResponseSimpleFactory;
import io.github.txx18.githubKG.service.RepoService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/repo")
public class RepoController {

    private final RepoService repoService;

    public RepoController(RepoService repoService) {
        this.repoService = repoService;
    }

    @RequestMapping(path = "/createRepo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory createRepoByLocalJsonFile(@RequestParam("filePath") String filePath) {
        String extend = StrUtil.sub(filePath, -5, filePath.length());
        if (!".json".equals(extend)) {
            return ResponseSimpleFactory.createResponse("fail", "path invalid!");
        }
        int res = repoService.createRepoByJsonFile(filePath);
        if (res != 1) {
            return ResponseSimpleFactory.createSimpleResponse("no");
        }
        return ResponseSimpleFactory.createSimpleResponse("ok");
    }

}