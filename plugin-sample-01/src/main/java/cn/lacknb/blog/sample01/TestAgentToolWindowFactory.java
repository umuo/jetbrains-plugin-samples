package cn.lacknb.blog.sample01;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class TestAgentToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        TestAgentToolWindow testAgentToolWindow = new TestAgentToolWindow(project);
        Content content = ContentFactory.getInstance().createContent(testAgentToolWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }

    /**
     * 静态辅助方法：唤醒 ToolWindow 并填充内容
     * @param project 当前项目
     * @param content 要填充到输入框的文本
     */
    public static void activateAndFill(@NotNull Project project, String content) {
        // TODO: 请确保这里的 "TestAgent" 与你 plugin.xml 中 <toolWindow id="..."> 的 id 一致
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("TestAgent");
        if (toolWindow != null) {
            toolWindow.activate(() -> {
                Content windowContent = toolWindow.getContentManager().getContent(0);
                if (windowContent != null && windowContent.getComponent() instanceof JComponent) {
                    JComponent mainPanel = (JComponent) windowContent.getComponent();
                    Object window = mainPanel.getClientProperty(TestAgentToolWindow.WINDOW_KEY);
                    if (window instanceof TestAgentToolWindow) {
                        ((TestAgentToolWindow) window).setInputText(content);
                    }
                }
            });
        }
    }
}

class TestAgentToolWindow {
    static final String WINDOW_KEY = "TestAgentToolWindowInstance";
    private final Project project;
    private final JPanel mainPanel;
    private final JPanel messagesPanel;
    private final JBScrollPane scrollPane;
    private final JTextArea inputArea;
    private final JButton sendButton;

    private SimpleAttributeSet regularStyle;

