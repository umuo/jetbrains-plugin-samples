package cn.lacknb.blog.llm.stream;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTextArea;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StreamMarkdownPanel extends JPanel {
    private final Project project;
    private final StringBuilder pendingBuffer = new StringBuilder();
    private final List<Component> blockComponents = new ArrayList<>();
    private final MarkdownRenderer renderer = new MarkdownRenderer();
    private final Timer updateTimer;
    private String pendingFragment = "";
    private BlockMode mode = BlockMode.TEXT;
    private String activeLanguage = "";
    private StringBuilder activeBuffer = new StringBuilder();
    private Component activeComponent;
    private int activeIndex = -1;
    private StringBuilder languageBuffer = new StringBuilder();
    private final StringBuilder codePendingBuffer = new StringBuilder();
    private final Timer codeFlushTimer;
    private CodeBlockPanel flushTarget;
    private static final int CODE_CHUNK_SIZE = 200;
    private static final int CODE_FLUSH_INTERVAL_MS = 16;
    private static final String CODE_COMMAND_GROUP = "LLM Stream Code";
    private static final int DELIMITER_TAIL = 12;
    private static final int RECOVERY_MIN_HASH_DEFAULT = 4;

    public StreamMarkdownPanel(Project project) {
        this.project = project;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UIUtil.getPanelBackground());
        updateTimer = new Timer(80, e -> flushPending());
        updateTimer.setRepeats(true);
        codeFlushTimer = new Timer(CODE_FLUSH_INTERVAL_MS, e -> flushCodePending(false));
        codeFlushTimer.setRepeats(true);
    }

    public void appendText(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        pendingBuffer.append(chunk);
        if (!updateTimer.isRunning()) {
            updateTimer.start();
        }
    }

    private void flushPending() {
        if (pendingBuffer.length() == 0) {
            updateTimer.stop();
            return;
        }
        String chunk = pendingBuffer.toString();
        pendingBuffer.setLength(0);
        consumeChunk(chunk, false);
    }

    public void finish() {
        String chunk = pendingBuffer.toString();
        pendingBuffer.setLength(0);
        String data = pendingFragment + chunk;
        pendingFragment = "";
        if (!data.isEmpty()) {
            consumeChunk(data, true);
        }
        if (!codeFlushTimer.isRunning() && codePendingBuffer.length() > 0) {
            flushCodePending(true);
        }
        if (mode == BlockMode.CODE_LANG) {
            activeLanguage = languageBuffer.toString().trim();
            mode = BlockMode.CODE;
            if (!activeLanguage.isBlank()) {
                upgradeCodeEditor();
            }
        }
    }

    private void consumeChunk(String chunk, boolean force) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        String data = pendingFragment + chunk;
        pendingFragment = "";
        int index = 0;
        while (index < data.length()) {
            if (mode == BlockMode.TEXT) {
                int codeIndex = findFenceIndex(data, index);
                int thinkIndex = findThinkIndex(data, index, "<think>");
                int next = nextDelimiter(codeIndex, thinkIndex);
                if (next == -1) {
                    if (force) {
                        appendTextBlock(data.substring(index));
                        return;
                    }
                    int tail = Math.min(DELIMITER_TAIL, data.length() - index);
                    if (data.length() - index <= DELIMITER_TAIL) {
                        pendingFragment = data.substring(index);
                        return;
                    }
                    appendTextBlock(data.substring(index, data.length() - tail));
                    pendingFragment = data.substring(data.length() - tail);
                    return;
                }
                if (next > index) {
                    appendTextBlock(data.substring(index, next));
                }
                if (next == codeIndex) {
                    activeLanguage = "";
                    switchToCode();
                    mode = BlockMode.CODE_LANG;
                    languageBuffer.setLength(0);
                    index = codeIndex + 3;
                    continue;
                }
                if (next == thinkIndex) {
                    switchToThink();
                    index = next + "<think>".length();
                    continue;
                }
            } else if (mode == BlockMode.CODE_LANG) {
                if (index >= data.length()) {
                    return;
                }
                char c = data.charAt(index);
                if (c == '\n' || c == '\r') {
                    activeLanguage = languageBuffer.toString().trim();
                    mode = BlockMode.CODE;
                    if (!activeLanguage.isBlank()) {
                        upgradeCodeEditor();
                    }
                    if (c == '\r' && index + 1 < data.length() && data.charAt(index + 1) == '\n') {
                        index += 2;
                    } else {
                        index += 1;
                    }
                    continue;
                }
                if (isLanguageChar(c)) {
                    languageBuffer.append(c);
                    index += 1;
                    continue;
                }
                activeLanguage = languageBuffer.toString().trim();
                mode = BlockMode.CODE;
                if (!activeLanguage.isBlank()) {
                    upgradeCodeEditor();
                }
            } else if (mode == BlockMode.CODE) {
                int codeIndex = findFenceIndex(data, index);
                if (codeIndex == -1) {
                    int minHashes = RECOVERY_MIN_HASH_DEFAULT;
                    if (isPlainTextLanguage(activeLanguage)) {
                        minHashes = 2;
                    }
                    int recoveryIndex = findMarkdownHeadingIndex(data, index, minHashes);
                    if (recoveryIndex != -1) {
                        if (recoveryIndex > index) {
                            appendCode(data.substring(index, recoveryIndex));
                        }
                        finishCodeBlock();
                        index = recoveryIndex;
                        continue;
                    }
                }
                if (codeIndex == -1) {
                    if (force) {
                        appendCode(data.substring(index));
                        return;
                    }
                    int tail = Math.min(DELIMITER_TAIL, data.length() - index);
                    if (data.length() - index <= DELIMITER_TAIL) {
                        pendingFragment = data.substring(index);
                        return;
                    }
                    appendCode(data.substring(index, data.length() - tail));
                    pendingFragment = data.substring(data.length() - tail);
                    return;
                }
                if (codeIndex > index) {
                    appendCode(data.substring(index, codeIndex));
                }
                finishCodeBlock();
                index = codeIndex + 3;
            } else if (mode == BlockMode.THINK) {
                int thinkEnd = findThinkIndex(data, index, "</think>");
                if (thinkEnd == -1) {
                    if (force) {
                        appendThink(data.substring(index));
                        return;
                    }
                    int tail = Math.min(DELIMITER_TAIL, data.length() - index);
                    if (data.length() - index <= DELIMITER_TAIL) {
                        pendingFragment = data.substring(index);
                        return;
                    }
                    appendThink(data.substring(index, data.length() - tail));
                    pendingFragment = data.substring(data.length() - tail);
                    return;
                }
                if (thinkEnd > index) {
                    appendThink(data.substring(index, thinkEnd));
                }
                finishThink();
                index = thinkEnd + "</think>".length();
            }
        }
    }

    private int nextDelimiter(int codeIndex, int thinkIndex) {
        if (codeIndex == -1) {
            return thinkIndex;
        }
        if (thinkIndex == -1) {
            return codeIndex;
        }
        return Math.min(codeIndex, thinkIndex);
    }

    private int findFenceIndex(String data, int from) {
        int len = data.length();
        for (int i = Math.max(0, from); i + 3 <= len; i++) {
            if (data.charAt(i) == '`' && data.startsWith("```", i) && isLineStartWithIndent(data, i)) {
                return i;
            }
        }
        return -1;
    }

    private int findMarkdownHeadingIndex(String data, int from, int minHashes) {
        int len = data.length();
        int i = Math.max(0, from);
        while (i < len) {
            if (!isLineStartWithIndent(data, i)) {
                i++;
                continue;
            }
            int j = i;
            while (j < len && (data.charAt(j) == ' ' || data.charAt(j) == '\t')) {
                j++;
            }
            int hashCount = 0;
            while (j < len && data.charAt(j) == '#') {
                hashCount++;
                j++;
            }
            if (hashCount >= minHashes && j < len && data.charAt(j) == ' ') {
                return i;
            }
            i = j + 1;
        }
        return -1;
    }

    private boolean isPlainTextLanguage(String language) {
        if (language == null) {
            return true;
        }
        String normalized = language.trim().toLowerCase();
        return normalized.isEmpty()
                || "text".equals(normalized)
                || "plaintext".equals(normalized)
                || "plain".equals(normalized);
    }

    private int findThinkIndex(String data, int from, String tag) {
        int idx = data.indexOf(tag, from);
        while (idx != -1) {
            if (isLineStartWithIndent(data, idx)) {
                return idx;
            }
            idx = data.indexOf(tag, idx + 1);
        }
        return -1;
    }

    private boolean isLineStartWithIndent(String data, int index) {
        int i = index - 1;
        while (i >= 0) {
            char c = data.charAt(i);
            if (c == '\n' || c == '\r') {
                return true;
            }
            if (c == ' ' || c == '\t') {
                i--;
                continue;
            }
            return false;
        }
        return true;
    }

    private boolean isLanguageChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '+' || c == '#' || c == '.' || c == '-';
    }

    private void switchToText() {
        mode = BlockMode.TEXT;
        activeLanguage = "";
        activeBuffer = new StringBuilder();
        activeComponent = createTextComponent("");
        activeIndex = blockComponents.size();
        blockComponents.add(activeComponent);
        add(activeComponent);
        revalidate();
    }

    private void switchToCode() {
        mode = BlockMode.CODE;
        activeBuffer = new StringBuilder();
        activeComponent = createStreamingCodeComponent(new MarkdownBlock(MarkdownBlock.Type.CODE, "", activeLanguage, false));
        activeIndex = blockComponents.size();
        blockComponents.add(activeComponent);
        add(activeComponent);
        revalidate();
    }

    private void switchToThink() {
        mode = BlockMode.THINK;
        activeBuffer = new StringBuilder();
        JLabel thinkLabel = new JLabel(renderer.toHtml("*" + "" + "*"));
        thinkLabel.setForeground(JBColor.GRAY);
        thinkLabel.setBorder(JBUI.Borders.empty(4, 8));
        thinkLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        activeComponent = thinkLabel;
        activeIndex = blockComponents.size();
        blockComponents.add(activeComponent);
        add(activeComponent);
        revalidate();
    }

    private void appendTextBlock(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (mode != BlockMode.TEXT || activeComponent == null) {
            switchToText();
        }
        activeBuffer.append(text);
        if (activeComponent instanceof JEditorPane) {
            ((JEditorPane) activeComponent).setText(renderer.toHtml(activeBuffer.toString()));
            activeComponent.revalidate();
        }
    }

    private void appendCode(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (mode != BlockMode.CODE || activeComponent == null) {
            switchToCode();
        }
        activeBuffer.append(text);
        if (activeComponent instanceof StreamingCodePanel) {
            ((StreamingCodePanel) activeComponent).appendText(text);
            activeComponent.revalidate();
        } else if (activeComponent instanceof CodeBlockPanel) {
            enqueueCode((CodeBlockPanel) activeComponent, text);
        }
    }

    private void appendThink(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (mode != BlockMode.THINK || activeComponent == null) {
            switchToThink();
        }
        activeBuffer.append(text);
        if (activeComponent instanceof JLabel) {
            ((JLabel) activeComponent).setText(renderer.toHtml("*" + activeBuffer + "*"));
            activeComponent.revalidate();
        }
    }

    private void finishCodeBlock() {
        if (activeComponent instanceof StreamingCodePanel) {
            String content = activeBuffer.toString();
            MarkdownBlock block = new MarkdownBlock(MarkdownBlock.Type.CODE, content, activeLanguage, true);
            Component replacement = createCodeComponent(block);
            remove(activeComponent);
            blockComponents.set(activeIndex, replacement);
            add(replacement, activeIndex);
        } else if (activeComponent instanceof CodeBlockPanel) {
            flushTarget = (CodeBlockPanel) activeComponent;
            if (codePendingBuffer.length() >= CODE_CHUNK_SIZE && !codeFlushTimer.isRunning()) {
                codeFlushTimer.start();
            } else if (codePendingBuffer.length() > 0 && !codeFlushTimer.isRunning()) {
                flushCodePending(true);
            }
        }
        activeComponent = null;
        activeIndex = -1;
        activeBuffer = new StringBuilder();
        mode = BlockMode.TEXT;
        activeLanguage = "";
        revalidate();
        repaint();
    }

    private void upgradeCodeEditor() {
        if (!(activeComponent instanceof StreamingCodePanel)) {
            return;
        }
        String content = activeBuffer.toString();
        MarkdownBlock block = new MarkdownBlock(MarkdownBlock.Type.CODE, content, activeLanguage, false);
        Component replacement = createCodeComponent(block);
        remove(activeComponent);
        blockComponents.set(activeIndex, replacement);
        add(replacement, activeIndex);
        activeComponent = replacement;
        revalidate();
        repaint();
    }

    private void enqueueCode(CodeBlockPanel panel, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        flushTarget = panel;
        codePendingBuffer.append(text);
        if (codePendingBuffer.length() < CODE_CHUNK_SIZE) {
            if (!codeFlushTimer.isRunning()) {
                flushCodePending(true);
            }
            return;
        }
        if (!codeFlushTimer.isRunning()) {
            codeFlushTimer.start();
        }
    }

    private void flushCodePending(boolean drainAll) {
        if (flushTarget == null) {
            codeFlushTimer.stop();
            codePendingBuffer.setLength(0);
            return;
        }
        if (codePendingBuffer.length() == 0) {
            codeFlushTimer.stop();
            flushTarget = null;
            return;
        }
        int len = drainAll ? codePendingBuffer.length() : Math.min(CODE_CHUNK_SIZE, codePendingBuffer.length());
        String text = codePendingBuffer.substring(0, len);
        codePendingBuffer.delete(0, len);
        appendToEditor(flushTarget, text);
        if (!drainAll && codePendingBuffer.length() == 0) {
            codeFlushTimer.stop();
            flushTarget = null;
        }
    }

    private void appendToEditor(CodeBlockPanel panel, String text) {
        EditorTextField editorTextField = panel.getEditorTextField();
        Runnable writeTask = () -> {
            Document doc = editorTextField.getDocument();
            doc.insertString(doc.getTextLength(), text);
        };
        if (ApplicationManager.getApplication().isDispatchThread()) {
            WriteCommandAction.runWriteCommandAction(project, CODE_COMMAND_GROUP, CODE_COMMAND_GROUP, writeTask);
        } else {
            CommandProcessor.getInstance().runUndoTransparentAction(() ->
                    WriteCommandAction.runWriteCommandAction(project, CODE_COMMAND_GROUP, CODE_COMMAND_GROUP, writeTask)
            );
        }
    }

    private void finishThink() {
        activeComponent = null;
        activeIndex = -1;
        activeBuffer = new StringBuilder();
        mode = BlockMode.TEXT;
        revalidate();
    }

    private Component createTextComponent(String text) {
        JEditorPane pane = new WidthTrackingHtmlPane(renderer.toHtml(text));
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.setFont(UIUtil.getLabelFont());
        pane.setBorder(JBUI.Borders.empty(4, 8));
        pane.setAlignmentX(Component.LEFT_ALIGNMENT);
        pane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return pane;
    }

    private Component createCodeComponent(MarkdownBlock block) {
        FileType fileType = PlainTextFileType.INSTANCE;
        String language = block.getLanguage();
        if (language != null && !language.isEmpty()) {
            FileType detected = FileTypeManager.getInstance().getFileTypeByExtension(language);
            if (detected != null) {
                fileType = detected;
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
            editor.setBackgroundColor(UIUtil.getPanelBackground());
            editor.getScrollPane().setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            editor.getScrollPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        });
        editorTextField.setBorder(BorderFactory.createCompoundBorder(
                JBUI.Borders.empty(4, 8),
                JBUI.Borders.customLine(JBColor.border(), 1)
        ));
        editorTextField.setAlignmentX(Component.LEFT_ALIGNMENT);
        editorTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        WriteCommandAction.runWriteCommandAction(project, () -> {
            Document doc = editorTextField.getDocument();
            doc.setText(block.getContent());
        });

        CodeBlockPanel wrapper = new CodeBlockPanel(project, editorTextField, language);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return wrapper;
    }

    private Component createStreamingCodeComponent(MarkdownBlock block) {
        StreamingCodePanel panel = new StreamingCodePanel(block.getContent());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return panel;
    }

    private static class CodeBlockPanel extends JPanel {
        private final Project project;
        private final EditorTextField editorTextField;
        private final JLabel languageLabel;
        private String language;

        CodeBlockPanel(Project project, EditorTextField editorTextField, String language) {
            super(new BorderLayout());
            this.project = project;
            this.editorTextField = editorTextField;
            this.language = language == null ? "" : language;
            setBackground(UIUtil.getPanelBackground());
            setBorder(JBUI.Borders.empty());

            languageLabel = new JLabel(languageLabelText(this.language));
            languageLabel.setFont(UIUtil.getLabelFont().deriveFont(UIUtil.getFontSize(UIUtil.FontSize.SMALL)));
            languageLabel.setForeground(UIUtil.getContextHelpForeground());

            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(UIUtil.getPanelBackground());

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            left.setBackground(UIUtil.getPanelBackground());
            left.add(languageLabel);

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
            right.setBackground(UIUtil.getPanelBackground());
            right.add(createIconAction(AllIcons.Actions.Copy, "Copy code", this::copyToClipboard));
            right.add(createIconAction(AllIcons.Actions.Replace, "Replace in editor", this::replaceInEditor));
            right.add(createIconAction(AllIcons.Actions.MenuPaste, "Insert at caret", this::insertInEditor));

            header.add(left, BorderLayout.WEST);
            header.add(right, BorderLayout.EAST);

            add(header, BorderLayout.NORTH);
            add(editorTextField, BorderLayout.CENTER);
        }

        EditorTextField getEditorTextField() {
            return editorTextField;
        }

        void updateLanguage(String newLanguage) {
            String next = newLanguage == null ? "" : newLanguage;
            if (!Objects.equals(language, next)) {
                language = next;
                languageLabel.setText(languageLabelText(language));
            }
        }

        private String languageLabelText(String language) {
            return language == null || language.isBlank() ? "plaintext" : language;
        }

        private JLabel createIconAction(javax.swing.Icon icon, String tooltip, Runnable action) {
            JLabel label = new JLabel(icon);
            label.setToolTipText(tooltip);
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    action.run();
                }
            });
            return label;
        }

        private void copyToClipboard() {
            StringSelection selection = new StringSelection(editorTextField.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        }

        private void replaceInEditor() {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor == null) {
                return;
            }
            String text = editorTextField.getText();
            WriteCommandAction.runWriteCommandAction(project, () -> {
                Document doc = editor.getDocument();
                int start = editor.getSelectionModel().getSelectionStart();
                int end = editor.getSelectionModel().getSelectionEnd();
                if (start != end) {
                    doc.replaceString(start, end, text);
                } else {
                    doc.setText(text);
                }
            });
        }

        private void insertInEditor() {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor == null) {
                return;
            }
            String text = editorTextField.getText();
            WriteCommandAction.runWriteCommandAction(project, () -> {
                Document doc = editor.getDocument();
                int offset = editor.getCaretModel().getOffset();
                doc.insertString(offset, text);
            });
        }
    }

    private static class StreamingCodePanel extends JPanel {
        private final JTextArea textArea;

        StreamingCodePanel(String text) {
            super(new BorderLayout());
            setBackground(UIUtil.getPanelBackground());
            setBorder(JBUI.Borders.compound(
                    JBUI.Borders.empty(4, 8),
                    JBUI.Borders.customLine(JBColor.border(), 1)
            ));

            textArea = new JTextArea(text == null ? "" : text);
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setOpaque(false);
            textArea.setBorder(JBUI.Borders.empty(4, 6));
            String fontName = EditorColorsManager.getInstance()
                    .getGlobalScheme()
                    .getEditorFontName();
            int fontSize = EditorColorsManager.getInstance()
                    .getGlobalScheme()
                    .getEditorFontSize();
            textArea.setFont(new java.awt.Font(fontName, java.awt.Font.PLAIN, fontSize));
            add(textArea, BorderLayout.CENTER);
        }

        void appendText(String more) {
            if (more != null && !more.isEmpty()) {
                textArea.append(more);
            }
        }

        void setText(String text) {
            textArea.setText(text == null ? "" : text);
        }
    }

    private enum BlockMode {
        TEXT,
        CODE_LANG,
        CODE,
        THINK
    }

    private static class WidthTrackingHtmlPane extends JEditorPane {
        WidthTrackingHtmlPane(String html) {
            super("text/html", html);
        }

        @Override
        public Dimension getPreferredSize() {
            Container parent = getParent();
            if (parent != null && parent.getWidth() > 0) {
                setSize(new Dimension(parent.getWidth(), Integer.MAX_VALUE));
            }
            return super.getPreferredSize();
        }
    }
}
