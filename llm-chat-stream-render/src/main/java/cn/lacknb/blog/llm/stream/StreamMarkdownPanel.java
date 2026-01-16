package cn.lacknb.blog.llm.stream;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
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
import javax.swing.SwingConstants;
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
    private final StringBuilder fullBuffer = new StringBuilder();
    private final StringBuilder pendingBuffer = new StringBuilder();
    private final List<Component> blockComponents = new ArrayList<>();
    private final MarkdownRenderer renderer = new MarkdownRenderer();
    private List<MarkdownBlock> currentBlocks = new ArrayList<>();
    private final Timer updateTimer;

    public StreamMarkdownPanel(Project project) {
        this.project = project;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UIUtil.getPanelBackground());
        updateTimer = new Timer(80, e -> flushPending());
        updateTimer.setRepeats(true);
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
        fullBuffer.append(chunk);
        List<MarkdownBlock> newBlocks = StreamMarkdownParser.parse(fullBuffer.toString());
        updateBlocks(newBlocks);
    }

    private void updateBlocks(List<MarkdownBlock> newBlocks) {
        boolean layoutChanged = false;
        for (int i = 0; i < newBlocks.size(); i++) {
            MarkdownBlock newBlock = newBlocks.get(i);
            if (i < blockComponents.size()) {
                MarkdownBlock oldBlock = currentBlocks.get(i);
                boolean sameType = oldBlock.getType() == newBlock.getType();
                boolean sameMeta = true;
                if (newBlock.getType() == MarkdownBlock.Type.CODE) {
                    sameMeta = Objects.equals(oldBlock.getLanguage(), newBlock.getLanguage());
                } else if (newBlock.getType() == MarkdownBlock.Type.TOOL) {
                    sameMeta = Objects.equals(oldBlock.getToolName(), newBlock.getToolName());
                }
                if (sameType && sameMeta) {
                    updateComponent(blockComponents.get(i), oldBlock, newBlock);
                } else {
                    remove(blockComponents.get(i));
                    Component replacement = createComponent(newBlock);
                    blockComponents.set(i, replacement);
                    add(replacement, i);
                    layoutChanged = true;
                }
            } else {
                Component component = createComponent(newBlock);
                blockComponents.add(component);
                add(component);
                layoutChanged = true;
            }
        }

        while (blockComponents.size() > newBlocks.size()) {
            Component extra = blockComponents.remove(blockComponents.size() - 1);
            remove(extra);
            layoutChanged = true;
        }

        currentBlocks = newBlocks;
        if (layoutChanged) {
            revalidate();
        }
        repaint();
    }

    private Component createComponent(MarkdownBlock block) {
        switch (block.getType()) {
            case CODE:
                return createCodeComponent(block);
            case TOOL:
                return createToolComponent(block);
            case THINK:
                JLabel thinkLabel = new JLabel(renderer.toHtml("*" + block.getContent() + "*"));
                thinkLabel.setForeground(JBColor.GRAY);
                thinkLabel.setBorder(JBUI.Borders.empty(4, 8));
                thinkLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                return thinkLabel;
            default:
                return createTextComponent(block.getContent());
        }
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

    private Component createToolComponent(MarkdownBlock block) {
        ToolCallPanel panel = new ToolCallPanel(project, block.getToolName(), block.getContent());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return panel;
    }

    private void updateComponent(Component component, MarkdownBlock oldBlock, MarkdownBlock newBlock) {
        if (newBlock.getType() == MarkdownBlock.Type.CODE) {
            if (component instanceof CodeBlockPanel) {
                CodeBlockPanel wrapper = (CodeBlockPanel) component;
                wrapper.updateLanguage(newBlock.getLanguage());
                EditorTextField editorTextField = wrapper.getEditorTextField();
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    Document doc = editorTextField.getDocument();
                    String oldText = oldBlock.getContent();
                    String newText = newBlock.getContent();
                    if (newText.startsWith(oldText)) {
                        doc.insertString(doc.getTextLength(), newText.substring(oldText.length()));
                    } else {
                        doc.setText(newText);
                    }
                });
            }
        } else if (newBlock.getType() == MarkdownBlock.Type.TOOL) {
            if (component instanceof ToolCallPanel) {
                ((ToolCallPanel) component).updateContent(newBlock.getToolName(), newBlock.getContent());
            }
        } else if (newBlock.getType() == MarkdownBlock.Type.THINK) {
            if (component instanceof JLabel) {
                ((JLabel) component).setText(renderer.toHtml("*" + newBlock.getContent() + "*"));
            }
        } else if (component instanceof JEditorPane) {
            ((JEditorPane) component).setText(renderer.toHtml(newBlock.getContent()));
        }
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

    private static class ToolCallPanel extends JPanel {
        private final Project project;
        private final JLabel titleLabel;
        private final JLabel toggleLabel;
        private final EditorTextField paramsField;
        private final JPanel paramsWrapper;
        private boolean collapsed = true;

        ToolCallPanel(Project project, String toolName, String params) {
            super(new BorderLayout());
            this.project = project;
            setBackground(UIUtil.getPanelBackground());
            setBorder(JBUI.Borders.compound(
                    JBUI.Borders.customLine(JBColor.border(), 1),
                    JBUI.Borders.empty(6, 8)
            ));

            titleLabel = new JLabel(titleText(toolName));
            titleLabel.setFont(UIUtil.getLabelFont().deriveFont(UIUtil.getFontSize(UIUtil.FontSize.SMALL)));
            titleLabel.setForeground(UIUtil.getLabelForeground());

            toggleLabel = new JLabel("Show params");
            toggleLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            toggleLabel.setForeground(UIUtil.getContextHelpForeground());
            toggleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            toggleLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    setCollapsed(!collapsed);
                }
            });

            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(UIUtil.getPanelBackground());
            header.add(titleLabel, BorderLayout.WEST);
            header.add(toggleLabel, BorderLayout.EAST);

            paramsField = new EditorTextField("", project, PlainTextFileType.INSTANCE);
            paramsField.setOneLineMode(false);
            paramsField.setViewer(true);
            paramsField.ensureWillComputePreferredSize();
            paramsField.addSettingsProvider(editor -> {
                editor.getSettings().setUseSoftWraps(true);
                editor.getSettings().setLineNumbersShown(false);
                editor.getSettings().setGutterIconsShown(false);
                editor.setBackgroundColor(UIUtil.getPanelBackground());
                editor.getScrollPane().setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                editor.getScrollPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            });
            paramsField.setBorder(JBUI.Borders.empty(4, 0, 0, 0));

            paramsWrapper = new JPanel(new BorderLayout());
            paramsWrapper.setBackground(UIUtil.getPanelBackground());
            paramsWrapper.add(paramsField, BorderLayout.CENTER);
            paramsWrapper.setVisible(false);

            add(header, BorderLayout.NORTH);
            add(paramsWrapper, BorderLayout.CENTER);

            updateContent(toolName, params);
        }

        void updateContent(String toolName, String params) {
            titleLabel.setText(titleText(toolName));
            WriteCommandAction.runWriteCommandAction(project, () -> {
                Document doc = paramsField.getDocument();
                doc.setText(params == null ? "" : params);
            });
        }

        private void setCollapsed(boolean value) {
            collapsed = value;
            paramsWrapper.setVisible(!value);
            toggleLabel.setText(value ? "Show params" : "Hide params");
            revalidate();
            repaint();
        }

        private String titleText(String toolName) {
            String name = toolName == null || toolName.isBlank() ? "unknown" : toolName;
            return "Tool call: " + name;
        }
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
