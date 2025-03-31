package com.modulith.infraagentteam.infra.adapters.deployment.cloud.aws.ecs;

import com.modulith.infraagentteam.domain.deployment.model.DeploymentConfig;
import com.modulith.infraagentteam.domain.deployment.model.Infrastructure;
import com.modulith.infraagentteam.domain.deployment.model.Network;
import com.modulith.infraagentteam.domain.deployment.model.Service;
import com.modulith.infraagentteam.infra.adapters.deployment.DeploymentHandler;
import com.modulith.infraagentteam.infra.adapters.deployment.cloud.terraform.TerraformExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("aws-ecs-terraform")
@RequiredArgsConstructor
public class AwsEcsTerraformBuilder implements DeploymentHandler {

    private final TerraformExecutor terraformExecutor;

    @Override
    public void handle(DeploymentConfig config) {
        System.out.println("Generating Terraform script for AWS ECS deployment...");

        String terraformScript = generateTerraformScript(config);
        deployWithTerraform(terraformScript);
    }

    private String generateTerraformScript(DeploymentConfig config) {
        Infrastructure infra = config.getInfrastructure();
        Map<String, Service> services = config.getServices();
        Network network = config.getNetwork();

        Service mainService = services.values().iterator().next();


        return """
                provider "aws" {
                  region = "%s"
                  access_key = "%s"
                  secret_key = "%s"
                }
                
                resource "aws_security_group" "ecs_sg" {
                  vpc_id = "%s"
                
                  ingress {
                    from_port   = %d
                    to_port     = %d
                    protocol    = "tcp"
                    cidr_blocks = ["0.0.0.0/0"]
                  }
                  egress {
                    from_port   = 0
                    to_port     = 0
                    protocol    = "-1"
                    cidr_blocks = ["0.0.0.0/0"]
                  }
                  tags = { Name = "ecs-security-group" }
                }
                
                resource "aws_ecs_cluster" "main" {
                  name = "ecs-cluster"
                }
                
                resource "aws_ecs_task_definition" "app_task" {
                  family = "%s"
                  requires_compatibilities = ["FARGATE"]
                  network_mode = "awsvpc"
                  memory = %d
                  cpu = %d
                  execution_role_arn = aws_iam_role.ecs_task_execution_role.arn
                  container_definitions = jsonencode([{
                    name = "%s"
                    image = "%s"
                    memory = %d
                    cpu = %d
                    essential = true
                    portMappings = [{
                      containerPort = %d
                      hostPort = %d
                    }]
                    environment = [
                      { name = "SPRING_PROFILES_ACTIVE", value = "prod" }
                    ]
                  }])
                }
                
                resource "aws_ecs_service" "app_service" {
                  name = "%s"
                  cluster = aws_ecs_cluster.main.id
                  task_definition = aws_ecs_task_definition.app_task.arn
                  desired_count = %d
                  launch_type = "FARGATE"
                  network_configuration {
                    subnets = [%s]
                    security_groups = ["%s"]
                    assign_public_ip = true
                  }
                }
                
                resource "aws_iam_role" "ecs_task_execution_role" {
                  name = "ecsTaskExecutionRole"
                
                  assume_role_policy = jsonencode({
                    Version = "2012-10-17"
                    Statement = [{
                      Effect = "Allow"
                      Principal = {
                        Service = "ecs-tasks.amazonaws.com"
                      }
                      Action = "sts:AssumeRole"
                    }]
                  })
                }
                
                resource "aws_iam_policy_attachment" "ecs_task_execution_attach" {
                  name       = "ecsTaskExecutionPolicyAttachment"
                  roles      = [aws_iam_role.ecs_task_execution_role.name]
                  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
                }
                
                """
                .formatted(
                        infra.getRegion(),
                        infra.getCredentials().getAccessKey(),
                        infra.getCredentials().getSecretKey(),
                        network.getVpc(),
                        mainService.getPort(), mainService.getPort(),
                        mainService.getName(),
                        Integer.parseInt(mainService.getResources().getMemory().replace("Mi", "")), // "2048Mi" → 2048
                        Integer.parseInt(mainService.getResources().getCpu()),
                        mainService.getName(),
                        mainService.getImage(),
                        Integer.parseInt(mainService.getResources().getMemory().replace("Mi", "")),
                        Integer.parseInt(mainService.getResources().getCpu()),
                        mainService.getPort(),
                        mainService.getPort(),
                        mainService.getName(),
                        mainService.getReplicas(),
                        formatSubnets(network.getSubnets()),
                        network.getSecurityGroup()
                );
    }

    private String formatSubnets(List<String> subnets) {
        return subnets.stream()
                .map(subnet -> "\"" + subnet + "\"")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    private void deployWithTerraform(String terraformScript) {
        terraformExecutor.deploy(terraformScript);
    }
}
