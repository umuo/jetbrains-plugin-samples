package cn.lacknb.blog.sample01;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TestAgentToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        TestAgentToolWindow testAgentToolWindow = new TestAgentToolWindow(project);
        Content content = ContentFactory.getInstance().createContent(testAgentToolWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}

class TestAgentToolWindow {
    private final Project project;
    private final JPanel mainPanel;
    private final JPanel messagesPanel;
    private final JBScrollPane scrollPane;
    private final JTextArea inputArea;
    private final JButton sendButton;

    public TestAgentToolWindow(Project project) {
        this.project = project;
        this.mainPanel = new JPanel(new BorderLayout());
        this.messagesPanel = new ScrollablePanel();
        this.inputArea = new JTextArea(3, 50);
        this.sendButton = new JButton("发送");

        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(UIUtil.getPanelBackground());
        messagesPanel.setBorder(JBUI.Borders.empty()); // Add padding around the message list

        scrollPane = new JBScrollPane(messagesPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(JBUI.Borders.empty());

        setupInputPanel();
        setupInitialMessages();

        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void setupInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(JBUI.Borders.empty(5));
        inputPanel.setBackground(UIUtil.getTextFieldBackground());

        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setFont(UIUtil.getLabelFont());
        inputArea.setBorder(JBUI.Borders.empty(5));
        inputArea.setBackground(UIUtil.getTextFieldBackground());
        inputArea.setForeground(UIUtil.getTextFieldForeground());

        JBScrollPane inputScrollPane = new JBScrollPane(inputArea);
        inputScrollPane.setBorder(BorderFactory.createLineBorder(JBColor.border(), 1));
        inputScrollPane.setPreferredSize(new Dimension(0, 70));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(UIUtil.getPanelBackground());
        sendButton.addActionListener(e -> sendMessage());
        buttonPanel.add(sendButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(UIUtil.getPanelBackground());
        bottomPanel.add(inputScrollPane, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    private void setupInitialMessages() {
        addUserMessage("你是谁?科技部寄快递砂石款打开时大卡司绝对不是不打卡花洒副科级阿是繁华落尽卡死发哈卡随机发货卡机顺丰航空");

        addAssistantMessage(
                "TestAgent执行第1轮",
                "用户向我打招呼，我需要礼貌地了解介绍自己。",
                "TestAgent有一个问题：",
                "我是您的智能助手，专注于软件开发和工程任务。我可以帮助您编写代码、分析问题、优化性能以及提供最佳实践建议。请问有什么我可以帮您的吗？"
        );

        addUserMessage("say hi");

        addAssistantMessage(
                "TestAgent执行第2轮",
                "用户想要我打招呼，我将友好回应。",
                "✓ 任务完成",
                "您好！有什么我可以帮您的吗？"
        );
    }

    private void addUserMessage(String message) {
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout());
        messagePanel.setBackground(UIUtil.getPanelBackground());
        // Add border to distinguish messages

        messagePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.border()),
                JBUI.Borders.emptyTop(10)
        ));
        messagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // User icon and name
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        headerPanel.setBackground(UIUtil.getPanelBackground());

        JLabel iconLabel = new JLabel(createUserIcon());
        JBLabel nameLabel = new JBLabel("用户");
        nameLabel.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD));

        headerPanel.add(iconLabel);
        headerPanel.add(nameLabel);

        // Message content

        JTextArea messageText = new JTextArea(message);
        messageText.setEditable(false);
        messageText.setLineWrap(true);
        messageText.setWrapStyleWord(true);
        messageText.setBackground(UIUtil.getPanelBackground());
        messageText.setForeground(UIUtil.getLabelForeground());
        messageText.setFont(UIUtil.getLabelFont());
        messageText.setBorder(JBUI.Borders.empty(10, 15, 15, 15));
        messageText.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        // Removed setSize and setPreferredSize to allow dynamic resizing

        messagePanel.add(headerPanel, BorderLayout.NORTH);
        messagePanel.add(messageText, BorderLayout.CENTER);

        messagesPanel.add(messagePanel);
        // messagesPanel.add(Box.createRigidArea(new Dimension(0, 3))); // Increase spacing between messages
        messagesPanel.revalidate();
        messagesPanel.repaint();

        SwingUtilities.invokeLater(() -> scrollToBottom());
    }

    private void addAssistantMessage(String round, String thinking, String status, String response) {
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout());
        messagePanel.setBackground(UIUtil.getPanelBackground());
        // Add border to distinguish messages
        messagePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.border()),
                JBUI.Borders.empty(10)
        ));
        messagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Assistant icon and round info
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        headerPanel.setBackground(UIUtil.getPanelBackground());

        JLabel iconLabel = new JLabel(createAssistantIcon());
        JBLabel roundLabel = new JBLabel(round);
        roundLabel.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD));

        headerPanel.add(iconLabel);
        headerPanel.add(roundLabel);

        // Content panel

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(UIUtil.getPanelBackground());
        contentPanel.setBorder(JBUI.Borders.empty());
        contentPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Thinking process
        JTextArea thinkingText = new JTextArea(thinking);
        thinkingText.setEditable(false);
        thinkingText.setLineWrap(true);
        thinkingText.setWrapStyleWord(true);
        thinkingText.setBackground(UIUtil.getPanelBackground());
        thinkingText.setFont(UIUtil.getLabelFont());
        thinkingText.setForeground(JBColor.GRAY);
        thinkingText.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        thinkingText.setBorder(JBUI.Borders.empty(10, 15, 15, 15));
        contentPanel.add(thinkingText);
        // contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Status
        // JBLabel statusLabel = new JBLabel(status);
        // statusLabel.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD));
        // if (status.contains("✓")) {
        //     statusLabel.setForeground(new JBColor(new Color(0x4CAF50), new Color(0x50A14F)));
        // }
        // contentPanel.add(statusLabel);
        // contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Response
        JTextArea responseText = new JTextArea(response);
        responseText.setEditable(false);
        responseText.setLineWrap(true);
        responseText.setWrapStyleWord(true);
        responseText.setBackground(UIUtil.getPanelBackground());
        responseText.setForeground(UIUtil.getLabelForeground());
        responseText.setFont(UIUtil.getLabelFont());
        responseText.setBorder(JBUI.Borders.empty(10, 15, 15, 15));

        responseText.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        contentPanel.add(responseText);

        messagePanel.add(headerPanel, BorderLayout.NORTH);
        messagePanel.add(contentPanel, BorderLayout.CENTER);

        messagesPanel.add(messagePanel);
        // messagesPanel.add(Box.createRigidArea(new Dimension(0, 3))); // Increase spacing between messages
        messagesPanel.revalidate();
        messagesPanel.repaint();

        SwingUtilities.invokeLater(() -> scrollToBottom());
    }

    private void sendMessage() {
        String message = inputArea.getText().trim();
        if (!message.isEmpty()) {
            addUserMessage(message);
            inputArea.setText("");

            // Simulate assistant response
            SwingUtilities.invokeLater(() -> {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                addAssistantMessage(
                        "TestAgent执行第" + ((messagesPanel.getComponentCount() / 2) + 1) + "轮",
                        "正在处理用户的请求...",
                        "✓ 任务完成",
                        "我已收到您的消息：\"" + message + "\""
                );
            });
        }
    }

    private void scrollToBottom() {
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }

    private Icon createUserIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new JBColor(new Color(0x2196F3), new Color(0x3F51B5)));
                g2d.fillOval(x, y, 24, 24);
                g2d.setColor(JBColor.WHITE);
                g2d.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD));
                g2d.drawString("U", x + 7, y + 17);
                g2d.dispose();
            }

            @Override
            public int getIconWidth() {
                return 24;
            }

            @Override
            public int getIconHeight() {
                return 24;
            }
        };
    }

    private Icon createAssistantIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new JBColor(new Color(0x9C27B0), new Color(0xAB47BC)));
                g2d.fillOval(x, y, 24, 24);
                g2d.setColor(JBColor.WHITE);
                g2d.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD));
                g2d.drawString("A", x + 7, y + 17);
                g2d.dispose();
            }

            @Override
            public int getIconWidth() {
                return 24;
            }

            @Override
            public int getIconHeight() {
                return 24;
            }
        };
    }

    public JComponent getContent() {
        return mainPanel;
    }

    private static class ScrollablePanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 10;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 10;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}