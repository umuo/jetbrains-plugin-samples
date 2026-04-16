package cn.lacknb.blog.llm.stream;

public class AuthConfig {
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String clientId;
    private String clientSecret;
    private String scope;
    private String audience;

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getScope() {
        return scope;
    }

    public String getAudience() {
        return audience;
    }

    public boolean isComplete() {
        return notBlank(authorizationEndpoint) && notBlank(tokenEndpoint) && notBlank(clientId);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
