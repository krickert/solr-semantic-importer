package com.krickert.search.indexer;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "indexer-web-ui",
                version = "0.0"
        )
)
public class IndexerAdminApplication {

    public static void main(String[] args) {
        boolean runIndexer = false;
        for (String arg : args) {
            if ("--run-indexer".equals(arg)) {
                runIndexer = true;
                break;
            }
        }
        if (runIndexer) {
            try (ApplicationContext context = Micronaut.build(args).banner(false).start()) {
                SemanticIndexer indexer = context.getBean(SemanticIndexer.class);
                indexer.runDefaultExportJob();
                System.out.println("Indexing completed.");
                System.exit(0);
            } catch (IndexingFailedExecption e) {
                System.err.println("There was an indexing failure");
                System.exit(1);
            }
        } else {
            Micronaut.run(IndexerAdminApplication.class, args);
        }

    }
}