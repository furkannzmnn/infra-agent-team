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
    private Integer port;
    private Integer replicas;
    private Resources resources;
    private List<EnvironmentVariable> envVars;
    private boolean internal = true;

    public Resources getResources() {
        if (resources == null) {
            return Resources.builder()
                    .cpu("1024")
                    .memory("2048")
                    .build();
        }
        return resources;
    }
}
