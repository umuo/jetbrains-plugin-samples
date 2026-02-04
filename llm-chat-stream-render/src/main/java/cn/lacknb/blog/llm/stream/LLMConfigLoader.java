package cn.lacknb.blog.llm.stream;

import com.google.gson.Gson;
import com.intellij.openapi.project.Project;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LLMConfigLoader {
    private static final String CONFIG_FILE = ".llm-chat-stream-render.json";
    private static final String EMBEDDED_CLASS = "cn.lacknb.blog.llm.stream.EmbeddedLLMConfig";
    private static final String ALLOW_FILE_PROPERTY = "llm.config.allowFile";

    private LLMConfigLoader() {
    }

    public static LLMConfig load(Project project) {
        EmbeddedLoadResult embedded = tryLoadEmbedded();
        boolean allowFile = Boolean.getBoolean(ALLOW_FILE_PROPERTY);
        if (embedded.present && !allowFile) {
            return embedded.config;
        }
        LLMConfig fileConfig = loadFromFile(project);
        if (fileConfig != null) {
            return fileConfig;
        }
        return embedded.config;
    }

    private static EmbeddedLoadResult tryLoadEmbedded() {
        try {
            Class<?> clazz = Class.forName(EMBEDDED_CLASS);
            Method loadMethod = clazz.getDeclaredMethod("load");
            Object value = loadMethod.invoke(null);
            return new EmbeddedLoadResult(true, (LLMConfig) value);
        } catch (ClassNotFoundException e) {
            return new EmbeddedLoadResult(false, null);
        } catch (Exception e) {
            return new EmbeddedLoadResult(true, null);
        }
    }

    private static LLMConfig loadFromFile(Project project) {
        Path path = resolveConfigPath(project);
        if (path == null || !Files.exists(path)) {
            return null;
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            return new Gson().fromJson(json, LLMConfig.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static Path resolveConfigPath(Project project) {
        String envPath = System.getenv("LLM_CONFIG_PATH");
        if (envPath != null && !envPath.isBlank()) {
            Path path = Paths.get(envPath);
            if (Files.exists(path)) {
                return path;
            }
        }
        String basePath = project != null ? project.getBasePath() : null;
        if (basePath != null) {
            Path cursor = Paths.get(basePath);
            while (cursor != null) {
                Path path = cursor.resolve(CONFIG_FILE);
                if (Files.exists(path)) {
                    return path;
                }
                cursor = cursor.getParent();
            }
        }
        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isBlank()) {
            Path path = Paths.get(userDir, CONFIG_FILE);
            if (Files.exists(path)) {
                return path;
            }
        }
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isBlank()) {
            Path path = Paths.get(userHome, CONFIG_FILE);
            if (Files.exists(path)) {
                return path;
            }
        }
        return null;
    }

    private static final class EmbeddedLoadResult {
        private final boolean present;
        private final LLMConfig config;

        private EmbeddedLoadResult(boolean present, LLMConfig config) {
            this.present = present;
            this.config = config;
        }
    }
}
