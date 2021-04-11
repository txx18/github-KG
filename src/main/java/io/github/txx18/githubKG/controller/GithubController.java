package io.github.txx18.githubKG.controller;

import cn.hutool.core.util.StrUtil;
import io.github.txx18.githubKG.exception.DAOException;
import io.github.txx18.githubKG.model.DependencyPackage;
import io.github.txx18.githubKG.model.Page;
import io.github.txx18.githubKG.model.ResponseSimpleFactory;
import io.github.txx18.githubKG.service.GithubService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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

    @RequestMapping(path = "/exp/recommend/package", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory recommendPackagesExperiment(@RequestParam("repo_portrait_dic") String repoPortraitJsonStr,
                                                             @RequestParam("reco_method") String recoMethod,
                                                             @RequestParam("topN") int topN) {
        List<Map<String, Object>> recoRecordList;
        try {
            recoRecordList = githubService.recommendPackagesExperiment(repoPortraitJsonStr, recoMethod, topN);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseSimpleFactory.createResponse(e.getMessage());
        }
/*        List<String> recoPackageList = new ArrayList<>();
        for (Map<String, Object> record : recoRecordList) {
            recoPackageList.add(((String) record.get("nameWithManager")));
        }*/
        return ResponseSimpleFactory.createDataSuccessResponse(recoRecordList);
    }

    @RequestMapping(path = "/refactor/repo_co_package_repo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory refactorRepoCoPackageRepo(@RequestParam("nameWithManager") String nameWithManager) {
        String res;
        try {
            res = githubService.refactorRepoCoPackageRepo(nameWithManager);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseSimpleFactory.createResponse(e.getMessage());
        }
        if (!"ok".equals(res)) {
            return ResponseSimpleFactory.createSimpleResponse("no");
        }
        return ResponseSimpleFactory.createSimpleResponse("ok");
    }


    @RequestMapping(path = "/recommend/package", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory recommendPackages(@RequestParam("json_str") String jsonStr,
                                                   @RequestParam int pageNum,
                                                   @RequestParam int pageSize) {
        Page<DependencyPackage> page;
        try {
            page = githubService.recommendPackages(jsonStr, pageNum, pageSize);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseSimpleFactory.createResponse(e.getMessage());
        }
        if (page.getTotal() == 0) {
            return ResponseSimpleFactory.createResponse("no recommend packages");
        }
        return ResponseSimpleFactory.createDataSuccessResponse(page);
    }

    @RequestMapping(path = "/delete/depends_on", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory deleteRepoDependsOnPackage(@RequestParam("nameWithOwner") String nameWithOwner,
                                                            @RequestParam("nameWithManager") String nameWithManager) {
        String res = null;
        try {
            res = githubService.deleteRepoDependsOnPackage(nameWithOwner, nameWithManager);
        } catch (DAOException e) {
            e.printStackTrace();
        }
        if (!"ok".equals(res)) {
            return ResponseSimpleFactory.createSimpleResponse("no");
        }
        return ResponseSimpleFactory.createSimpleResponse("ok");
    }

    @RequestMapping(path = "/create/depends_on", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory createRepoDependsOnPackage(@RequestParam("nameWithOwner") String nameWithOwner,
                                                            @RequestParam("nameWithManager") String nameWithManager,
                                                            @RequestParam("requirements") String requirements) {
        String res = null;
        try {
            res = githubService.createRepoDependsOnPackage(nameWithOwner, nameWithManager, requirements);
        } catch (DAOException e) {
            e.printStackTrace();
        }
        if (!"ok".equals(res)) {
            return ResponseSimpleFactory.createSimpleResponse("no");
        }
        return ResponseSimpleFactory.createSimpleResponse("ok");
    }

    @RequestMapping(path = "/update/repo/IDF", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory updateRepoIDF(@RequestParam("nameWithOwner") String nameWithOwner) {
        String res = null;
        try {
            res = githubService.updateRepoIDF(nameWithOwner);
        } catch (DAOException e) {
            e.printStackTrace();
        }
        if (!"ok".equals(res)) {
            return ResponseSimpleFactory.createSimpleResponse("no");
        }
        return ResponseSimpleFactory.createSimpleResponse("ok");
    }


    @RequestMapping(path = "/update/package/IDF", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory updatePackageIDF(@RequestParam("nameWithManager") String nameWithManager) {
        String res = null;
        try {
            res = githubService.updatePackageIDF(nameWithManager);
        } catch (DAOException e) {
            e.printStackTrace();
        }
        if (!"ok".equals(res)) {
            return ResponseSimpleFactory.createSimpleResponse("no");
        }
        return ResponseSimpleFactory.createSimpleResponse("ok");
    }

    @RequestMapping(path = "/refactor/package/CoOccurrence", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory refactorPackageCoOccur(@RequestParam("nameWithOwner") String nameWithOwner) {
        String res;
        try {
            res = githubService.refactorPackageCoOccur(nameWithOwner);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseSimpleFactory.createResponse(e.getMessage());
        }
        if (!"ok".equals(res)) {
            return ResponseSimpleFactory.createSimpleResponse("no");
        }
        return ResponseSimpleFactory.createSimpleResponse("ok");
    }

    @RequestMapping(path = "/create/repo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory createRepoByLocalJsonFile(@RequestParam("filePath") String filePath) {
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


}
