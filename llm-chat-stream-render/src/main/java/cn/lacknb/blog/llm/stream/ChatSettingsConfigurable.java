package cn.lacknb.blog.llm.stream;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

public class ChatSettingsConfigurable implements SearchableConfigurable {

    private final Project project;
    private ChatSettingsPanel settingsPanel;

    public ChatSettingsConfigurable(Project project) {
        this.project = project;
    }

    @Override
    public @NotNull String getId() {
        return "cn.lacknb.blog.llm.stream.ChatSettings";
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "ChatSettings";
    }

    @Override
    public @Nullable JComponent createComponent() {
        settingsPanel = new ChatSettingsPanel();
        ChatSettingsState.getInstance(project).setSettingsPanelInitialized(true);
        reset();
        return settingsPanel.getComponent();
    }

    @Override
    public boolean isModified() {
        ChatSettingsState state = ChatSettingsState.getInstance(project);
        return settingsPanel != null
                && settingsPanel.isAutoFixUnitTestEnabled() != state.isAutoFixUnitTestEnabled();
    }

    @Override
    public void apply() {
        ChatSettingsState state = ChatSettingsState.getInstance(project);
        if (settingsPanel != null) {
            state.setAutoFixUnitTestEnabled(settingsPanel.isAutoFixUnitTestEnabled());
        }
    }

    @Override
    public void reset() {
        ChatSettingsState state = ChatSettingsState.getInstance(project);
        if (settingsPanel != null) {
            settingsPanel.setAutoFixUnitTestEnabled(state.isAutoFixUnitTestEnabled());
        }
    }
}
