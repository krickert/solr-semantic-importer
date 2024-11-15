package com.krickert.search.indexer.solr.vector.event;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.config.VectorConfig;
import com.krickert.search.indexer.solr.SchemaConstants;
import com.krickert.search.indexer.solr.client.SolrClientService;
import com.krickert.search.indexer.tracker.IndexingTracker;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

@Singleton
public class ChunkDocumentListener implements DocumentListener {

    private static final Logger log = LoggerFactory.getLogger(ChunkDocumentListener.class);
    private static final Integer DEFAULT_BATCH_SIZE = 3;
    private final Map<String, VectorConfig> chunkVectorConfig;
    private final ConcurrentUpdateHttp2SolrClient vectorSolrClient;
    private final IndexingTracker indexingTracker;
    private final Integer batchSize;
    private final ChunkDocumentCreator chunkDocumentCreator;

    public ChunkDocumentListener(IndexerConfiguration indexerConfiguration,
                                 SolrClientService solrClientService,
                                 IndexingTracker indexingTracker, ChunkDocumentCreator chunkDocumentCreator) {
        this.chunkVectorConfig = indexerConfiguration.getChunkVectorConfig();
        this.vectorSolrClient = solrClientService.vectorConcurrentClient();
        this.indexingTracker = indexingTracker;
        this.chunkDocumentCreator = chunkDocumentCreator;
        Integer vectorBatchSize = indexerConfiguration.getIndexerConfigurationProperties().getVectorBatchSize();
        if (vectorBatchSize == null || vectorBatchSize < 1) {
            this.batchSize = DEFAULT_BATCH_SIZE;
        } else {
            this.batchSize = vectorBatchSize;
        }
        log.info("Batch size for the chunk listener is set to {}", this.batchSize);
    }

    @Override
    public void processDocument(SolrInputDocument document) {
        log.info("Processing side vector for document with ID: {}", document.getFieldValue(SchemaConstants.ID));

        assertRequiredFieldsPresent(document);

        String origDocId = document.getFieldValue(SchemaConstants.ID).toString();

        chunkVectorConfig.forEach((fieldName, vectorConfig) -> processField(document, fieldName, vectorConfig, origDocId));
    }

    private void assertRequiredFieldsPresent(SolrInputDocument document) {
        assert document.getFieldValue(SchemaConstants.ID) != null;
        assert document.getFieldValue(SchemaConstants.CRAWL_ID) != null;

        if (document.getFieldValue(SchemaConstants.CRAWL_DATE) == null) {
            log.warn("CRAWL_DATE should never be null: {}", document);
        }
    }

    private void processField(SolrInputDocument document, String fieldName, VectorConfig vectorConfig, String origDocId) {
        Object fieldValue = document.getFieldValue(fieldName);

        if (fieldValue == null) {
            log.warn("Field '{}' is null for document with ID '{}'. Skipping processing for this field.", fieldName, origDocId);
            indexingTracker.vectorDocumentProcessed();
            return;
        }
        String fieldData = fieldValue.toString();
        String crawlId = document.getFieldValue(SchemaConstants.CRAWL_ID).toString();
        Object dateCreated = document.getFieldValue(SchemaConstants.CRAWL_DATE);
        processChunkField(fieldName, vectorConfig, fieldData, origDocId, crawlId, dateCreated);
    }

    private void processChunkField(String fieldName, VectorConfig vectorConfig, String fieldData, String origDocId, String crawlId, Object dateCreated) {
        List<SolrInputDocument> docs = chunkDocumentCreator.getChunkedSolrInputDocuments(fieldName, vectorConfig, fieldData, origDocId,
                crawlId, dateCreated);
        boolean hasError = false;
        for (int i = 0; i < docs.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, docs.size());
            List<SolrInputDocument> chunkDocuments = docs.subList(i, endIndex);
            try {
                log.info("Adding chunks for parent id {} with {} documents to the {} collection with type VECTOR and document chunk batch {}", origDocId, chunkDocuments.size(), vectorConfig.getDestinationCollection(), i);
                vectorSolrClient.add(vectorConfig.getDestinationCollection(), chunkDocuments);
                log.info("Addded {} documents to the {} collection with type VECTOR and document chunk batch {}", chunkDocuments.size(), vectorConfig.getDestinationCollection(), i);
            } catch (SolrServerException | IOException e) {
                log.error("Could not process document with ID {} due to error: {}", origDocId, e.getMessage());
                hasError = true;
            }
        }
        if (hasError) {
            indexingTracker.vectorDocumentFailed();
        } else {
            indexingTracker.vectorDocumentProcessed();
        }
    }



}