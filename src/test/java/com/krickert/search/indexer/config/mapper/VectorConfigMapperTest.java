package com.krickert.search.indexer.config.mapper;

import com.krickert.search.indexer.grpc.HnswOptions;
import com.krickert.search.indexer.grpc.SimilarityFunction;
import com.krickert.search.indexer.grpc.SolrCollectionCreationConfig;
import com.krickert.search.service.ChunkOptions;
import com.krickert.search.service.DocumentEmbeddingModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VectorConfigMapperTest {

    @Test
    void testMappingToProto() {
        // Arrange
        com.krickert.search.indexer.config.VectorConfig javaConfig = createTestJavaConfig();

        // Act
        com.krickert.search.indexer.grpc.VectorConfig protoConfig = VectorConfigMapper.toProto(javaConfig);

        // Assert
        assertNotNull(protoConfig);
        assertEquals(javaConfig.getChunkField(), protoConfig.getChunkField());
        assertEquals(javaConfig.getChunkOverlap().intValue(), protoConfig.getChunkOptions().getOverlap());
        assertEquals(javaConfig.getChunkSize().intValue(), protoConfig.getChunkOptions().getLength());
        assertEquals(javaConfig.getModel(), protoConfig.getEmbeddingModel().getEmbeddingModel());
        assertEquals(javaConfig.getDestinationCollection(), protoConfig.getDestinationCollection());
        assertEquals(javaConfig.getFieldVectorName(), protoConfig.getChunkFieldVectorName());
        assertEquals(javaConfig.getSimilarityFunction(), protoConfig.getSimilarityFunction().name());
        assertEquals(javaConfig.getHnswMaxConnections().intValue(), protoConfig.getHnswOptions().getHnswMaxConnections());
        assertEquals(javaConfig.getHnswBeamWidth().intValue(), protoConfig.getHnswOptions().getHnswBeamWidth());
        assertEquals(javaConfig.getCollectionCreation().getCollectionConfigFile(), protoConfig.getCollectionCreation().getCollectionConfigFile());
        assertEquals(javaConfig.getCollectionCreation().getCollectionConfigName(), protoConfig.getCollectionCreation().getCollectionConfigName());
        assertEquals(javaConfig.getCollectionCreation().getNumberOfShards(), protoConfig.getCollectionCreation().getNumberOfShards());
        assertEquals(javaConfig.getCollectionCreation().getNumberOfReplicas(), protoConfig.getCollectionCreation().getNumberOfReplicas());
    }

    @Test
    void testMappingToJava() {
        // Arrange
        com.krickert.search.indexer.grpc.VectorConfig protoConfig = createTestProtoConfig();

        // Act
        com.krickert.search.indexer.config.VectorConfig javaConfig = VectorConfigMapper.toJava(protoConfig);

        // Assert
        assertNotNull(javaConfig);
        assertEquals(protoConfig.getChunkField(), javaConfig.getChunkField());
        assertEquals(protoConfig.getChunkOptions().getOverlap(), javaConfig.getChunkOverlap().intValue());
        assertEquals(protoConfig.getChunkOptions().getLength(), javaConfig.getChunkSize().intValue());
        assertEquals(protoConfig.getEmbeddingModel().getEmbeddingModel(), javaConfig.getModel());
        assertEquals(protoConfig.getDestinationCollection(), javaConfig.getDestinationCollection());
        assertEquals(protoConfig.getChunkFieldVectorName(), javaConfig.getFieldVectorName());
        assertEquals(protoConfig.getSimilarityFunction().name(), javaConfig.getSimilarityFunction());
        assertEquals(protoConfig.getHnswOptions().getHnswMaxConnections(), javaConfig.getHnswMaxConnections().intValue());
        assertEquals(protoConfig.getHnswOptions().getHnswBeamWidth(), javaConfig.getHnswBeamWidth().intValue());
        assertEquals(protoConfig.getCollectionCreation().getCollectionConfigFile(), javaConfig.getCollectionCreation().getCollectionConfigFile());
        assertEquals(protoConfig.getCollectionCreation().getCollectionConfigName(), javaConfig.getCollectionCreation().getCollectionConfigName());
        assertEquals(protoConfig.getCollectionCreation().getNumberOfShards(), javaConfig.getCollectionCreation().getNumberOfShards());
        assertEquals(protoConfig.getCollectionCreation().getNumberOfReplicas(), javaConfig.getCollectionCreation().getNumberOfReplicas());
    }

    private com.krickert.search.indexer.config.VectorConfig createTestJavaConfig() {
        com.krickert.search.indexer.config.VectorConfig javaConfig = new com.krickert.search.indexer.config.VectorConfig();
        javaConfig.setChunkOverlap(5);
        javaConfig.setChunkSize(10);
        javaConfig.setChunkField(true);
        javaConfig.setModel("ALL_MINILM_L12_V2");
        javaConfig.setDestinationCollection("myCollection");
        javaConfig.setFieldVectorName("chunkVector");
        javaConfig.setSimilarityFunction("COSINE");
        javaConfig.setHnswMaxConnections(16);
        javaConfig.setHnswBeamWidth(8);

        com.krickert.search.indexer.config.VectorConfig.VectorCollectionCreationConfig collectionCreation = new com.krickert.search.indexer.config.VectorConfig.VectorCollectionCreationConfig();
        collectionCreation.setCollectionConfigFile("/path/to/config");
        collectionCreation.setCollectionConfigName("testCollection");
        collectionCreation.setNumberOfShards(3);
        collectionCreation.setNumberOfReplicas(2);
        javaConfig.setCollectionCreation(collectionCreation);

        return javaConfig;
    }

    private com.krickert.search.indexer.grpc.VectorConfig createTestProtoConfig() {
        return com.krickert.search.indexer.grpc.VectorConfig.newBuilder()
                .setChunkOptions(ChunkOptions.newBuilder().setOverlap(5).setLength(10).build())
                .setChunkField(true)
                .setEmbeddingModel(DocumentEmbeddingModel.newBuilder().setEmbeddingModel("ALL_MINILM_L12_V2").build())
                .setDestinationCollection("myCollection")
                .setChunkFieldVectorName("chunkVector")
                .setSimilarityFunction(SimilarityFunction.COSINE)
                .setHnswOptions(HnswOptions.newBuilder().setHnswMaxConnections(16).setHnswBeamWidth(8).build())
                .setCollectionCreation(SolrCollectionCreationConfig.newBuilder()
                        .setCollectionConfigFile("/path/to/config")
                        .setCollectionConfigName("testCollection")
                        .setNumberOfShards(3)
                        .setNumberOfReplicas(2)
                        .build())
                .build();
    }
}