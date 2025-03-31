package com.modulith.infraagentteam.infra.adapters.deployment.cloud.aws.cloudformation;

import com.modulith.infraagentteam.domain.deployment.model.*;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.*;
import com.modulith.infraagentteam.infra.adapters.deployment.DeploymentHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
        System.out.println("Generating AWS ECS CloudFormation template...");

        String cloudformationScript = generateCloudformationScript(config);
        System.out.println("Generated CloudFormation script:\n" + cloudformationScript);
        deployWithCloudformation(cloudformationScript, config.getInfrastructure(), config.getType());
    }

    private String generateCloudformationScript(DeploymentConfig config) {
        try {
            String template = new String(Files.readAllBytes(new ClassPathResource("templates/ecs-cloudformation.yml").getFile().toPath()));
            Map<String, Service> services = config.getServices();
            Network network = config.getNetwork();

            template = template.replace("{{VPC}}", network.getVpc());
            template = template.replace("{{SUBNETS}}", generateSubnetsJson(network.getSubnets()));
            template = template.replace("{{SECURITY_GROUP}}", network.getSecurityGroup());

            StringBuilder taskDefinitions = new StringBuilder();
            StringBuilder servicesBuilder = new StringBuilder();
            StringBuilder targetGroups = new StringBuilder();
            StringBuilder listeners = new StringBuilder();

            int listenerPort = 80;
            for (Map.Entry<String, Service> entry : services.entrySet()) {
                String serviceName = entry.getKey();
                Service service = entry.getValue();

                taskDefinitions.append(generateTaskDefinition(serviceName, service, services));

                if (!service.isInternal()) {
                    targetGroups.append(generateTargetGroup(serviceName, service));
                    listeners.append(generateListenerRule(serviceName, service, listenerPort++));
                }

                servicesBuilder.append(generateEcsService(serviceName, service, network));
            }

            template = template.replace("{{TASK_DEFINITIONS}}", taskDefinitions.toString());
            template = template.replace("{{TARGET_GROUPS}}", targetGroups.toString());
            template = template.replace("{{LISTENER_RULES}}", listeners.toString());
            template = template.replace("{{ECS_SERVICES}}", servicesBuilder.toString());

            template = template.replace("{{VPC}}", network.getVpc());

            return template;
        } catch (IOException e) {
            throw new RuntimeException("Error reading CloudFormation template", e);
        }
    }

    private String generateTaskDefinition(String serviceName, Service service, Map<String, Service> services) {
        StringBuilder taskDef = new StringBuilder();
        taskDef.append("  ").append(serviceName).append("TaskDefinition:\n");
        taskDef.append("    Type: AWS::ECS::TaskDefinition\n");
        taskDef.append("    Properties:\n");
        taskDef.append("      Family: ").append(serviceName).append("\n");
        taskDef.append("      Cpu: ").append(service.getResources().getCpu()).append("\n");
        taskDef.append("      Memory: ").append(service.getResources().getMemory()).append("\n");
        taskDef.append("      NetworkMode: awsvpc\n");
        taskDef.append("      RequiresCompatibilities:\n");
        taskDef.append("        - FARGATE\n");
        taskDef.append("      ExecutionRoleArn: !GetAtt EcsTaskExecutionRole.Arn\n");
        taskDef.append("      TaskRoleArn: !GetAtt EcsTaskRole.Arn\n");
        taskDef.append("      ContainerDefinitions:\n");
        taskDef.append("        - Name: ").append(serviceName).append("\n");
        taskDef.append("          Image: ").append(service.getImage()).append("\n");
        taskDef.append("          PortMappings:\n");
        taskDef.append("            - ContainerPort: ").append(service.getPort()).append("\n");
        taskDef.append("              Protocol: tcp\n");

        if (service.getEnvVars() != null && !service.getEnvVars().isEmpty()) {
            taskDef.append("          Environment:\n");
            for (EnvironmentVariable envVar : service.getEnvVars()) {
                taskDef.append("            - Name: ").append(envVar.getKey()).append("\n");
                taskDef.append("              Value: ").append(resolveEnvVarValue(envVar.getValue(), services)).append("\n");
            }
        }

        taskDef.append("\n");
        return taskDef.toString();
    }

    private String generateTargetGroup(String serviceName, Service service) {
        StringBuilder targetGroup = new StringBuilder();
        targetGroup.append("  ").append(serviceName).append("TargetGroup:\n");
        targetGroup.append("    Type: AWS::ElasticLoadBalancingV2::TargetGroup\n");
        targetGroup.append("    Properties:\n");
        targetGroup.append("      Name: ").append(serviceName).append("-tg\n");
        targetGroup.append("      Port: ").append(service.getPort()).append("\n");
        targetGroup.append("      Protocol: HTTP\n");
        targetGroup.append("      TargetType: ip\n");
        targetGroup.append("      VpcId: ").append("{{VPC}}").append("\n");
        targetGroup.append("      HealthCheckPath: /\n");
        targetGroup.append("      HealthCheckIntervalSeconds: 30\n");
        targetGroup.append("      HealthCheckTimeoutSeconds: 5\n");
        targetGroup.append("      HealthyThresholdCount: 2\n");
        targetGroup.append("      UnhealthyThresholdCount: 2\n");
        targetGroup.append("\n");
        return targetGroup.toString();
    }

    private String generateListenerRule(String serviceName, Service service, int port) {
        StringBuilder listener = new StringBuilder();
        listener.append("  ").append(serviceName).append("ListenerRule:\n");
        listener.append("    Type: AWS::ElasticLoadBalancingV2::ListenerRule\n");
        listener.append("    DependsOn:\n");
        listener.append("      - ApplicationLoadBalancerListener\n");
        listener.append("      - ").append(serviceName).append("TargetGroup\n");
        listener.append("    Properties:\n");
        listener.append("      ListenerArn: !Ref ApplicationLoadBalancerListener\n");
        listener.append("      Priority: ").append(port).append("\n");
        listener.append("      Conditions:\n");
        listener.append("        - Field: path-pattern\n");
        listener.append("          Values:\n");
        listener.append("            - /").append(serviceName).append("/*\n");
        listener.append("      Actions:\n");
        listener.append("        - Type: forward\n");
        listener.append("          TargetGroupArn: !Ref ").append(serviceName).append("TargetGroup\n");
        listener.append("\n");
        return listener.toString();
    }

    private String generateEcsService(String serviceName, Service service, Network network) {
        StringBuilder ecsService = new StringBuilder();
        ecsService.append("  ").append(serviceName).append("Service:\n");
        ecsService.append("    Type: AWS::ECS::Service\n");
        ecsService.append("    DependsOn:\n");
        ecsService.append("      - ApplicationLoadBalancer\n");
        if (!service.isInternal()) {
            ecsService.append("      - ").append(serviceName).append("TargetGroup\n");
            ecsService.append("      - ").append(serviceName).append("ListenerRule\n");
        }
        ecsService.append("    Properties:\n");
        ecsService.append("      ServiceName: ").append(serviceName).append("\n");
        ecsService.append("      Cluster: !Ref EcsCluster\n");
        ecsService.append("      TaskDefinition: !Ref ").append(serviceName).append("TaskDefinition\n");
        ecsService.append("      DesiredCount: ").append(service.getReplicas() != null ? service.getReplicas() : 1).append("\n");
        ecsService.append("      LaunchType: FARGATE\n");
        ecsService.append("      NetworkConfiguration:\n");
        ecsService.append("        AwsvpcConfiguration:\n");
        ecsService.append("          Subnets: ").append(generateSubnetsJson(network.getSubnets())).append("\n");
        ecsService.append("          SecurityGroups:\n");
        ecsService.append("            - ").append(network.getSecurityGroup()).append("\n");
        ecsService.append("          AssignPublicIp: ").append(service.isInternal() ? "DISABLED" : "ENABLED").append("\n");
        if (!service.isInternal()) {
            ecsService.append("      LoadBalancers:\n");
            ecsService.append("        - TargetGroupArn: !Ref ").append(serviceName).append("TargetGroup\n");
            ecsService.append("          ContainerName: ").append(serviceName).append("\n");
            ecsService.append("          ContainerPort: ").append(service.getPort()).append("\n");
        }
        ecsService.append("\n");
        return ecsService.toString();
    }

    private String resolveEnvVarValue(String value, Map<String, Service> services) {
        if (value != null && value.startsWith("${{services.")) {
            String[] parts = value.substring(2, value.length() - 2).split("\\.");
            if (parts.length == 3) {
                String serviceName = parts[1];
                String property = parts[2];
                Service referencedService = services.get(serviceName);
                if (referencedService != null) {
                    if ("host".equals(property)) {
                        return referencedService.getName() + ".internal";
                    }
                }
            }
        }
        return value;
    }

    private String generateSubnetsJson(List<String> subnets) {
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        for (int i = 0; i < subnets.size(); i++) {
            json.append("      \"").append(subnets.get(i).trim()).append("\"");
            if (i < subnets.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("    ]");
        return json.toString();
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

                Thread.sleep(5000);
            } catch (Exception e) {
                throw new RuntimeException("Error checking stack status", e);
            }
        }
    }
}
