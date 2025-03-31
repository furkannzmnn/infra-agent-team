package com.modulith.infraagentteam.infra.adapters.deployment.cloud.docker;

import com.modulith.infraagentteam.domain.deployment.model.DeploymentConfig;
import com.modulith.infraagentteam.infra.adapters.deployment.DeploymentHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Files;

@Component("onprem-docker-compose")
@RequiredArgsConstructor
public class DockerComposeDeploymentHandler implements DeploymentHandler {

    private final DockerComposeTemplateGenerator templateGenerator;

    @Override
    public void handle(DeploymentConfig config) {
        System.out.println("Generating Docker Compose configuration for on-prem deployment...");

        String dockerComposeConfig = templateGenerator.generateTemplate(config);
        deployWithDockerCompose(dockerComposeConfig);
    }

    private void deployWithDockerCompose(String dockerComposeConfig) {
        try {
            Path tempFile = Files.createTempFile("docker-compose", ".yml");
            File file = tempFile.toFile();
            System.out.println("Docker Compose file created at: " + file.getAbsolutePath());

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(dockerComposeConfig);
                writer.flush();
            }

            ProcessBuilder processBuilder = new ProcessBuilder(
                "docker-compose", "-f", file.getAbsolutePath(), "up", "-d"
            );
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Docker Compose deployment failed with exit code: " + exitCode);
            }

            System.out.println("Docker Compose deployment completed successfully!");
        } catch (Exception e) {
            throw new RuntimeException("Error deploying with Docker Compose", e);
        }
    }
} 