package com.krickert.search.indexer.config.mapper;


import com.krickert.search.indexer.grpc.HnswOptions;
import com.krickert.search.indexer.grpc.SimilarityFunction;
import com.krickert.search.indexer.grpc.SolrCollectionCreationConfig;
import com.krickert.search.indexer.grpc.VectorConfig;
import com.krickert.search.service.ChunkOptions;
import com.krickert.search.service.DocumentEmbeddingModel;
import org.apache.commons.lang3.StringUtils;

public class VectorConfigMapper {

    public static com.krickert.search.indexer.config.VectorConfig toJava(VectorConfig protoConfig) {
        com.krickert.search.indexer.config.VectorConfig javaConfig = new com.krickert.search.indexer.config.VectorConfig();

        if (protoConfig.hasChunkOptions()) {
            javaConfig.setChunkOverlap(protoConfig.getChunkOptions().getOverlap());
            javaConfig.setChunkSize(protoConfig.getChunkOptions().getLength());
        }


        javaConfig.setChunkField(protoConfig.getChunkField());
        javaConfig.setMaxChars(protoConfig.getMaxChars());

        if (protoConfig.hasEmbeddingModel()) {
            javaConfig.setModel(protoConfig.getEmbeddingModel().getEmbeddingModel());
        }

        javaConfig.setDestinationCollection(protoConfig.getDestinationCollection());
        javaConfig.setFieldVectorName(protoConfig.getChunkFieldVectorName());

        if (protoConfig.hasSimilarityFunction()) {
            javaConfig.setSimilarityFunction(protoConfig.getSimilarityFunction().name());
        }

        if (protoConfig.hasHnswOptions()) {
            javaConfig.setHnswMaxConnections(protoConfig.getHnswOptions().getHnswMaxConnections());
            javaConfig.setHnswBeamWidth(protoConfig.getHnswOptions().getHnswBeamWidth());
        }

        if (protoConfig.hasCollectionCreation()) {
            com.krickert.search.indexer.config.VectorConfig.VectorCollectionCreationConfig collectionCreation = new com.krickert.search.indexer.config.VectorConfig.VectorCollectionCreationConfig();
            collectionCreation.setCollectionConfigFile(protoConfig.getCollectionCreation().getCollectionConfigFile());
            collectionCreation.setCollectionConfigName(protoConfig.getCollectionCreation().getCollectionConfigName());
            collectionCreation.setNumberOfShards(protoConfig.getCollectionCreation().getNumberOfShards());
            collectionCreation.setNumberOfReplicas(protoConfig.getCollectionCreation().getNumberOfReplicas());
            javaConfig.setCollectionCreation(collectionCreation);
        }

        return javaConfig;
    }

    public static VectorConfig toProto(com.krickert.search.indexer.config.VectorConfig javaConfig) {
        VectorConfig.Builder protoBuilder = VectorConfig.newBuilder();

        if (javaConfig.getChunkOverlap() != null && javaConfig.getChunkSize() != null) {
            ChunkOptions chunkOptions = ChunkOptions.newBuilder()
                    .setOverlap(javaConfig.getChunkOverlap())
                    .setLength(javaConfig.getChunkSize())
                    .build();
            protoBuilder.setChunkOptions(chunkOptions);
        }

        protoBuilder.setChunkField(javaConfig.getChunkField());

        if (javaConfig.getMaxChars() != null) {
            protoBuilder.setMaxChars(javaConfig.getMaxChars());
        }

        if (javaConfig.getModel() != null) {
            DocumentEmbeddingModel embeddingModel = DocumentEmbeddingModel.newBuilder()
                    .setEmbeddingModel(javaConfig.getModel())
                    .build();
            protoBuilder.setEmbeddingModel(embeddingModel);
        }

        if (StringUtils.isNotEmpty(javaConfig.getDestinationCollection())) {
            protoBuilder.setDestinationCollection(javaConfig.getDestinationCollection());
        }
        protoBuilder.setChunkFieldVectorName(javaConfig.getFieldVectorName());

        if (javaConfig.getSimilarityFunction() != null) {
            protoBuilder.setSimilarityFunction(SimilarityFunction.valueOf(javaConfig.getSimilarityFunction().toUpperCase()));
        }

        if (javaConfig.getHnswMaxConnections() != null && javaConfig.getHnswBeamWidth() != null) {
            HnswOptions hnswOptions = HnswOptions.newBuilder()
                    .setHnswMaxConnections(javaConfig.getHnswMaxConnections())
                    .setHnswBeamWidth(javaConfig.getHnswBeamWidth())
                    .build();
            protoBuilder.setHnswOptions(hnswOptions);
        }

        if (javaConfig.getCollectionCreation() != null) {
            SolrCollectionCreationConfig collectionCreation = SolrCollectionCreationConfig.newBuilder()
                    .setCollectionConfigFile(javaConfig.getCollectionCreation().getCollectionConfigFile())
                    .setCollectionConfigName(javaConfig.getCollectionCreation().getCollectionConfigName())
                    .setNumberOfShards(javaConfig.getCollectionCreation().getNumberOfShards())
                    .setNumberOfReplicas(javaConfig.getCollectionCreation().getNumberOfReplicas())
                    .build();
            protoBuilder.setCollectionCreation(collectionCreation);
        }

        return protoBuilder.build();
    }
}