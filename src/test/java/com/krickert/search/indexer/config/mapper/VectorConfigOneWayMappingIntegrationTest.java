package com.krickert.search.indexer.config.mapper;

import com.krickert.search.indexer.config.VectorConfig;
import com.krickert.search.indexer.grpc.SimilarityFunction;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
public class VectorConfigOneWayMappingIntegrationTest {

    @Inject
    Map<String, VectorConfig> configPropertiesMap;

    @Test
    void testTitleConfig() {
        VectorConfig titleConfig = configPropertiesMap.get("title");
        assertNotNull(titleConfig);

        // Convert to gRPC config
        com.krickert.search.indexer.grpc.VectorConfig grpcConfig = VectorConfigMapper.toProto(titleConfig);

        // Assertions
        assertFalse(grpcConfig.getChunkField());
        assertFalse(grpcConfig.hasChunkOptions());
        assertEquals("mini-LM", grpcConfig.getEmbeddingModel().getEmbeddingModel());
        assertEquals("title-vector", grpcConfig.getChunkFieldVectorName());
        assertEquals(SimilarityFunction.COSINE, grpcConfig.getSimilarityFunction());
        assertEquals(16, grpcConfig.getHnswOptions().getHnswMaxConnections());
        assertEquals(100, grpcConfig.getHnswOptions().getHnswBeamWidth());
    }

    @Test
    void testBodyConfig() {
        VectorConfig bodyConfig = configPropertiesMap.get("body");
        assertNotNull(bodyConfig);

        // Convert to gRPC config
        com.krickert.search.indexer.grpc.VectorConfig grpcConfig = VectorConfigMapper.toProto(bodyConfig);

        // Assertions
        assertTrue(grpcConfig.getChunkField());
        assertEquals(30, grpcConfig.getChunkOptions().getOverlap());
        assertEquals(300, grpcConfig.getChunkOptions().getLength());
        assertEquals("mini-LM", grpcConfig.getEmbeddingModel().getEmbeddingModel());
        assertEquals("body-vectors", grpcConfig.getDestinationCollection());
        assertEquals("chunk-vector", grpcConfig.getChunkFieldVectorName());
        assertEquals(SimilarityFunction.COSINE, grpcConfig.getSimilarityFunction());
        assertEquals(16, grpcConfig.getHnswOptions().getHnswMaxConnections());
        assertEquals(100, grpcConfig.getHnswOptions().getHnswBeamWidth());
        assertEquals("classpath:default-chunk-config.zip", grpcConfig.getCollectionCreation().getCollectionConfigFile());
        assertEquals("vector-config", grpcConfig.getCollectionCreation().getCollectionConfigName());
        assertEquals(1, grpcConfig.getCollectionCreation().getNumberOfShards());
        assertEquals(2, grpcConfig.getCollectionCreation().getNumberOfReplicas());
    }
}