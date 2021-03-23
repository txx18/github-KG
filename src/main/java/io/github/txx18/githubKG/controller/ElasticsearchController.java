package io.github.txx18.githubKG.controller;

import io.github.txx18.githubKG.model.ResponseSimpleFactory;
import io.github.txx18.githubKG.service.ElasticSearchService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ShaneTang
 * @create 2021-03-23 20:04
 */
@RestController
@RequestMapping("/es")
public class ElasticsearchController {

    private final ElasticSearchService elasticSearchService;

    public ElasticsearchController(ElasticSearchService elasticSearchService) {
        this.elasticSearchService = elasticSearchService;
    }

    @RequestMapping(path = "/create/batch/repo", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseSimpleFactory importRepo(@RequestBody JSONObject filePath) {
    public ResponseSimpleFactory importRepo(@RequestParam String jsonStr) {
        System.out.println("----------------------------------");
        String res;
        try {
            res = elasticSearchService.importRepo(jsonStr);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseSimpleFactory.createResponse(e.getMessage());
        }
        if (!"ok".equals(res)) {
            return ResponseSimpleFactory.createSimpleResponse("no");
        }
        return ResponseSimpleFactory.createSimpleResponse("ok");
    }
}
