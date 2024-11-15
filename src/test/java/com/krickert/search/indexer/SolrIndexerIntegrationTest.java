package com.krickert.search.indexer;

import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.config.SolrConfigurationModifier;
import com.krickert.search.indexer.enhancers.ProtobufToSolrDocument;
import com.krickert.search.indexer.grpc.TestClients;
import com.krickert.search.indexer.solr.SolrDocumentConverter;
import com.krickert.search.indexer.test.TestContainersManager;
import com.krickert.search.model.pipe.PipeDocument;
import com.krickert.search.model.test.util.TestDataHelper;
import io.micronaut.context.annotation.Requires;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import static com.google.common.base.Preconditions.checkNotNull;

@MicronautTest
public class SolrIndexerIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(SolrIndexerIntegrationTest.class);


    private final TestContainersManager testContainersManager;
    private final SolrDynamicClient solrDynamicClient;
    private final SemanticIndexer semanticIndexer;
    private final IndexerConfiguration indexerConfiguration;
    private final ProtobufToSolrDocument protobufToSolrDocument = new ProtobufToSolrDocument();

    @BeforeEach
    void setUp() {
        Map<String, GenericContainer<?>> containerMap = testContainersManager.getContainers();
        for (String containerName : containerMap.keySet()) {
            GenericContainer<?> container = containerMap.get(containerName);
            log.info("Container [{}]: Host:[{}] Port Bindings:[{}] ", containerName, container.getHost(), container.getExposedPorts());
        }
    }

    @Inject
    public SolrIndexerIntegrationTest(
            TestContainersManager testContainersManager,
            TestClients testClients,
            SolrDynamicClient solrDynamicClient,
            SemanticIndexer semanticIndexer,
            SolrConfigurationModifier solrConfigurationModifier,
            IndexerConfiguration indexerConfiguration,
            TestContainersManager testContainers) {
        log.info("Test Containers: {} ", testContainers);
        checkNotNull(testClients);
        checkNotNull(solrConfigurationModifier);
        this.testContainersManager = testContainersManager;
        this.solrDynamicClient = solrDynamicClient;
        this.semanticIndexer = semanticIndexer;
        this.indexerConfiguration = indexerConfiguration;
        log.info("Indexer configuration: {}", indexerConfiguration);
        log.info("testContainers: {}", testContainers);
    }


    @Test
    void testSemanticIndexer() throws IndexingFailedExecption {
        //this would just run, but we first have to setup the source and destination solr
        setupSolr7ForExportTest();
        UUID uuid = UUID.randomUUID();
        semanticIndexer.runDefaultExportJob(uuid);

        //let's reindex everything - see if it works or messes up
        uuid = UUID.randomUUID();
        semanticIndexer.runDefaultExportJob(uuid);
    }

    private void setupSolr7ForExportTest() {
        String solrUrl = indexerConfiguration.getSourceSolrConfiguration().getConnection().getUrl();
        String collection = indexerConfiguration.getSourceSolrConfiguration().getCollection();
        log.info("Solr source collection: {} being created from the solr host: {}", collection, solrUrl);
        solrDynamicClient.createCollection(solrUrl, collection);
        log.info("Solr source collection created: {}", collection);
        Collection<PipeDocument> protos = TestDataHelper.getFewHunderedPipeDocuments().stream().filter(doc -> doc.getDocumentType().equals("ARTICLE")).toList();
        List<SolrInputDocument> solrDocuments = protos.stream().map(protobufToSolrDocument::convertProtobufToSolrDocument).collect(Collectors.toList());
        solrDynamicClient.sendJsonToSolr(solrUrl, collection, SolrDocumentConverter.convertSolrDocumentsToJson(solrDocuments));
        solrDynamicClient.commit(solrUrl, collection);
        log.info("Solr protocol buffer documents have been imported to Solr 7.  We are ready to start the test.");
    }


}

