package cn.lacknb.blog.llm.stream;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class OAuthLoginService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CALLBACK_TIMEOUT_SECONDS = 180;

    private final Project project;
    private final MyAuthService authService;
    private final AtomicLong loginAttemptCounter = new AtomicLong();

    public OAuthLoginService(Project project) {
        this.project = project;
        this.authService = project.getService(MyAuthService.class);
    }

    public void startLogin() {
        long attemptId = loginAttemptCounter.incrementAndGet();
        AuthConfig config = authService.getAuthConfig();
        if (config == null) {
            applyIfLatest(attemptId, () -> authService.setLoggedOut("无法登录：缺少认证配置，请在 .llm-chat-stream-render.json 中补充 auth 配置。"));
            return;
        }
        if (!config.isComplete()) {
            applyIfLatest(attemptId, () -> authService.setLoggedOut("无法登录：认证配置不完整，至少需要 authorizationEndpoint、tokenEndpoint、clientId。"));
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "LLM 登录", false) {
            @Override
            public void run(ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                applyIfLatest(attemptId, () -> authService.updateStatus("正在等待浏览器完成登录..."));
                try (CallbackServer callbackServer = new CallbackServer()) {
                    callbackServer.start();
                    String state = randomToken();
                    String redirectUri = callbackServer.getRedirectUri();
                    String url = buildAuthorizationUrl(config, redirectUri, state);
                    BrowserUtil.browse(url);
                    Map<String, String> params = callbackServer.awaitCallback(CALLBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    if (params == null) {
                        applyIfLatest(attemptId, () -> authService.setLoggedOut("登录超时：等待浏览器回调超时。"));
                        return;
                    }
                    if (params.containsKey("error")) {
                        applyIfLatest(attemptId, () -> authService.setLoggedOut("登录失败：" + params.get("error")));
                        return;
                    }
                    if (!state.equals(params.get("state"))) {
                        applyIfLatest(attemptId, () -> authService.setLoggedOut("登录失败：回调状态不匹配。"));
                        return;
                    }
                    String code = params.get("code");
                    if (code == null || code.trim().isEmpty()) {
                        applyIfLatest(attemptId, () -> authService.setLoggedOut("登录失败：回调中未包含授权码。"));
                        return;
                    }
                    String accessToken = "placeholder-token-" + code;
                    applyIfLatest(attemptId, () -> authService.setLoggedIn(accessToken, "已认证", "已通过占位版 OAuth 流程登录。"));
                } catch (Exception e) {
                    applyIfLatest(attemptId, () -> authService.setLoggedOut("登录失败：" + e.getMessage()));
                } finally {
                    ApplicationManager.getApplication().invokeLater(() -> applyIfLatest(attemptId, () -> authService.updateStatus(authService.isLoggedIn()
                            ? authService.getSession().getStatusMessage()
                            : authService.getUnauthenticatedMessage())));
                }
            }
        });
    }

    public boolean isLoginInProgress() {
        return false;
    }

    private void applyIfLatest(long attemptId, Runnable action) {
        if (attemptId == loginAttemptCounter.get()) {
            action.run();
        }
    }

    private String buildAuthorizationUrl(AuthConfig config, String redirectUri, String state) {
        StringBuilder url = new StringBuilder(config.getAuthorizationEndpoint());
        if (!config.getAuthorizationEndpoint().contains("?")) {
            url.append('?');
        } else if (!config.getAuthorizationEndpoint().endsWith("&") && !config.getAuthorizationEndpoint().endsWith("?")) {
            url.append('&');
        }
        appendParam(url, "response_type", "code");
        appendParam(url, "client_id", config.getClientId());
        appendParam(url, "redirect_uri", redirectUri);
        appendParam(url, "state", state);
        appendParam(url, "scope", config.getScope());
        appendParam(url, "audience", config.getAudience());
        return url.toString();
    }

    private static void appendParam(StringBuilder url, String key, @Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (url.charAt(url.length() - 1) != '?' && url.charAt(url.length() - 1) != '&') {
            url.append('&');
        }
        url.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
        url.append('=');
        url.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    private static String randomToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
