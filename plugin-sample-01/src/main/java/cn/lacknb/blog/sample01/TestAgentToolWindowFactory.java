/**
 * TestAgentToolWindowFactory 类
 * 这是一个工具窗口工厂类，用于创建和初始化 TestAgent 工具窗口
 * 该工具窗口提供了一个与AI助手交互的界面
 */
package cn.lacknb.blog.sample01;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
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
    /**
     * 创建工具窗口内容
     * 此方法在工具窗口首次创建时被调用，负责初始化窗口内容
     *
     * @param project   当前项目实例
     * @param toolWindow 工具窗口实例
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 创建 TestAgentToolWindow 实例
        TestAgentToolWindow testAgentToolWindow = new TestAgentToolWindow(project);
        // 创建内容实例并将其添加到工具窗口管理器
        Content content = ContentFactory.getInstance().createContent(testAgentToolWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }

    /**
     * 静态辅助方法：唤醒 ToolWindow 并填充内容
     * 此方法允许外部代码激活工具窗口并将指定内容填充到输入框中
     * 
     * @param project 当前项目
     * @param content 要填充到输入框的文本
     */
    public static void activateAndFill(@NotNull Project project, String content) {
        // 通过ID获取工具窗口实例，ID必须与 plugin.xml 中定义的ID一致
        // TODO: 请确保这里的 "TestAgent" 与你 plugin.xml 中 <toolWindow id="..."> 的 id 一致
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("TestAgent");
        if (toolWindow != null) {
            // 激活工具窗口并执行填充操作
            toolWindow.activate(() -> {
                // 获取窗口内容
                Content windowContent = toolWindow.getContentManager().getContent(0);
                if (windowContent != null && windowContent.getComponent() instanceof JComponent) {
                    // 获取主面板组件
                    JComponent mainPanel = (JComponent) windowContent.getComponent();
                    // 从面板的客户端属性中获取 TestAgentToolWindow 实例
                    Object window = mainPanel.getClientProperty(TestAgentToolWindow.WINDOW_KEY);
                    if (window instanceof TestAgentToolWindow) {
                        // 调用 TestAgentToolWindow 的 setInputText 方法填充内容
                        ((TestAgentToolWindow) window).setInputText(content);
                    }
                }
            });
        }
    }
}

/**
 * TestAgentToolWindow 类
 * 这是工具窗口的主界面类，提供了一个聊天界面，用户可以与AI助手进行交互
 * 界面包含消息显示区域、输入区域和发送按钮
 */
class TestAgentToolWindow {
    // 用于在组件中存储当前窗口实例的键名
    static final String WINDOW_KEY = "TestAgentToolWindowInstance";
    // 当前项目实例
    private final Project project;
    // 主面板，使用 BorderLayout 布局
    private final JPanel mainPanel;
    // 消息显示面板，用于显示对话历史
    private final JPanel messagesPanel;
    // 消息面板的滚动条
    private final JBScrollPane scrollPane;
    // 用户输入文本的区域
    private final JTextArea inputArea;
    // 发送消息的按钮
    private final JButton sendButton;

    // 文本样式属性
    private SimpleAttributeSet regularStyle;

    /**
     * 构造函数，初始化 TestAgentToolWindow
     * 
     * @param project 当前项目实例
     */
    public TestAgentToolWindow(Project project) {
        this.project = project;
        this.mainPanel = new JPanel(new BorderLayout());
        // 将当前实例绑定到 Panel 上，方便后续获取
        this.mainPanel.putClientProperty(WINDOW_KEY, this);
        this.messagesPanel = new ScrollablePanel();
        this.inputArea = new JTextArea(3, 50);
        this.sendButton = new JButton("发送");

        // 初始化文本样式
        initStyles();

        // 添加工具栏
        mainPanel.add(createToolbar(), BorderLayout.NORTH);

        // 设置消息面板布局和样式
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(UIUtil.getPanelBackground());
        messagesPanel.setBorder(JBUI.Borders.empty()); // Add padding around the message list

        // 创建滚动面板
        scrollPane = new JBScrollPane(messagesPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(JBUI.Borders.empty());

        // 设置输入面板
        setupInputPanel();
        // 设置初始消息
        setupInitialMessages();

        // 将滚动面板添加到主面板的中央位置
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * 创建工具栏
     */
    private JComponent createToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();
        
        // 添加 "New Chat" 按钮
        group.add(new AnAction("New Chat", "Start a new chat session", AllIcons.General.Add) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // 清空消息面板
                messagesPanel.removeAll();
                messagesPanel.revalidate();
                messagesPanel.repaint();
                
                // 清空输入框
                inputArea.setText("");
                
                // 重新初始化欢迎消息
                setupInitialMessages();
            }
        });

