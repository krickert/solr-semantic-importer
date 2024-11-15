package com.krickert.search.indexer.test;

import com.krickert.search.indexer.config.IndexerConfiguration;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public final class TestContainersManager {
    private static final Logger log = LoggerFactory.getLogger(TestContainersManager.class);

    private static final String SOLR9_IMAGE = "solr:9.7.0";
    private static final String SOLR7_IMAGE = "solr:7.7.3";
    private static final String VECTORIZER_IMAGE = "krickert/vectorizer:1.0-SNAPSHOT";
    private static final String CHUNKER_IMAGE = "krickert/chunker:1.0-SNAPSHOT";

    private static String chunkerHost;
    private static String vectorizerHost;
    private static Integer chunkerPort;
    private static Integer vectorizerPort;
    private static String chunkerUrl;
    private static String vectorizerUrl;
    private static String solr7Url;
    private static String solr9Url;

    private static final SolrContainer solr7Container = new SolrContainer(DockerImageName.parse(SOLR7_IMAGE)) {
        @Override
        protected void configure() {
            this.addExposedPort(8983);
            this.withZookeeper(true);
            this.withAccessToHost(true);
            String command = "solr -c -f";
            this.setCommand(command);
        }
    };

    private static final SolrContainer solr9Container = new SolrContainer(DockerImageName.parse(SOLR9_IMAGE)) {
        @Override
        protected void configure() {
            this.addExposedPort(8983);
            this.withZookeeper(true);
            this.withAccessToHost(true);
            String command = "solr -c -f";
            this.setCommand(command);
            this.waitStrategy =
                    new LogMessageWaitStrategy()
                            .withRegEx(".*Server Started.*")
                            .withStartupTimeout(Duration.of(180, ChronoUnit.SECONDS));
        }
    };

    private static final GenericContainer<?> vectorizerContainer =
            new GenericContainer<>(DockerImageName.parse(VECTORIZER_IMAGE))
                    .withExposedPorts(50401, 60401)
                    .withEnv("JAVA_OPTS", "-Xmx5g")
                    .withLogConsumer(new Slf4jLogConsumer(log)) // Log output for better debugging
                    .withEnv("MICRONAUT_SERVER_NETTY_THREADS", "1000")
                    .withEnv("MICRONAUT_EXECUTORS_DEFAULT_THREADS", "500")
                    .withStartupTimeout(Duration.of(180, ChronoUnit.SECONDS))
                    .waitingFor(Wait.forListeningPort());

    private static final GenericContainer<?> chunkerContainer = new GenericContainer<>(DockerImageName.parse(CHUNKER_IMAGE))
            .withExposedPorts(50403, 60403)
            .withEnv("JAVA_OPTS", "-Xmx5g") // Allocate 5GB for the JVM
            .withEnv("MICRONAUT_SERVER_NETTY_THREADS", "1000")
            .withEnv("MICRONAUT_EXECUTORS_DEFAULT_THREADS", "500")
            .withStartupTimeout(Duration.of(300, ChronoUnit.SECONDS)) // Increase startup timeout to 300 seconds
            .waitingFor(Wait.forHttp("/health") // Wait for a health check endpoint
                    .forPort(60403)
                    .withStartupTimeout(Duration.of(300, ChronoUnit.SECONDS)))
            .withLogConsumer(new Slf4jLogConsumer(log)) // Log output for better debugging
            .waitingFor(Wait.forListeningPort()); // Also wait for the port to be ready

    public static final Map<String,GenericContainer<?>> allContainers = Map.of("solr7", solr7Container, "solr9", solr9Container,
                    "vectorizerContainer", vectorizerContainer, "chunkerContainer", chunkerContainer);

    private static final AtomicBoolean containersStarted = new AtomicBoolean(false);
    private final IndexerConfiguration indexerConfiguration;

    public static void startContainers() {
        if (containersStarted.compareAndSet(false, true)) {
            try {
                // Start all containers

                solr7Container.start();
                solr9Container.start();
                vectorizerContainer.start();
                chunkerContainer.start();
                log.info("Solr 7 running on {}:{}", solr7Container.getHost(), solr7Container.getMappedPort(8983));
                log.info("Solr 9 running on {}:{}", solr9Container.getHost(), solr9Container.getMappedPort(8983));
                log.info("Vectorizer running on {}:{}", vectorizerContainer.getHost(), vectorizerContainer.getMappedPort(50401));
                log.info("Chunker running on {}:{}", chunkerContainer.getHost(), chunkerContainer.getMappedPort(50403));

                solr7Url = "http://" + solr7Container.getHost() + ":" + solr7Container.getMappedPort(8983) + "/solr";
                solr9Url = "http://" + solr9Container.getHost() + ":" + solr9Container.getMappedPort(8983) + "/solr";

                // Set system properties for container URLs to be accessible in the tests
                System.setProperty("solr-config.source.connection.url", solr7Url);
                System.setProperty("solr-config.destination.connection.url", solr9Url);

                System.setProperty("grpc-test-client-config.chunker.grpc-test-port", String.valueOf(chunkerContainer.getMappedPort(50403)));
                System.setProperty("grpc-test-client-config.vectorizer.grpc-test-port", String.valueOf(vectorizerContainer.getMappedPort(50401)));
                vectorizerHost = vectorizerContainer.getHost();
                vectorizerPort = vectorizerContainer.getMappedPort(50401);
                vectorizerUrl = "http://" + vectorizerHost + ":" + vectorizerPort;
                chunkerHost = chunkerContainer.getHost();
                chunkerPort = chunkerContainer.getMappedPort(50403);
                chunkerUrl = "http://" + chunkerHost + ":" + chunkerPort;

                System.setProperty("indexer.vector-grpc-channel", vectorizerUrl);
                System.setProperty("indexer.chunk-grpc-channel", chunkerUrl);

            } catch (Exception e) {
                log.error("Error initializing containers: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
    }

    // Private constructor to prevent instantiation
    public TestContainersManager(IndexerConfiguration indexerConfiguration) {
        startContainers();
        this.indexerConfiguration = indexerConfiguration;
        indexerConfiguration.getSourceSolrConfiguration().getConnection().setUrl(solr7Url);
        indexerConfiguration.getDestinationSolrConfiguration().getConnection().setUrl(solr9Url);
        indexerConfiguration.getIndexerConfigurationProperties().setVectorGrpcChannel(vectorizerUrl);
        indexerConfiguration.getIndexerConfigurationProperties().setChunkerGrpcChannel(chunkerUrl);
        System.setProperty("indexer.vector-grpc-channel", vectorizerUrl);
        System.setProperty("indexer.chunker-grpc-channel", chunkerUrl);
    }

    // Public method to get SolrClient instance
    public static Http2SolrClient createSolrClient() {
        return new Http2SolrClient.Builder(solr9Url).build();
    }

    public static String getVectorizerUrl() {
        return vectorizerUrl;
    }

    public static Integer getChunkerPort() {
        return chunkerPort;
    }

    public static Integer getVectorizerPort() {
        return vectorizerPort;
    }

    public static String getVectorizerHost() {
        return vectorizerHost;
    }
    public static String getChunkerHost() {
        return chunkerHost;
    }
    public static String getChunkerUrl() {
        return chunkerUrl;
    }

    public static String getSolr9BaseUrl() {
        return solr9Url;
    }
    public static String getSolr7BaseUrl() {
        return solr7Url;
    }
    // Cleanup method to stop containers after the entire test suite
    public static void stopContainers() {
        if (solr9Container.isRunning()) {
            solr9Container.stop();
        }
        if (vectorizerContainer != null && vectorizerContainer.isRunning()) {
            vectorizerContainer.stop();
        }
        if (chunkerContainer != null && chunkerContainer.isRunning()) {
            chunkerContainer.stop();
        }
    }

    public Map<String, GenericContainer<?>> getContainers() {
        return allContainers;
    }

    public SolrContainer getContainer7() {
        return solr7Container;
    }
}