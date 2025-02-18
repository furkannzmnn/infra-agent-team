package com.modulith.infraagentteam.infra.adapters.deployment.webhook;

import com.modulith.infraagentteam.domain.deployment.model.DeploymentConfig;
import com.modulith.infraagentteam.domain.deployment.port.DeploymentPort;
import com.modulith.infraagentteam.domain.shared.model.query.BitbucketFileRetrieveRequest;
import com.modulith.infraagentteam.infra.adapters.deployment.DeploymentProcessor;
import com.modulith.infraagentteam.infra.adapters.deployment.bitbucket.BitbucketClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DeploymentAdapter implements DeploymentPort {

    private final BitbucketClient bitbucketClient;
    private final DeploymentProcessor deploymentProcessor;

    public DeploymentAdapter(BitbucketClient bitbucketClient, DeploymentProcessor deploymentProcessor) {
        this.bitbucketClient = bitbucketClient;
        this.deploymentProcessor = deploymentProcessor;
    }

    @Override
    public DeploymentConfig retrieve(BitbucketFileRetrieveRequest request) {
        return bitbucketClient.callFile(request);
    }

    @Override
    public void start(DeploymentConfig config) {
        deploymentProcessor.processDeployment(config);
    }
}
