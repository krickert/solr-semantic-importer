package com.krickert.search.indexer.tracker;

import com.krickert.search.indexer.dto.IndexingStatus;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Singleton
public class IndexingTracker {
    private static final Logger log = LoggerFactory.getLogger(IndexingTracker.class);
    private final IndexingStatus mainTaskStatus = new IndexingStatus();
    private final IndexingStatus vectorTaskStatus = new IndexingStatus();
    private final List<IndexingStatus> indexingHistory = new LinkedList<>();

    private final AtomicLong mainDocumentsFound = new AtomicLong(0);
    private final AtomicLong vectorDocumentsFound = new AtomicLong(0);
    private final AtomicInteger mainDocumentsProcessed = new AtomicInteger(0);
    private final AtomicInteger mainDocumentsFailed = new AtomicInteger(0);
    private final AtomicInteger vectorDocumentsProcessed = new AtomicInteger(0);
    private final AtomicInteger vectorDocumentsFailed = new AtomicInteger(0);
    private final Integer maxHistorySize;

    private LocalDateTime timeStarted;

    @Inject
    public IndexingTracker(@Value("${indexer-manager.max-history-size}") Integer maxHistorySize) {
        this.maxHistorySize = maxHistorySize == null || maxHistorySize < 0 ? 100 : maxHistorySize;
    }

    public IndexingStatus getMainTaskStatus() {
        return mainTaskStatus;
    }

    public IndexingStatus getVectorTaskStatus() {
        return vectorTaskStatus;
    }

    public synchronized void reset() {
        resetStatus(mainTaskStatus);
        resetStatus(vectorTaskStatus);

        mainDocumentsFound.set(0);
        vectorDocumentsFound.set(0);
        mainDocumentsProcessed.set(0);
        mainDocumentsFailed.set(0);
        vectorDocumentsProcessed.set(0);
        vectorDocumentsFailed.set(0);

        timeStarted = null;
    }

    private void resetStatus(IndexingStatus status) {
        status.setIndexingId(null);
        status.setIndexerConfiguration(null);
        status.setTotalDocumentsFound(0);
        status.setTotalDocumentsProcessed(0);
        status.setTotalDocumentsFailed(0);
        status.setPercentComplete(0);
        status.setTimeStarted(null);
        status.setEndTime(null);
        status.setCurrentStatusMessage(null);
        status.setAverageDocsPerSecond(0);
    }

    public synchronized void startTracking(Long totalDocumentsFound, String crawlId) {
        timeStarted = LocalDateTime.now();
        initializeStatus(mainTaskStatus, crawlId, timeStarted);
        initializeStatus(vectorTaskStatus, crawlId, timeStarted);
        setTotalDocumentsFound(totalDocumentsFound);
        updateProgress();
        log.info("Indexing started with indexing id: {}", crawlId);
    }

    private void initializeStatus(IndexingStatus status, String crawlId, LocalDateTime timeStarted) {
        status.setIndexingId(crawlId);
        status.setTimeStarted(timeStarted);
        status.setCurrentStatusMessage("started");
        status.setOverallStatus(IndexingStatus.OverallStatus.RUNNING);
    }

    public synchronized void setTotalDocumentsFound(Long totalDocuments) {
        mainDocumentsFound.set(totalDocuments);
        vectorDocumentsFound.set(totalDocuments);
    }

    public synchronized void updateProgress() {
        updateProgress(TaskType.MAIN);
        updateProgress(TaskType.VECTOR);
    }

    private void updateProgress(TaskType taskType) {
        switch (taskType) {
            case MAIN:
                updateProgressForStatus(mainTaskStatus, mainDocumentsFound, mainDocumentsProcessed, mainDocumentsFailed);
                break;
            case VECTOR:
                updateProgressForStatus(vectorTaskStatus, vectorDocumentsFound, vectorDocumentsProcessed, vectorDocumentsFailed);
                break;
            default:
                throw new IllegalArgumentException("Unknown task type: " + taskType);
        }
    }

    private void updateProgressForStatus(IndexingStatus status, AtomicLong documentsFound, AtomicInteger documentsProcessed, AtomicInteger documentsFailed) {
        if (status.getOverallStatus() == IndexingStatus.OverallStatus.NOT_STARTED) {
            return;
        }

        status.setTotalDocumentsFound(documentsFound.get());
        status.setTotalDocumentsProcessed(documentsProcessed.get());
        status.setTotalDocumentsFailed(documentsFailed.get());

        // Update percent complete
        long totalFound = documentsFound.get();
        int totalProcessed = documentsProcessed.get();
        status.setPercentComplete(totalFound > 0 ? ((float) totalProcessed / totalFound) * 100 : 0);

        // Update average documents per second
        LocalDateTime endTime = status.getEndTime() != null ? status.getEndTime() : LocalDateTime.now();
        long durationInSeconds = Duration.between(status.getTimeStarted(), endTime).getSeconds();
        float avgDocsPerSecond = durationInSeconds > 0 ? (float) totalProcessed / durationInSeconds : 0;
        status.setAverageDocsPerSecond(avgDocsPerSecond);
    }

