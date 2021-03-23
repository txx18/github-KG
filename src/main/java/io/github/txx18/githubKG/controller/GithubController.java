package io.github.txx18.githubKG.controller;

import cn.hutool.core.util.StrUtil;
import io.github.txx18.githubKG.exception.DAOException;
import io.github.txx18.githubKG.model.ResponseSimpleFactory;
import io.github.txx18.githubKG.service.GithubService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ShaneTang
 * @create 2021-03-23 19:14
 */
@RestController
@RequestMapping("/github")
public class GithubController {

    private final GithubService githubService;

    public GithubController(GithubService githubService) {
        this.githubService = githubService;
    }

    @RequestMapping(path = "/create/batch/repo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory createRepoByLocalJsonFile(@RequestParam("filePath") String filePath)  {
        String extend = StrUtil.sub(filePath, -5, filePath.length());
        if (!".json".equals(extend)) {
            return ResponseSimpleFactory.createResponse("fail", "path invalid!");
        }
        int res = 0;
        try {
            res = githubService.insertRepoByJsonFile(filePath);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseSimpleFactory.createResponse(e.getMessage());
        }
        if (res != 1) {
            return ResponseSimpleFactory.createSimpleResponse("no");
        }
        return ResponseSimpleFactory.createSimpleResponse("ok");
    }

    /**
     * TODO [WIP] 同现网络，又不知道何日再研究了
     *
     * @return
     */
    @RequestMapping(path = "/transCoOccurrenceNetwork", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory transCoOccurrenceNetwork() {
        int res = 0;
        try {
            res = githubService.transCoOccurrenceNetwork();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseSimpleFactory.createResponse(e.getMessage());
        }
        if (res != 1) {
            return ResponseSimpleFactory.createSimpleResponse("no");
        }
        return ResponseSimpleFactory.createSimpleResponse("ok");
    }

    @RequestMapping(path = "/CoOccurrenceNetworkNoRequirements", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory transCoOccurrenceNetworkNoRequirements() {
        String res;
        try {
            res = githubService.transCoOccurrenceNetworkNoRequirements();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseSimpleFactory.createResponse(e.getMessage());
        }
        if (!"ok".equals(res)) {
            return ResponseSimpleFactory.createSimpleResponse("no");
        }
        return ResponseSimpleFactory.createSimpleResponse("ok");
    }

    @RequestMapping(path = "/updateTfIdf", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory updateTfIdf(@RequestParam("ownerWithName") String ownerWithName) {
        int res = 0;
        try {
            res = githubService.updateTfIdf(ownerWithName);
        } catch (DAOException e) {
            e.printStackTrace();
        }
        if (res != 1) {
            return ResponseSimpleFactory.createSimpleResponse("no");
        }
        return ResponseSimpleFactory.createSimpleResponse("ok");
    }
}
