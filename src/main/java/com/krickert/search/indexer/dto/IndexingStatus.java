package com.krickert.search.indexer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.krickert.search.indexer.config.IndexerConfiguration;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

import java.time.LocalDateTime;

@Serdeable
@Introspected
public class IndexingStatus {

    public enum OverallStatus {
        NONE_AVAILABLE,
        NOT_STARTED,
        RUNNING,
        COMPLETED,
        FAILED,
        ABORTED,
        COMPLETED_WITH_ERRORS
    }

    @JsonProperty("indexing_id")
    private String indexingId;

    @JsonProperty("indexer_configuration")
    private IndexerConfiguration indexerConfiguration;

    @JsonProperty("total_documents_found")
    private long totalDocumentsFound;

    @JsonProperty("total_documents_processed")
    private int totalDocumentsProcessed;

    @JsonProperty("total_documents_failed")
    private int totalDocumentsFailed;

    @JsonProperty("percent_complete")
    private float percentComplete;

    @JsonProperty("chunks_processed")
    private int chunksProcessed;

    @JsonProperty("chunks_per_document")
    private float chunksPerDocument;

    @JsonProperty("time_started")
    private LocalDateTime timeStarted;

    @JsonProperty("end_time")
    private LocalDateTime endTime;

    @JsonProperty("current_status_message")
    private String currentStatusMessage;

    @JsonProperty("overall_status")
    private OverallStatus overallStatus;

    @JsonProperty("last_run")
    private LocalDateTime lastRun;

    // New fields for average statistics
    @JsonProperty("average_docs_per_second")
    private float averageDocsPerSecond;

    @JsonProperty("average_chunks_per_second")
    private float averageChunksPerSecond;

    // Field for deleting documents at the end
    @JsonProperty("total_documents_deleted")
    private int totalDocumentsDeleted;

    // Default constructor
    public IndexingStatus() {
        this.overallStatus = OverallStatus.NOT_STARTED; // By default, the status is NOT_STARTED
    }

    // Single string constructor
    public IndexingStatus(String message) {
        this.currentStatusMessage = message;
        this.overallStatus = OverallStatus.NOT_STARTED; // Default
    }

    // Parameterized constructor
    public IndexingStatus(String indexingId, IndexerConfiguration indexerConfiguration, long totalDocumentsFound,
                          int totalDocumentsProcessed, int totalDocumentsFailed, float percentComplete,
                          int chunksProcessed, float chunksPerDocument, LocalDateTime timeStarted,
                          LocalDateTime endTime, String currentStatusMessage, float averageDocsPerSecond,
                          float averageChunksPerSecond, int totalDocumentsDeleted, OverallStatus overallStatus,
                          LocalDateTime lastRun) {
        this.indexingId = indexingId;
        this.indexerConfiguration = indexerConfiguration;
        this.totalDocumentsFound = totalDocumentsFound;
        this.totalDocumentsProcessed = totalDocumentsProcessed;
        this.totalDocumentsFailed = totalDocumentsFailed;
        this.percentComplete = percentComplete;
        this.chunksProcessed = chunksProcessed;
        this.chunksPerDocument = chunksPerDocument;
        this.timeStarted = timeStarted;
        this.endTime = endTime;
        this.currentStatusMessage = currentStatusMessage;
        this.averageDocsPerSecond = averageDocsPerSecond;
        this.averageChunksPerSecond = averageChunksPerSecond;
        this.totalDocumentsDeleted = totalDocumentsDeleted;
        this.overallStatus = overallStatus;
        this.lastRun = lastRun;
    }

    // Getters and setters
    public String getIndexingId() {
        return indexingId;
    }

    public void setIndexingId(String indexingId) {
        this.indexingId = indexingId;
    }

    public IndexerConfiguration getIndexerConfiguration() {
        return indexerConfiguration;
    }

    public void setIndexerConfiguration(IndexerConfiguration indexerConfiguration) {
        this.indexerConfiguration = indexerConfiguration;
    }

    public long getTotalDocumentsFound() {
        return totalDocumentsFound;
    }

    public void setTotalDocumentsFound(long totalDocumentsFound) {
        this.totalDocumentsFound = totalDocumentsFound;
    }

    public int getTotalDocumentsProcessed() {
        return totalDocumentsProcessed;
    }

