package cn.lacknb.blog.llm.stream;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class LLMChatToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        LLMChatToolWindow window = new LLMChatToolWindow(project);
        Content content = ContentFactory.getInstance().createContent(window.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
