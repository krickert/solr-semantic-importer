package com.krickert.search.indexer.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
@JsonInclude(JsonInclude.Include.NON_NULL)
@Serdeable
public class IndexerConfiguration {

    @JsonProperty("name")
    private String name = "default";

    @JsonProperty("indexer-config")
    private final IndexerConfigurationProperties indexerConfigurationProperties;

    @JsonProperty("solr-config")
    private final Map<String, SolrConfiguration> solrConfiguration;

    @JsonProperty("vector-config")
    private final Map<String, VectorConfig> vectorConfig;

    private final Map<String, VectorConfig> inlineVectorConfig;

    private final Map<String, VectorConfig> chunkVectorConfig;

    @Inject
    public IndexerConfiguration(
            @JsonProperty("indexerConfigurationProperties") IndexerConfigurationProperties indexerConfigurationProperties,
            Collection<SolrConfiguration> solrConfigurations,
            @JsonProperty("vectorConfig") Map<String, VectorConfig> vectorConfig
    ) {
        this.solrConfiguration = solrConfigurations.stream()
                .collect(Collectors.toMap(SolrConfiguration::getName, solrConfiguration -> solrConfiguration));
        String destinationCollection = solrConfiguration.get("destination").getCollection();

        this.indexerConfigurationProperties = indexerConfigurationProperties;
        this.vectorConfig = createVectorConfig(checkNotNull(vectorConfig));
        this.chunkVectorConfig = this.vectorConfig.entrySet().stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getValue().getChunkField() &&
                        !destinationCollection.equals(entry.getValue().getDestinationCollection()))) // Handles nulls by treating them as false
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.inlineVectorConfig = this.vectorConfig.entrySet().stream()
                .filter(entry -> (entry.getValue().getDestinationCollection() != null && entry.getValue().getDestinationCollection().equals(destinationCollection)) || Boolean.FALSE.equals(entry.getValue().getChunkField())) // Handles nulls by treating them as false
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    /**
     * Creates a new vector configuration map where the keys are updated based on a field name
     * specified within each VectorConfig object. If the field name is not specified, the original key is used.
     *
     * @param vectorConfig the original vector configuration map with keys as strings
     *                      and values as VectorConfig objects.
     * @return a new map with updated keys from the field names specified in the VectorConfig objects.
     */
    private Map<String, VectorConfig> createVectorConfig(Map<String, VectorConfig> vectorConfig) {
        // Initialize a new HashMap with the same size as the input map for efficient memory allocation
        Map<String, VectorConfig> result = new HashMap<>(vectorConfig.size());

        // Iterate over each entry in the input map
        for (Map.Entry<String, VectorConfig> entry : vectorConfig.entrySet()) {
            String key = entry.getKey();                // Get current key
            String fieldNameInConfig = entry.getValue().getFieldName(); // Get field name from config

            if (fieldNameInConfig == null) {
                entry.getValue().setFieldName(key);
            }
            // Put the possibly new key and the same value into the result map
            result.put(key, entry.getValue());
        }

        // Return the newly created map
        return result;
    }

    public IndexerConfigurationProperties getIndexerConfigurationProperties() {
        return indexerConfigurationProperties;
    }

    public SolrConfiguration getSourceSolrConfiguration() {
        return solrConfiguration.get("source");
    }

    public SolrConfiguration getDestinationSolrConfiguration() {
        return solrConfiguration.get("destination");
    }

    public Map<String, VectorConfig> getVectorConfig() {
        return vectorConfig;
    }

    protected Map<String, SolrConfiguration> getSolrConfiguration() {
        return solrConfiguration;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("indexerConfigurationProperties", indexerConfigurationProperties)
                .add("solrConfiguration", solrConfiguration)
                .add("vectorConfig", vectorConfig)
                .toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, VectorConfig> getInlineVectorConfig() {
        return inlineVectorConfig;
    }

    public Map<String, VectorConfig> getChunkVectorConfig() {
        return chunkVectorConfig;
    }
}