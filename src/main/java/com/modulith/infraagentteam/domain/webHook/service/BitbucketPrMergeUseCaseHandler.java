package com.modulith.infraagentteam.domain.webHook.service;

import com.modulith.infraagentteam.domain.deployment.model.DeploymentConfig;
import com.modulith.infraagentteam.domain.deployment.port.DeploymentPort;
import com.modulith.infraagentteam.domain.shared.model.query.FileRetrieveRequest;
import com.modulith.infraagentteam.domain.shared.model.query.GitType;
import com.modulith.infraagentteam.domain.webHook.usecase.BitbucketPrMergeUseCase;
import com.modulith.infraagentteam.infra.adapters.persistence.redis.RedisCommitHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BitbucketPrMergeUseCaseHandler {

    private final DeploymentPort deploymentYamlPort;
    private final RedisCommitHandler redisCommitHandler;

    public void handle(BitbucketPrMergeUseCase payload) {
        log.info("Handling Bitbucket PR merge webhook: {}", payload);

        redisCommitHandler.markCommitAsProcessed(payload.commitId());

        DeploymentConfig config = deploymentYamlPort.retrieve(FileRetrieveRequest.builder()
                .commit(payload.commitId())
                .repo(payload.repositoryName())
                .fileName("deployment.yml")
                .gitType(GitType.BITBUCKET)
                .build());

        log.info("Deployment config retrieved: {}", config);

        deploymentYamlPort.start(config);
    }
}
