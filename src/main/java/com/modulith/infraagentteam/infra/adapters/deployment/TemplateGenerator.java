package com.modulith.infraagentteam.infra.adapters.deployment;

import com.modulith.infraagentteam.domain.deployment.model.DeploymentConfig;

public interface TemplateGenerator {
    String generateTemplate(DeploymentConfig config);
} 