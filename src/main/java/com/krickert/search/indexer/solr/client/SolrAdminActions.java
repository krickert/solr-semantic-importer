package com.krickert.search.indexer.solr.client;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.config.SolrConfiguration;
import com.krickert.search.indexer.config.VectorConfig;
import com.krickert.search.indexer.util.FileLoader;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.core.util.CollectionUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.ConfigSetAdminRequest;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.apache.solr.client.solrj.response.*;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class SolrAdminActions {
    private static final Logger log = LoggerFactory.getLogger(SolrAdminActions.class);

    private final SolrClient inlineSolrClient;
    private final SolrClient vectorSolrClient;
    private final Collection<String> vectorCollections;
    private final FileLoader fileLoader;


    @Inject
    public SolrAdminActions(SolrClientService solrClientService,
                            FileLoader fileLoader,
                            IndexerConfiguration indexerConfiguration) {
        checkNotNull(solrClientService);
        checkNotNull(indexerConfiguration);
        this.inlineSolrClient = checkNotNull(solrClientService.inlineSolrClient());
        this.vectorSolrClient = checkNotNull(solrClientService.vectorSolrClient());
        if (CollectionUtils.isNotEmpty(indexerConfiguration.getChunkVectorConfig())) {
            this.vectorCollections = indexerConfiguration.getChunkVectorConfig().values().stream().map(VectorConfig::getDestinationCollection).collect(Collectors.toList());
        } else {
            this.vectorCollections = List.of();
        }
        this.fileLoader = checkNotNull(fileLoader);
        log.info("SolrAdminActions is created");
    }

    /**
     * Checks if the connection to the Solr server is alive by sending a ping request.
     *
     * @return true if the ping is successful, false otherwise
     */
    public boolean isSolrServerAlive() {
        return isSolrServerAlive(inlineSolrClient);
    }

    public boolean isSolrServerAlive(SolrClient solrClient) {
        try {
            SolrPingResponse pingResponse = solrClient.ping("dummy");
            return pingResponse.getStatus() == 0;
        } catch (SolrServerException | IOException e) {
            log.error("Exception thrown", e);
            return false;
        }
    }

    public boolean doesCollectionExist(String collectionName) {
        return doesCollectionExist(collectionName, inlineSolrClient);
    }

    /**
     * Checks if a specific collection exists on the Solr server.
     *
     * @param collectionName the name of the collection to check
     * @return true if the collection exists, false otherwise
     */
    public boolean doesCollectionExist(String collectionName, SolrClient solrClient) {
        try {
            solrClient.ping(collectionName);
            log.info("collection {} exists", collectionName);
            return true;
        } catch (BaseHttpSolrClient.RemoteSolrException | SolrServerException | IOException e) {
            log.info("collection {} does not exist", collectionName);
            return false;
        }
    }


    public boolean doesConfigExist(String configToCheck) {
        return doesConfigExist(inlineSolrClient, configToCheck);
    }


    public boolean doesConfigExist(SolrClient solrClient, String configToCheck) {
        checkNotNull(solrClient);
        checkNotNull(configToCheck);
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("action", "LIST");

        // Make the request to the Solr Admin API to list config sets
        GenericSolrRequest request = new GenericSolrRequest(GenericSolrRequest.METHOD.GET, "/admin/configs", params);

        try {
            SimpleSolrResponse response = request.process(solrClient);
            // Extract config sets from the response
            NamedList<Object> responseNamedList = response.getResponse();
            @SuppressWarnings("unchecked")
            ArrayList<String> configSets = (ArrayList<String>) responseNamedList.get("configSets");

            if (configSets == null) {
                log.info("There are no configsets in this solr instance.");
                return false;
            }
            boolean configExists = false;

            for (String config : configSets) {
                if (configToCheck.equals(config)) {
                    configExists = true;
                    break;
                }
            }
            return configExists;
        } catch (SolrServerException | IOException e) {
            log.error("exception thrown", e);
            throw new RuntimeException(e);
        }
    }

    public void uploadConfigSet(String configFile, String configName) {
        uploadConfigSet(inlineSolrClient, configFile, configName);
    }

    public void uploadConfigSet(SolrClient client, String configFile, String configName) {
        ConfigSetAdminRequest.Upload request = new ConfigSetAdminRequest.Upload();
        request.setConfigSetName(configName);
        final File file = fileLoader.loadZipFile(configFile);
        request.setUploadFile(file, "zip");
        // Execute the request
        final ConfigSetAdminResponse response;
        try {
            response = request.process(client);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }

        // Check the response status
        if (response.getStatus() == 0) {
            log.info("Configset uploaded successfully!");
        } else {
            log.error("Error uploading configset: {}", response);
        }
    }

    public void createCollection(String destinationCollection, SolrConfiguration.SolrCollectionCreationConfig config) {
        createCollection(inlineSolrClient, destinationCollection, config.getCollectionConfigName(), config.getCollectionConfigFile(), config.getNumberOfReplicas(), config.getNumberOfShards());
    }

    public void createCollection(String destinationCollection, VectorConfig.VectorCollectionCreationConfig config) {
        createCollection(vectorSolrClient, destinationCollection, config.getCollectionConfigName(), config.getCollectionConfigFile(), config.getNumberOfReplicas(), config.getNumberOfShards());
    }

    public void createCollection(SolrClient solrClient, String collectionName,
                                 String collectionConfigName, String collectionConfigFile, int numberOfReplicas,
                                 int numberOfShards) {
        if (doesCollectionExist(collectionName)) {
            log.info("Collection {} exists.  No need to create it.", collectionName);
            return;
        }
        log.info("Collection {} does not exist.  Creating it.", collectionName);
        if (!doesConfigExist(collectionConfigName)) {
            uploadConfigSet(
                    collectionConfigFile, collectionConfigName);
        } else {
            log.info("Configuration exists.  Will create the collection.");
        }
        try {
            // Create the collection request
            CollectionAdminRequest.Create createRequest = CollectionAdminRequest.createCollection(
                    collectionName, collectionConfigName, numberOfShards, numberOfReplicas);
            // Process the request
            CollectionAdminResponse response = createRequest.process(solrClient);
            checkResponseStatus(collectionName, response);
        } catch (IOException | SolrServerException e) {
            throw new RuntimeException(e);
        }
    }

    private static void checkResponseStatus(String collectionName, CollectionAdminResponse response) {
        if (response.isSuccess()) {
            log.info("Collection {} created successfully Response: {}.", collectionName, response);
        } else {
            log.error("Error creating collection: " + response.getErrorMessages());
            throw new IllegalStateException("Error creating collection: " + response.getErrorMessages());
        }
    }


    public void commit(String collectionName) {
        try {
            inlineSolrClient.commit(collectionName);
        } catch (SolrServerException | IOException e) {
            log.error("Could not commit collection {} due to {}", collectionName, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void commitVectorCollections() {
        vectorCollections.forEach(this::commit);
    }

    public int deleteOrphansAfterIndexing(String collection, String crawlId) {
        int numDeleted = 0;

        try {
            // Retrieve facet information for unique crawl IDs and their counts
            SolrQuery query = new SolrQuery();
            query.setQuery("*:*");
            query.setRows(0);  // We don't need actual rows, just the facets
            query.addFacetField("crawl_id");
            query.setFacetLimit(-1);  // Ensure we get all unique crawl IDs

            QueryResponse queryResponse = inlineSolrClient.query(collection, query);
            FacetField facetField = queryResponse.getFacetField("crawl_id");
            List<FacetField.Count> facetCounts = facetField.getValues();

            for (FacetField.Count count : facetCounts) {
                log.info("Crawl ID: {}, Count: {}", count.getName(), count.getCount());
            }

            // Delete documents with the specified crawlId
            UpdateResponse deleteResponse = inlineSolrClient.deleteByQuery(collection, "-crawl_id:" + crawlId);
            inlineSolrClient.commit(collection);

            // Log and return the number of documents deleted
            numDeleted = deleteResponse.getStatus() == 0 ? deleteResponse.getResponse().get("numFound") != null ? Integer.parseInt(deleteResponse.getResponse().get("numFound").toString()) : 0 : 0;
            log.info("Number of documents deleted with crawlId {}: {}", crawlId, numDeleted);

        } catch (SolrServerException e) {
            log.error("Failed to delete documents from collection: {} for crawlId: {}. SolrServerException occurred.", collection, crawlId, e);
            throw new RuntimeException("An error occurred while deleting documents from Solr collection: " + collection, e);
        } catch (IOException e) {
            log.error("Failed to delete documents from collection: {} for crawlId: {}. IOException occurred.", collection, crawlId, e);
            throw new RuntimeException("An IO error occurred while deleting documents from Solr collection: " + collection, e);
        }

        return numDeleted;
    }


}
