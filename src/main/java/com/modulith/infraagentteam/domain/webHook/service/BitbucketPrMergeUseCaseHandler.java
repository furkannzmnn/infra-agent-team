package com.modulith.infraagentteam.domain.webHook.service;

import com.modulith.infraagentteam.domain.deployment.model.DeploymentConfig;
import com.modulith.infraagentteam.domain.deployment.port.DeploymentPort;
import com.modulith.infraagentteam.domain.shared.model.query.BitbucketFileRetrieveRequest;
import com.modulith.infraagentteam.domain.webHook.usecase.BitbucketPrMergeUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BitbucketPrMergeUseCaseHandler {

    private final DeploymentPort deploymentYamlPort;

    public void handle(BitbucketPrMergeUseCase payload) {

        log.info("Handling Bitbucket PR merge webhook: {}", payload);

        DeploymentConfig config = deploymentYamlPort.retrieve(BitbucketFileRetrieveRequest.builder()
                .commit(payload.commitId())
                .repo(payload.repositoryName())
                .fileName("deployment.yml")
                .build());

        log.info("Deployment config retrieved: {}", config);

        deploymentYamlPort.start(config);
    }
}
