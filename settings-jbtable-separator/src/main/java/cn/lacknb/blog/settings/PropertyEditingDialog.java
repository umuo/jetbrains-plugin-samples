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
        // 创建属性值编辑器 - 这是一个支持Java语法高亮的文本编辑器组件
        propertyValueEditor = new EditorTextField(
            // 使用EditorFactory创建文档，初始内容为当前属性的值
            EditorFactory.getInstance().createDocument(this.property.value),
            // 传入当前项目上下文
            project,
            // 指定文件类型为Java，这样编辑器会应用Java语法高亮和相关设置
            JavaFileType.INSTANCE
        ) {
            @Override
            protected EditorEx createEditor() {
                // 调用父类方法创建基础编辑器
                EditorEx editor = super.createEditor();
                
                // 获取编辑器的设置对象，用于配置编辑器外观和行为
                EditorSettings settings = editor.getSettings();
                
                // 配置编辑器设置：
                settings.setLineNumbersShown(true);           // 显示行号 - 方便用户定位代码位置
                settings.setIndentGuidesShown(true);          // 显示缩进引导线 - 帮助识别代码块结构
                settings.setFoldingOutlineShown(true);        // 显示代码折叠大纲 - 支持代码折叠功能
                settings.setAdditionalLinesCount(0);          // 设置额外的行数为0 - 不添加额外的空白行
                settings.setAdditionalColumnsCount(0);        // 设置额外的列数为0 - 不添加额外的空白列
                settings.setLineMarkerAreaShown(false);       // 隐藏行标记区域 - 简化界面，隐藏不需要的标记
                
                // 返回配置完成的编辑器实例
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
