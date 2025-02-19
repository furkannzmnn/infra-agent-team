package com.modulith.infraagentteam.infra.deployment.adapter;

import com.modulith.infraagentteam.domain.deployment.model.*;
import com.modulith.infraagentteam.domain.deployment.port.DeploymentPort;
import com.modulith.infraagentteam.domain.shared.model.query.FileRetrieveRequest;

import java.util.List;
import java.util.Map;

class FakeDeploymentAdapter implements DeploymentPort {

    @Override
    public DeploymentConfig retrieve(FileRetrieveRequest request) {
        return DeploymentConfig.builder()
                .infrastructure(Infrastructure.builder()
                        .provider("aws")
                        .deploymentTool("terraform")
                        .region("us-east-1")
                        .credentials(Credentials.builder()
                                .accessKey("YOUR_AWS_ACCESS_KEY")
                                .secretKey("YOUR_AWS_SECRET_KEY")
                                .build())
                        .build())
                .services(Map.of(
                        "backend", Service.builder()
                                .name("backend-service")
                                .image("myrepo/backend:latest")
                                .port(8080)
                                .envVars(List.of(
                                        EnvironmentVariable.builder()
                                                .key("DATABASE_URL")
                                                .value("jdbc:postgresql://db:5432/mydatabase")
                                                .build(),
                                        EnvironmentVariable.builder()
                                                .key("SPRING_PROFILES_ACTIVE")
                                                .value("prod")
                                                .build()
                                ))
                                .replicas(3)
                                .resources(Resources.builder()
                                        .cpu("1024")
                                        .memory("2048Mi")
                                        .build())
                                .build(),
                        "frontend", Service.builder()
                                .name("frontend-service")
                                .image("myrepo/frontend:latest")
                                .port(3000)
                                .replicas(2)
                                .resources(Resources.builder()
                                        .cpu("512")
                                        .memory("1024Mi")
                                        .build())
                                .build(),
                        "redis", Service.builder()
                                .image("redis:latest")
                                .port(6379)
                                .build()
                ))
                .network(Network.builder()
                        .vpc("vpc-abc123")
                        .subnets(List.of("subnet-1111", "subnet-2222"))
                        .securityGroup("sg-xyz456")
                        .build())
                .monitoring(Monitoring.builder()
                        .enabled(true)
                        .tool("prometheus")
                        .build())
                .build();
    }

    @Override
    public void start(DeploymentConfig config) {
        System.out.println("Starting deployment: " + config);
    }
}