package com.krickert.search.indexer.grpc;

import com.krickert.search.indexer.test.TestContainersManager;
import com.krickert.search.service.ChunkServiceGrpc;
import com.krickert.search.service.EmbeddingServiceGrpc;
import io.micronaut.context.env.Environment;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest(environments = Environment.TEST)
class ClientGrpcServiceTest {

    @Inject
    TestContainersManager testContainersManager;

    @Inject
    TestClients testClients;

    @Inject
    @Named("inlineEmbeddingService")
    EmbeddingServiceGrpc.EmbeddingServiceBlockingStub inlineEmbeddingService;

    @Inject
    @Named("vectorEmbeddingService")
    EmbeddingServiceGrpc.EmbeddingServiceBlockingStub vectorEmbeddingService;

    @Inject
    @Named("inlineChunkerService")
    ChunkServiceGrpc.ChunkServiceBlockingStub inlineChunkerService;

    @Inject
    @Named("vectorChunkerService")
    ChunkServiceGrpc.ChunkServiceBlockingStub vectorChunkerService;

    @Test
    void testInlineEmbeddingService() {
        assertNotNull(inlineEmbeddingService, "InlineEmbeddingService should not be null");
    }

    @Test
    void testVectorEmbeddingService() {
        assertNotNull(vectorEmbeddingService, "VectorEmbeddingService should not be null");
    }

    @Test
    void testInlineChunkerService() {
        assertNotNull(inlineChunkerService, "inlineChunkerService should not be null");
    }

    @Test
    void testVectorChunkerService() {
        assertNotNull(vectorChunkerService, "vectorChunkerService should not be null");
    }
}
