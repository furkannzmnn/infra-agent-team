package com.modulith.infraagentteam.infra.adapters.deployment.cloud.docker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modulith.infraagentteam.domain.deployment.model.DeploymentConfig;
import com.modulith.infraagentteam.domain.deployment.model.EnvironmentVariable;
import com.modulith.infraagentteam.domain.deployment.model.Network;
import com.modulith.infraagentteam.domain.deployment.model.Service;
import com.modulith.infraagentteam.infra.adapters.deployment.TemplateGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DockerComposeTemplateGenerator implements TemplateGenerator {
    
    @Override
    public String generateTemplate(DeploymentConfig config) {
        Map<String, Service> services = config.getServices();
        Network network = config.getNetwork();

        StringBuilder dockerCompose = new StringBuilder();
        dockerCompose.append("version: '3.8'\n\n");
        dockerCompose.append("services:\n");

        for (Map.Entry<String, Service> entry : services.entrySet()) {
            Service service = entry.getValue();
            String serviceName = entry.getKey();
            dockerCompose.append(generateServiceDefinition(serviceName, service, network));
        }

        return dockerCompose.toString();
    }

    private String generateServiceDefinition(String serviceName, Service service, Network network) {
        StringBuilder serviceDef = new StringBuilder();
        serviceDef.append("  ").append(serviceName).append(":\n");
        serviceDef.append("    image: ").append(service.getImage()).append("\n");
        serviceDef.append("    container_name: ").append(service.getName() != null ? service.getName() : serviceName).append("\n");
        serviceDef.append("    ports:\n");
        serviceDef.append("      - \"").append(service.getPort()).append(":").append(service.getPort()).append("\"");
        
        if (service.getResources() != null) {
            serviceDef.append("\n    deploy:\n");
            serviceDef.append("      resources:\n");
            serviceDef.append("        limits:\n");
            serviceDef.append("          cpus: '").append(service.getResources().getCpu()).append("'\n");
            serviceDef.append("          memory: ").append(service.getResources().getMemory()).append("\n");
        }

        if (service.getEnvVars() != null && !service.getEnvVars().isEmpty()) {
            serviceDef.append("\n    environment:\n");
            for (EnvironmentVariable envVar : service.getEnvVars()) {
                serviceDef.append("      - ").append(envVar.getKey()).append("=").append(resolveEnvVarValue(envVar.getValue())).append("\n");
            }
        }

        serviceDef.append("\n");
        return serviceDef.toString();
    }

    private String resolveEnvVarValue(String value) {
        if (value != null && value.startsWith("${{services.")) {
            String[] parts = value.substring(2, value.length() - 2).split("\\.");
            if (parts.length == 3) {
                String serviceName = parts[1];
                return "${" + serviceName + "}";
            }
        }
        return value;
    }
} 