package io.github.txx18.githubKG.controller;

import io.github.txx18.githubKG.service.QueryService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author ShaneTang
 * @create 2020-12-23 16:14
 */
@RestController
@RequestMapping("/query")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }
}