    public synchronized void finalizeTracking(TaskType taskType) {
        switch (taskType) {
            case MAIN:
                finalizeStatus(mainTaskStatus);
                break;
            case VECTOR:
                finalizeStatus(vectorTaskStatus);
                break;
            default:
                throw new IllegalArgumentException("Unknown task type: " + taskType);
        }
    }

    private void finalizeStatus(IndexingStatus status) {
        status.setEndTime(LocalDateTime.now());
        status.setCurrentStatusMessage("completed");
        status.setOverallStatus(IndexingStatus.OverallStatus.COMPLETED);
        calculateFinalStatistics(status);
        recordHistory(status);
    }

    private void calculateFinalStatistics(IndexingStatus status) {
        if (status.getEndTime() != null && status.getTimeStarted() != null) {
            long durationInSeconds = Duration.between(status.getTimeStarted(), status.getEndTime()).getSeconds();
            if (durationInSeconds > 0) {
                status.setAverageDocsPerSecond((float) status.getTotalDocumentsProcessed() / durationInSeconds);
            }
        }
    }

    private synchronized void recordHistory(IndexingStatus status) {
        if (indexingHistory.size() >= maxHistorySize) {
            indexingHistory.remove(0);
        }
        indexingHistory.add(status.clone());
    }

    public synchronized List<IndexingStatus> getHistory(int limit) {
        return indexingHistory.stream().limit(limit).collect(Collectors.toList());
    }

    public void documentProcessed() {
        documentProcessed(TaskType.MAIN);
    }

    public void vectorDocumentProcessed() {
        documentProcessed(TaskType.VECTOR);
    }

    public void documentFailed() {
        log.info("DOCUMENT FAILURE IN MAIN DOC");
        documentFailed(TaskType.MAIN);
    }

    public void vectorDocumentFailed() {
        log.info("DOCUMENT FAILURE IN VECTOR DOC");

        documentFailed(TaskType.VECTOR);
    }

    private void documentProcessed(TaskType taskType) {
        synchronized (this) {
            switch (taskType) {
                case MAIN:
                    mainDocumentsProcessed.incrementAndGet();
                    break;
                case VECTOR:
                    vectorDocumentsProcessed.incrementAndGet();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown task type: " + taskType);
            }
            updateProgress(taskType);
            checkIfFinished();
        }
    }

    private void documentFailed(TaskType taskType) {
        synchronized (this) {
            switch (taskType) {
                case MAIN:
                    mainDocumentsFailed.incrementAndGet();
                    break;
                case VECTOR:
                    vectorDocumentsFailed.incrementAndGet();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown task type: " + taskType);
            }
            updateProgress(taskType);
            checkIfFinished();
        }
    }

    private void checkIfFinished() {
        synchronized (this) {
            boolean isMainFinished = mainDocumentsProcessed.get() + mainDocumentsFailed.get() == mainDocumentsFound.get();
            boolean isVectorFinished = vectorDocumentsProcessed.get() + vectorDocumentsFailed.get() == vectorDocumentsFound.get();

            if (isMainFinished) {
                finalizeTracking(TaskType.MAIN);
            }
            if (isVectorFinished) {
                finalizeTracking(TaskType.VECTOR);
            }
        }
    }

    public synchronized void markIndexingAsFailed() {
        markIndexingAsFailed(TaskType.MAIN);
        markIndexingAsFailed(TaskType.VECTOR);
    }

    public synchronized void markIndexingAsFailed(TaskType taskType) {
        switch (taskType) {
            case MAIN:
                markStatusAsFailed(mainTaskStatus);
                break;
            case VECTOR:
                markStatusAsFailed(vectorTaskStatus);
                break;
            default:
                throw new IllegalArgumentException("Unknown task type: " + taskType);
        }
    }

    private void markStatusAsFailed(IndexingStatus status) {
        status.setOverallStatus(IndexingStatus.OverallStatus.FAILED);
        status.setCurrentStatusMessage("failed");
    }

    // Helper functions
    public synchronized float getPercentComplete(TaskType taskType) {
        return switch (taskType) {
            case MAIN -> mainTaskStatus.getPercentComplete();
            case VECTOR -> vectorTaskStatus.getPercentComplete();
        };
    }

    public synchronized float getAverageDocsPerSecond(TaskType taskType) {
        LocalDateTime endTime;
        long durationInSeconds;

        switch (taskType) {
            case MAIN:
                endTime = mainTaskStatus.getEndTime() != null ? mainTaskStatus.getEndTime() : LocalDateTime.now();
                durationInSeconds = Duration.between(mainTaskStatus.getTimeStarted(), endTime).getSeconds();
                return durationInSeconds > 0 ? (float) mainDocumentsProcessed.get() / durationInSeconds : 0;

            case VECTOR:
                endTime = vectorTaskStatus.getEndTime() != null ? vectorTaskStatus.getEndTime() : LocalDateTime.now();
                durationInSeconds = Duration.between(vectorTaskStatus.getTimeStarted(), endTime).getSeconds();
                return durationInSeconds > 0 ? (float) vectorDocumentsProcessed.get() / durationInSeconds : 0;

            default:
                throw new IllegalArgumentException("Unknown task type: " + taskType);
        }
    }

    public enum TaskType {
        MAIN, VECTOR
    }
}