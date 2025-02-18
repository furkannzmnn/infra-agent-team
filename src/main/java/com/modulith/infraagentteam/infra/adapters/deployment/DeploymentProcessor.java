package com.modulith.infraagentteam.infra.adapters.deployment;

import com.modulith.infraagentteam.domain.deployment.model.DeploymentConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DeploymentProcessor {

    private final Map<String, DeploymentHandler> handlers;

    public void processDeployment(DeploymentConfig config) {

        String key = config.getType() + "-" + config.getInfrastructure().getDeploymentTool();
        DeploymentHandler handler = handlers.get(key);

        if (handler != null) {
            handler.handle(config);
        } else {
            throw new UnsupportedOperationException("Unsupported deployment type: " + key);
        }

    }
}
