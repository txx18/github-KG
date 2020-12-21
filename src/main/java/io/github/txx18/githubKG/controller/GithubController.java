package io.github.txx18.githubKG.controller;

import io.github.txx18.githubKG.model.ResponseSimpleFactory;
import io.github.txx18.githubKG.service.GithubService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ShaneTang
 * @create 2020-12-21 9:38
 */
@RestController
@RequestMapping("/github")
public class GithubController {

    private final GithubService githubService;

    public GithubController(GithubService githubService) {
        this.githubService = githubService;
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

    @RequestMapping(path = "/transCoOccurrenceNetworkNoRequirements", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory transCoOccurrenceNetworkNoRequirements() {
        int res = 0;
        try {
            res = githubService.transCoOccurrenceNetworkNoRequirements();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseSimpleFactory.createResponse(e.getMessage());
        }
        if (res != 1) {
            return ResponseSimpleFactory.createSimpleResponse("no");
        }
        return ResponseSimpleFactory.createSimpleResponse("ok");
    }
}