    public void setTotalDocumentsProcessed(int totalDocumentsProcessed) {
        this.totalDocumentsProcessed = totalDocumentsProcessed;
    }

    public int getTotalDocumentsFailed() {
        return totalDocumentsFailed;
    }

    public void setTotalDocumentsFailed(int totalDocumentsFailed) {
        this.totalDocumentsFailed = totalDocumentsFailed;
    }

    public float getPercentComplete() {
        return percentComplete;
    }

    public void setPercentComplete(float percentComplete) {
        this.percentComplete = percentComplete;
    }

    public int getChunksProcessed() {
        return chunksProcessed;
    }

    public void setChunksProcessed(int chunksProcessed) {
        this.chunksProcessed = chunksProcessed;
    }

    public float getChunksPerDocument() {
        return chunksPerDocument;
    }

    public void setChunksPerDocument(float chunksPerDocument) {
        this.chunksPerDocument = chunksPerDocument;
    }

    public LocalDateTime getTimeStarted() {
        return timeStarted;
    }

    public void setTimeStarted(LocalDateTime timeStarted) {
        this.timeStarted = timeStarted;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getCurrentStatusMessage() {
        return currentStatusMessage;
    }

    public void setCurrentStatusMessage(String currentStatusMessage) {
        this.currentStatusMessage = currentStatusMessage;
    }

    public OverallStatus getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(OverallStatus overallStatus) {
        this.overallStatus = overallStatus;
    }

    public LocalDateTime getLastRun() {
        return lastRun;
    }

    public void setLastRun(LocalDateTime lastRun) {
        this.lastRun = lastRun;
    }

    public float getAverageDocsPerSecond() {
        return averageDocsPerSecond;
    }

    public void setAverageDocsPerSecond(float averageDocsPerSecond) {
        this.averageDocsPerSecond = averageDocsPerSecond;
    }

    public float getAverageChunksPerSecond() {
        return averageChunksPerSecond;
    }

    public void setAverageChunksPerSecond(float averageChunksPerSecond) {
        this.averageChunksPerSecond = averageChunksPerSecond;
    }

    public int getTotalDocumentsDeleted() {
        return totalDocumentsDeleted;
    }

    public void setTotalDocumentsDeleted(int totalDocumentsDeleted) {
        this.totalDocumentsDeleted = totalDocumentsDeleted;
    }

    @Override
    public IndexingStatus clone() {
        IndexingStatus clone = new IndexingStatus();
        clone.setIndexingId(this.indexingId);
        clone.setIndexerConfiguration(this.indexerConfiguration);
        clone.setTotalDocumentsFound(this.totalDocumentsFound);
        clone.setTotalDocumentsProcessed(this.totalDocumentsProcessed);
        clone.setTotalDocumentsFailed(this.totalDocumentsFailed);
        clone.setPercentComplete(this.percentComplete);
        clone.setChunksProcessed(this.chunksProcessed);
        clone.setChunksPerDocument(this.chunksPerDocument);
        clone.setTimeStarted(this.timeStarted);
        clone.setEndTime(this.endTime);
        clone.setCurrentStatusMessage(this.currentStatusMessage);
        clone.setAverageDocsPerSecond(this.averageDocsPerSecond);
        clone.setAverageChunksPerSecond(this.averageChunksPerSecond);
        clone.setTotalDocumentsDeleted(this.totalDocumentsDeleted);
        clone.setOverallStatus(this.overallStatus);
        clone.setLastRun(this.lastRun);
        return clone;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("indexingId", indexingId)
                .add("indexerConfiguration", indexerConfiguration)
                .add("totalDocumentsFound", totalDocumentsFound)
                .add("totalDocumentsProcessed", totalDocumentsProcessed)
                .add("totalDocumentsFailed", totalDocumentsFailed)
                .add("percentComplete", percentComplete)
                .add("chunksProcessed", chunksProcessed)
                .add("chunksPerDocument", chunksPerDocument)
                .add("timeStarted", timeStarted)
                .add("endTime", endTime)
                .add("currentStatus", currentStatusMessage)
                .add("overallStatus", overallStatus)
                .add("lastRun", lastRun)
                .add("averageDocsPerSecond", averageDocsPerSecond)
                .add("averageChunksPerSecond", averageChunksPerSecond)
                .add("totalDocumentsDeleted", totalDocumentsDeleted)
                .toString();
    }
}
