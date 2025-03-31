package com.modulith.infraagentteam.infra.adapters.deployment.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.modulith.infraagentteam.domain.deployment.model.DeploymentConfig;
import com.modulith.infraagentteam.domain.shared.model.query.FileRetrieveRequest;
import com.modulith.infraagentteam.domain.shared.model.query.GitType;
import com.modulith.infraagentteam.infra.adapters.deployment.bitbucket.BasicAuthInterceptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import static com.modulith.infraagentteam.infra.shared.YamlParser.parseYaml;

@Component
public class GithubClient implements GitClient {

    private final RestTemplate restTemplate;

    public GithubClient(RestTemplate restTemplate, BasicAuthInterceptor basicAuthInterceptor) {
        this.restTemplate = restTemplate;
        restTemplate.getInterceptors().add(basicAuthInterceptor);
    }

    public DeploymentConfig callFile(FileRetrieveRequest retrieveRequest) {

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

    @Override
    public boolean isSupportedClient(GitType type) {
        return GitType.GITHUB.equals(type);
    }
}
