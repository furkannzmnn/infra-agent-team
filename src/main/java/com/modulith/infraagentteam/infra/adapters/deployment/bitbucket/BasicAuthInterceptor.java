package com.modulith.infraagentteam.infra.adapters.deployment.bitbucket;

import com.modulith.infraagentteam.infra.config.BitbucketProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class BasicAuthInterceptor implements ClientHttpRequestInterceptor {

    private final BitbucketProperties bitbucketProperties;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().setBasicAuth(bitbucketProperties.getUsername(), bitbucketProperties.getAppPassword());
        return execution.execute(request, body);
    }
}
