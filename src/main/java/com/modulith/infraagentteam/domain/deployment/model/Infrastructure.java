package com.modulith.infraagentteam.domain.deployment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Infrastructure {
    private String provider; // aws, gcp, azure, on-prem
    private String deploymentTool; // terraform, cloudformation, helm, docker-compose
    private String region;
    private Credentials credentials;
}
