package com.krickert.search.indexer.service;

import com.krickert.search.service.ChunkServiceGrpc;
import com.krickert.search.service.EmbeddingServiceGrpc;
import com.krickert.search.service.HealthCheckRequest;
import io.grpc.StatusRuntimeException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class HealthService {
    private static final Logger log = LoggerFactory.getLogger(HealthService.class);
    private final EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingService;
    private final ChunkServiceGrpc.ChunkServiceBlockingStub chunkService;

    @Inject
    public HealthService(@Named("vectorEmbeddingService") EmbeddingServiceGrpc.EmbeddingServiceBlockingStub vectorEmbeddingService,
                         @Named("vectorChunkerService") ChunkServiceGrpc.ChunkServiceBlockingStub chunkService) {
        this.embeddingService = vectorEmbeddingService;
        this.chunkService = chunkService;
        log.info("Set up the chunking and embedding service managed channels.");
    }

    public boolean checkVectorizerHealth() {
        try {
            return embeddingService.check(HealthCheckRequest.newBuilder().build()) != null;
        } catch (StatusRuntimeException e) {
            log.error("vector service not running. semantic indexing is going to be disabled.");
            return false;
        }
    }

    public boolean checkChunkerHealth() {
        try {
            return chunkService.check(HealthCheckRequest.newBuilder().build()) != null;
        } catch (StatusRuntimeException e) {
            log.error("vector service not running. semantic indexing is going to be disabled.");
            return false;
        }
    }

    public boolean isGrpcServiceAvailable() {
        return checkVectorizerHealth() && checkChunkerHealth();
    }
}