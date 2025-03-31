package com.modulith.infraagentteam.domain.deployment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resources {
    private String cpu;
    private String memory;

    public String getCpu() {
        return Objects.requireNonNullElse(cpu, "1024");
    }

    public String getMemory() {
        return Objects.requireNonNullElse(memory, "2048");
    }
}
