package com.krickert.search.indexer.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@EachProperty("vector-config")
@Introspected
@Serdeable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VectorConfig {

    @JsonProperty("field-name")
    private String fieldName;

    @JsonProperty("chunk-overlap")
    private Integer chunkOverlap;

    @JsonProperty("chunkSize")
    private Integer chunkSize;

    @JsonProperty("chunk-field")
    private Boolean chunkField;

    @JsonProperty("max-chars")
    private Integer maxChars;

    @JsonProperty("model")
    private String model;

    @JsonProperty("destination-collection")
    private String destinationCollection;

    @JsonProperty("field-vector-name")
    private String fieldVectorName;

    @JsonProperty("chunk-field-name-requested")
    private String chunkFieldNameRequested;

    @JsonProperty("similarity-function")
    private String similarityFunction;

    @JsonProperty("hnsw-max-connections")
    private Integer hnswMaxConnections;

    @JsonProperty("hnsw-beam-width")
    private Integer hnswBeamWidth;

    @JsonProperty("collection-creation")
    private VectorCollectionCreationConfig collectionCreation;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Integer getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(Integer chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Boolean getChunkField() {
        return chunkField;
    }

    public void setChunkField(Boolean chunkField) {
        this.chunkField = chunkField;
    }

    public Integer getMaxChars() {
        return maxChars;
    }

    public void setMaxChars(Integer maxChars) {
        this.maxChars = maxChars;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getDestinationCollection() {
        return destinationCollection;
    }

    public void setDestinationCollection(String destinationCollection) {
        this.destinationCollection = destinationCollection;
    }

    public String getFieldVectorName() {
        return fieldVectorName;
    }

    public void setFieldVectorName(String fieldVectorName) {
        this.fieldVectorName = fieldVectorName;
    }

    public String getChunkFieldNameRequested() {
        return chunkFieldNameRequested;
    }

    public void setChunkFieldNameRequested(String chunkFieldNameRequested) {
        this.chunkFieldNameRequested = chunkFieldNameRequested;
    }

    public String getSimilarityFunction() {
        return similarityFunction;
    }

    public void setSimilarityFunction(String similarityFunction) {
        this.similarityFunction = similarityFunction;
    }

    public Integer getHnswMaxConnections() {
        return hnswMaxConnections;
    }

    public void setHnswMaxConnections(Integer hnswMaxConnections) {
        this.hnswMaxConnections = hnswMaxConnections;
    }

    public Integer getHnswBeamWidth() {
        return hnswBeamWidth;
    }

    public void setHnswBeamWidth(Integer hnswBeamWidth) {
        this.hnswBeamWidth = hnswBeamWidth;
    }

    public VectorCollectionCreationConfig getCollectionCreation() {
        return collectionCreation;
    }

    public void setCollectionCreation(VectorCollectionCreationConfig collectionCreation) {
        this.collectionCreation = collectionCreation;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("fieldName", fieldName)
                .add("chunkOverlap", chunkOverlap)
                .add("chunkSize", chunkSize)
                .add("chunkField", chunkField)
                .add("maxChars", maxChars)
                .add("model", model)
                .add("destinationCollection", destinationCollection)
                .add("fieldVectorName", fieldVectorName)
                .add("chunkFieldNameRequested", chunkFieldNameRequested)
                .add("similarityFunction", similarityFunction)
                .add("hnswMaxConnections", hnswMaxConnections)
                .add("hnswBeamWidth", hnswBeamWidth)
                .add("collectionCreation", collectionCreation)
                .toString();
    }

    @ConfigurationProperties("collection-creation")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Serdeable
    public static class VectorCollectionCreationConfig {

        @JsonProperty("collection-config-file")
        private String collectionConfigFile;

        @JsonProperty("collection-config-name")
        private String collectionConfigName;

        @JsonProperty("number-of-shards")
        private int numberOfShards;

        @JsonProperty("number-of-replicas")
        private int numberOfReplicas;

        public String getCollectionConfigFile() {
            return collectionConfigFile;
        }

        public void setCollectionConfigFile(String collectionConfigFile) {
            this.collectionConfigFile = collectionConfigFile;
        }

        public String getCollectionConfigName() {
            return collectionConfigName;
        }

        public void setCollectionConfigName(String collectionConfigName) {
            this.collectionConfigName = collectionConfigName;
        }

        public int getNumberOfShards() {
            return numberOfShards;
        }

        public void setNumberOfShards(int numberOfShards) {
            this.numberOfShards = numberOfShards;
        }

        public int getNumberOfReplicas() {
            return numberOfReplicas;
        }

        public void setNumberOfReplicas(int numberOfReplicas) {
            this.numberOfReplicas = numberOfReplicas;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("collectionConfigFile", collectionConfigFile)
                    .add("collectionConfigName", collectionConfigName)
                    .add("numberOfShards", numberOfShards)
                    .add("numberOfReplicas", numberOfReplicas)
                    .toString();
        }
    }
}