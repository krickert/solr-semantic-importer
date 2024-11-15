package com.krickert.search.indexer.solr.vector.event;

import com.krickert.search.indexer.config.VectorConfig;
import com.krickert.search.indexer.solr.SchemaConstants;
import com.krickert.search.service.*;
import io.micronaut.context.annotation.Value;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Singleton
public class ChunkDocumentCreator {
    private static final Logger log = LoggerFactory.getLogger(ChunkDocumentCreator.class);
    private final ChunkServiceGrpc.ChunkServiceBlockingStub chunkServiceBlockingStub;
    private final EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingServiceBlockingStub;
    private final int batchSize;

    @Inject
    public ChunkDocumentCreator(@Named("chunkService") ChunkServiceGrpc.ChunkServiceBlockingStub chunkServiceBlockingStub,
                                @Named("vectorEmbeddingService") EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingServiceBlockingStub,
                                @Value("${indexer.vector-batch-size:3}") int batchSize) {
        this.chunkServiceBlockingStub = chunkServiceBlockingStub;
        this.embeddingServiceBlockingStub = embeddingServiceBlockingStub;
        this.batchSize = batchSize;
    }

    @NotNull List<SolrInputDocument> getChunkedSolrInputDocuments(String fieldName, VectorConfig vectorConfig, String fieldData, String origDocId, String crawlId, Object dateCreated) {
        ChunkReply chunkerReply = getChunks(fieldData, vectorConfig);
        log.info("There are {} chunks in document with ID {}", chunkerReply.getChunksCount(), origDocId);

        List<String> chunksList = chunkerReply.getChunksList();
        List<SolrInputDocument> docs = new ArrayList<>(chunksList.size());
        for (int i = 0; i < chunksList.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, chunksList.size());
            List<String> chunkBatch = chunksList.subList(i, endIndex);

            EmbeddingsVectorsReply batchReply = getEmbeddingsVectorsReply(chunkBatch);
            docs.addAll(createChunkDocuments(fieldName, batchReply.getEmbeddingsList(),
                    chunkBatch, i, origDocId, crawlId, dateCreated, vectorConfig.getFieldVectorName()));
        }
        return docs;
    }


    protected ChunkReply getChunks(String fieldData, VectorConfig vectorConfig) {
        return chunkServiceBlockingStub.chunk(createChunkRequest(fieldData, vectorConfig));
    }
    @Retryable(attempts = "3", delay = "1s", multiplier = "2.0", includes = {io.grpc.StatusRuntimeException.class})
    protected ChunkReply getChunks(ChunkRequest request) {
        return chunkServiceBlockingStub.chunk(request);
    }

    @Retryable(attempts = "3", delay = "1s", multiplier = "2.0", includes = {io.grpc.StatusRuntimeException.class})
    protected EmbeddingsVectorsReply getEmbeddingsVectorsReply(List<String> fieldDataList) {
        return embeddingServiceBlockingStub.createEmbeddingsVectors(EmbeddingsVectorsRequest.newBuilder().addAllText(fieldDataList).build());
    }

    public SolrInputDocument createSolrInputDocument(String origDocId, String chunk, int chunkNumber, Collection<Float> vector, String parentFieldName, String crawlId, Object dateCreated, String vectorFieldName) {
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

    public List<SolrInputDocument> createChunkDocuments(String fieldName, List<EmbeddingsVectorReply> embeddingsList,
                                                               List<String> chunksList, int chunkBatch, String origDocId, String crawlId, Object dateCreated, String chunkVectorFieldName) {
        List<SolrInputDocument> chunkDocuments = new ArrayList<>(chunksList.size());

        for (int i = 0; i < chunksList.size(); i++) {
            SolrInputDocument docToAdd = createSolrInputDocument(origDocId, chunksList.get(i), i * (chunkBatch + 1),
                    embeddingsList.get(i).getEmbeddingsList(), fieldName, crawlId, dateCreated, chunkVectorFieldName);
            chunkDocuments.add(docToAdd);
        }

        return chunkDocuments;
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
