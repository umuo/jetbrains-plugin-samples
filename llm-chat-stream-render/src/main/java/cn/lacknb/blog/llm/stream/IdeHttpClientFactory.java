package cn.lacknb.blog.llm.stream;

import com.intellij.util.net.HttpConfigurable;

import java.net.Authenticator;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;

public final class IdeHttpClientFactory {
    private IdeHttpClientFactory() {
    }

    public static HttpClient create(Duration connectTimeout) {
        HttpConfigurable proxySettings = HttpConfigurable.getInstance();
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL);
        ProxySelector selector = proxySettings.getOnlyBySettingsSelector();
        if (selector != null) {
            builder.proxy(selector);
        }
        Authenticator authenticator = java.net.Authenticator.getDefault();
        if (authenticator != null) {
            builder.authenticator(authenticator);
        }
        return builder.build();
    }
}
