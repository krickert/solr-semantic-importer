package com.krickert.search.indexer.service;

import com.krickert.search.indexer.IndexingFailedExecption;
import com.krickert.search.indexer.SemanticIndexer;
import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.dto.IndexingStatus;
import com.krickert.search.indexer.tracker.IndexingTracker;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Singleton
public class IndexerService {

    private final SemanticIndexer semanticIndexer;
    private final HealthService healthService;
    private final Map<String, IndexerConfiguration> configurations = new HashMap<>();
    private final Lock startIndexingLock = new ReentrantLock();
    private final IndexingTracker indexingTracker;

    @Inject
    public IndexerService(SemanticIndexer semanticIndexer,
                          HealthService healthService,
                          IndexingTracker indexingTracker) {
        this.semanticIndexer = semanticIndexer;
        this.healthService = healthService;
        this.indexingTracker = indexingTracker;
    }

    public String startIndexing() {
        startIndexingLock.lock();
        try {
            if (!isIndexerReady()) {
                return "Indexer is not ready. Please check the status of vectorizer and chunker services.";
            }
            UUID indexingId = generateIndexingId();

            new Thread(() -> {
                try {
                    semanticIndexer.runDefaultExportJob(indexingId);
                } catch (IndexingFailedExecption e) {
                    throw new RuntimeException(e);
                }
            }).start();

            return "Indexing job started with ID: " + indexingId;
        } finally {
            startIndexingLock.unlock();
        }
    }

    public String startIndexingByName(String configName) {
        IndexerConfiguration config = configurations.get(configName);
        if (config == null) {
            return "Configuration not found: " + configName;
        }
        return startIndexing();
    }

    public boolean isIndexerReady() {
        return healthService.checkVectorizerHealth() && healthService.checkChunkerHealth() && !isIndexingInProgress();
    }

    public boolean isIndexingInProgress() {
        IndexingStatus currentStatus = getCurrentStatus();
        return IndexingStatus.OverallStatus.RUNNING.equals(currentStatus.getOverallStatus());
    }

    public IndexingStatus getCurrentStatus() {
        IndexingStatus status = indexingTracker.getMainTaskStatus();
        if (status == null) { // If no status is available, set it as NONE_AVAILABLE
            status = new IndexingStatus();
            status.setOverallStatus(IndexingStatus.OverallStatus.NONE_AVAILABLE);
        }
        return status;
    }

    private UUID generateIndexingId() {
        return UUID.randomUUID();
    }

    public List<IndexingStatus> getHistory(int limit) {
        return indexingTracker.getHistory(limit);
    }

    public void registerConfiguration(IndexerConfiguration config) {
        configurations.put(config.getName(), config);
    }

    public IndexingStatus getStatusByCrawlId(UUID crawlId) {
        return null;
    }
}