package cn.lacknb.blog.settings;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * 自定义规则编辑对话框 (V1.4 - 优化布局和尺寸)
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
        ruleValueArea = new JBTextArea(this.rule.value); // 移除固定的行列数，使其能自由缩放

        setTitle(rule != null ? "编辑自定义规则" : "添加自定义规则");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        // 主面板使用 BorderLayout，能更好地处理窗口缩放
        JPanel mainPanel = new JPanel(new BorderLayout(0, JBUI.scale(5)));

        // --- 顶部面板，用于放置“规则名” ---
        JPanel namePanel = new JPanel(new BorderLayout(JBUI.scale(5), 0));
        namePanel.add(new JBLabel("规则名:"), BorderLayout.WEST);
        namePanel.add(ruleNameField, BorderLayout.CENTER);
        mainPanel.add(namePanel, BorderLayout.NORTH);

        // --- 中心面板，用于放置“规则内容”，此部分将随窗口缩放 ---
        JPanel valueWrapperPanel = new JPanel(new BorderLayout(0, JBUI.scale(5)));
        valueWrapperPanel.add(new JBLabel("规则内容:"), BorderLayout.NORTH);

        // “填充默认规则”链接
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        linkPanel.add(new ActionLink("填充默认规则", (ActionListener) e -> ruleValueArea.setText(DEFAULT_RULE_TEXT)));

        // 包含文本域和链接的面板
        JPanel valueContentPanel = new JPanel(new BorderLayout());
        valueContentPanel.add(linkPanel, BorderLayout.NORTH);
        valueContentPanel.add(new JBScrollPane(ruleValueArea), BorderLayout.CENTER); // 将文本域放入滚动面板

        valueWrapperPanel.add(valueContentPanel, BorderLayout.CENTER);
        mainPanel.add(valueWrapperPanel, BorderLayout.CENTER);

        // 为对话框设置一个合适的初始尺寸
        mainPanel.setPreferredSize(new Dimension(450, 300));

        return mainPanel;
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
