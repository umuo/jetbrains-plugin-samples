package cn.lacknb.blog.llm.stream;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class LLMChatToolWindow {
    public static final String TOOL_WINDOW_ID = "LLM Chat Stream";
    private static final String WINDOW_KEY = "LLMChatToolWindowInstance";

    private final Project project;
    private final JPanel mainPanel;
    private final JPanel messagesPanel;
    private final JBScrollPane scrollPane;
    private final JTextArea inputArea;
    private final JButton sendButton;
    private final JButton stopButton;
    private final JBLabel statusLabel;
    private final OpenAIChatService chatService;
    private final List<ChatMessage> history = new ArrayList<>();
    private OpenAIChatService.StreamSession currentSession;
    private long requestCounter = 0L;
    private long activeRequestId = -1L;

    private boolean streaming = false;
    private final javax.swing.Timer scrollTimer;
    private boolean autoScrollEnabled = true;
    private boolean programmaticScroll = false;

    public LLMChatToolWindow(Project project) {
        this.project = project;
        LLMConfig config = LLMConfigLoader.load(project);
        String baseUrl = config != null ? config.getBaseUrl() : System.getenv("OPENAI_BASE_URL");
        String model = config != null ? config.getModel() : System.getenv("OPENAI_MODEL");
        String apiKey = config != null ? config.getApiKey() : System.getenv("OPENAI_API_KEY");
        this.chatService = new OpenAIChatService(baseUrl, model, apiKey);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.putClientProperty(WINDOW_KEY, this);

        messagesPanel = new ScrollablePanel();
        messagesPanel.setLayout(new javax.swing.BoxLayout(messagesPanel, javax.swing.BoxLayout.Y_AXIS));
        messagesPanel.setBackground(UIUtil.getPanelBackground());
        messagesPanel.setBorder(JBUI.Borders.empty());

        scrollPane = new JBScrollPane(messagesPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(JBUI.Borders.empty());
        installScrollBehavior();

        inputArea = new JTextArea(3, 40);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setFont(UIUtil.getLabelFont());

        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());

        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopCurrentStream("Generation stopped.", true));

        statusLabel = new JBLabel("Idle");
        statusLabel.setForeground(UIUtil.getLabelInfoForeground());

        mainPanel.add(createToolbar(), BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(createInputPanel(), BorderLayout.SOUTH);

        registerSendShortcut();
        scrollTimer = new javax.swing.Timer(100, e -> flushScroll());
        scrollTimer.setRepeats(true);
        String tip = "Hello! Ask a question below. Markdown and code blocks are supported.";
        if (config == null) {
            tip += "\n\nTip: set OPENAI_API_KEY or rebuild the plugin with an embedded config.";
        }
        addAssistantInfo(tip, false);
    }

    public JComponent getContent() {
        return mainPanel;
    }

    public static LLMChatToolWindow findInstance(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow == null) {
            return null;
        }
        for (Content content : toolWindow.getContentManager().getContents()) {
            JComponent component = content.getComponent();
            Object instance = component.getClientProperty(WINDOW_KEY);
            if (instance instanceof LLMChatToolWindow) {
                return (LLMChatToolWindow) instance;
            }
        }
        return null;
    }

    public static void showAndSubmit(Project project, String prompt) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow == null) {
            return;
        }
        toolWindow.show(() -> {
            LLMChatToolWindow instance = findInstance(project);
            if (instance != null) {
                instance.submitPrompt(prompt, true);
            }
        });
    }

    private JComponent createToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(new AnAction("New Chat", "Start a new chat session", AllIcons.General.Add) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                clearChat();
                addAssistantInfo("New chat started.", false);
            }
        });

        group.add(new AnAction("Clear", "Clear chat history", AllIcons.Actions.GC) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                clearChat();
            }
        });

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("LLMChatToolbar", group, true);
        toolbar.setTargetComponent(mainPanel);
        return toolbar.getComponent();
    }

    private JComponent createInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0));
        inputPanel.setBackground(UIUtil.getPanelBackground());

        JBScrollPane inputScroll = new JBScrollPane(inputArea);
        inputScroll.setBorder(JBUI.Borders.empty(8, 8, 8, 8));
        inputScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttonPanel.setBackground(UIUtil.getPanelBackground());
        buttonPanel.add(statusLabel);
        buttonPanel.add(stopButton);
        buttonPanel.add(sendButton);

        inputPanel.add(inputScroll, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        return inputPanel;
    }

    private void registerSendShortcut() {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK);
        inputArea.getInputMap(JComponent.WHEN_FOCUSED).put(keyStroke, "sendMessage");
        inputArea.getActionMap().put("sendMessage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
    }

    private void sendMessage() {
        String text = inputArea.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        inputArea.setText("");
        submitPrompt(text, false);
    }

    public void submitPrompt(String text, boolean newSession) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        if (streaming) {
            stopCurrentStream("Previous task stopped.", false);
        }

        if (newSession) {
            clearChat();
        } else {
            history.clear();
        }
        addUserMessage(text);
        history.add(new ChatMessage("user", text));

        StreamMarkdownPanel assistantPanel = addAssistantMessagePanel();
        ChatMessage assistantMessage = new ChatMessage("assistant", "");
        history.add(assistantMessage);

        setStreaming(true);
        long requestId = ++requestCounter;
        activeRequestId = requestId;
        List<ChatMessage> requestMessages = new ArrayList<>(history);
        requestMessages.remove(requestMessages.size() - 1);

        currentSession = chatService.streamChatCompletion(requestMessages, new OpenAIChatService.StreamHandler() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public void onDelta(String text) {
                if (requestId != activeRequestId) {
                    return;
                }
                buffer.append(text);
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (requestId != activeRequestId) {
                        return;
                    }
                    assistantPanel.appendText(text);
                    requestAutoScroll();
                });
            }

            @Override
            public void onComplete(String fullText) {
                if (requestId != activeRequestId) {
                    return;
                }
                assistantMessage.setContent(fullText);
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (requestId != activeRequestId) {
                        return;
                    }
                    assistantPanel.finish();
                    setStreaming(false);
                });
            }

            @Override
            public void onError(Throwable error) {
                if (requestId != activeRequestId) {
                    return;
                }
                String message = "**Error:** " + error.getMessage();
                assistantMessage.setContent(message);
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (requestId != activeRequestId) {
                        return;
                    }
                    assistantPanel.appendText("\n\n" + message);
                    assistantPanel.finish();
                    requestAutoScroll();
                    setStreaming(false);
                });
            }
        });
    }

    private void setStreaming(boolean value) {
        streaming = value;
        statusLabel.setText(value ? "Generating..." : "Idle");
        sendButton.setEnabled(!value);
        inputArea.setEditable(!value);
        stopButton.setEnabled(value);
        if (!value) {
            currentSession = null;
        }
        if (!value) {
            scrollTimer.stop();
        }
    }

    private void stopCurrentStream(String reason, boolean addNotice) {
        if (!streaming) {
            return;
        }
        OpenAIChatService.StreamSession session = currentSession;
        if (session != null) {
            session.cancel();
        }
        activeRequestId = -1L;
        setStreaming(false);
        if (addNotice) {
            addAssistantInfo(reason, false);
        }
    }

    private void clearChat() {
        messagesPanel.removeAll();
        messagesPanel.revalidate();
        messagesPanel.repaint();
        history.clear();
    }

    private void addUserMessage(String text) {
        JPanel bubble = new JPanel(new BorderLayout());
        bubble.setBackground(new JBColor(0xE3F2FD, 0x2F3E46));
        bubble.setBorder(JBUI.Borders.empty(8, 10));
        bubble.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JTextArea area = new WidthTrackingTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setFont(UIUtil.getLabelFont());

        bubble.add(area, BorderLayout.CENTER);

        JPanel row = createMessageRow(bubble, true);
        messagesPanel.add(row);
        messagesPanel.revalidate();
        messagesPanel.repaint();
        scrollToBottom();
    }

    private StreamMarkdownPanel addAssistantMessagePanel() {
        JPanel bubble = new JPanel(new BorderLayout());
        bubble.setBackground(UIUtil.getPanelBackground());
        bubble.setBorder(JBUI.Borders.compound(JBUI.Borders.customLine(JBColor.border(), 1), JBUI.Borders.empty(6, 8)));
        bubble.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        StreamMarkdownPanel panel = new StreamMarkdownPanel(project);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        bubble.add(panel, BorderLayout.CENTER);

        JPanel row = createMessageRow(bubble, false);
        messagesPanel.add(row);
        messagesPanel.revalidate();
        messagesPanel.repaint();
        scrollToBottom();
        return panel;
    }

    private JPanel createMessageRow(JComponent bubble, boolean alignRight) {
        JPanel row = new JPanel();
        row.setLayout(new javax.swing.BoxLayout(row, javax.swing.BoxLayout.X_AXIS));
        row.setBackground(UIUtil.getPanelBackground());
        row.setBorder(JBUI.Borders.empty(6, 8));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        if (alignRight) {
            row.add(javax.swing.Box.createHorizontalGlue());
            row.add(bubble);
        } else {
            row.add(bubble);
            row.add(javax.swing.Box.createHorizontalGlue());
        }
        return row;
    }

    private static class WidthTrackingTextArea extends JTextArea {
        WidthTrackingTextArea(String text) {
            super(text);
        }

        @Override
        public Dimension getPreferredSize() {
            java.awt.Container parent = getParent();
            if (parent != null && parent.getWidth() > 0) {
                setSize(new Dimension(parent.getWidth(), Integer.MAX_VALUE));
            }
            return super.getPreferredSize();
        }
    }

    private void addAssistantInfo(String text, boolean remember) {
        StreamMarkdownPanel panel = addAssistantMessagePanel();
        panel.appendText(text);
        if (remember) {
            history.add(new ChatMessage("assistant", text));
        }
    }

    private void scrollToBottom() {
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        programmaticScroll = true;
        try {
            vertical.setValue(vertical.getMaximum());
        } finally {
            programmaticScroll = false;
        }
    }

    private void requestAutoScroll() {
        if (!autoScrollEnabled) {
            return;
        }
        if (!scrollTimer.isRunning()) {
            scrollTimer.start();
        }
    }

    private void flushScroll() {
        if (!autoScrollEnabled) {
            scrollTimer.stop();
            return;
        }
        scrollToBottom();
    }

    private boolean isNearBottom() {
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        int max = vertical.getMaximum();
        int extent = vertical.getModel().getExtent();
        int value = vertical.getValue();
        return max - (value + extent) < 40;
    }

    private void installScrollBehavior() {
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        vertical.addAdjustmentListener(e -> {
            if (programmaticScroll) {
                return;
            }
            if (e.getValueIsAdjusting()) {
                if (!isNearBottom()) {
                    autoScrollEnabled = false;
                    scrollTimer.stop();
                }
            } else if (isNearBottom()) {
                autoScrollEnabled = true;
            }
        });
    }

    private static class ScrollablePanel extends JPanel implements javax.swing.Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
            return 10;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
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
