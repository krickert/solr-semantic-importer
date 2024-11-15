package com.krickert.search.indexer.grpc;

import com.krickert.search.service.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddingServiceMockTest {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceMockTest.class);

    @Test
    void testMock() {
        EmbeddingServiceGrpc.EmbeddingServiceBlockingStub mockStub = EmbeddingServiceMock.createMock();

        // Test createEmbeddingsVector
        EmbeddingsVectorRequest vectorRequest = EmbeddingsVectorRequest.newBuilder()
                .setText("dummy text")
                .build();
        EmbeddingsVectorReply vectorReply = mockStub.createEmbeddingsVector(vectorRequest);
        log.debug("createEmbeddingsVector: " + vectorReply.getEmbeddingsList());

        // Test createEmbeddingsVectors
        EmbeddingsVectorsRequest vectorsRequest = EmbeddingsVectorsRequest.newBuilder()
                .addText("text one")
                .addText("text two")
                .build();
        EmbeddingsVectorsReply vectorsReply = mockStub.createEmbeddingsVectors(vectorsRequest);
        log.debug("createEmbeddingsVectors: " + vectorsReply.getEmbeddingsList());

        // Test check (health check)
        HealthCheckRequest healthRequest = HealthCheckRequest.newBuilder().build();
        HealthCheckReply healthReply = mockStub.check(healthRequest);
        log.debug("check: " + healthReply.getStatus());
    }
}