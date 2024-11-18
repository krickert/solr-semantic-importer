package com.krickert.search.indexer.config;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
public class VectorConfigTest {

    @Inject
    Map<String, VectorConfig> vectorConfigMap;

    @Test
    void testVectorConfig() {
        // Vector Configuration Assertions
        assertNotNull(vectorConfigMap);

        VectorConfig titleConfig = vectorConfigMap.get("title");
        assertNotNull(titleConfig);
        //Titles don't chunk and nor should you
        assertFalse(titleConfig.getChunkField());
        assertNull(titleConfig.getChunkOverlap());
        assertNull(titleConfig.getChunkSize());
        assertEquals(2000, titleConfig.getMaxChars());
        assertEquals("mini-LM", titleConfig.getModel());
        assertFalse(titleConfig.getChunkField());
        VectorConfig bodyConfig = vectorConfigMap.get("body");
        assertNotNull(bodyConfig);
        assertTrue(bodyConfig.getChunkField());
        assertEquals(30, bodyConfig.getChunkOverlap());
        assertEquals(300, bodyConfig.getChunkSize());
        assertEquals("mini-LM", bodyConfig.getModel());
        assertEquals("body-vectors", bodyConfig.getDestinationCollection());
        assertEquals("chunk-vector", bodyConfig.getFieldVectorName());
        assertEquals("cosine", bodyConfig.getSimilarityFunction());
        assertEquals(16, bodyConfig.getHnswMaxConnections());
        assertEquals(100, bodyConfig.getHnswBeamWidth());
        assertEquals("classpath:default-chunk-config.zip", bodyConfig.getCollectionCreation().getCollectionConfigFile());
        assertEquals("vector-config", bodyConfig.getCollectionCreation().getCollectionConfigName());
        assertEquals(1, bodyConfig.getCollectionCreation().getNumberOfShards());
        assertEquals(2, bodyConfig.getCollectionCreation().getNumberOfReplicas());
    }
}
