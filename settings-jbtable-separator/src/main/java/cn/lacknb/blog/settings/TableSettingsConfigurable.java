package cn.lacknb.blog.settings;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * 设置面板配置类
 * 实现一个带有 Key-Value 表格和工具栏的设置页面
 */
public class TableSettingsConfigurable implements Configurable {

    private JPanel mainPanel;
    private JBTable table;
    private DefaultTableModel tableModel;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Table Separator Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        // 创建主面板
        mainPanel = new JPanel(new BorderLayout());

        // 创建表格模型
        tableModel = new DefaultTableModel(new String[]{"Key", "Value"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 默认不可编辑,通过编辑按钮编辑
            }
        };

        // 创建 JBTable
        table = new JBTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        // 添加一些示例数据
        tableModel.addRow(new Object[]{"example.key1", "value1"});
        tableModel.addRow(new Object[]{"example.key2", "value2"});

        // 使用 ToolbarDecorator 创建工具栏
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(table)
                .setAddAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        addRow();
                    }
                })
                .setAddIcon(AllIcons.General.Add)
                .setRemoveAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        removeRow();
                    }
                })
                .setEditAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        editRow();
                    }
                })
                .setPreferredSize(new Dimension(600, 400));

        // 将装饰后的面板添加到主面板
        mainPanel.add(decorator.createPanel(), BorderLayout.CENTER);

        return mainPanel;
    }

    /**
     * 新增行
     */
    private void addRow() {
        // 创建对话框输入 Key 和 Value
        JTextField keyField = new JTextField();
        JTextField valueField = new JTextField();

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        inputPanel.add(new JLabel("Key:"));
        inputPanel.add(keyField);
        inputPanel.add(new JLabel("Value:"));
        inputPanel.add(valueField);

        int result = JOptionPane.showConfirmDialog(
                mainPanel,
                inputPanel,
                "Add New Entry",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String key = keyField.getText().trim();
            String value = valueField.getText().trim();

            if (!key.isEmpty()) {
                tableModel.addRow(new Object[]{key, value});
            } else {
                Messages.showWarningDialog(mainPanel, "Key cannot be empty!", "Warning");
            }
        }
    }

    /**
     * 删除行
     */
    private void removeRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            int confirm = Messages.showYesNoDialog(
                    mainPanel,
                    "Are you sure you want to delete this entry?",
                    "Confirm Delete",
                    Messages.getQuestionIcon()
            );

            if (confirm == Messages.YES) {
                tableModel.removeRow(selectedRow);
            }
        } else {
            Messages.showWarningDialog(mainPanel, "Please select a row to delete!", "Warning");
        }
    }

    /**
     * 编辑行
     */
    private void editRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            String currentKey = (String) tableModel.getValueAt(selectedRow, 0);
            String currentValue = (String) tableModel.getValueAt(selectedRow, 1);

            JTextField keyField = new JTextField(currentKey);
            JTextField valueField = new JTextField(currentValue);

            JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
            inputPanel.add(new JLabel("Key:"));
            inputPanel.add(keyField);
            inputPanel.add(new JLabel("Value:"));
            inputPanel.add(valueField);

            int result = JOptionPane.showConfirmDialog(
                    mainPanel,
                    inputPanel,
                    "Edit Entry",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result == JOptionPane.OK_OPTION) {
                String newKey = keyField.getText().trim();
                String newValue = valueField.getText().trim();

                if (!newKey.isEmpty()) {
                    tableModel.setValueAt(newKey, selectedRow, 0);
                    tableModel.setValueAt(newValue, selectedRow, 1);
                } else {
                    Messages.showWarningDialog(mainPanel, "Key cannot be empty!", "Warning");
                }
            }
        } else {
            Messages.showWarningDialog(mainPanel, "Please select a row to edit!", "Warning");
        }
    }

    @Override
    public boolean isModified() {
        // 这里可以实现实际的修改检测逻辑
        return false;
    }

    @Override
    public void apply() {
        // 这里可以实现保存设置的逻辑
        // 例如: 保存到 PersistentStateComponent
    }

    @Override
    public void reset() {
        // 这里可以实现重置设置的逻辑
    }
}
