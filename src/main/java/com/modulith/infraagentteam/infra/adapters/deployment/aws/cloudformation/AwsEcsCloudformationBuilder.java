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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component("aws-ecs-cloudformation")
@RequiredArgsConstructor
public class AwsEcsCloudformationBuilder implements DeploymentHandler {

    @Override
    public void handle(DeploymentConfig config) {
        System.out.println("Generating Cloudformation script for AWS ECS deployment...");

        String cloudformationScript = generateCloudformationScript(config);
        deployWithCloudformation(cloudformationScript, config.getInfrastructure());
    }

    private String generateCloudformationScript(DeploymentConfig config) {
        Map<String, Service> services = config.getServices();
        Network network = config.getNetwork();

        Service mainService = services.values().iterator().next();

        try {
            ClassPathResource resource = new ClassPathResource("templates/ecs-cloudformation.yml");
            String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            String envVarsTemplate = generateEnvVarsTemplate(mainService.getEnvVars());

            return template
                    .replace("{{CLUSTER_NAME}}", "ecs-cluster")
                    .replace("{{SECURITY_GROUP}}", network.getSecurityGroup())
                    .replace("{{SUBNET_1}}", network.getSubnets().get(0))
                    .replace("{{SUBNET_2}}", network.getSubnets().get(1))
                    .replace("{{VPC_ID}}", network.getVpc())
                    .replace("{{CPU}}", mainService.getResources().getCpu())
                    .replace("{{MEMORY}}", mainService.getResources().getMemory().replace("Mi", ""))
                    .replace("{{IMAGE}}", mainService.getImage())
                    .replace("{{PORT}}", String.valueOf(mainService.getPort()))
                    .replace("{{REPLICAS}}", String.valueOf(mainService.getReplicas()))
                    .replace("{{#each ENV_VARS}}", envVarsTemplate);
        } catch (IOException e) {
            throw new RuntimeException("Error reading CloudFormation template", e);
        }
    }

    private String generateEnvVarsTemplate(List<EnvironmentVariable> envVars) {
        if (envVars == null || envVars.isEmpty()) {
            return "";
        }

        return envVars.stream()
                .map(envVar -> String.format("            - Name: \"%s\"\n              Value: \"%s\"", 
                        envVar.getKey(), envVar.getValue()))
                .collect(Collectors.joining("\n"));
    }

    private void deployWithCloudformation(String cloudformationScript, Infrastructure infrastructure) {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                infrastructure.getCredentials().getAccessKey(),
                infrastructure.getCredentials().getSecretKey()
        );

        CloudFormationClient cloudFormationClient = CloudFormationClient.builder()
                .region(Region.of(infrastructure.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();

        String stackName = "ecs-stack-" + UUID.randomUUID().toString().substring(0, 8);

        CreateStackRequest createStackRequest = CreateStackRequest.builder()
                .stackName(stackName)
                .templateBody(cloudformationScript)
                .capabilities(Capability.CAPABILITY_IAM)
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

    private void waitForStackCreation(CloudFormationClient cloudFormationClient, String stackName) {
        boolean stackCreationComplete = false;
        while (!stackCreationComplete) {
            try {
                DescribeStacksRequest describeStacksRequest = DescribeStacksRequest.builder()
                        .stackName(stackName)
                        .build();
                DescribeStacksResponse describeStacksResponse = cloudFormationClient.describeStacks(describeStacksRequest);
                Stack stack = describeStacksResponse.stacks().getFirst();

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
