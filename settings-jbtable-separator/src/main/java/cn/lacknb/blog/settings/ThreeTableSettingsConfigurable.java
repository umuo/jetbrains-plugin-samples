package cn.lacknb.blog.settings;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 三表格联动设置面板 (V4.0 - 集成属性代码编辑器)
 * @author gitsilence
 */
public class ThreeTableSettingsConfigurable implements SearchableConfigurable {

    // UI Components
    private JPanel mainPanel;
    private JBTable leftTable;
    private DefaultTableModel leftTableModel;
    private JBTable rightTopTable;
    private DefaultTableModel rightTopTableModel;
    private JBTable rightBottomTable;
    private DefaultTableModel rightBottomTableModel;

    // UI State
    private int leftSelectedIndex = -1;
    private Map<Integer, AppSettingsState.TableData> uiDataMap = new HashMap<>();
    private final Project project;

    private static final int DEFAULT_LEFT_ITEMS_COUNT = 4;

    public ThreeTableSettingsConfigurable(Project project) {
        this.project = project;
    }

    @Override
    public @NotNull @NonNls String getId() {
        return "ThreeTableSettingsConfigurable";
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Three Table Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mainPanel = new JPanel(new BorderLayout(10, 0));
        mainPanel.add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLeftPanel(), createRightPanel()) {{
            setDividerLocation(250);
            setResizeWeight(0.3);
        }}, BorderLayout.CENTER);
        reset();
        return mainPanel;
    }

    //<editor-fold desc="UI Creation">
    private JPanel createLeftPanel() {
        leftTableModel = new DefaultTableModel(new Object[]{"选项"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        leftTable = new JBTable(leftTableModel);
        setupTable(leftTable);

        leftTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = leftTable.rowAtPoint(e.getPoint());
                if (row >= 0) selectLeftRow(row);
            }
        });

        return ToolbarDecorator.createDecorator(leftTable)
                .setAddAction(b -> addLeftRow())
                .setRemoveAction(b -> removeLeftRow())
                .setEditAction(b -> editLeftRow())
                .setEditActionUpdater(e -> isLeftRowEditable())
                .setRemoveActionUpdater(e -> isLeftRowEditable())
                .setPreferredSize(new Dimension(250, 400))
                .createPanel();
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 10));
        panel.add(createRightTopPanel());
        panel.add(createRightBottomPanel());
        return panel;
    }

    private JPanel createRightTopPanel() {
        rightTopTableModel = new DefaultTableModel(new Object[]{"规则"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        rightTopTable = new JBTable(rightTopTableModel);
        setupTable(rightTopTable);
        rightTopTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = rightTopTable.rowAtPoint(e.getPoint());
                if (row >= 0) selectRightTopRow(row);
            }
        });

        ToolbarDecorator rightTopToolbar = ToolbarDecorator.createDecorator(rightTopTable)
                .setAddAction(b -> addRightTopRow())
                .setRemoveAction(b -> removeRightTopRow())
                .setEditAction(b -> editRightTopRow())
                .setAddActionUpdater(e -> !isLeftRowDefault())
                .setRemoveActionUpdater(e -> !isLeftRowDefault() && rightTopTable.getSelectedRow() != -1)
                .setEditActionUpdater(e -> rightTopTable.getSelectedRow() != -1);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder("规则列表"));
        topPanel.add(rightTopToolbar.createPanel(), BorderLayout.CENTER);
        return topPanel;
    }

    private JPanel createRightBottomPanel() {
        rightBottomTableModel = new DefaultTableModel(new Object[]{"属性"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        rightBottomTable = new JBTable(rightBottomTableModel);
        setupTable(rightBottomTable);
        rightBottomTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = rightBottomTable.rowAtPoint(e.getPoint());
                if (row >= 0) selectRightBottomRow(row);
            }
        });

        ToolbarDecorator bottomDecorator = ToolbarDecorator.createDecorator(rightBottomTable)
                .setAddAction(b -> addRightBottomRow())
                .setRemoveAction(b -> removeRightBottomRow())
                .setEditAction(b -> editRightBottomRow())
                .setRemoveActionUpdater(e -> rightBottomTable.getSelectedRow() != -1)
                .setEditActionUpdater(e -> rightBottomTable.getSelectedRow() != -1);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createTitledBorder("属性"));
        bottomPanel.add(bottomDecorator.createPanel(), BorderLayout.CENTER);
        return bottomPanel;
    }

    private void setupTable(JBTable table) {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(25);
        table.getColumnModel().getColumn(0).setCellRenderer(new RadioButtonRenderer());
        table.getColumnModel().getColumn(0).setCellEditor(new RadioButtonEditor(new JCheckBox()));
    }
    //</editor-fold>

    //<editor-fold desc="Configurable Core Methods">
    @Override
    public boolean isModified() {
        AppSettingsState persistedState = AppSettingsState.getInstance();
        if (persistedState.leftSelectedIndex != this.leftSelectedIndex) return true;
        if (persistedState.leftTableItems.size() != leftTableModel.getRowCount()) return true;
        for (int i = 0; i < leftTableModel.getRowCount(); i++) {
            AppSettingsState.LeftTableItem persistedLeftItem = i < persistedState.leftTableItems.size() ? persistedState.leftTableItems.get(i) : null;
            if (persistedLeftItem == null) return true;
            String uiLeftItemName = (String) leftTableModel.getValueAt(i, 0);
            if (!Objects.equals(uiLeftItemName, persistedLeftItem.name)) return true;
            AppSettingsState.TableData uiTableData = uiDataMap.get(i);
            if (!Objects.equals(uiTableData, persistedLeftItem.tableData)) return true;
        }
        return false;
    }

    @Override
    public void apply() {
        AppSettingsState state = AppSettingsState.getInstance();
        state.leftTableItems.clear();
        state.leftSelectedIndex = this.leftSelectedIndex;
        for (int i = 0; i < leftTableModel.getRowCount(); i++) {
            AppSettingsState.LeftTableItem item = new AppSettingsState.LeftTableItem();
            item.name = (String) leftTableModel.getValueAt(i, 0);
            item.isDefault = isRowDefault(i);
            item.tableData = deepCopyTableData(uiDataMap.getOrDefault(i, new AppSettingsState.TableData()));
            state.leftTableItems.add(item);
        }
    }

    @Override
    public void reset() {
        AppSettingsState state = AppSettingsState.getInstance();
        leftTableModel.setRowCount(0);
        uiDataMap.clear();
        if (state.leftTableItems.isEmpty()) {
            initSampleData();
        } else {
            this.leftSelectedIndex = state.leftSelectedIndex;
            for (int i = 0; i < state.leftTableItems.size(); i++) {
                AppSettingsState.LeftTableItem item = state.leftTableItems.get(i);
                leftTableModel.addRow(new Object[]{item.name});
                uiDataMap.put(i, deepCopyTableData(item.tableData));
            }
        }
        int indexToSelect = (this.leftSelectedIndex >= 0 && this.leftSelectedIndex < leftTableModel.getRowCount()) ? this.leftSelectedIndex : 0;
        if (leftTableModel.getRowCount() > 0) {
            selectLeftRow(indexToSelect);
            leftTable.setRowSelectionInterval(indexToSelect, indexToSelect);
        } else {
            loadRightTablesFromUiMap(-1);
        }
    }
    //</editor-fold>

    //<editor-fold desc="Data Initialization & Management">
    private void initSampleData() {
        String[] defaultNames = {"默认规则1", "默认规则2", "默认规则3", "默认规则4"};
        for (int i = 0; i < defaultNames.length; i++) {
            leftTableModel.addRow(new Object[]{defaultNames[i]});
            AppSettingsState.TableData data = new AppSettingsState.TableData();
            data.ruleItems.add(new AppSettingsState.RuleItem("规则1", "Value1"));
            data.ruleItems.add(new AppSettingsState.RuleItem("规则2", "Value2"));
            data.ruleItems.add(new AppSettingsState.RuleItem("规则3", "Value3"));
            data.bottomItems.add(new AppSettingsState.PropertyItem("示例属性", "public class Sample {}"));
            uiDataMap.put(i, data);
        }
    }

    private void loadRightTablesFromUiMap(int row) {
        AppSettingsState.TableData data = uiDataMap.get(row);
        boolean isDefault = isRowDefault(row);

        rightTopTableModel.setRowCount(0);
        if (isDefault) {
            rightTopTableModel.addRow(new Object[]{"默认规则 (点击编辑)"});
        } else if (data != null) {
            for (AppSettingsState.CustomRule rule : data.customRules) {
                rightTopTableModel.addRow(new Object[]{rule.name});
            }
        }

        rightBottomTableModel.setRowCount(0);
        if (data != null) {
            for (AppSettingsState.PropertyItem item : data.bottomItems) {
                rightBottomTableModel.addRow(new Object[]{item.name});
            }
        }
        mainPanel.revalidate();
        mainPanel.repaint();
    }
    //</editor-fold>

    //<editor-fold desc="Row Selection & State Check">
    private void selectLeftRow(int row) {
        if (row < 0 || row >= leftTableModel.getRowCount()) return;
        leftSelectedIndex = row;
        leftTable.repaint();
        loadRightTablesFromUiMap(row);
    }

    private void selectRightTopRow(int row) {
        if (leftSelectedIndex < 0 || row < 0 || isLeftRowDefault()) return;
        AppSettingsState.TableData data = uiDataMap.get(leftSelectedIndex);
        if (data != null) {
            data.selectedCustomRuleIndex = row;
            rightTopTable.repaint();
        }
    }

    private void selectRightBottomRow(int row) {
        if (leftSelectedIndex < 0 || row < 0) return;
        AppSettingsState.TableData data = uiDataMap.get(leftSelectedIndex);
        if (data != null) {
            data.bottomSelectedIndex = row;
            rightBottomTable.repaint();
        }
    }

    private boolean isLeftRowDefault() {
        return isRowDefault(leftTable.getSelectedRow());
    }

    private boolean isRowDefault(int row) {
        return row >= 0 && row < DEFAULT_LEFT_ITEMS_COUNT;
    }
    
    private boolean isLeftRowEditable() {
        return !isLeftRowDefault();
    }
    //</editor-fold>

    //<editor-fold desc="CRUD Operations">
    private void addLeftRow() {
        String name = Messages.showInputDialog(mainPanel, "请输入选项名称:", "添加选项", null);
        if (name != null && !name.trim().isEmpty()) {
            leftTableModel.addRow(new Object[]{name.trim()});
            uiDataMap.put(leftTableModel.getRowCount() - 1, new AppSettingsState.TableData());
        }
    }

    private void removeLeftRow() {
        int row = leftTable.getSelectedRow();
        if (row < 0 || !isLeftRowEditable()) return;
        if (Messages.showYesNoDialog(mainPanel, "确定要删除该选项吗？", "确认删除", Messages.getQuestionIcon()) == Messages.YES) {
            leftTableModel.removeRow(row);
            Map<Integer, AppSettingsState.TableData> newMap = new HashMap<>();
            for (int i = 0; i < leftTableModel.getRowCount(); i++) {
                newMap.put(i, uiDataMap.get(i < row ? i : i + 1));
            }
            uiDataMap = newMap;
            if (leftSelectedIndex == row) {
                leftSelectedIndex = -1;
                loadRightTablesFromUiMap(-1);
            } else if (leftSelectedIndex > row) {
                leftSelectedIndex--;
            }
            leftTable.repaint();
        }
    }

    private void editLeftRow() {
        int row = leftTable.getSelectedRow();
        if (row < 0 || !isLeftRowEditable()) return;
        String currentName = (String) leftTableModel.getValueAt(row, 0);
        String newName = Messages.showInputDialog(mainPanel, "请输入新的选项名称:", "编辑选项", null, currentName, null);
        if (newName != null && !newName.trim().isEmpty()) {
            leftTableModel.setValueAt(newName.trim(), row, 0);
        }
    }

    private void editRightTopRow() {
        int row = rightTopTable.getSelectedRow();
        if (row < 0) return;
        if (isLeftRowDefault()) {
            AppSettingsState.TableData data = uiDataMap.get(leftSelectedIndex);
            if (data == null) return;
            RuleEditingDialog dialog = new RuleEditingDialog(mainPanel, data.ruleItems);
            if (dialog.showAndGet()) {
                data.ruleItems = dialog.getUpdatedRules();
            }
        } else {
            AppSettingsState.TableData data = uiDataMap.get(leftSelectedIndex);
            if (data == null || row >= data.customRules.size()) return;
            AppSettingsState.CustomRule ruleToEdit = data.customRules.get(row);
            CustomRuleDialog dialog = new CustomRuleDialog(mainPanel, ruleToEdit);
            if (dialog.showAndGet()) {
                loadRightTablesFromUiMap(leftSelectedIndex);
            }
        }
    }
    
    private void addRightTopRow() {
        if (isLeftRowDefault()) return;
        CustomRuleDialog dialog = new CustomRuleDialog(mainPanel, null);
        if (dialog.showAndGet()) {
            AppSettingsState.CustomRule newRule = dialog.getRule();
            if (!newRule.name.isEmpty()) {
                AppSettingsState.TableData data = uiDataMap.get(leftSelectedIndex);
                if (data != null) {
                    data.customRules.add(newRule);
                    loadRightTablesFromUiMap(leftSelectedIndex);
                }
            }
        }
    }

    private void removeRightTopRow() {
        if (isLeftRowDefault()) return;
        int row = rightTopTable.getSelectedRow();
        if (row < 0) return;
        AppSettingsState.TableData data = uiDataMap.get(leftSelectedIndex);
        if (data != null && row < data.customRules.size()) {
            data.customRules.remove(row);
            if (data.selectedCustomRuleIndex == row) {
                data.selectedCustomRuleIndex = -1;
            } else if (data.selectedCustomRuleIndex > row) {
                data.selectedCustomRuleIndex--;
            }
            loadRightTablesFromUiMap(leftSelectedIndex);
        }
    }

    private void addRightBottomRow() {
        PropertyEditingDialog dialog = new PropertyEditingDialog(project, mainPanel, null);
        if (dialog.showAndGet()) {
            AppSettingsState.PropertyItem newItem = dialog.getProperty();
            if (!newItem.name.isEmpty()) {
                AppSettingsState.TableData data = uiDataMap.get(leftSelectedIndex);
                if (data != null) {
                    data.bottomItems.add(newItem);
                    loadRightTablesFromUiMap(leftSelectedIndex);
                }
            }
        }
    }

    private void removeRightBottomRow() {
        int row = rightBottomTable.getSelectedRow();
        if (row < 0) return;
        AppSettingsState.TableData data = uiDataMap.get(leftSelectedIndex);
        if (data != null && row < data.bottomItems.size()) {
            data.bottomItems.remove(row);
            if (data.bottomSelectedIndex == row) {
                data.bottomSelectedIndex = -1;
            } else if (data.bottomSelectedIndex > row) {
                data.bottomSelectedIndex--;
            }
            loadRightTablesFromUiMap(leftSelectedIndex);
        }
    }

    private void editRightBottomRow() {
        int row = rightBottomTable.getSelectedRow();
        if (row < 0) return;
        AppSettingsState.TableData data = uiDataMap.get(leftSelectedIndex);
        if (data == null || row >= data.bottomItems.size()) return;
        AppSettingsState.PropertyItem itemToEdit = data.bottomItems.get(row);
        PropertyEditingDialog dialog = new PropertyEditingDialog(project, mainPanel, itemToEdit);
        if (dialog.showAndGet()) {
            loadRightTablesFromUiMap(leftSelectedIndex);
        }
    }
    //</editor-fold>

    //<editor-fold desc="Renderers & Editors & Helpers">
    class RadioButtonRenderer extends JRadioButton implements TableCellRenderer {
        RadioButtonRenderer() { setHorizontalAlignment(LEFT); }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value != null ? value.toString() : "");
            boolean checked = false;
            if (table == leftTable) {
                checked = (row == leftSelectedIndex);
            } else {
                AppSettingsState.TableData data = uiDataMap.get(leftSelectedIndex);
                if (table == rightTopTable) {
                    if (isLeftRowDefault()) {
                        checked = (row == 0);
                    } else if (data != null) {
                        checked = (row == data.selectedCustomRuleIndex);
                    }
                } else if (table == rightBottomTable) {
                    if (data != null) {
                        checked = (row == data.bottomSelectedIndex);
                    }
                }
            }
            setSelected(checked);
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            return this;
        }
    }

    static class RadioButtonEditor extends DefaultCellEditor {
        RadioButtonEditor(JCheckBox checkBox) { super(checkBox); }
    }

    private AppSettingsState.TableData deepCopyTableData(AppSettingsState.TableData original) {
        AppSettingsState.TableData copy = new AppSettingsState.TableData();
        copy.selectedCustomRuleIndex = original.selectedCustomRuleIndex;
        copy.bottomSelectedIndex = original.bottomSelectedIndex;
        copy.customRules = new ArrayList<>();
        for (AppSettingsState.CustomRule rule : original.customRules) {
            copy.customRules.add(new AppSettingsState.CustomRule(rule.name, rule.value));
        }
        copy.bottomItems = new ArrayList<>();
        for (AppSettingsState.PropertyItem item : original.bottomItems) {
            copy.bottomItems.add(new AppSettingsState.PropertyItem(item.name, item.value));
        }
        copy.ruleItems = new ArrayList<>();
        for (AppSettingsState.RuleItem item : original.ruleItems) {
            copy.ruleItems.add(new AppSettingsState.RuleItem(item.name, item.value));
        }
        return copy;
    }
    //</editor-fold>
}
