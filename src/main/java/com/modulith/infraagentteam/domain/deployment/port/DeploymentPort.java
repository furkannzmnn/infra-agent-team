package com.modulith.infraagentteam.domain.deployment.port;

import com.modulith.infraagentteam.domain.deployment.model.DeploymentConfig;
import com.modulith.infraagentteam.domain.shared.model.query.BitbucketFileRetrieveRequest;

public interface DeploymentPort {

    DeploymentConfig retrieve(BitbucketFileRetrieveRequest request);

    void start(DeploymentConfig config);

}
