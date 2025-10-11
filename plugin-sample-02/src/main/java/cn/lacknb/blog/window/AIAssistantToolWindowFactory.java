package cn.lacknb.blog.window;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author gitsilence
 */
public class AIAssistantToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 创建对话区域
        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        
        // 创建输入区域
        JPanel inputPanel = new JPanel(new BorderLayout());
        JTextArea inputArea = new JTextArea(3, 40);
        JScrollPane inputScrollPane = new JScrollPane(inputArea);
        JButton sendButton = new JButton("发送");
        sendButton.setToolTipText("<html><b>标题</b><br><font color='gray'>这是灰色的说明文字</font><br>多行文字1<br>多行文字2<br>多行文字3</html>");
        // 设置输入面板布局
        inputPanel.add(inputScrollPane, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        // 添加组件到主面板
        mainPanel.add(chatScrollPane, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        
        // 添加发送按钮事件监听
        sendButton.addActionListener(e -> {
            String userInput = inputArea.getText().trim();
            if (!userInput.isEmpty()) {
                // 添加用户输入到聊天区域
                chatArea.append("用户: " + userInput + "\n");
                // TODO: 调用AI服务处理用户输入
                inputArea.setText("");
            }
        });
        
        // 创建内容并添加到工具窗口
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(mainPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}