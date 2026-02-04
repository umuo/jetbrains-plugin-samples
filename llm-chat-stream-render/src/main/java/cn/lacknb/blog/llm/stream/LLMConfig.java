package cn.lacknb.blog.llm.stream;

public class LLMConfig {
    private String baseUrl;
    private String apiKey;
    private String model;

    public LLMConfig() {
    }

    public LLMConfig(String baseUrl, String apiKey, String model) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }
}
