package com.krickert.search.indexer;

import com.krickert.search.indexer.dto.IndexingStatus;

import java.util.UUID;

public interface SemanticIndexer {
    void runDefaultExportJob(UUID crawlId) throws IndexingFailedExecption;

    void updateCrawlStatus(UUID crawlId, IndexingStatus.OverallStatus status, String message);
}
