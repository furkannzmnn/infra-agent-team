package com.modulith.infraagentteam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.modulith.infraagentteam.domain",
        "com.modulith.infraagentteam.infra"
})
public class InfraAgentTeamApplication {

    public static void main(String[] args) {
        SpringApplication.run(InfraAgentTeamApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
