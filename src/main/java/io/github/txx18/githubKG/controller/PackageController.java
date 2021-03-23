package io.github.txx18.githubKG.controller;

import io.github.txx18.githubKG.model.ResponseSimpleFactory;
import io.github.txx18.githubKG.service.PackageService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ShaneTang
 * @create 2021-03-17 10:12
 */
@RestController
@RequestMapping("/package")
public class PackageController {

    private final PackageService packageService;

    public PackageController(PackageService packageService) {
        this.packageService = packageService;
    }

    @RequestMapping(path = "/importRepo", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseSimpleFactory importRepo(@RequestBody JSONObject filePath) {
    public ResponseSimpleFactory importRepo(@RequestParam String jsonStr) {
        System.out.println("----------------------------------");
        String res = "";
        try {
            res = packageService.importRepo(jsonStr);
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
