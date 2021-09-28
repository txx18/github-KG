package io.github.txx18.githubKG.controller;

import io.github.txx18.githubKG.exception.DAOException;
import io.github.txx18.githubKG.model.DependencyPackage;
import io.github.txx18.githubKG.model.Page;
import io.github.txx18.githubKG.model.ResponseSimpleFactory;
import io.github.txx18.githubKG.service.GraphService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author ShaneTang
 * @create 2021-05-09 15:48
 */
@RestController
@RequestMapping("/graph")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @RequestMapping(path = "/exp/recommend/interactive/entities", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory recommendEntitiesExperimentInteractive(@RequestParam("input_repo_portrait_dic") String inputRepoPortraitJsonStr,
                                                                        @RequestParam("kwargs") String kwargsJsonStr) {
        Map<String, List<Map<String, Object>>> recoEntitiesRecordListMap;
        try {
            recoEntitiesRecordListMap = graphService.recommendEntitiesExperimentInteractive(inputRepoPortraitJsonStr, kwargsJsonStr);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseSimpleFactory.createResponse(e.getMessage());
        }
        return ResponseSimpleFactory.createDataSuccessResponse(recoEntitiesRecordListMap);
    }

    @RequestMapping(path = "/delete/repo_all_relation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory deleteRepoAllRelation(@RequestParam("nameWithOwner") String nameWithOwner) {
        String res = null;
        try {
            res = graphService.deleteRepoAllRelation(nameWithOwner);
        } catch (DAOException e) {
            e.printStackTrace();
        }
        if (!"ok".equals(res)) {
            return ResponseSimpleFactory.createSimpleResponse("no");
        }
        return ResponseSimpleFactory.createSimpleResponse("ok");
    }

    @RequestMapping(path = "/exp/recommend/package", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory recommendPackagesExperiment(@RequestParam("repo_portrait_dic") String repoPortraitJsonStr,
                                                             @RequestParam("kwargs") String kwargsJsonStr) {
        List<Map<String, Object>> recoRecordList;
        try {
            recoRecordList = graphService.recommendPackagesExperiment(repoPortraitJsonStr, kwargsJsonStr);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseSimpleFactory.createResponse(e.getMessage());
        }
        return ResponseSimpleFactory.createDataSuccessResponse(recoRecordList);
    }


    @RequestMapping(path = "/recommend/package", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory recommendPackages(@RequestParam("json_str") String jsonStr,
                                                   @RequestParam int pageNum,
                                                   @RequestParam int pageSize) {
        Page<DependencyPackage> page;
        try {
            page = graphService.recommendPackages(jsonStr, pageNum, pageSize);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseSimpleFactory.createResponse(e.getMessage());
        }
        if (page.getTotal() == 0) {
            return ResponseSimpleFactory.createResponse("no recommend packages");
        }
        return ResponseSimpleFactory.createDataSuccessResponse(page);
    }
}
