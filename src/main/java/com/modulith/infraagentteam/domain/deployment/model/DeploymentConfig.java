package com.modulith.infraagentteam.domain.deployment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@Data
public class DeploymentConfig {
    private String type;
    private Infrastructure infrastructure;
    private Map<String, Service> services;
    private Network network;
    private Monitoring monitoring;
}


