package com.krickert.search.indexer.grpc;

import com.google.protobuf.Empty;
import com.krickert.search.indexer.grpc.SolrConfigurationServiceGrpc.SolrConfigurationServiceImplBase;
import io.grpc.stub.StreamObserver;
import io.micronaut.grpc.annotation.GrpcService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@GrpcService
public class SolrConfigurationService extends SolrConfigurationServiceImplBase {

    // ConcurrentHashMap to store the configurations
    private final Map<String, SolrConfig> configurations = new ConcurrentHashMap<>();

    @Override
    public void getSolrConfiguration(GetSolrConfigurationRequest request, StreamObserver<SolrConfig> responseObserver) {
        SolrConfig configuration = configurations.get(request.getId());

        // If the configuration does not exist, return an empty response
        if (configuration == null) {
            responseObserver.onError(new RuntimeException("Configuration not found for ID: " + request.getId()));
            return;
        }

        responseObserver.onNext(configuration);
        responseObserver.onCompleted();
    }

    @Override
    public void updateSolrConfiguration(UpdateSolrConfigurationRequest request, StreamObserver<Empty> responseObserver) {
        configurations.put(request.getId(), request.getConfiguration());

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}