package cn.lacknb.blog.llm.stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AuthSession {
    private final boolean loggedIn;
    private final String accessToken;
    private final String accountLabel;
    private final String statusMessage;
    private final long expiresAtEpochMillis;

    private AuthSession(boolean loggedIn,
                        @Nullable String accessToken,
                        @Nullable String accountLabel,
                        @Nullable String statusMessage,
                        long expiresAtEpochMillis) {
        this.loggedIn = loggedIn;
        this.accessToken = accessToken;
        this.accountLabel = accountLabel;
        this.statusMessage = statusMessage;
        this.expiresAtEpochMillis = expiresAtEpochMillis;
    }

    public static AuthSession loggedOut(@Nullable String statusMessage) {
        return new AuthSession(false, null, null, statusMessage, 0L);
    }

    public static AuthSession loggedIn(@NotNull String accessToken, @Nullable String accountLabel, @Nullable String statusMessage) {
        return loggedIn(accessToken, accountLabel, statusMessage, 0L);
    }

    public static AuthSession loggedIn(@NotNull String accessToken,
                                       @Nullable String accountLabel,
                                       @Nullable String statusMessage,
                                       long expiresAtEpochMillis) {
        return new AuthSession(true, accessToken, accountLabel, statusMessage, expiresAtEpochMillis);
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    @Nullable
    public String getAccessToken() {
        return accessToken;
    }

    @Nullable
    public String getAccountLabel() {
        return accountLabel;
    }

    @Nullable
    public String getStatusMessage() {
        return statusMessage;
    }

    public long getExpiresAtEpochMillis() {
        return expiresAtEpochMillis;
    }

    public boolean isExpired() {
        return expiresAtEpochMillis > 0 && System.currentTimeMillis() >= expiresAtEpochMillis;
    }
}
