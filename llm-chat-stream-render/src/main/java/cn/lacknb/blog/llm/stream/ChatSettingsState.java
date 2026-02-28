package cn.lacknb.blog.llm.stream;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "cn.lacknb.blog.llm.stream.ChatSettingsState",
        storages = @Storage("llm-chat-stream-render.xml")
)
public class ChatSettingsState implements PersistentStateComponent<ChatSettingsState.State> {

    /**
     * 仅用于持久化的状态对象。
     * 后续新增配置项优先放在这里，运行期字段放在外部类。
     */
    public static class State {
        public boolean autoFixUnitTestEnabled = false;
    }

    private State state = new State();

    /**
     * 运行期临时状态示例（不持久化）。
     */
    private transient boolean settingsPanelInitialized;

    public static ChatSettingsState getInstance(@NotNull Project project) {
        return project.getService(ChatSettingsState.class);
    }

    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public boolean isAutoFixUnitTestEnabled() {
        return state.autoFixUnitTestEnabled;
    }

    public void setAutoFixUnitTestEnabled(boolean enabled) {
        state.autoFixUnitTestEnabled = enabled;
    }

    public boolean isSettingsPanelInitialized() {
        return settingsPanelInitialized;
    }

    public void setSettingsPanelInitialized(boolean settingsPanelInitialized) {
        this.settingsPanelInitialized = settingsPanelInitialized;
    }
}
