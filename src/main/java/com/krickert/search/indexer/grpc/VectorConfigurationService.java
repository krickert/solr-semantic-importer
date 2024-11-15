package com.krickert.search.indexer.grpc;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import io.micronaut.grpc.annotation.GrpcService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@GrpcService
public class VectorConfigurationService extends VectorConfigServiceGrpc.VectorConfigServiceImplBase {

    // ConcurrentHashMap to store the configurations
    private final Map<String, VectorConfig> configurations = new ConcurrentHashMap<>();

    @Override
    public void getVectorConfig(GetVectorConfigRequest request, StreamObserver<VectorConfig> responseObserver) {
        VectorConfig configuration = configurations.get(request.getId());

        // If the configuration does not exist, return an error
        if (configuration == null) {
            responseObserver.onError(new RuntimeException("Configuration not found for ID: " + request.getId()));
            return;
        }

        responseObserver.onNext(configuration);
        responseObserver.onCompleted();
    }

    @Override
    public void updateVectorConfig(UpdateVectorConfigRequest request, StreamObserver<Empty> responseObserver) {
        configurations.put(request.getId(), request.getConfiguration());

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}