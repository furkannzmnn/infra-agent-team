package com.modulith.infraagentteam.infra.adapters.deployment.github;

import com.modulith.infraagentteam.domain.deployment.model.DeploymentConfig;
import com.modulith.infraagentteam.domain.shared.model.query.FileRetrieveRequest;
import com.modulith.infraagentteam.domain.shared.model.query.GitType;

public interface GitClient {
    boolean isSupportedClient(GitType type);
    DeploymentConfig callFile(FileRetrieveRequest request);
}
