package com.krickert.search.indexer.solr.vector.event;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.config.VectorConfig;
import com.krickert.search.indexer.solr.SchemaConstants;
import com.krickert.search.indexer.solr.client.SolrClientService;
import com.krickert.search.indexer.tracker.IndexingTracker;
import com.krickert.search.service.*;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
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
    private final ChunkServiceGrpc.ChunkServiceBlockingStub chunkServiceBlockingStub;
    private final EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingServiceBlockingStub;
    private final ConcurrentUpdateHttp2SolrClient vectorSolrClient;
    private final IndexingTracker indexingTracker;
    private final Integer batchSize;

    public ChunkDocumentListener(IndexerConfiguration indexerConfiguration,
                                 @Named("chunkService") ChunkServiceGrpc.ChunkServiceBlockingStub chunkServiceBlockingStub,
                                 @Named("vectorEmbeddingService") EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingServiceBlockingStub,
                                 SolrClientService solrClientService,
                                 IndexingTracker indexingTracker) {
        this.chunkVectorConfig = indexerConfiguration.getChunkVectorConfig();
        this.chunkServiceBlockingStub = chunkServiceBlockingStub;
        this.embeddingServiceBlockingStub = embeddingServiceBlockingStub;
        this.vectorSolrClient = solrClientService.vectorConcurrentClient();
        this.indexingTracker = indexingTracker;
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
        ChunkRequest request = createChunkRequest(fieldData, vectorConfig);
        ChunkReply chunkerReply = getChunks(request);

        log.info("There are {} chunks in document with ID {}", chunkerReply.getChunksCount(), origDocId);

        List<String> chunksList = chunkerReply.getChunksList();
        boolean hasError = false;
        for (int i = 0; i < chunksList.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, chunksList.size());
            List<String> chunkBatch = chunksList.subList(i, endIndex);

            EmbeddingsVectorsReply batchReply = getEmbeddingsVectorsReply(chunkBatch);
            List<SolrInputDocument> chunkDocuments = createChunkDocuments(fieldName, batchReply.getEmbeddingsList(), chunkBatch, i, origDocId, crawlId, dateCreated, vectorConfig.getFieldVectorName());

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

    private List<SolrInputDocument> createChunkDocuments(String fieldName, List<EmbeddingsVectorReply> embeddingsList, List<String> chunksList, int chunkBatch, String origDocId, String crawlId, Object dateCreated, String chunkVectorFieldName) {
        List<SolrInputDocument> chunkDocuments = new ArrayList<>(chunksList.size());

        for (int i = 0; i < chunksList.size(); i++) {
            SolrInputDocument docToAdd = createSolrInputDocument(origDocId, chunksList.get(i), i * (chunkBatch + 1), embeddingsList.get(i).getEmbeddingsList(), fieldName, crawlId, dateCreated, chunkVectorFieldName);
            chunkDocuments.add(docToAdd);
        }

        return chunkDocuments;
    }

    @Retryable(attempts = "3", delay = "1s", multiplier = "2.0", includes = {io.grpc.StatusRuntimeException.class})
    protected ChunkReply getChunks(ChunkRequest request) {
        return chunkServiceBlockingStub.chunk(request);
    }

    @Retryable(attempts = "3", delay = "1s", multiplier = "2.0", includes = {io.grpc.StatusRuntimeException.class})
    protected EmbeddingsVectorsReply getEmbeddingsVectorsReply(List<String> fieldDataList) {
        return embeddingServiceBlockingStub.createEmbeddingsVectors(EmbeddingsVectorsRequest.newBuilder().addAllText(fieldDataList).build());
    }

    public static SolrInputDocument createSolrInputDocument(String origDocId, String chunk, int chunkNumber, Collection<Float> vector, String parentFieldName, String crawlId, Object dateCreated, String vectorFieldName) {
        String docId = origDocId + "#" + StringUtils.leftPad(String.valueOf(chunkNumber), 7, "0");

        SolrInputDocument document = new SolrInputDocument();
        document.addField(SchemaConstants.ID, docId);
        document.addField("doc_id", docId);
        document.addField("parent_id", origDocId);
        document.addField("chunk", chunk);
        document.addField("chunk_number", chunkNumber);
        document.addField(vectorFieldName, vector);
        document.addField("parent_field_name", parentFieldName);
        document.addField(SchemaConstants.CRAWL_ID, crawlId);
        document.addField(SchemaConstants.CRAWL_DATE, dateCreated);

        return document;
    }

    private ChunkRequest createChunkRequest(String fieldData, VectorConfig vectorConfig) {
        return ChunkRequest.newBuilder()
                .setText(fieldData)
                .setOptions(ChunkOptions.newBuilder()
                        .setLength(vectorConfig.getChunkSize())
                        .setOverlap(vectorConfig.getChunkOverlap())
                        .build())
                .build();
    }
}