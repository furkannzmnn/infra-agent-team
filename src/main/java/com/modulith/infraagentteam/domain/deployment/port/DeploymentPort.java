package com.modulith.infraagentteam.domain.deployment.port;

import com.modulith.infraagentteam.domain.deployment.model.DeploymentConfig;
import com.modulith.infraagentteam.domain.shared.model.query.FileRetrieveRequest;

public interface DeploymentPort {

    DeploymentConfig retrieve(FileRetrieveRequest request);

    void start(DeploymentConfig config);

}
