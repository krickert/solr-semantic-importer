package com.krickert.search.indexer.config;

import com.krickert.search.indexer.test.TestContainersManager;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Named;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.krickert.search.indexer.test.TestContainersManager.*;

@Factory
public class SolrConfigurationModifier {

    private static final Logger log = LoggerFactory.getLogger(SolrConfigurationModifier.class);
    private final IndexerConfiguration originalConfiguration;
    private final String solr9Url;

    public SolrConfigurationModifier(IndexerConfiguration originalConfiguration, TestContainersManager testContainersManager) {
        this.originalConfiguration = originalConfiguration;
        String solr7Url = getSolr7BaseUrl();
        this.solr9Url = getSolr9BaseUrl();

        // Set system properties for container URLs to be accessible in the tests
        System.setProperty("solr-config.source.connection.url", solr7Url);
        System.setProperty("solr-config.destination.connection.url", solr9Url);
        System.setProperty("indexer.vector-grpc-channel", getVectorizerUrl());
        System.setProperty("indexer.chunker-grpc-channel", getChunkerUrl());

        log.info("setting the configuration so it has:\n\tsolr7Url: {}\n\tsolr9Url: {}", solr7Url, solr9Url);
        originalConfiguration.getSourceSolrConfiguration().getConnection().setUrl(solr7Url);
        originalConfiguration.getDestinationSolrConfiguration().getConnection().setUrl(solr9Url);
        originalConfiguration.getIndexerConfigurationProperties().setVectorGrpcChannel(getVectorizerUrl());
        originalConfiguration.getIndexerConfigurationProperties().setChunkerGrpcChannel(getChunkerUrl());
        log.info("Solr testing property setting complete");
    }

    @Replaces(Http2SolrClient.class)
    @Bean
    @Named("solrClient")
    public Http2SolrClient createSolrClient() {
        return new Http2SolrClient.Builder(solr9Url)
                .withDefaultCollection(originalConfiguration.getDestinationSolrConfiguration().getCollection())
                .withFollowRedirects(true)
                .build();
    }

    @Replaces(ConcurrentUpdateHttp2SolrClient.class)
    @Bean
    @Named("concurrentClient")
    public ConcurrentUpdateHttp2SolrClient createConcurrentUpdateSolrClient() {
        Http2SolrClient solrClient = createSolrClient();
        return new ConcurrentUpdateHttp2SolrClient.Builder(solr9Url, solrClient, false)
                .withQueueSize(originalConfiguration.getDestinationSolrConfiguration().getConnection().getQueueSize())
                .withThreadCount(originalConfiguration.getDestinationSolrConfiguration().getConnection().getThreadCount())
                .build();
    }


}