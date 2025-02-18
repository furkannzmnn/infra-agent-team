package com.modulith.infraagentteam.infra.adapters.deployment.bitbucket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.modulith.infraagentteam.domain.deployment.model.DeploymentConfig;
import com.modulith.infraagentteam.domain.shared.model.query.BitbucketFileRetrieveRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static com.modulith.infraagentteam.infra.shared.YamlParser.parseYaml;

@Component
public class BitbucketClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper yamlMapper;
    private final ObjectMapper jsonMapper;


    public BitbucketClient(RestTemplate restTemplate, ObjectMapper yamlMapper, BasicAuthInterceptor basicAuthInterceptor) {
        this.restTemplate = restTemplate;
        restTemplate.getInterceptors().add(basicAuthInterceptor);
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.jsonMapper = new ObjectMapper();
    }

    public DeploymentConfig callFile(BitbucketFileRetrieveRequest retrieveRequest) {

        try {
            String repoFullName = retrieveRequest.repo();
            String commitSha = retrieveRequest.commit();
            String fileName = retrieveRequest.fileName();

            String url = "https://api.bitbucket.org/2.0/repositories/" + repoFullName + "/src/" + commitSha + "/" + fileName;

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String yamlContent = response.getBody();
                return parseYaml(yamlContent);
            } else {
                throw new RuntimeException("Failed to fetch deployment.yml. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch deployment.yml", e);
        }

    }

}
