package com.krickert.search.indexer.grpc;

import com.krickert.search.indexer.test.TestContainersManager;
import com.krickert.search.service.ChunkServiceGrpc;
import com.krickert.search.service.EmbeddingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.grpc.annotation.GrpcChannel;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.krickert.search.indexer.test.TestContainersManager.*;

@Factory
@Requires(env = Environment.TEST)
public class TestClients {
    private static final Logger log = LoggerFactory.getLogger(TestClients.class);

    @Bean
    @Named("inlineEmbeddingService")
    public EmbeddingServiceGrpc.EmbeddingServiceBlockingStub inlineEmbeddingService() {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(getVectorizerHost(), getVectorizerPort())
                .usePlaintext()  // This disables TLS. Use `.useTransportSecurity()` if needed
                .build();

        log.info("Creating test inline embedding service with channel {}", channel.toString());
        return EmbeddingServiceGrpc.newBlockingStub(channel);
    }

    @Bean
    @Named("vectorEmbeddingService")
    public EmbeddingServiceGrpc.EmbeddingServiceBlockingStub
    vectorEmbeddingService() {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(getVectorizerHost(), getVectorizerPort())
                .usePlaintext()  // This disables TLS. Use `.useTransportSecurity()` if needed
                .build();
        log.info("Creating test vector embedding service with channel {}", channel.toString());
        return EmbeddingServiceGrpc.newBlockingStub(channel);
    }

    @Bean
    @Named("vectorChunkerService")
    ChunkServiceGrpc.ChunkServiceBlockingStub vectorChunkService() {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(getChunkerHost(), getChunkerPort())
                .usePlaintext()  // This disables TLS. Use `.useTransportSecurity()` if needed
                .build();
        log.info("Creating test vector chunker service with channel {}", channel.toString());
        return ChunkServiceGrpc.newBlockingStub(
                channel
        );
    }
    @Bean
    @Named("inlineChunkerService")
    ChunkServiceGrpc.ChunkServiceBlockingStub inlineChunkService() {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(getChunkerHost(), getChunkerPort())
                .usePlaintext()  // This disables TLS. Use `.useTransportSecurity()` if needed
                .build();
        return ChunkServiceGrpc.newBlockingStub(channel);
    }


}
