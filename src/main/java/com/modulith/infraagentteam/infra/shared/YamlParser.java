package com.modulith.infraagentteam.infra.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.modulith.infraagentteam.domain.deployment.model.DeploymentConfig;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import java.io.IOException;

public class YamlParser {

    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    public static DeploymentConfig parseYaml(String yamlContent) {
        try {
            return yamlMapper.readValue(yamlContent, DeploymentConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing YAML file: " + e.getMessage(), e);
        }
    }

}
