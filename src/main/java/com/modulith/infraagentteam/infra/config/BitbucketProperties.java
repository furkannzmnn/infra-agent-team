package com.modulith.infraagentteam.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "bitbucket")
@Component
public class BitbucketProperties {
    private String username;
    private String appPassword;
}
