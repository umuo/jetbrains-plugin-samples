package cn.lacknb.blog.llm.stream;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.Nullable;

public interface AuthStatusListener {
    Topic<AuthStatusListener> TOPIC = Topic.create("llm-chat-auth-status", AuthStatusListener.class);

    void authStatusChanged(AuthSession session, @Nullable String message);
}
