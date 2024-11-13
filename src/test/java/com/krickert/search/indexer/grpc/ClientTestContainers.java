package com.krickert.search.indexer.grpc;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.solr.SolrTestContainers;
import com.krickert.search.indexer.solr.client.SolrClientService;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

@Requires(env = Environment.TEST)
@Singleton
public class ClientTestContainers {

    private static final Logger log = LoggerFactory.getLogger(ClientTestContainers.class);

    private static final Map<String, GenericContainer<?>> containers = Maps.newHashMap();
    private static final Map<String, GrpcEntry> containerRegistry = Maps.newHashMap();

    private final Map<String, GrpcClientConfig> grpcClientConfigs;

    record GrpcEntry(String name, String host, Integer grpcPort, Integer restPort) {
    }

    @Inject
    public ClientTestContainers(Map<String, GrpcClientConfig> grpcClientConfigs,
                                SolrTestContainers solrTestContainers,
                                IndexerConfiguration configuration,
                                SolrClientService solrClientService) {
        this.grpcClientConfigs = grpcClientConfigs;
        initializeContainers(grpcClientConfigs, configuration);
        checkNotNull(solrTestContainers);
        checkNotNull(solrClientService);
    }

    private void initializeContainers(Map<String, GrpcClientConfig> grpcClientConfigs, IndexerConfiguration configuration) {
        for (Map.Entry<String, GrpcClientConfig> entry : grpcClientConfigs.entrySet()) {
            String serviceName = getServiceName(entry.getValue().getDockerImageName());
            if (!containers.containsKey(serviceName)) {
                containers.put(serviceName, createContainer(entry.getValue(), configuration));
            }
        }
    }

    private GenericContainer<?> createContainer(GrpcClientConfig config, IndexerConfiguration configuration) {
        DockerImageName imageName = DockerImageName.parse(config.getDockerImageName());

        GenericContainer<?> container = new GenericContainer<>(imageName)
                .withExposedPorts(config.getGrpcMappedPort(), config.getRestMappedPort())
                .withEnv("MY_ENV_VAR", "my-value")
                .withEnv("JAVA_OPTS", "-Xmx5g")
                .withEnv("MICRONAUT_SERVER_NETTY_THREADS", "1000") // Set Netty event loop threads
                .withEnv("MICRONAUT_EXECUTORS_DEFAULT_THREADS", "500")
                .withCreateContainerCmdModifier(cmd -> configureContainer(cmd, config));

        try {
            container.start();
            Integer grpcPort = container.getMappedPort(config.getGrpcMappedPort());
            Integer restPort = container.getMappedPort(config.getRestMappedPort());

            assert container.isCreated();
            assert container.isRunning();

            log.info("Container {} started with gRPC port: {}, REST port: {}",
                    config.getDockerImageName(), grpcPort, restPort);

            final String service = getServiceName(config.getDockerImageName());
            containerRegistry.put(service, new GrpcEntry(
                    config.getDockerImageName(),
                    container.getHost(),
                    grpcPort,
                    restPort));
            if (service.equals("vectorizer") || service.equals("chunker")) {
                configuration.getIndexerConfigurationProperties().setVectorGrpcChannel("localhost:" + grpcPort);
            }

        } catch (Exception e) {
            log.error("Error when starting container for image {}, exception: {}", config.getDockerImageName(), ExceptionUtils.getStackTrace(e));
            container.stop();
            throw e;
        }
        return container;
    }

    private static String getServiceName(String dockerImageName) {
        if (dockerImageName.contains("vectorizer")) {
            return "vectorizer";
        } else if (dockerImageName.contains("chunker")) {
            return "chunker";
        } else {
            return "unknown";
        }
    }

    public Integer getGrpcPort(String containerName) {
        return containerRegistry.get(containerName).grpcPort;
    }

    private void configureContainer(CreateContainerCmd cmd, GrpcClientConfig config) {
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withMemory(1024 * 1024 * 1024L)
                .withMemorySwap(1024 * 1024 * 1024L)
                .withPortBindings(
                        new PortBinding(Ports.Binding.bindPort(config.getGrpcTestPort()),
                                new ExposedPort(config.getGrpcMappedPort())),
                        new PortBinding(Ports.Binding.bindPort(config.getRestTestPort()),
                                new ExposedPort(config.getRestMappedPort()))
                );
        cmd.withHostConfig(hostConfig);
    }

    public Map<String, GenericContainer<?>> getContainers() {
        return containers;
    }

    public Map<String, GrpcClientConfig> getGrpcClientConfigs() {
        return grpcClientConfigs;
    }
}
