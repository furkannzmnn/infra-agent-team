package com.modulith.infraagentteam.infra.adapters.deployment.webhook;

import com.modulith.infraagentteam.domain.deployment.model.DeploymentConfig;
import com.modulith.infraagentteam.domain.deployment.port.DeploymentPort;
import com.modulith.infraagentteam.domain.shared.model.query.FileRetrieveRequest;
import com.modulith.infraagentteam.infra.adapters.deployment.DeploymentProcessor;
import com.modulith.infraagentteam.infra.adapters.deployment.github.GitClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class DeploymentAdapter implements DeploymentPort {

    private final List<GitClient> gitClientList;
    private final DeploymentProcessor deploymentProcessor;

    public DeploymentAdapter(List<GitClient> gitClientList, DeploymentProcessor deploymentProcessor) {
        this.gitClientList = gitClientList;
        this.deploymentProcessor = deploymentProcessor;
    }

    @Override
    public DeploymentConfig retrieve(FileRetrieveRequest request) {
        return gitClientList.stream()
                .filter(client -> client.isSupportedClient(request.gitType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported git type: " + request.gitType()))
                .callFile(request);
    }

    @Override
    public void start(DeploymentConfig config) {
        deploymentProcessor.processDeployment(config);
    }
}
