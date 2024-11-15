package com.krickert.search.indexer.grpc;

import com.krickert.search.indexer.test.TestContainersManager;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

@MicronautTest
@Testcontainers
public class GrpcContainerIntegrationTest {

    @Inject
    TestContainersManager testContainersManager;

    @Test
    void test() {
        Assertions.assertNotNull(testContainersManager);
        for (String containerName : testContainersManager.getContainers().keySet()) {
            GenericContainer<?> container = testContainersManager.getContainers().get(containerName);
            Assertions.assertNotNull(container);
            Assertions.assertTrue(container.isRunning());
            Assertions.assertTrue(container.isCreated());
            Assertions.assertNotNull(container.getContainerId());
        }
    }

}
