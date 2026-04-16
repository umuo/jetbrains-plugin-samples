package cn.lacknb.blog.llm.stream;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import org.jetbrains.annotations.NotNull;

public class MyStatusBarWidgetFactory implements StatusBarWidgetFactory {
    @Override
    public @NotNull String getId() {
        return "llm.chat.auth.status";
    }

    @Override
    public @NotNull String getDisplayName() {
        return "LLM Chat 登录状态";
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return true;
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new MyStatusBarWidget(project);
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        widget.dispose();
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        return true;
    }
}