        group.add(new AnAction("Clear", "Clear chat history", AllIcons.Actions.GC) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                messagesPanel.removeAll();
                messagesPanel.revalidate();
                messagesPanel.repaint();
            }
        });
        
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("TestAgentToolbar", group, true);
        toolbar.setTargetComponent(mainPanel);
        return toolbar.getComponent();
    }

    /**
     * 初始化文本样式
     * 设置默认文本的字体、颜色和大小
     */
    private void initStyles() {
        regularStyle = new SimpleAttributeSet();
        StyleConstants.setFontFamily(regularStyle, UIUtil.getLabelFont().getFamily());
        StyleConstants.setForeground(regularStyle, UIUtil.getLabelForeground());
        StyleConstants.setFontSize(regularStyle, UIUtil.getLabelFont().getSize());
    }

    /**
     * 设置输入面板
     * 创建并配置用户输入区域和发送按钮
     */
    private void setupInputPanel() {
        // 创建输入面板，使用 BorderLayout 布局
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(JBUI.Borders.empty(5));
        inputPanel.setBackground(UIUtil.getTextFieldBackground());

        // 配置输入区域的属性
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setFont(UIUtil.getLabelFont());
        inputArea.setBorder(JBUI.Borders.empty(5));
        inputArea.setBackground(UIUtil.getTextFieldBackground());
        inputArea.setForeground(UIUtil.getTextFieldForeground());

        // 创建输入区域的滚动面板
        JBScrollPane inputScrollPane = new JBScrollPane(inputArea);
        // 使用复合边框
        inputScrollPane.setBorder(BorderFactory.createCompoundBorder(
                // 外边距
                BorderFactory.createEmptyBorder(2, 2, 2,2),
                // 边框
                BorderFactory.createLineBorder(JBColor.border(), 1)
        ));
        inputScrollPane.setPreferredSize(new Dimension(0, 70));

        // 创建按钮面板，放置发送按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(UIUtil.getPanelBackground());
        // 为发送按钮添加点击事件监听器
        sendButton.addActionListener(e -> sendMessage());
        buttonPanel.add(sendButton);

        // 创建底部面板，包含输入区域和按钮
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(UIUtil.getPanelBackground());
        bottomPanel.add(inputScrollPane, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 将底部面板添加到主面板的南部位置
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * 设置输入区域的文本
     * 该方法用于从外部设置输入框的文本内容
     * 
     * @param text 要设置的文本内容
     */
    void setInputText(String text) {
        inputArea.setText(text);
    }

    /**
     * 设置初始消息
     * 在窗口初始化时添加一些示例对话，展示界面效果
     */
    private void setupInitialMessages() {
        // 添加一个用户消息作为示例
        addUserMessage("你是谁?科技部寄快递砂石款打开时大卡司绝对不是不打卡花洒副科级阿是繁华落尽卡死发哈卡随机发货卡机顺丰航空");

        // 添加一个AI助手的回复作为示例
        addAssistantMessage(
                "TestAgent执行第1轮",
                "用户向我打招呼，我需要礼貌地了解介绍自己。",
                "TestAgent有一个问题：",
                "我是您的智能助手，专注于软件开发和工程任务。我可以帮助您编写代码、分析问题、优化性能以及提供最佳实践建议。请问有什么我可以帮您的吗？",
                false
        );

        // 添加另一个用户消息
        addUserMessage("say hi");

        // 添加AI助手的回复
        addAssistantMessage(
                "TestAgent执行第2轮",
                "用户想要我打招呼，我将友好回应。",
                "✓ 任务完成",
                "您好！有什么我可以帮您的吗？",
                false
        );
    }

    /**
     * 添加用户消息到对话界面
     * 创建一个包含用户头像、用户名和消息内容的面板，并将其添加到消息面板中
     * 
     * @param message 用户发送的消息文本
     */
    private void addUserMessage(String message) {
        // 创建消息面板，使用 BorderLayout 布局
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout());
        messagePanel.setBackground(UIUtil.getPanelBackground());
        // Add border to distinguish messages

        // 设置面板边框和间距
        messagePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.border()),
                JBUI.Borders.emptyTop(10)
        ));
        messagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // 创建头部面板，包含用户图标和用户名
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        headerPanel.setBackground(UIUtil.getPanelBackground());

        // 创建用户图标和标签
        JLabel iconLabel = new JLabel(createUserIcon());
        JBLabel nameLabel = new JBLabel("用户");
        nameLabel.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD));

        headerPanel.add(iconLabel);
        headerPanel.add(nameLabel);

        // 消息内容文本区域
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

        // 将头部和消息内容添加到消息面板
        messagePanel.add(headerPanel, BorderLayout.NORTH);
        messagePanel.add(messageText, BorderLayout.CENTER);

        // 将消息面板添加到消息区域
        messagesPanel.add(messagePanel);
        // messagesPanel.add(Box.createRigidArea(new Dimension(0, 3))); // Increase spacing between messages
        messagesPanel.revalidate();
        messagesPanel.repaint();

        // 滚动到底部以显示最新消息
        SwingUtilities.invokeLater(this::scrollToBottom);
    }

    /**
     * 添加AI助手消息到对话界面
     * 创建一个包含助手头像、轮次信息、思考过程和响应内容的面板
     * 支持流式显示和普通显示两种模式
     * 
     * @param round 轮次信息，表示对话的第几轮
     * @param thinking 思考过程，显示AI的推理过程
     * @param status 状态信息，表示任务完成情况
     * @param response 响应内容，AI的最终回复
     * @param stream 是否以流式方式显示内容
     */
    private void addAssistantMessage(String round, String thinking, String status, String response, boolean stream) {
        // 创建消息面板，使用 BorderLayout 布局
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout());
        messagePanel.setBackground(UIUtil.getPanelBackground());
        // Add border to distinguish messages
        messagePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.border()),
                JBUI.Borders.emptyTop(10)
        ));
        messagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // 创建头部面板，包含助手图标和轮次信息
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        headerPanel.setBackground(UIUtil.getPanelBackground());

        // 创建助手图标和轮次标签
        JLabel iconLabel = new JLabel(createAssistantIcon());
        JBLabel roundLabel = new JBLabel(round);
        roundLabel.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD));

        headerPanel.add(iconLabel);
        headerPanel.add(roundLabel);

        // 创建内容面板，用于显示思考过程和响应内容
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(UIUtil.getPanelBackground());
        contentPanel.setBorder(JBUI.Borders.empty());
        
        // 创建思考过程文本区域
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

        // 解析响应内容中的Markdown格式
        List<MarkdownSegment> segments = parseMarkdown(response);
        List<StreamingBlock> blocks = new ArrayList<>();

        // 预创建块，但不立即添加到UI（如果是流式显示）
        for (MarkdownSegment seg : segments) {
            if (seg.isCode) {
                // 为代码创建 EditorTextField
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
                // 为普通文本创建 JTextPane
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

        // 将头部和内容面板添加到消息面板
        messagePanel.add(headerPanel, BorderLayout.NORTH);
        messagePanel.add(contentPanel, BorderLayout.CENTER);

        // 将消息面板添加到消息区域
        messagesPanel.add(messagePanel);
        messagesPanel.revalidate();
        messagesPanel.repaint();

        // 滚动到底部以显示最新消息
        SwingUtilities.invokeLater(this::scrollToBottom);

        // 根据是否流式显示来处理内容
        if (stream) {
            streamBlocks(contentPanel, blocks);
        } else {
            for (StreamingBlock block : blocks) {
                contentPanel.add(block.getComponent());
                block.append(block.getFullText());
            }
        }
    }

    /**
     * 以流式方式显示内容块
     * 逐字符显示内容，模拟打字机效果
     * 
     * @param contentPanel 内容面板，用于显示流式内容
     * @param blocks 要流式显示的块列表
     */
    private void streamBlocks(JPanel contentPanel, List<StreamingBlock> blocks) {
        // 创建定时器，每10毫秒处理一个字符
        Timer timer = new Timer(10, null); // 10ms delay per char
        final Iterator<StreamingBlock> blockIterator = blocks.iterator();
        final AtomicReference<StreamingBlock> currentBlock = new AtomicReference<>();
        final AtomicInteger charIndex = new AtomicInteger(0);

        timer.addActionListener(e -> {
            if (currentBlock.get() == null) {
                if (blockIterator.hasNext()) {
                    StreamingBlock nextBlock = blockIterator.next();
                    currentBlock.set(nextBlock);
                    // 只有在开始流式显示时才将组件添加到UI
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

    /**
     * 解析Markdown格式的文本
     * 将文本分割为普通文本和代码块段落
     * 
     * @param text 要解析的Markdown格式文本
     * @return Markdown段落列表，包含文本和代码块信息
     */
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

    /**
     * 发送消息处理方法
     * 获取输入区域的文本，添加为用户消息，并模拟AI助手的响应
     */
    private void sendMessage() {
        String message = inputArea.getText().trim();
        if (!message.isEmpty()) {
            addUserMessage(message);
            inputArea.setText("");

            // 显示加载状态
            LoadingComponent loadingComponent = new LoadingComponent("Thinking");
            JPanel loadingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            loadingPanel.setBackground(UIUtil.getPanelBackground());
            loadingPanel.setBorder(JBUI.Borders.empty(10, 15, 10, 15));
            loadingPanel.add(loadingComponent);
            
            messagesPanel.add(loadingPanel);
            messagesPanel.revalidate();
            messagesPanel.repaint();
            scrollToBottom();

            // 模拟助手响应的延迟
            Timer delayTimer = new Timer(1500, e -> {
                messagesPanel.remove(loadingPanel);
                messagesPanel.revalidate();
                messagesPanel.repaint();

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
            delayTimer.setRepeats(false);
            delayTimer.start();
        }
    }

    /**
     * 滚动到消息面板的底部
     * 用于确保最新消息始终可见
     */
    private void scrollToBottom() {
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }

    /**
     * 创建用户图标
     * 返回一个圆形的用户图标，显示字母"U"
     * 
     * @return 用户图标实例
     */
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

    /**
     * 创建助手图标
     * 返回一个圆形的助手图标，显示字母"A"
     * 
     * @return 助手图标实例
     */
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

    /**
     * 获取主面板组件
     * 返回工具窗口的主面板，用于添加到IDE的工具窗口中
     * 
     * @return 主面板组件
     */
    public JComponent getContent() {
        return mainPanel;
    }

    /**
     * 可滚动面板类
     * 实现 Scrollable 接口，提供可滚动的面板功能
     */
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

    /**
     * Markdown段落类
     * 用于表示解析后的Markdown文本段落，区分普通文本和代码块
     */
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

    /**
     * 流式块接口
     * 定义流式显示内容块的基本操作
     */
    interface StreamingBlock {
        void append(String text);
        String getFullText();
        JComponent getComponent();
    }

    /**
     * 文本流式块类
     * 实现流式显示普通文本块的功能
     */
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

    /**
     * 代码流式块类
     * 实现流式显示代码块的功能
     */
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

/**
 * 加载组件类
 * 显示带有动画省略号的加载提示
 */
class LoadingComponent extends JLabel {
    private final Timer timer;
    private int dotCount = 0;
    private final String baseText;

    public LoadingComponent(String text) {
        this.baseText = text;
        this.setFont(UIUtil.getLabelFont());
        this.setForeground(UIUtil.getContextHelpForeground()); // 使用灰色字体
        this.setText(baseText);

        // 每 400 毫秒更新一次
        this.timer = new Timer(400, e -> {
            dotCount = (dotCount + 1) % 4; // 0, 1, 2, 3 循环
            StringBuilder sb = new StringBuilder(baseText);
            for (int i = 0; i < dotCount; i++) {
                sb.append(".");
            }
            setText(sb.toString());
        });
    }

    // 组件显示时开始动画
    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    // 组件移除时停止动画 (非常重要，防止内存泄漏)
    @Override
    public void removeNotify() {
        super.removeNotify();
        timer.stop();
    }
}