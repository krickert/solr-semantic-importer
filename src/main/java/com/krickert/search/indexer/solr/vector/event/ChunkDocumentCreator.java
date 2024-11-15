package com.krickert.search.indexer.solr.vector.event;

import com.krickert.search.indexer.config.VectorConfig;
import com.krickert.search.indexer.solr.SchemaConstants;
import com.krickert.search.service.*;
import io.micronaut.context.annotation.Prototype;
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

public final class ChunkDocumentCreator {
    private static final Logger log = LoggerFactory.getLogger(ChunkDocumentCreator.class);
    private final ChunkServiceGrpc.ChunkServiceBlockingStub chunkServiceBlockingStub;
    private final EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingServiceBlockingStub;
    private final int batchSize;

    public ChunkDocumentCreator(ChunkServiceGrpc.ChunkServiceBlockingStub chunkServiceBlockingStub,
                                EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingServiceBlockingStub,
                                int batchSize) {
        this.chunkServiceBlockingStub = chunkServiceBlockingStub;
        this.embeddingServiceBlockingStub = embeddingServiceBlockingStub;
        this.batchSize = batchSize;
    }

    @NotNull List<SolrInputDocument> getChunkedSolrInputDocuments(ChunkDocumentRequest request) {
        ChunkReply chunkerReply = getChunks(request);
        log.info("There are {} chunks in document with ID {}", chunkerReply.getChunksCount(), request.getOrigDocId());

        List<String> chunksList = chunkerReply.getChunksList();
        List<SolrInputDocument> docs = new ArrayList<>(chunksList.size());
        for (int i = 0; i < chunksList.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, chunksList.size());
            List<String> chunkBatch = chunksList.subList(i, endIndex);

            EmbeddingsVectorsReply batchReply = getEmbeddingsVectorsReply(chunkBatch);
            docs.addAll(createChunkDocuments(request, batchReply.getEmbeddingsList(), chunkBatch, i));
        }
        return docs;
    }

    protected ChunkReply getChunks(ChunkDocumentRequest request) {
        return chunkServiceBlockingStub.chunk(createChunkRequest(request.getFieldData(), request.getVectorConfig()));
    }

    @Retryable(attempts = "3", delay = "1s", multiplier = "2.0", includes = {io.grpc.StatusRuntimeException.class})
    protected EmbeddingsVectorsReply getEmbeddingsVectorsReply(List<String> fieldDataList) {
        return embeddingServiceBlockingStub.createEmbeddingsVectors(EmbeddingsVectorsRequest.newBuilder().addAllText(fieldDataList).build());
    }

    public List<SolrInputDocument> createChunkDocuments(ChunkDocumentRequest request, List<EmbeddingsVectorReply> embeddingsList,
                                                        List<String> chunksList, int chunkBatch) {
        List<SolrInputDocument> chunkDocuments = new ArrayList<>(chunksList.size());

        for (int i = 0; i < chunksList.size(); i++) {
            SolrInputDocument docToAdd = createSolrInputDocument(request, chunksList.get(i), i * (chunkBatch + 1), embeddingsList.get(i).getEmbeddingsList());
            chunkDocuments.add(docToAdd);
        }

        return chunkDocuments;
    }

    public SolrInputDocument createSolrInputDocument(ChunkDocumentRequest request, String chunk, int chunkNumber, Collection<Float> vector) {
        String docId = request.getOrigDocId() + request.getFieldName() + "#" + StringUtils.leftPad(String.valueOf(chunkNumber), 7, "0");

        SolrInputDocument document = new SolrInputDocument();
        document.addField(SchemaConstants.ID, docId);
        document.addField(SchemaConstants.DOC_ID, docId);
        document.addField(SchemaConstants.PARENT_ID, request.getOrigDocId());
        document.addField(SchemaConstants.CHUNK, chunk);
        document.addField(SchemaConstants.CHUNK_NUMBER, chunkNumber);
        document.addField(request.getVectorConfig().getFieldVectorName(), vector);
        document.addField(SchemaConstants.PARENT_FIELD_NAME, request.getFieldName());
        document.addField(SchemaConstants.CRAWL_ID, request.getCrawlId());
        document.addField(SchemaConstants.CRAWL_DATE, request.getDateCreated());
        document.addField(SchemaConstants.CREATION_DATE, request.getDateCreated());
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