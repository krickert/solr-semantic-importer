package com.krickert.search.indexer.solr.vector.event;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.config.VectorConfig;
import com.krickert.search.indexer.solr.SchemaConstants;
import com.krickert.search.indexer.solr.client.SolrClientService;
import com.krickert.search.indexer.tracker.IndexingTracker;
import com.krickert.search.service.ChunkServiceGrpc;
import com.krickert.search.service.EmbeddingServiceGrpc;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
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
    private final Http2SolrClient vectorSolrClient;
    private final IndexingTracker indexingTracker;
    private final Integer batchSize;
    private final ChunkDocumentCreator chunkDocumentCreator;
    private final String parentCollection;

    public ChunkDocumentListener(IndexerConfiguration indexerConfiguration,
                                 SolrClientService solrClientService,
                                 IndexingTracker indexingTracker,
                                 @Named("vectorEmbeddingService") EmbeddingServiceGrpc.EmbeddingServiceBlockingStub vectorEmbeddingService,
                                 @Named("vectorChunkerService") ChunkServiceGrpc.ChunkServiceBlockingStub chunkingService) {
        this.chunkVectorConfig = indexerConfiguration.getChunkVectorConfig();
        this.vectorSolrClient = solrClientService.vectorSolrClient();
        this.indexingTracker = indexingTracker;
        this.chunkDocumentCreator = new ChunkDocumentCreator(chunkingService, vectorEmbeddingService, 3);
        Integer vectorBatchSize = indexerConfiguration.getIndexerConfigurationProperties().getVectorBatchSize();
        this.batchSize = vectorBatchSize == null || vectorBatchSize < 1 ? DEFAULT_BATCH_SIZE : vectorBatchSize;
        log.info("Batch size for the chunk listener is set to {}", this.batchSize);
        this.parentCollection = indexerConfiguration.getDestinationSolrConfiguration().getCollection();
    }

    @Override
    public void processDocument(SolrInputDocument document) {
        log.info("Processing side vector for document with ID: {}", document.getFieldValue(SchemaConstants.ID));

        assertRequiredFieldsPresent(document);

        String origDocId = document.getFieldValue(SchemaConstants.ID).toString();

        chunkVectorConfig.forEach((fieldName, vectorConfig) -> {
            try {
                log.info("processing field {} for document with ID {}", fieldName, origDocId);
                processField(new ChunkDocumentRequest(document, fieldName, vectorConfig, origDocId, parentCollection));
            } catch (RuntimeException e) {
                log.error("could not process document with id {} due to error: {}", origDocId, e.getMessage(), e);
                indexingTracker.vectorDocumentFailed();
            }}
        );
    }

    private void assertRequiredFieldsPresent(SolrInputDocument document) {
        assert document.getFieldValue(SchemaConstants.ID) != null;
        assert document.getFieldValue(SchemaConstants.CRAWL_ID) != null;

        if (document.getFieldValue(SchemaConstants.CRAWL_DATE) == null) {
            log.warn("CRAWL_DATE should never be null: {}", document);
        }
    }

    private void processField(ChunkDocumentRequest request) {
        Object fieldValue = request.getDocument().getFieldValue(request.getFieldName());

        if (fieldValue == null) {
            log.warn("Field '{}' is null for document with ID '{}'. Skipping processing for this field.", request.getFieldName(), request.getOrigDocId());
            indexingTracker.vectorDocumentProcessed();
            return;
        }
        request.setFieldData(fieldValue.toString());
        request.setCrawlId(request.getDocument().getFieldValue(SchemaConstants.CRAWL_ID).toString());
        request.setDateCreated(request.getDocument().getFieldValue(SchemaConstants.CRAWL_DATE));
        processChunkField(request);
    }

    private void processChunkField(ChunkDocumentRequest request) {
        List<SolrInputDocument> docs = chunkDocumentCreator.getChunkedSolrInputDocuments(request);
        boolean hasError = false;
        for (int i = 0; i < docs.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, docs.size());
            List<SolrInputDocument> chunkDocuments = docs.subList(i, endIndex);
            try {
                log.info("Adding chunks for parent id {} with {} documents to the {} collection with type VECTOR and document chunk batch {}", request.getOrigDocId(), chunkDocuments.size(), request.getVectorConfig().getDestinationCollection(), i);
                vectorSolrClient.add(request.getVectorConfig().getDestinationCollection(), chunkDocuments);
                log.info("Added {} documents to the {} collection with type VECTOR and document chunk batch {}", chunkDocuments.size(), request.getVectorConfig().getDestinationCollection(), i);
            } catch (SolrServerException | IOException e) {
                log.error("Could not process document with ID {} due to error: {}", request.getOrigDocId(), e.getMessage());
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