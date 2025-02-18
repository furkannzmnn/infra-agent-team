package com.modulith.infraagentteam.infra.adapters.deployment.aws;

import com.modulith.infraagentteam.domain.deployment.model.DeploymentConfig;
import com.modulith.infraagentteam.domain.deployment.model.Infrastructure;
import com.modulith.infraagentteam.domain.deployment.model.Network;
import com.modulith.infraagentteam.domain.deployment.model.Service;
import com.modulith.infraagentteam.infra.adapters.deployment.DeploymentHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("aws-ecs-terraform")
public class AwsEcsTerraformBuilder implements DeploymentHandler {

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
                  memory = "%s"
                  cpu = "%s"
                  execution_role_arn = aws_iam_role.ecs_task_execution_role.arn
                  container_definitions = jsonencode([{
                    name = "%s"
                    image = "%s"
                    memory = "%s"
                    cpu = "%s"
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
                """
                .formatted(
                        infra.getRegion(),
                        network.getVpc(),
                        mainService.getPort(), mainService.getPort(),
                        mainService.getName(),
                        mainService.getResources().getMemory(),
                        mainService.getResources().getCpu(),
                        mainService.getName(),
                        mainService.getImage(),
                        mainService.getResources().getMemory(),
                        mainService.getResources().getCpu(),
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

    }
}
