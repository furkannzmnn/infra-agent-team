package com.modulith.infraagentteam.infra.shared;

import java.nio.charset.StandardCharsets;

public class EncodingFixer {
    public static String fixEncoding(String yamlContent) {
        return new String(yamlContent.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
