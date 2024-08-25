package com.krickert.search.indexer.controller;

import io.micronaut.http.annotation.*;

@Controller("/api/indexer-web-ui")
public class IndexerWebUiController {

    @Get(uri = "/", produces = "text/plain")
    public String index() {
        return "Example Response";
    }
}