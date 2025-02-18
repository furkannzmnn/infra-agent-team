package com.modulith.infraagentteam.domain.deployment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Service {
    private String name;
    private String image;
    private int port;
    private List<EnvironmentVariable> envVars;
    private int replicas;
    private Resources resources;
}
