package com.krickert.search.indexer.solr.vector.event;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.config.VectorConfig;
import com.krickert.search.indexer.solr.client.SolrClientService;
import com.krickert.search.indexer.tracker.IndexingTracker;
import com.krickert.search.service.EmbeddingServiceGrpc;
import com.krickert.search.service.EmbeddingsVectorReply;
import com.krickert.search.service.EmbeddingsVectorRequest;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

@Singleton
public class InlineDocumentListener implements DocumentListener {

    private static final Logger log = LoggerFactory.getLogger(InlineDocumentListener.class);
    private final Map<String, VectorConfig> inlineVectorConfig;
    private final EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingServiceBlockingStub;
    private final ConcurrentUpdateHttp2SolrClient inlineSolrClient;
    private final String destinationCollectionName;
    private final IndexingTracker indexingTracker;

    public InlineDocumentListener(SolrClientService solrClientService,
                                  IndexerConfiguration indexerConfiguration,
                                  @Named("inlineEmbeddingService") EmbeddingServiceGrpc.EmbeddingServiceBlockingStub inlineEmbeddingService,
                                  IndexingTracker indexingTracker) {

        this.inlineSolrClient =  solrClientService.inlineConcurrentClient();
        this.inlineVectorConfig = indexerConfiguration.getInlineVectorConfig();
        this.embeddingServiceBlockingStub = inlineEmbeddingService;
        this.destinationCollectionName = indexerConfiguration.getDestinationSolrConfiguration().getCollection();
        this.indexingTracker = indexingTracker;
    }

    @Override
    public void processDocument(SolrInputDocument document) {
        String origDocId = document.getFieldValue("id").toString();
        log.info("Processing inline vector for document with ID: {}", origDocId);
        try {
            inlineVectorConfig.forEach((fieldName, vectorConfig) -> {
                String fieldData = Optional.ofNullable(document.getFieldValue(fieldName))
                        .map(Object::toString)
                        .orElse(null);
                processInlineDocumentField(document, fieldName, fieldData, origDocId);
            });
        } catch (RuntimeException e) {
            log.error("could not process document with id {} due to error: {}", origDocId, e.getMessage());
            indexingTracker.documentFailed();
            return;
        }
        try {
            inlineSolrClient.add(destinationCollectionName, document);
        } catch (Exception e) {
            log.error("could not process document with id {} due to error: {}", origDocId, e.getMessage());
            indexingTracker.documentFailed();
            return;
        }
        indexingTracker.documentProcessed();
    }

    private void processInlineDocumentField(SolrInputDocument solrInputDocument, String fieldName, String fieldData, String origDocId) {
        // If the field data is null, log a warning and return early
        if (fieldData == null) {
            log.warn("Field data for {} is null in document with id {}", fieldName, origDocId);
            return;
        }

        // Retrieve the vector configuration for the given field name
        VectorConfig vectorConfig = inlineVectorConfig.get(fieldName);

        // Determine the final field data, possibly truncated if it exceeds the maximum allowed characters
        String finalFieldData = getFinalFieldData(fieldData, vectorConfig);

        // Get the name of the vector field from the configuration
        String vectorFieldName = vectorConfig.getChunkFieldVectorName();

        // Generate embeddings vector reply based on the processed field data
        EmbeddingsVectorReply embeddingsVectorReply = getEmbeddingsVectorReply(finalFieldData);

        // Add the embeddings to the Solr input document
        solrInputDocument.addField(vectorFieldName, embeddingsVectorReply.getEmbeddingsList());
    }

    private String getFinalFieldData(String fieldData, VectorConfig vectorConfig) {
        // Check if the vector config has a valid maximum character limit and truncate if necessary
        if (vectorConfig.getMaxChars() != null && vectorConfig.getMaxChars() > 0 && fieldData.length() > vectorConfig.getMaxChars()) {
            return StringUtils.truncate(fieldData, vectorConfig.getMaxChars());
        }
        return fieldData;
    }

    @Retryable(attempts = "3", delay = "1s", multiplier = "2.0", includes = {io.grpc.StatusRuntimeException.class})
    protected EmbeddingsVectorReply getEmbeddingsVectorReply(String fieldData) {
        return embeddingServiceBlockingStub.createEmbeddingsVector(EmbeddingsVectorRequest.newBuilder().setText(fieldData).build());
    }

}