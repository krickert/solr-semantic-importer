package com.krickert.search.indexer.grpc;

import com.krickert.search.service.ChunkServiceGrpc;
import com.krickert.search.service.EmbeddingServiceGrpc;
import com.krickert.search.service.PipeServiceGrpc;
import io.grpc.ManagedChannel;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.grpc.annotation.GrpcChannel;
import jakarta.inject.Named;
import jdk.jfr.Name;

@Factory
@Requires(notEnv = Environment.TEST)
public class Clients {

    @Bean
    PipeServiceGrpc.PipeServiceBlockingStub pipeServiceBlockingStub(
            @GrpcChannel("${indexer.vector-grpc-channel}")
            ManagedChannel channel) {
        return PipeServiceGrpc.newBlockingStub(
                channel
        );
    }

    @Bean
    @Named("inlineEmbeddingService")
    EmbeddingServiceGrpc.EmbeddingServiceBlockingStub inlineEmbeddingServiceBlockingStub (
            @GrpcChannel("${indexer.vector-grpc-channel}")
            ManagedChannel channel) {
        return EmbeddingServiceGrpc.newBlockingStub(
                channel
        );
    }


    @Bean
    @Named("vectorEmbeddingService")
    EmbeddingServiceGrpc.EmbeddingServiceBlockingStub vectorEmbeddingServiceBlockingStub(
            @GrpcChannel("${indexer.vector-grpc-channel}")
            ManagedChannel channel) {
        return EmbeddingServiceGrpc.newBlockingStub(
                channel
        );
    }

    @Bean
    @Named("vectorChunkerService")
    ChunkServiceGrpc.ChunkServiceBlockingStub vectorChunkServiceBlockingStub(
            @GrpcChannel("${indexer.chunker-grpc-channel}")
            ManagedChannel channel) {
        return ChunkServiceGrpc.newBlockingStub(
                channel
        );
    }

    @Bean
    @Named("inlineChunkerService")
    ChunkServiceGrpc.ChunkServiceBlockingStub inlineChunkServiceBlockingStub(
            @GrpcChannel("${indexer.chunker-grpc-channel}")
            ManagedChannel channel) {
        return ChunkServiceGrpc.newBlockingStub(
                channel
        );
    }

}
