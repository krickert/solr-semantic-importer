package com.krickert.search.indexer;

import com.krickert.search.indexer.solr.httpclient.admin.AddVectorFieldRequest;
import com.krickert.search.indexer.solr.httpclient.admin.AddVectorTypeFieldRequest;
import com.krickert.search.indexer.solr.httpclient.admin.SolrAdminTaskClient;
import com.krickert.search.indexer.solr.httpclient.admin.SolrSchemaRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Controller("/solrAdmin")
public class SolrAdminController {

    private static final Logger log = LoggerFactory.getLogger(SolrAdminController.class);

    @Inject
    private SolrAdminTaskClient solrAdminTaskClient;

    @Post("/createCollection")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> createCollection(@Body Map<String, Object> json) {
        String name = (String) json.get("name");
        int numShards = (int) json.get("numShards");
        int replicationFactor = (int) json.get("replicationFactor");
        String configName = (String) json.get("configName");

        log.info("Creating collection {} with {} shards, replication factor {}, and config {}", name, numShards, replicationFactor, configName);

        solrAdminTaskClient.createCollection(name, numShards, replicationFactor, configName);
        return Map.of("status", "Collection creation request sent");
    }

    @Post("/uploadConfiguration")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> uploadConfiguration(@Part("configName") String configName, @Part("file") CompletedFileUpload configZipFile) {
        log.info("Uploading configuration with name {}", configName);

        try (InputStream configZipStream = configZipFile.getInputStream()) {
            solrAdminTaskClient.uploadConfiguration(configName, configZipStream);
            return Map.of("status", "Configuration upload request sent");
        } catch (IOException e) {
            log.error("Error uploading configuration", e);
            return Map.of("status", "Error uploading configuration", "error", e.getMessage());
        }
    }

    @Post("/addField/{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> addField(@PathVariable String collection, @Body SolrSchemaRequest addSchemaRequest) {
        log.info("Adding field to collection {}", collection);

        solrAdminTaskClient.addField(collection, addSchemaRequest);
        return Map.of("status", "Field addition request sent");
    }

    @Post("/addVectorField/{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> addVectorField(@PathVariable String collection, @Body AddVectorFieldRequest addVectorFieldRequest) {
        log.info("Adding vector field to collection {}", collection);

        solrAdminTaskClient.addVectorField(collection, addVectorFieldRequest);
        return Map.of("status", "Vector field addition request sent");
    }

    @Post("/addVectorTypeField/{collection}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> addVectorTypeField(@PathVariable String collection, @Body AddVectorTypeFieldRequest addVectorTypeFieldRequest) {
        log.info("Adding vector type field to collection {}", collection);

        solrAdminTaskClient.addVectorTypeField(collection, addVectorTypeFieldRequest);
        return Map.of("status", "Vector type field addition request sent");
    }

    @Delete("/deleteCollection/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> deleteCollection(@PathVariable String name) {
        log.info("Deleting collection {}", name);

        solrAdminTaskClient.deleteCollection(name);
        return Map.of("status", "Collection delete request sent");
    }

    @Get("/commit/{collection}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> commit(@PathVariable String collection) {
        log.info("Committing collection {}", collection);

        solrAdminTaskClient.commit(collection);
        return Map.of("status", "Commit request sent");
    }

    @Get("/ping/{collection}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> ping(@PathVariable String collection) {
        log.info("Pinging collection {}", collection);

        solrAdminTaskClient.ping(collection);
        return Map.of("status", "Ping request sent");
    }
}