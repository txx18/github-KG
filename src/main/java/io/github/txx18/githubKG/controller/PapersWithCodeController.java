package io.github.txx18.githubKG.controller;

import io.github.txx18.githubKG.model.ResponseSimpleFactory;
import io.github.txx18.githubKG.service.PapersWithCodeService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ShaneTang
 * @create 2020-12-12 17:25
 */
@RestController
@RequestMapping("/paperswithcode")
public class PapersWithCodeController {

    private final PapersWithCodeService papersWithCodeService;


    public PapersWithCodeController(PapersWithCodeService papersWithCodeService) {
        this.papersWithCodeService = papersWithCodeService;
    }

    @RequestMapping(path = "/importEvaluationTablesJson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory importEvaluationTablesJson(@RequestParam String filePath) {
        int res = 0;
        try {
            res = papersWithCodeService.importEvaluationTablesJson(filePath);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseSimpleFactory.createResponse(e.getMessage());
        }
        if (res != 1) {
            return ResponseSimpleFactory.createSimpleResponse("no");
        }
        return ResponseSimpleFactory.createSimpleResponse("ok");
    }

    @RequestMapping(path = "/importLinksBetweenPapersAndCodeJson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory importLinksBetweenPapersAndCodeJson(@RequestParam String filePath) {
        int res = 0;
        try {
            res = papersWithCodeService.importLinksBetweenPapersAndCodeJson(filePath);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseSimpleFactory.createResponse(e.getMessage());
        }
        if (res != 1) {
            return ResponseSimpleFactory.createSimpleResponse("no");
        }
        return ResponseSimpleFactory.createSimpleResponse("ok");
    }

    @RequestMapping(path = "/importMethodsJson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory importMethodsJson(@RequestParam String filePath) {
        int res = 0;
        try {
            res = papersWithCodeService.importMethodsJson(filePath);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseSimpleFactory.createResponse(e.getMessage());
        }
        if (res != 1) {
            return ResponseSimpleFactory.createSimpleResponse("no");
        }
        return ResponseSimpleFactory.createSimpleResponse("ok");
    }

    @RequestMapping(path = "/importPapersWithAbstractJson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseSimpleFactory importPapersWithAbstractJson(@RequestParam String filePath) {
        int res = 0;
        try {
            res = papersWithCodeService.importPapersWithAbstractJson(filePath);
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