    public TestAgentToolWindow(Project project) {
        this.project = project;
        this.mainPanel = new JPanel(new BorderLayout());
        // 将当前实例绑定到 Panel 上，方便后续获取
        this.mainPanel.putClientProperty(WINDOW_KEY, this);
        this.messagesPanel = new ScrollablePanel();
        this.inputArea = new JTextArea(3, 50);
        this.sendButton = new JButton("发送");

        initStyles();

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

    private void initStyles() {
        regularStyle = new SimpleAttributeSet();
        StyleConstants.setFontFamily(regularStyle, UIUtil.getLabelFont().getFamily());
        StyleConstants.setForeground(regularStyle, UIUtil.getLabelForeground());
        StyleConstants.setFontSize(regularStyle, UIUtil.getLabelFont().getSize());
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
        // 使用复合边框
        inputScrollPane.setBorder(BorderFactory.createCompoundBorder(
                // 外边距
                BorderFactory.createEmptyBorder(2, 2, 2,2),
                // 边框
                BorderFactory.createLineBorder(JBColor.border(), 1)
        ));
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

    void setInputText(String text) {
        inputArea.setText(text);
    }

    private void setupInitialMessages() {
        addUserMessage("你是谁?科技部寄快递砂石款打开时大卡司绝对不是不打卡花洒副科级阿是繁华落尽卡死发哈卡随机发货卡机顺丰航空");

        addAssistantMessage(
                "TestAgent执行第1轮",
                "用户向我打招呼，我需要礼貌地了解介绍自己。",
                "TestAgent有一个问题：",
                "我是您的智能助手，专注于软件开发和工程任务。我可以帮助您编写代码、分析问题、优化性能以及提供最佳实践建议。请问有什么我可以帮您的吗？",
                false
        );

        addUserMessage("say hi");

        addAssistantMessage(
                "TestAgent执行第2轮",
                "用户想要我打招呼，我将友好回应。",
                "✓ 任务完成",
                "您好！有什么我可以帮您的吗？",
                false
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

        SwingUtilities.invokeLater(this::scrollToBottom);
    }

    private void addAssistantMessage(String round, String thinking, String status, String response, boolean stream) {
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout());
        messagePanel.setBackground(UIUtil.getPanelBackground());
        // Add border to distinguish messages
        messagePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.border()),
                JBUI.Borders.emptyTop(10)
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
        
        // Thinking process
        JTextArea thinkingText = new JTextArea(thinking);
        thinkingText.setEditable(false);
        thinkingText.setLineWrap(true);
        thinkingText.setWrapStyleWord(true);
        thinkingText.setBackground(UIUtil.getPanelBackground());
        thinkingText.setFont(UIUtil.getLabelFont());
        thinkingText.setForeground(JBColor.GRAY);
        thinkingText.setBorder(JBUI.Borders.empty(10, 15, 5, 15));
        thinkingText.setAlignmentX(Component.LEFT_ALIGNMENT);
        thinkingText.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        contentPanel.add(thinkingText);

        // Response
        List<MarkdownSegment> segments = parseMarkdown(response);
        List<StreamingBlock> blocks = new ArrayList<>();

        // Pre-create blocks but don't add them to UI yet if streaming
        for (MarkdownSegment seg : segments) {
            if (seg.isCode) {
                // Create EditorTextField for code
                FileType fileType = PlainTextFileType.INSTANCE;
                if (seg.language != null) {
                    FileType foundType = FileTypeManager.getInstance().getFileTypeByExtension(seg.language);
                    if (foundType != null) {
                        fileType = foundType;
                    }
                }

                EditorTextField editorTextField = new EditorTextField("", project, fileType);
                editorTextField.setOneLineMode(false);
                editorTextField.setViewer(true);
                editorTextField.ensureWillComputePreferredSize();
                editorTextField.addSettingsProvider(editor -> {
                    editor.getSettings().setUseSoftWraps(true);
                    editor.getSettings().setLineNumbersShown(true);
                    editor.getSettings().setGutterIconsShown(false);
                    editor.setBackgroundColor(null); // Use default scheme background
                    editor.getScrollPane().setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                    editor.getScrollPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                });
                editorTextField.setBorder(JBUI.Borders.empty(5, 15, 5, 15));
                editorTextField.setAlignmentX(Component.LEFT_ALIGNMENT);
                
                blocks.add(new CodeStreamingBlock(editorTextField, seg.text, project));
            } else {
                // Create JTextPane for regular text
                JTextPane textPane = new JTextPane();
                textPane.setEditable(false);
                textPane.setBackground(UIUtil.getPanelBackground());
                textPane.setBorder(JBUI.Borders.empty(5, 15, 5, 15));
                textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
                textPane.setAlignmentX(Component.LEFT_ALIGNMENT);
                textPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
                
                blocks.add(new TextStreamingBlock(textPane, seg.text, regularStyle));
            }
        }

        messagePanel.add(headerPanel, BorderLayout.NORTH);
        messagePanel.add(contentPanel, BorderLayout.CENTER);

        messagesPanel.add(messagePanel);
        messagesPanel.revalidate();
        messagesPanel.repaint();

        SwingUtilities.invokeLater(this::scrollToBottom);

        if (stream) {
            streamBlocks(contentPanel, blocks);
        } else {
            for (StreamingBlock block : blocks) {
                contentPanel.add(block.getComponent());
                block.append(block.getFullText());
            }
        }
    }

    private void streamBlocks(JPanel contentPanel, List<StreamingBlock> blocks) {
        Timer timer = new Timer(10, null); // 10ms delay per char
        final Iterator<StreamingBlock> blockIterator = blocks.iterator();
        final AtomicReference<StreamingBlock> currentBlock = new AtomicReference<>();
        final AtomicInteger charIndex = new AtomicInteger(0);

        timer.addActionListener(e -> {
            if (currentBlock.get() == null) {
                if (blockIterator.hasNext()) {
                    StreamingBlock nextBlock = blockIterator.next();
                    currentBlock.set(nextBlock);
                    // Add component to UI only when we start streaming it
                    contentPanel.add(nextBlock.getComponent());
                    contentPanel.revalidate();
                    contentPanel.repaint();
                    charIndex.set(0);
                } else {
                    timer.stop();
                    return;
                }
            }

            StreamingBlock block = currentBlock.get();
            String fullText = block.getFullText();

            if (charIndex.get() < fullText.length()) {
                block.append(String.valueOf(fullText.charAt(charIndex.get())));
                charIndex.incrementAndGet();
                scrollToBottom();
            } else {
                currentBlock.set(null);
            }
        });
        timer.start();
    }

    private List<MarkdownSegment> parseMarkdown(String text) {
        List<MarkdownSegment> segments = new ArrayList<>();
        String[] parts = text.split("```");
        for (int i = 0; i < parts.length; i++) {
            boolean isCode = (i % 2 != 0);
            String content = parts[i];
            String language = null;

            if (isCode) {
                int firstNewline = content.indexOf('\n');
                if (firstNewline != -1) {
                    String firstLine = content.substring(0, firstNewline).trim();
                    if (!firstLine.isEmpty() && !firstLine.contains(" ") && firstLine.length() < 20) {
                        language = firstLine;
                        content = content.substring(firstNewline + 1);
                    }
                }
            }

            if (!content.isEmpty()) {
                segments.add(new MarkdownSegment(content, isCode, language));
            }
        }
        return segments;
    }

    private void sendMessage() {
        String message = inputArea.getText().trim();
        if (!message.isEmpty()) {
            addUserMessage(message);
            inputArea.setText("");

            // Simulate assistant response
            SwingUtilities.invokeLater(() -> {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                String longResponse = "我已收到您的消息：\"" + message + "\"\n\n" +
                        "这是一个模拟的长文本回复，用于测试流式输出和代码块的显示效果。\n\n" +
                        "```java\n" +
                        "/**\n" +
                        " * 这是一个示例类\n" +
                        " */\n" +
                        "public class StreamTest {\n" +
                        "    public static void main(String[] args) {\n" +
                        "        System.out.println(\"Hello, Streaming World!\");\n" +
                        "        // 模拟复杂的逻辑处理\n" +
                        "        processData();\n" +
                        "    }\n" +
                        "\n" +
                        "    private static void processData() {\n" +
                        "        // ... implementation ...\n" +
                        "    }\n" +
                        "}\n" +
                        "```\n\n" +
                        "请注意，上述代码仅为演示目的。在实际应用中，您可能需要根据具体需求进行调整。";

                addAssistantMessage(
                        "TestAgent执行第" + ((messagesPanel.getComponentCount() / 2) + 1) + "轮",
                        "正在思考如何回复用户...",
                        "✓ 任务完成",
                        longResponse,
                        true
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

    private static class MarkdownSegment {
        String text;
        boolean isCode;
        String language;

        MarkdownSegment(String text, boolean isCode, String language) {
            this.text = text;
            this.isCode = isCode;
            this.language = language;
        }
    }

    interface StreamingBlock {
        void append(String text);
        String getFullText();
        JComponent getComponent();
    }

    private static class TextStreamingBlock implements StreamingBlock {
        private final JTextPane textPane;
        private final String fullText;
        private final SimpleAttributeSet style;

        TextStreamingBlock(JTextPane textPane, String fullText, SimpleAttributeSet style) {
            this.textPane = textPane;
            this.fullText = fullText;
            this.style = style;
        }

        @Override
        public void append(String text) {
            try {
                StyledDocument doc = textPane.getStyledDocument();
                doc.insertString(doc.getLength(), text, style);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }

        @Override
        public String getFullText() {
            return fullText;
        }

        @Override
        public JComponent getComponent() {
            return textPane;
        }
    }

    private static class CodeStreamingBlock implements StreamingBlock {
        private final EditorTextField editorTextField;
        private final String fullText;
        private final Project project;

        CodeStreamingBlock(EditorTextField editorTextField, String fullText, Project project) {
            this.editorTextField = editorTextField;
            this.fullText = fullText;
            this.project = project;
        }

        @Override
        public void append(String text) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                Document doc = editorTextField.getDocument();
                doc.insertString(doc.getTextLength(), text);
                
                // Force re-layout to adjust height
                editorTextField.revalidate();
                editorTextField.repaint();
            });
        }

        @Override
        public String getFullText() {
            return fullText;
        }

        @Override
        public JComponent getComponent() {
            return editorTextField;
        }
    }
}