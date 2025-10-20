package cn.lacknb.blog.advanced.window;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * 简单的工具窗口，用于显示AI助手的输出信息。
 */
public class AIDevToolWindowFactory implements ToolWindowFactory {

    private static JTextArea OUTPUT_AREA;

    public static void append(Project project, String text) {
        if (OUTPUT_AREA != null) {
            OUTPUT_AREA.append(text);
            if (!text.endsWith("\n")) OUTPUT_AREA.append("\n");
        }
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JPanel mainPanel = new JPanel(new BorderLayout());
        OUTPUT_AREA = new JTextArea();
        OUTPUT_AREA.setEditable(false);
        JScrollPane scroll = new JScrollPane(OUTPUT_AREA);
        mainPanel.add(scroll, BorderLayout.CENTER);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(mainPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
