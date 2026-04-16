package cn.lacknb.blog.llm.stream;

import com.google.gson.Gson;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service(Service.Level.PROJECT)
public final class MyAuthService {
    private static final String SERVICE_NAME = "llm-chat-stream-render.auth.session";

    private final Project project;
    private final Gson gson = new Gson();
    private volatile AuthSession session;
    private volatile AuthConfig authConfig;

    public MyAuthService(Project project) {
        this.project = project;
        this.authConfig = AuthConfigLoader.load(project);
        this.session = AuthSession.loggedOut(defaultMessage(authConfig));
    }

    public void reloadConfig() {
        authConfig = AuthConfigLoader.load(project);
        if (!isLoggedIn()) {
            session = AuthSession.loggedOut(defaultMessage(authConfig));
        }
        publish(session, session.getStatusMessage());
    }

    public boolean isLoggedIn() {
        return session.isLoggedIn() && !session.isExpired();
    }

    @NotNull
    public AuthSession getSession() {
        return session;
    }

    @Nullable
    public AuthConfig getAuthConfig() {
        return authConfig;
    }

    @Nullable
    public String getBearerToken() {
        return session.getAccessToken();
    }

    @NotNull
    public String getUnauthenticatedMessage() {
        String message = session.getStatusMessage();
        if (message != null && !message.trim().isEmpty()) {
            return message;
        }
        return "请先登录后再使用 LLM Chat。";
    }

    public void setLoggedIn(@NotNull String accessToken, @Nullable String accountLabel, @Nullable String message) {
        setLoggedIn(accessToken, accountLabel, message, 0L);
    }

    public void setLoggedIn(@NotNull String accessToken,
                            @Nullable String accountLabel,
                            @Nullable String message,
                            long expiresAtEpochMillis) {
        session = AuthSession.loggedIn(accessToken, accountLabel, message, expiresAtEpochMillis);
        saveSession(session);
        publish(session, message);
    }

    public void setLoggedOut(@Nullable String message) {
        clearStoredSession();
        session = AuthSession.loggedOut(message != null ? message : defaultMessage(authConfig));
        publish(session, session.getStatusMessage());
    }

    public void updateStatus(@Nullable String message) {
        AuthSession current = session;
        session = current.isLoggedIn()
                ? AuthSession.loggedIn(current.getAccessToken(), current.getAccountLabel(), message, current.getExpiresAtEpochMillis())
                : AuthSession.loggedOut(message);
        if (session.isLoggedIn()) {
            saveSession(session);
        }
        publish(session, message);
    }

    public void restoreSession() {
        authConfig = AuthConfigLoader.load(project);
        AuthSession restored = loadStoredSession();
        if (restored == null) {
            session = AuthSession.loggedOut(defaultMessage(authConfig));
            publish(session, session.getStatusMessage());
            return;
        }
        if (restored.isExpired()) {
            clearStoredSession();
            session = AuthSession.loggedOut("当前登录状态已过期，请重新登录。");
            publish(session, session.getStatusMessage());
            return;
        }
        session = restored;
        publish(session, session.getStatusMessage());
    }

    private void publish(AuthSession currentSession, @Nullable String message) {
        project.getMessageBus().syncPublisher(AuthStatusListener.TOPIC).authStatusChanged(currentSession, message);
    }

    private void saveSession(@NotNull AuthSession authSession) {
        StoredSession storedSession = new StoredSession();
        storedSession.accessToken = authSession.getAccessToken();
        storedSession.accountLabel = authSession.getAccountLabel();
        storedSession.statusMessage = authSession.getStatusMessage();
        storedSession.expiresAtEpochMillis = authSession.getExpiresAtEpochMillis();
        PasswordSafe.getInstance().setPassword(getCredentialAttributes(), gson.toJson(storedSession));
    }

    @Nullable
    private AuthSession loadStoredSession() {
        String raw = PasswordSafe.getInstance().getPassword(getCredentialAttributes());
        if (StringUtil.isEmptyOrSpaces(raw)) {
            return null;
        }
        try {
            StoredSession storedSession = gson.fromJson(raw, StoredSession.class);
            if (storedSession == null || StringUtil.isEmptyOrSpaces(storedSession.accessToken)) {
                return null;
            }
            return AuthSession.loggedIn(
                    storedSession.accessToken,
                    storedSession.accountLabel,
                    storedSession.statusMessage,
                    storedSession.expiresAtEpochMillis
            );
        } catch (Exception ignore) {
            return null;
        }
    }

    private void clearStoredSession() {
        PasswordSafe.getInstance().setPassword(getCredentialAttributes(), null);
    }

    private CredentialAttributes getCredentialAttributes() {
        String projectKey = StringUtil.notNullize(project.getLocationHash(), "default");
        return new CredentialAttributes(SERVICE_NAME + "." + projectKey);
    }

    private static String defaultMessage(@Nullable AuthConfig config) {
        if (config == null) {
            return "无法登录：缺少认证配置，请在 .llm-chat-stream-render.json 中补充 认证配置。";
        }
        if (!config.isComplete()) {
            return "无法登录：认证配置不完整，至少需要 authorizationEndpoint、tokenEndpoint、clientId。";
        }
        return "当前未登录。";
    }

    private static final class StoredSession {
        private String accessToken;
        private String accountLabel;
        private String statusMessage;
        private long expiresAtEpochMillis;
    }
}
