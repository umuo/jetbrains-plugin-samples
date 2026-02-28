package cn.lacknb.blog.llm.stream;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class ChatSettingsPanel {

    private final JPanel rootPanel;
    private final JBCheckBox autoFixUnitTestCheckBox;

    public ChatSettingsPanel() {
        JPanel contentPanel = new JBPanel<>();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(JBUI.Borders.empty(12));

        JPanel headerPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headerPanel.add(new JBLabel("聊天能力配置"));
        headerPanel.setBorder(JBUI.Borders.emptyBottom(10));
        contentPanel.add(headerPanel);

        JPanel featurePanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        featurePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("功能开关"),
                JBUI.Borders.empty(8, 10)
        ));
        autoFixUnitTestCheckBox = new JBCheckBox("是否开启自动修复单测");
        featurePanel.add(autoFixUnitTestCheckBox);
        featurePanel.add(Box.createHorizontalStrut(JBUI.scale(10)));
        contentPanel.add(featurePanel);

        rootPanel = new JBPanel<>(new BorderLayout());
        rootPanel.add(contentPanel, BorderLayout.NORTH);
    }

    public JComponent getComponent() {
        return rootPanel;
    }

    public boolean isAutoFixUnitTestEnabled() {
        return autoFixUnitTestCheckBox.isSelected();
    }

    public void setAutoFixUnitTestEnabled(boolean enabled) {
        autoFixUnitTestCheckBox.setSelected(enabled);
    }
}
