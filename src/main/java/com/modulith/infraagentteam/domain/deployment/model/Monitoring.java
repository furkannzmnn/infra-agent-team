package com.modulith.infraagentteam.domain.deployment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Monitoring {
    private boolean enabled;
    private String tool; // prometheus, cloudwatch, stackdriver
}
