package cn.lacknb.blog.llm.stream;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.project.Project;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AuthConfigLoader {
    private AuthConfigLoader() {
    }

    public static AuthConfig load(Project project) {
        Path path = LLMConfigLoader.resolveConfigPath(project);
        if (path == null || !Files.exists(path)) {
            return null;
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("auth") || !root.get("auth").isJsonObject()) {
                return null;
            }
            return new Gson().fromJson(root.getAsJsonObject("auth"), AuthConfig.class);
        } catch (Exception e) {
            return null;
        }
    }
}
