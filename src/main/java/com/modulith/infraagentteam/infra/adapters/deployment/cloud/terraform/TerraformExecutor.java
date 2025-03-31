package com.modulith.infraagentteam.infra.adapters.deployment.cloud.terraform;

import com.microsoft.terraform.TerraformClient;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;

import static java.nio.file.Files.createTempFile;

@Component
@RequiredArgsConstructor
@Slf4j
public class TerraformExecutor {

    private final TerraformClient terraformClient;


    @SneakyThrows
    public void deploy(String terraformScript) {

        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        for (File file : tempDir.listFiles()) {
            if (file.getName().startsWith("terraform") && file.getName().endsWith(".tf")) {
                file.delete();
            }
        }

        Path tempFile = createTempFile("terraform", ".tf");
        File file = tempFile.toFile();
        System.out.println("Terraform script file created at: " + file.getAbsolutePath());

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(terraformScript);
            writer.flush();
        }


        this.terraformClient.setOutputListener(System.out::println);
        this.terraformClient.setErrorListener(System.err::println);
        this.terraformClient.setWorkingDirectory(tempFile.getParent());
        terraformClient.apply();
    }
}
