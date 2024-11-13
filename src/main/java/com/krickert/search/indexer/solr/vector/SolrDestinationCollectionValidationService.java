package com.krickert.search.indexer.solr.vector;

import com.google.common.base.MoreObjects;
import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.config.SolrConfiguration;
import com.krickert.search.indexer.config.VectorConfig;
import com.krickert.search.indexer.service.HealthService;
import com.krickert.search.indexer.solr.client.SolrAdminActions;
import com.krickert.search.indexer.solr.client.SolrClientService;
import com.krickert.search.indexer.solr.client.VectorFieldValidator;
import com.krickert.search.service.EmbeddingServiceGrpc;
import com.krickert.search.service.EmbeddingsVectorRequest;
import io.micronaut.core.util.StringUtils;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.SolrServerException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Singleton
public class SolrDestinationCollectionValidationService {
    private static final Logger log = LoggerFactory.getLogger(SolrDestinationCollectionValidationService.class);

    private final IndexerConfiguration indexerConfiguration;
    private final SolrAdminActions solrAdminActions;
    private final EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingServiceBlockingStub;
    private final Integer dimensionality;
    private final HealthService healthService;
    private final AtomicBoolean embeddingsUp = new AtomicBoolean(false);
    private final VectorFieldValidator vectorFieldValidator;


    @Scheduled(fixedRate = "20s", initialDelay = "200s")
    public void checkEmbeddingsUp() {
        if (healthService.checkVectorizerHealth()) {
            if (dimensionality == null || dimensionality < 1) {
                initializeDimensionality();
            }
            embeddingsUp.set(true);
        }
        if (!embeddingsUp.get()) {
            log.error("Embeddings are not up");
        }

    }

    @Inject
    public SolrDestinationCollectionValidationService(
            IndexerConfiguration indexerConfiguration,
            SolrClientService solrClientService,
            SolrAdminActions solrAdminActions,
            @Named("vectorEmbeddingService")
            EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingServiceBlockingStub,
            HealthService healthService,
            VectorFieldValidator vectorFieldValidator) {
        checkNotNull(solrClientService);
        this.indexerConfiguration = indexerConfiguration;
        this.solrAdminActions = solrAdminActions;
        this.embeddingServiceBlockingStub = embeddingServiceBlockingStub;
        this.healthService = checkNotNull(healthService);
        this.dimensionality = initializeDimensionality();
        this.vectorFieldValidator = vectorFieldValidator;
    }

    private Integer initializeDimensionality() {
        log.info("Initializing dimensionality by creating embeddings vector");
        try {
            int numberOfDimensions = embeddingServiceBlockingStub.createEmbeddingsVector(
                    EmbeddingsVectorRequest.newBuilder().setText("Dummy").build()).getEmbeddingsCount();
            assert numberOfDimensions > 0;
            log.info("Finished initializing dimensionality");
            return numberOfDimensions;
        } catch (Exception e) {
            log.error("Failed to initialize dimensionality due to: {}", e.getMessage(), e);
            embeddingsUp.set(false);
            // Return a default dimensionality when exception occurs
            return null; // if dimensions do no exist make it null
        }
    }

    public Integer getDimensionality() {
        return dimensionality;
    }

    public void validate() {
        log.info("Validating the destination collection");
        validateDestinationCollection();
        log.info("Validating the vector collections");
        validateVectorCollections();
    }

    private void validateVectorCollections() {
        Map<String, VectorConfig> configs = indexerConfiguration.getVectorConfig();

        Map<Boolean, Map<String, VectorConfig>> partitionedMaps = configs.entrySet()
                .stream()
                .collect(Collectors.partitioningBy(
                        entry -> entry.getValue().getChunkField(),
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
                ));

        Map<String, VectorConfig> inlineConfigs = partitionedMaps.get(false);
        inlineConfigs.forEach(this::validateInlineConfig);

        Map<String, VectorConfig> fieldsToChunk = partitionedMaps.get(true);
        fieldsToChunk.forEach(this::validateChunkFieldConfig);
    }

    private ValidateVectorFieldResponse validateInlineConfig(String fieldName, VectorConfig vectorConfig) {
        String destinationCollection = indexerConfiguration.getDestinationSolrConfiguration().getCollection();

        String vectorFieldName = vectorConfig.getFieldVectorName();
        if (StringUtils.isEmpty(vectorFieldName)) {
            vectorFieldName = fieldName + "-vector";
        }
        ValidateVectorFieldResponse response = validateVectorField(vectorFieldName, vectorConfig, destinationCollection);
        if (response.fieldChanged()) {
            //change the name of the field in the vector config
            vectorConfig.setFieldVectorName(response.fieldCreated());
            vectorConfig.setChunkFieldNameRequested(response.fieldRequested());
            log.info("NOTE!  FILED RQUEST IS NOT THE FIELD WE WILL USE {}", vectorConfig);
        }
        log.info("Vector field validated {}", response);
        return response;
    }

