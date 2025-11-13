package cn.lacknb.blog.settings;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 特殊规则编辑对话框
 */
public class RuleEditingDialog extends DialogWrapper {

    private JBTable table;
    private DefaultTableModel tableModel;
    private final List<AppSettingsState.RuleItem> originalRules;
    private List<AppSettingsState.RuleItem> workingRules;

    private static final int DEFAULT_ROW_COUNT = 3;

    public RuleEditingDialog(Component parent, List<AppSettingsState.RuleItem> rules) {
        super(parent, true);
        this.originalRules = rules;
        // 创建一个深拷贝用于编辑，避免直接修改原始数据
        this.workingRules = rules.stream()
                .map(item -> new AppSettingsState.RuleItem(item.name, item.value))
                .collect(Collectors.toList());
        setTitle("编辑默认规则");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        // 创建表格模型
        tableModel = new DefaultTableModel(new Object[]{"Name", "Value"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // "Name" 列永远不可编辑
                if (column == 0) {
                    return false;
                }
                // 默认行中，只有第一行的 "Value" 可以编辑
                if (row < DEFAULT_ROW_COUNT) {
                    return row == 0;
                }
                // 新增的行 "Value" 都可以编辑
                return true;
            }
        };

        // 创建表格
        table = new JBTable(tableModel);

        // 设置自定义渲染器，使不可编辑的单元格显示为灰色
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                // 首先，让默认渲染器完成其工作（例如，处理选择高亮和默认背景/前景）
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // 只有当单元格未被选中时，才根据可编辑性调整前景色
                // 如果被选中，则使用JTable的选中颜色，不进行额外修改
                if (!isSelected) {
                    if (!table.getModel().isCellEditable(row, column)) {
                        // 不可编辑的单元格显示为灰色
                        c.setForeground(JBColor.GRAY);
                    } else {
                        // 可编辑的单元格恢复默认前景色（通常是黑色）
                        c.setForeground(table.getForeground());
                    }
                }
                return c;
            }
        });

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        // 填充数据
        for (AppSettingsState.RuleItem rule : workingRules) {
            tableModel.addRow(new Object[]{rule.name, rule.value});
        }

        // 创建带工具栏的面板
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(table)
                .setAddAction(button -> addRow())
                .setRemoveAction(button -> removeRow())
                .setRemoveActionUpdater(e -> {
                    // 只有非默认行可以被删除
                    int selectedRow = table.getSelectedRow();
                    return selectedRow >= DEFAULT_ROW_COUNT;
                });

        JPanel panel = decorator.createPanel();
        panel.setPreferredSize(new Dimension(400, 300));
        return panel;
    }

    private void addRow() {
        int nextRuleIndex = tableModel.getRowCount() + 1;
        String newName = "规则" + nextRuleIndex;
        String newValue = ""; // 默认值为空
        tableModel.addRow(new Object[]{newName, newValue});
    }

    private void removeRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= DEFAULT_ROW_COUNT) {
            tableModel.removeRow(selectedRow);
            // 重新生成后续规则的名称
            for (int i = selectedRow; i < tableModel.getRowCount(); i++) {
                String updatedName = "规则" + (i + 1);
                tableModel.setValueAt(updatedName, i, 0);
            }
        }
    }

    /**
     * 当点击 OK 按钮时，将UI上的数据同步回 workingRules 列表
     */
    @Override
    protected void doOKAction() {
        // 停止任何正在进行的单元格编辑
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        
        workingRules.clear();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String name = (String) tableModel.getValueAt(i, 0);
            String value = (String) tableModel.getValueAt(i, 1);
            workingRules.add(new AppSettingsState.RuleItem(name, value));
        }
        super.doOKAction();
    }

    /**
     * 返回编辑后的规则列表
     */
    public List<AppSettingsState.RuleItem> getUpdatedRules() {
        return workingRules;
    }
}
