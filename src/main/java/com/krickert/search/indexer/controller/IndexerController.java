package com.krickert.search.indexer.controller;

import com.krickert.search.indexer.IndexingFailedExecption;
import com.krickert.search.indexer.SemanticIndexer;
import com.krickert.search.indexer.dto.IndexingStatus;
import com.krickert.search.indexer.service.HealthService;
import com.krickert.search.indexer.service.IndexerService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Controller("/index")
public class IndexerController {

    private final IndexerService indexerService;
    private final HealthService healthService;
    private final SemanticIndexer semanticIndexer;

    @Inject
    public IndexerController(IndexerService indexerService, HealthService healthService, SemanticIndexer semanticIndexer) {
        this.indexerService = indexerService;
        this.healthService = healthService;
        this.semanticIndexer = semanticIndexer;
    }

    @Post
    @Secured(SecurityRule.IS_ANONYMOUS)
    @ExecuteOn(TaskExecutors.IO)
    public HttpResponse<Map<String, String>> startIndexing() {
        UUID crawlId = UUID.randomUUID();
        CompletableFuture.runAsync(() -> {
            try {
                semanticIndexer.runDefaultExportJob(crawlId);
            } catch (IndexingFailedExecption e) {
                // Log error and update status map in SemanticIndexer
                semanticIndexer.updateCrawlStatus(crawlId, IndexingStatus.OverallStatus.FAILED, e.getMessage());
            }
        });

        // Build the response with an "accepted" status and a JSON body
        Map<String, String> responseBody = Collections.singletonMap("crawlId", crawlId.toString());
        return HttpResponse.accepted().body(responseBody);
    }

    @Get("/{crawlId}")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<IndexingStatus> getStatus(@PathVariable UUID crawlId) {
        try {
            IndexingStatus status = indexerService.getStatusByCrawlId(crawlId);
            return HttpResponse.ok(status);
        } catch (Exception e) {
            IndexingStatus errorStatus = new IndexingStatus();
            errorStatus.setCurrentStatusMessage("Error retrieving status: " + e.getMessage());
            errorStatus.setOverallStatus(IndexingStatus.OverallStatus.NOT_STARTED);
            return HttpResponse.serverError(errorStatus);
        }
    }

    @Get("/history")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<List<IndexingStatus>> getHistory(@QueryValue Optional<Integer> limit) {
        List<IndexingStatus> history;
        try {
            history = indexerService.getHistory(limit.orElse(10));
            if (history.isEmpty()) {
                IndexingStatus noHistoryStatus = new IndexingStatus("No history available");
                noHistoryStatus.setOverallStatus(IndexingStatus.OverallStatus.NONE_AVAILABLE);
                return HttpResponse.ok(Collections.singletonList(noHistoryStatus));
            }
            return HttpResponse.ok(history);
        } catch (Exception e) {
            IndexingStatus errorStatus = new IndexingStatus();
            errorStatus.setCurrentStatusMessage("Error retrieving history: " + e.getMessage());
            return HttpResponse.serverError(Collections.singletonList(errorStatus));
        }
    }

    @Get("/health")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Map<String, String>> checkHealth() {
        Map<String, String> healthStatus = new HashMap<>();
        try {
            boolean isVectorizerHealthy = healthService.checkVectorizerHealth();
            boolean isChunkerHealthy = healthService.checkChunkerHealth();
            healthStatus.put("vectorizer", isVectorizerHealthy ? "available" : "unavailable");
            healthStatus.put("chunker", isChunkerHealthy ? "available" : "unavailable");
            return HttpResponse.ok(healthStatus);
        } catch (Exception e) {
            healthStatus.put("status", "Error checking health: " + e.getMessage());
            return HttpResponse.serverError(healthStatus);
        }
    }
}
