package com.krickert.search.indexer;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.info.*;

@OpenAPIDefinition(
        info = @Info(
                title = "indexer-web-ui",
                version = "0.0"
        )
)
public class IndexerAdminApplication {

    public static void main(String[] args) {
        Micronaut.run(IndexerAdminApplication.class, args);
    }
}