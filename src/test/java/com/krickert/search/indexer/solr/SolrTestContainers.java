package com.krickert.search.indexer.solr;

import com.krickert.search.indexer.config.IndexerConfiguration;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Singleton
public class SolrTestContainers {

    private static final Logger log = LoggerFactory.getLogger(SolrTestContainers.class);
    private static SolrContainer container7;
    private static SolrContainer container9;
    private static boolean initialized = false;

    private final Integer solr7Port;
    private final Integer solr9Port;

    public SolrTestContainers(
            @Value("${solr7-test-port}") Integer solr7Port,
            @Value("${solr9-test-port}") Integer solr9Port,
            IndexerConfiguration indexerConfiguration) {
        this.solr7Port = solr7Port;
        this.solr9Port = solr9Port;
        initializeContainers();
        String solr7Url = "http://" + container7.getHost() + ":" + container7.getSolrPort() + "/solr";
        String solr9Url = "http://" + container9.getHost() + ":" + container9.getSolrPort() + "/solr";
        log.info("Created solr7 and solr9 containers.  URLs: \n\tSolr7: {}, \n\tSolr9: {}", solr7Url, solr9Url);
        indexerConfiguration.getSourceSolrConfiguration().getConnection().setUrl(solr7Url);
        indexerConfiguration.getDestinationSolrConfiguration().getConnection().setUrl(solr9Url);
        log.info("Indexer configuration set: " + indexerConfiguration);

    }

    private synchronized void initializeContainers() {
        if (!initialized) {
            DockerImageName SOLR7_IMAGE = DockerImageName.parse("solr:7.7.3");
            container7 = createContainer(SOLR7_IMAGE, solr7Port);

            DockerImageName SOLR9_IMAGE = DockerImageName.parse("solr:9.6.1");
            container9 = createContainer(SOLR9_IMAGE, solr9Port);

            initialized = true;
        }
    }

    private SolrContainer createContainer(DockerImageName image, Integer port) {
        SolrContainer container = new SolrContainer(image)
                .withZookeeper(true)
                .withAccessToHost(true);

        container.start();
        assertTrue(container.isRunning());
        log.info("Started Solr container: {}, port: {}", image, container.getSolrPort());
        return container;
    }

    public SolrContainer getContainer7() {
        return container7;
    }

    public SolrContainer getContainer9() {
        return container9;
    }

    @Override
    public String toString() {
        return "SolrTestContainers{" +
                "container7=" + container7 +
                ", container9=" + container9 +
                '}';
    }
}