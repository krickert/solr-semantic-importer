package com.krickert.search.indexer.config;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.net.Proxy;
import java.util.List;
import java.util.Map;

import static com.krickert.search.indexer.config.ConfigTestHelpers.testConnectionString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
public class IndexerConfigurationTest {

    @Inject
    ApplicationContext applicationContext;

    @Inject
    IndexerConfiguration indexerConfiguration;

    @Value("${grpc-test-client-config.chunker.grpc-test-port}")
    String chunkerPort;

    @Value("${grpc-test-client-config.vectorizer.grpc-test-port}")
    String vectorizerPort;

    @Test
    void testIndexerConfigurationPropertiesLoaded() {
        // Assertions for Indexer Configuration Properties
        IndexerConfigurationProperties properties = indexerConfiguration.getIndexerConfigurationProperties();
        assertNotNull(properties);
        assertNotNull(properties.getVectorGrpcChannel());
        assertTrue(properties.getVectorGrpcChannel().startsWith("localhost"));
        assertTrue(properties.getVectorGrpcChannel().endsWith(vectorizerPort));
        assertNotNull(properties.getChunkerGrpcChannel());
        assertTrue(properties.getChunkerGrpcChannel().endsWith(chunkerPort));
        assertEquals(3, properties.getLoopCheckSleepTimeSeconds());
        assertEquals(5, properties.getLoopMaxWarnings());
        assertEquals(2, properties.getVectorBatchSize());

        // Solr Configuration Assertions
        SolrConfiguration sourceConfig = indexerConfiguration.getSourceSolrConfiguration();
        assertNotNull(sourceConfig);
        assertEquals("7.7.3", sourceConfig.getVersion());
        assertEquals("source_collection", sourceConfig.getCollection());
        assertEquals(2, sourceConfig.getFilters().size());
        assertThat(sourceConfig.getFilters()).containsExactlyInAnyOrderElementsOf(List.of("-id:*.csv", "title:*"));

        SolrConfiguration.Connection sourceConnection = sourceConfig.getConnection();
        assertNotNull(sourceConnection);
        testConnectionString(sourceConnection);
        assertNotNull(sourceConnection.getAuthentication());
        assertTrue(sourceConnection.getAuthentication().isEnabled());
        assertEquals("basic", sourceConnection.getAuthentication().getType());
        assertEquals("dummy_user", sourceConnection.getAuthentication().getUserName());
        assertEquals("dummy_password", sourceConnection.getAuthentication().getPassword());
        assertEquals(Proxy.NO_PROXY, sourceConnection.getAuthentication().creaateProxy());
        assertNull(sourceConnection.getAuthentication().getProxySettings());

        assertNull(sourceConnection.getQueueSize());
        assertNull(sourceConnection.getThreadCount());
        SolrConfiguration destConfig = indexerConfiguration.getDestinationSolrConfiguration();
        assertNotNull(destConfig);
        assertEquals("9.6.1", destConfig.getVersion());
        assertEquals("destination_collection", destConfig.getCollection());
        assertEquals("classpath:semantic_example.zip", destConfig.getCollectionCreation().getCollectionConfigFile());
        assertEquals("semantic_example", destConfig.getCollectionCreation().getCollectionConfigName());
        assertEquals(1, destConfig.getCollectionCreation().getNumberOfShards());
        assertEquals(2, destConfig.getCollectionCreation().getNumberOfReplicas());

        SolrConfiguration.Connection destConnection = destConfig.getConnection();
        assertNotNull(destConnection);
        testConnectionString(destConnection);
        assertNotNull(destConnection.getQueueSize());
        assertEquals(2000, destConnection.getQueueSize());
        assertNotNull(destConnection.getThreadCount());
        assertEquals(3, destConnection.getThreadCount());
        assertNotNull(destConnection.getAuthentication());
        assertEquals(1, destConfig.getCollectionCreation().getNumberOfShards());
        assertEquals(2, destConfig.getCollectionCreation().getNumberOfReplicas());
        SolrConfiguration.Connection.Authentication destAuth = destConnection.getAuthentication();
        assertFalse(destAuth.isEnabled());
        assertEquals("jwt", destAuth.getType());
        assertEquals("my-client-secret", destAuth.getClientSecret());
        assertEquals("my-client-id", destAuth.getClientId());
        assertEquals("https://my-token-url.com/oauth2/some-token/v1/token", destAuth.getIssuer());
        assertEquals("issuer-auth-id", destAuth.getIssuerAuthId());
        assertEquals("your-subject", destAuth.getSubject());
        assertNotNull(destAuth.getProxySettings());
        assertTrue(destAuth.getProxySettings().isEnabled());
        assertEquals("localhost", destAuth.getProxySettings().getHost());
        assertEquals(8080, destAuth.getProxySettings().getPort());
        assertNotNull(destAuth.creaateProxy());
        assertEquals(Proxy.Type.HTTP, destAuth.creaateProxy().type());
        assertEquals("HTTP @ localhost/127.0.0.1:8080", destAuth.creaateProxy().toString());


        // Vector Configuration Assertions
        Map<String, VectorConfig> vectorConfigMap = indexerConfiguration.getVectorConfig();
        assertNotNull(vectorConfigMap);

        VectorConfig titleConfig = vectorConfigMap.get("title");
        assertNotNull(titleConfig);
        //Titles don't chunk and nor should you
        assertFalse(titleConfig.getChunkField());
        assertNull(titleConfig.getChunkOverlap());
        assertNull(titleConfig.getChunkSize());
        assertEquals("mini-LM", titleConfig.getModel());
        assertFalse(titleConfig.getChunkField());
        VectorConfig bodyConfig = vectorConfigMap.get("body");
        assertNotNull(bodyConfig);
        assertTrue(bodyConfig.getChunkField());
        assertEquals(30, bodyConfig.getChunkOverlap());
        assertEquals(300, bodyConfig.getChunkSize());
        assertEquals("mini-LM", bodyConfig.getModel());
        assertEquals("body-vectors", bodyConfig.getDestinationCollection());
        assertEquals("body-chunk-vector", bodyConfig.getFieldVectorName());
        assertEquals("cosine", bodyConfig.getSimilarityFunction());
        assertEquals(16, bodyConfig.getHnswMaxConnections());
        assertEquals(100, bodyConfig.getHnswBeamWidth());
        assertEquals("classpath:default-chunk-config.zip", bodyConfig.getCollectionCreation().getCollectionConfigFile());
        assertEquals("vector_config", bodyConfig.getCollectionCreation().getCollectionConfigName());
        assertEquals(1, bodyConfig.getCollectionCreation().getNumberOfShards());
        assertEquals(2, bodyConfig.getCollectionCreation().getNumberOfReplicas());
    }

}