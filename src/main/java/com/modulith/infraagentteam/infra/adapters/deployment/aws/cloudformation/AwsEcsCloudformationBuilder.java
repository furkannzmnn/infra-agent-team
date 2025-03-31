package com.modulith.infraagentteam.infra.adapters.deployment.aws.cloudformation;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.*;
import com.modulith.infraagentteam.domain.deployment.model.DeploymentConfig;
import com.modulith.infraagentteam.domain.deployment.model.Infrastructure;
import com.modulith.infraagentteam.domain.deployment.model.Network;
import com.modulith.infraagentteam.domain.deployment.model.Service;
import com.modulith.infraagentteam.domain.deployment.model.EnvironmentVariable;
import com.modulith.infraagentteam.infra.adapters.deployment.DeploymentHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

@Component("aws-ecs-cloudformation")
@RequiredArgsConstructor
public class AwsEcsCloudformationBuilder implements DeploymentHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(DeploymentConfig config) {
        System.out.println("Generating Cloudformation script for AWS ECS deployment...");

        String cloudformationScript = generateCloudformationScript(config);
        deployWithCloudformation(cloudformationScript, config.getInfrastructure(), config.getType());
    }

    private String generateCloudformationScript(DeploymentConfig config) {
        Map<String, Service> services = config.getServices();
        Network network = config.getNetwork();

        Service mainService = services.values().iterator().next();

        try {
            ClassPathResource resource = new ClassPathResource("templates/ecs-cloudformation.yml");
            String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            String envVarsJson = generateEnvVarsJson(mainService.getEnvVars());
            String subnetsJson = generateSubnetsJson(network.getSubnets());

            return template
                    .replace("{{VPC_ID}}", network.getVpc().trim())
                    .replace("{{CPU}}", mainService.getResources().getCpu())
                    .replace("{{MEMORY}}", mainService.getResources().getMemory().replace("Mi", ""))
                    .replace("{{IMAGE}}", mainService.getImage())
                    .replace("{{PORT}}", String.valueOf(mainService.getPort()))
                    .replace("{{REPLICAS}}", String.valueOf(mainService.getReplicas()))
                    .replace("{{ENV_VARS}}", envVarsJson)
                    .replace("{{SUBNETS}}", subnetsJson)
                    .replace("{{SECURITY_GROUP}}", network.getSecurityGroup().trim());
        } catch (IOException e) {
            throw new RuntimeException("Error reading CloudFormation template", e);
        }
    }

    private String generateSubnetsJson(List<String> subnets) {
        try {
            List<String> cleanedSubnets = subnets.stream()
                    .map(String::trim)
                    .toList();
            return objectMapper.writeValueAsString(cleanedSubnets);
        } catch (IOException e) {
            throw new RuntimeException("Error generating subnets JSON", e);
        }
    }

    private String generateEnvVarsJson(List<EnvironmentVariable> envVars) {
        if (envVars == null || envVars.isEmpty()) {
            return "[]";
        }

        List<Map<String, String>> envVarList = new ArrayList<>();
        for (EnvironmentVariable envVar : envVars) {
            envVarList.add(Map.of(
                "Name", envVar.getKey(),
                "Value", envVar.getValue()
            ));
        }

        try {
            return objectMapper.writeValueAsString(envVarList);
        } catch (IOException e) {
            throw new RuntimeException("Error generating environment variables JSON", e);
        }
    }

    private void deployWithCloudformation(String cloudformationScript, Infrastructure infrastructure, String type) {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                infrastructure.getCredentials().getAccessKey(),
                infrastructure.getCredentials().getSecretKey()
        );

        CloudFormationClient cloudFormationClient = CloudFormationClient.builder()
                .region(Region.of(infrastructure.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();

        String stackName = type + "-" + UUID.randomUUID().toString().substring(0, 8);

        try {

            if (hasAlreadyCreatedStack(type, cloudFormationClient)) return;

        } catch (Exception e) {
            System.out.println("Error checking existing stacks: " + e.getMessage());
        }

        CreateStackRequest createStackRequest = CreateStackRequest.builder()
                .stackName(stackName)
                .templateBody(cloudformationScript)
                .capabilities(Capability.CAPABILITY_IAM, Capability.CAPABILITY_NAMED_IAM)
                .onFailure(OnFailure.DELETE)
                .build();

        try {
            CreateStackResponse createStackResponse = cloudFormationClient.createStack(createStackRequest);
            System.out.println("Stack creation initiated with ID: " + createStackResponse.stackId());

            // Wait for stack creation to complete
            waitForStackCreation(cloudFormationClient, stackName);
        } catch (Exception e) {
            throw new RuntimeException("Error deploying CloudFormation stack", e);
        }
    }

    private static boolean hasAlreadyCreatedStack(String type, CloudFormationClient cloudFormationClient) {
        ListStacksRequest listStacksRequest = ListStacksRequest.builder()
                .stackStatusFilters(StackStatus.CREATE_COMPLETE, StackStatus.UPDATE_COMPLETE)
                .build();
        ListStacksResponse listStacksResponse = cloudFormationClient.listStacks(listStacksRequest);

        for (StackSummary stack : listStacksResponse.stackSummaries()) {
            if (stack.stackName().startsWith(type)) {
                System.out.println("Stack already exists for this PR: " + stack.stackName());
                return true;
            }
        }
        return false;
    }

    private void waitForStackCreation(CloudFormationClient cloudFormationClient, String stackName) {
        boolean stackCreationComplete = false;
        while (!stackCreationComplete) {
            try {
                DescribeStacksRequest describeStacksRequest = DescribeStacksRequest.builder()
                        .stackName(stackName)
                        .build();
                DescribeStacksResponse describeStacksResponse = cloudFormationClient.describeStacks(describeStacksRequest);
                Stack stack = describeStacksResponse.stacks().get(0);

                String stackStatus = stack.stackStatus().toString();
                System.out.println("Stack status: " + stackStatus);

                if (stackStatus.equals(StackStatus.CREATE_COMPLETE.toString())) {
                    stackCreationComplete = true;
                    System.out.println("Stack creation completed successfully!");
                } else if (stackStatus.equals(StackStatus.CREATE_FAILED.toString())) {
                    throw new RuntimeException("Stack creation failed");
                }

                Thread.sleep(5000); // Wait 5 seconds before checking again
            } catch (Exception e) {
                throw new RuntimeException("Error checking stack status", e);
            }
        }
    }
}
