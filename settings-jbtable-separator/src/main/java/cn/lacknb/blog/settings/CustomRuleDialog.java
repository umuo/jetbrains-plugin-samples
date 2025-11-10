package cn.lacknb.blog.settings;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.ActionLink; // FIX: 使用新的 ActionLink
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * 自定义规则编辑对话框 (V1.3 - 使用推荐的 ActionLink 组件)
 */
public class CustomRuleDialog extends DialogWrapper {

    private final JBTextField ruleNameField;
    private final JBTextArea ruleValueArea;
    private final AppSettingsState.CustomRule rule;

    private static final String DEFAULT_RULE_TEXT = "这是默认填充的规则内容。\n" +
            "你可以基于这个内容进行修改。\n" +
            "支持多行输入。";

    public CustomRuleDialog(@Nullable JComponent parent, AppSettingsState.CustomRule rule) {
        super(parent, true);
        this.rule = rule != null ? rule : new AppSettingsState.CustomRule("", "");

        ruleNameField = new JBTextField(this.rule.name);
        ruleValueArea = new JBTextArea(this.rule.value, 10, 50);

        setTitle(rule != null ? "编辑自定义规则" : "添加自定义规则");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JBScrollPane scrollPane = new JBScrollPane(ruleValueArea);

        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        // FIX: 使用新的 ActionLink 构造函数
        linkPanel.add(new ActionLink("填充默认规则", (ActionListener) e -> ruleValueArea.setText(DEFAULT_RULE_TEXT)));

        JPanel valuePanel = new JPanel(new BorderLayout());
        valuePanel.add(linkPanel, BorderLayout.NORTH);
        valuePanel.add(scrollPane, BorderLayout.CENTER);

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("规则名:"), ruleNameField, true)
                .addLabeledComponent(new JBLabel("规则内容:"), valuePanel, true)
                .getPanel();
    }

    @Override
    protected void doOKAction() {
        rule.name = ruleNameField.getText().trim();
        rule.value = ruleValueArea.getText();
        super.doOKAction();
    }

    public AppSettingsState.CustomRule getRule() {
        return rule;
    }
}
