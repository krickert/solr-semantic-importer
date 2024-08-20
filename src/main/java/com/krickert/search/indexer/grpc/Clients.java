package com.krickert.search.indexer.grpc;

import com.krickert.search.service.ChunkServiceGrpc;
import com.krickert.search.service.EmbeddingServiceGrpc;
import com.krickert.search.service.PipeServiceGrpc;
import io.grpc.ManagedChannel;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.grpc.annotation.GrpcChannel;

@Factory
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
    EmbeddingServiceGrpc.EmbeddingServiceBlockingStub embeddingServiceBlockingStub(
            @GrpcChannel("${indexer.vector-grpc-channel}")
            ManagedChannel channel) {
        return EmbeddingServiceGrpc.newBlockingStub(
                channel
        );
    }

    @Bean
    ChunkServiceGrpc.ChunkServiceBlockingStub chunkServiceBlockingStub(
            @GrpcChannel("${indexer.chunker-grpc-channel}")
            ManagedChannel channel) {
        return ChunkServiceGrpc.newBlockingStub(
                channel
        );
    }
}