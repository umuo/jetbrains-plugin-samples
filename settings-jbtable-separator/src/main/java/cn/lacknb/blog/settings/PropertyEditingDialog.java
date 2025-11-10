package cn.lacknb.blog.settings;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * 属性编辑对话框 (V1.2 - 修复 FileType)
 */
public class PropertyEditingDialog extends DialogWrapper {

    private final JBTextField propertyNameField;
    private final EditorTextField propertyValueEditor;
    private final AppSettingsState.PropertyItem property;

    private static final String DEFAULT_PROPERTY_TEXT = "public class HelloWorld {\n" +
            "    public static void main(String[] args) {\n" +
            "        System.out.println(\"Hello, World!\");\n" +
            "    }\n" +
            "}";

    public PropertyEditingDialog(@Nullable Project project, @Nullable JComponent parent, AppSettingsState.PropertyItem property) {
        super(parent, true);
        this.property = property != null ? property : new AppSettingsState.PropertyItem("", "");

        propertyNameField = new JBTextField(this.property.name);

        // FIX: 使用 JavaFileType.INSTANCE 获取正确的 Java 文件类型
        propertyValueEditor = new EditorTextField(EditorFactory.getInstance().createDocument(this.property.value), project, JavaFileType.INSTANCE) {
            @Override
            protected EditorEx createEditor() {
                EditorEx editor = super.createEditor();
                EditorSettings settings = editor.getSettings();
                settings.setLineNumbersShown(true);
                settings.setIndentGuidesShown(true);
                settings.setFoldingOutlineShown(true);
                settings.setAdditionalLinesCount(0);
                settings.setAdditionalColumnsCount(0);
                settings.setLineMarkerAreaShown(false);
                return editor;
            }
        };
        propertyValueEditor.setOneLineMode(false);
        propertyValueEditor.setPreferredSize(new Dimension(500, 300));

        setTitle(property != null ? "编辑属性" : "添加属性");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        linkPanel.add(new ActionLink("填充通用示例", (ActionListener) e -> propertyValueEditor.setText(DEFAULT_PROPERTY_TEXT)));

        JPanel valuePanel = new JPanel(new BorderLayout());
        valuePanel.add(linkPanel, BorderLayout.NORTH);
        valuePanel.add(propertyValueEditor, BorderLayout.CENTER);

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("属性名:"), propertyNameField, true)
                .addLabeledComponent(new JBLabel("属性值 (Java):"), valuePanel, true)
                .getPanel();
    }

    @Override
    protected void doOKAction() {
        property.name = propertyNameField.getText().trim();
        property.value = propertyValueEditor.getText();
        super.doOKAction();
    }

    public AppSettingsState.PropertyItem getProperty() {
        return property;
    }

    @Override
    public void dispose() {
        if (propertyValueEditor.getEditor() != null) {
            EditorFactory.getInstance().releaseEditor(propertyValueEditor.getEditor());
        }
        super.dispose();
    }
}
