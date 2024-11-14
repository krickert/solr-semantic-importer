package com.krickert.search.indexer.solr.client;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.config.SolrConfiguration;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotNull;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Factory
public class SolrClientService {
    private static final Logger log = LoggerFactory.getLogger(SolrClientService.class);

    private final OktaAuthenticatedHttpListenerFactory authenticatedRequestResponseListener;
    private final IndexerConfiguration indexerConfiguration;

    @Inject
    public SolrClientService(OktaAuthenticatedHttpListenerFactory authenticatedRequestResponseListener,
                             IndexerConfiguration indexerConfiguration) {
        log.info("Creating solr client service");
        this.authenticatedRequestResponseListener = authenticatedRequestResponseListener;
        this.indexerConfiguration = indexerConfiguration;
        
    }

    @Bean
    @Named("vectorSolrClient")
    public Http2SolrClient vectorSolrClient() {
        log.info("Creating destination vector solr client");
        Http2SolrClient client = createClient();
        log.info("Destination vector solr client created.");
        return client;
    }

    @Bean
    @Named("vectorConcurrentClient")
    public ConcurrentUpdateHttp2SolrClient vectorConcurrentClient() {
        try {
            log.info("Creating inlineConcurrentClient bean");
            String solrUrl = indexerConfiguration.getDestinationSolrConfiguration().getConnection().getUrl();
            return new ConcurrentUpdateHttp2SolrClient.Builder(solrUrl, inlineSolrClient(), false)
                    .withQueueSize(indexerConfiguration.getDestinationSolrConfiguration().getConnection().getQueueSize())
                    .withThreadCount(indexerConfiguration.getDestinationSolrConfiguration().getConnection().getThreadCount())
                    .build();
        } catch (Exception e) {
            log.error("Error creating inlineConcurrentClient bean", e);
            throw e;
        }
    }


    @Bean
    @Named("inlineSolrClient")
    public Http2SolrClient inlineSolrClient() {
        log.info("Creating inline destination solr client");
        Http2SolrClient client = createClient();
        log.info("Destination inline solr client created.");
        return client;
    }

    @Bean
    @Named("inlineConcurrentClient")
    @Requires(bean = Http2SolrClient.class)
    public ConcurrentUpdateHttp2SolrClient inlineConcurrentClient() {
        try {
            log.info("Creating inlineConcurrentClient bean");
            String solrUrl = indexerConfiguration.getDestinationSolrConfiguration().getConnection().getUrl();
            return new ConcurrentUpdateHttp2SolrClient.Builder(solrUrl, inlineSolrClient(), false)
                    .withQueueSize(indexerConfiguration.getDestinationSolrConfiguration().getConnection().getQueueSize())
                    .withThreadCount(indexerConfiguration.getDestinationSolrConfiguration().getConnection().getThreadCount())
                    .build();
        } catch (Exception e) {
            log.error("Error creating inlineConcurrentClient bean", e);
            throw e;
        }
    }

    private @NotNull Http2SolrClient createClient() {
        String solrUrl = indexerConfiguration.getDestinationSolrConfiguration().getConnection().getUrl();
        String collection = indexerConfiguration.getDestinationSolrConfiguration().getCollection();
        SolrConfiguration.Connection.Authentication auth =
                indexerConfiguration.getDestinationSolrConfiguration().getConnection().getAuthentication();

        Http2SolrClient.Builder clientBuilder = new Http2SolrClient.Builder(solrUrl)
                .withDefaultCollection(collection)
                .withFollowRedirects(true);

        if (auth.isEnabled()) {
            if ("basic".equals(auth.getType())) {
                assert auth.getUserName() != null;
                assert auth.getPassword() != null;
                clientBuilder.withBasicAuthCredentials(auth.getUserName(), auth.getPassword());
            }
        }

        // Add the listener factory to the builder
        clientBuilder.withListenerFactory(List.of(authenticatedRequestResponseListener));

        return clientBuilder.build();
    }


}