    private void validateChunkFieldConfig(String fieldName, VectorConfig vectorConfig) {
        String destinationCollection = vectorConfig.getDestinationCollection();
        if (StringUtils.isEmpty(destinationCollection)) {
            destinationCollection = generateDestinationCollectionName(fieldName);
        }

        if (solrAdminActions.doesCollectionExist(destinationCollection)) {
            log.info("Collection {} already exists", destinationCollection);
        } else {
            log.info("Creating collection {} ", destinationCollection);
            solrAdminActions.createCollection(destinationCollection, vectorConfig.getCollectionCreation());
        }

        String vectorFieldName = vectorConfig.getFieldVectorName();
        if (StringUtils.isEmpty(vectorFieldName)) {
            vectorFieldName = fieldName + "-vector";
        }

        ValidateVectorFieldResponse response = validateVectorField(vectorFieldName, vectorConfig, destinationCollection);
        if (response.fieldChanged()) {
            vectorConfig.setFieldVectorName(response.fieldCreated());
            vectorConfig.setChunkFieldNameRequested(response.fieldRequested());
            log.info("NOTE!  FILED RQUEST IS NOT THE FIELD WE WILL USE {}", vectorConfig);
        }
    }

    private @NotNull String generateDestinationCollectionName(String fieldName) {
        return indexerConfiguration.getDestinationSolrConfiguration().getCollection() +
                "-" + fieldName + "-chunks";
    }

    private ValidateVectorFieldResponse validateVectorField(String vectorFieldName, VectorConfig vectorConfig, String destinationCollection) {
        try {
            String fieldCreated = vectorFieldValidator.validateVectorField(
                    vectorFieldName,
                    vectorConfig.getSimilarityFunction(),
                    vectorConfig.getHnswMaxConnections(),
                    vectorConfig.getHnswBeamWidth(),
                    dimensionality,
                    destinationCollection);
            return new ValidateVectorFieldResponse(fieldCreated, vectorFieldName, true, new ValidateVectorFieldRequest(vectorFieldName, vectorConfig, destinationCollection), dimensionality);
        } catch (IOException | SolrServerException e) {
            throw new RuntimeException(e);
        }
    }

    protected record ValidateVectorFieldRequest(String vectorFieldName, VectorConfig vectorConfig,
                                                String destinationCollection) {
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("vectorFieldName", vectorFieldName)
                    .add("vectorConfig", vectorConfig)
                    .add("destinationCollection", destinationCollection)
                    .toString();
        }
    }

    protected record ValidateVectorFieldResponse(String fieldCreated, String fieldRequested,
                                                 boolean fieldCreatedSuccessfully, ValidateVectorFieldRequest request,
                                                 Integer dimensionality) {

        public boolean fieldChanged() {
            return !fieldRequested.equals(fieldCreated);
        }
    }

    private void validateDestinationCollection() {
        if (solrAdminActions.doesCollectionExist(indexerConfiguration.getDestinationSolrConfiguration().getCollection())) {
            log.info("Destination collection already exists {}", indexerConfiguration.getDestinationSolrConfiguration().getCollection());
        } else {
            SolrConfiguration destinationSolrConfiguration = indexerConfiguration.getDestinationSolrConfiguration();
            String destinationCollection = destinationSolrConfiguration.getCollection();
            solrAdminActions.createCollection(destinationCollection, destinationSolrConfiguration.getCollectionCreation());
        }
    }

    public List<String> getVectorDestinationCollections() {
        List<String> collections = new ArrayList<>();
        for (Map.Entry<String, VectorConfig> entry : indexerConfiguration.getVectorConfig().entrySet()) {
            String fieldName = entry.getKey();
            VectorConfig vectorConfig = entry.getValue();
            String destinationCollection = vectorConfig.getDestinationCollection();

            if (isEmpty(destinationCollection) && entry.getValue().getChunkField()) {
                destinationCollection = indexerConfiguration.getDestinationSolrConfiguration().getCollection() +
                        "-" + fieldName + "-chunks";
            }

            collections.add(destinationCollection);
        }

        return collections;
    }
}