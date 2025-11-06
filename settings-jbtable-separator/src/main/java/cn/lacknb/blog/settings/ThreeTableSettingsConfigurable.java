package cn.lacknb.blog.settings;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

/**
 * 三表格联动设置面板
 * 功能说明：
 * 1. 左侧一个表格，右侧两个表格（上下布局）
 * 2. 每个表格都是单列，每行前面都有单选框（Radio Button）
 * 3. 左侧表格切换选项时，右侧两个表格会显示对应的数据
 * 4. 每个表格都可以选中一行，选中状态会被保存
 * @author gitsilence
 */
public class ThreeTableSettingsConfigurable implements SearchableConfigurable {

    // ========== 界面组件 ==========

    /** 主面板 */
    private JPanel mainPanel;

    // --- 左侧表格相关 ---
    /** 左侧表格组件 */
    private JBTable leftTable;
    /** 左侧表格数据模型 */
    private DefaultTableModel leftTableModel;
    /** 左侧表格的单选按钮组（暂未使用，保留以便后续扩展） */
    private ButtonGroup leftButtonGroup;
    /** 左侧表格当前选中的行索引，-1 表示未选中 */
    private int leftSelectedIndex = -1;

    // --- 右侧上方表格相关 ---
    /** 右侧上方表格组件 */
    private JBTable rightTopTable;
    /** 右侧上方表格数据模型 */
    private DefaultTableModel rightTopTableModel;
    // private ButtonGroup rightTopButtonGroup; // 暂未使用

    // --- 右侧下方表格相关 ---
    /** 右侧下方表格组件 */
    private JBTable rightBottomTable;
    /** 右侧下方表格数据模型 */
    private DefaultTableModel rightBottomTableModel;
    // private ButtonGroup rightBottomButtonGroup; // 暂未使用

    // ========== 数据存储 ==========

    /**
     * 存储每个左侧选项对应的右侧表格数据和选中状态
     * Key: 左侧表格的行索引
     * Value: 对应的右侧表格数据（TableData 对象）
     */
    private Map<Integer, TableData> dataMap = new HashMap<>();

    @Override
    public @NotNull @NonNls String getId() {
        return "ThreeTableSettingsConfigurable";
    }

    /**
     * 内部类：存储右侧两个表格的数据和选中状态
     * 作用：为每个左侧选项保存独立的右侧表格数据
     */
    private static class TableData {
        /** 右侧上方表格的数据项列表 */
        java.util.List<String> topItems = new java.util.ArrayList<>();
        /** 右侧下方表格的数据项列表 */
        java.util.List<String> bottomItems = new java.util.ArrayList<>();
        /** 右侧上方表格选中的行索引，-1 表示未选中 */
        int topSelectedIndex = -1;
        /** 右侧下方表格选中的行索引，-1 表示未选中 */
        int bottomSelectedIndex = -1;
    }

    // ========== 配置接口实现 ==========

    /**
     * 返回设置页面的显示名称
     * 这个名称会显示在 Settings 对话框的左侧菜单中
     */
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Three Table Settings";
    }

    /**
     * 创建设置面板的 UI 组件
     * 这个方法在用户打开设置页面时被调用
     * @return 返回主面板组件
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        // 创建主面板，使用 BorderLayout 布局，左右间距 10 像素
        mainPanel = new JPanel(new BorderLayout(10, 0));

        // 创建左侧面板（包含左侧表格和工具栏）
        JPanel leftPanel = createLeftPanel();

        // 创建右侧面板（包含上下两个表格）
        JPanel rightPanel = createRightPanel();

        // 使用 JSplitPane 创建可调整大小的分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(250);  // 设置分割线初始位置在 250 像素处
        splitPane.setResizeWeight(0.3);      // 设置调整窗口大小时，左侧占 30% 的权重

        mainPanel.add(splitPane, BorderLayout.CENTER);

        // 初始化示例数据（包括默认规则、生成规则、单测示例）
        initSampleData();

        return mainPanel;
    }

    // ========== UI 创建方法 ==========

    /**
     * 创建左侧面板
     * 包含：单列表格 + 工具栏（添加、删除、编辑按钮）
     */
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // leftButtonGroup = new ButtonGroup(); // 预留的单选按钮组

        // 创建单列表格模型
        leftTableModel = new DefaultTableModel(new Object[]{"选项"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                // 指定列的数据类型为 String
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                // 设置单元格不可直接编辑，只能通过编辑按钮修改
                return false;
            }
        };

        // 创建 JBTable（JetBrains 的表格组件，比标准 JTable 更适合 IntelliJ 插件）
        leftTable = new JBTable(leftTableModel);
        leftTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);  // 设置为单选模式
        leftTable.getTableHeader().setReorderingAllowed(false);           // 禁止拖动列头调整顺序
        leftTable.setRowHeight(25);                                       // 设置行高为 25 像素

        // 为表格的第一列（唯一列）设置自定义渲染器，显示 Radio 按钮
        leftTable.getColumnModel().getColumn(0).setCellRenderer(new RadioButtonRenderer());
        // 为表格的第一列设置自定义编辑器（虽然不可编辑，但需要编辑器来处理点击事件）
        leftTable.getColumnModel().getColumn(0).setCellEditor(new RadioButtonEditor(new JCheckBox()));

        // 添加鼠标点击监听器，点击表格任意位置都会选中该行
        leftTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // 获取鼠标点击位置对应的行索引
                int row = leftTable.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    // 选中该行，并切换右侧表格的显示内容
                    selectLeftRow(row);
                }
            }
        });

        // 使用 ToolbarDecorator 创建工具栏，提供添加、删除、编辑功能
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(leftTable)
                // 添加按钮的动作
                .setAddAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        addLeftRow();  // 调用添加行的方法
                    }
                })
                .setAddIcon(AllIcons.General.Add)  // 设置添加按钮的图标
                // 删除按钮的动作
                .setRemoveAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        removeLeftRow();  // 调用删除行的方法
                    }
                })
                // 编辑按钮的动作
                .setEditAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        editLeftRow();  // 调用编辑行的方法
                    }
                })
                .setPreferredSize(new Dimension(250, 400));  // 设置首选大小

        // 将装饰后的面板（表格+工具栏）添加到左侧面板
        panel.add(decorator.createPanel(), BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建右侧面板
     * 包含：上下两个表格，每个表格都有独立的工具栏
     */
    private JPanel createRightPanel() {
        // 使用 GridLayout 创建 2 行 1 列的布局，垂直间距 10 像素
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 10));

        // ========== 创建右侧上方表格 ==========

        // rightTopButtonGroup = new ButtonGroup(); // 预留的单选按钮组

        // 创建上方表格的数据模型
        rightTopTableModel = new DefaultTableModel(new Object[]{"配置项"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;  // 不可直接编辑
            }
        };

        // 创建上方表格
        rightTopTable = new JBTable(rightTopTableModel);
        rightTopTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rightTopTable.setRowHeight(25);
        // 设置 Radio 按钮渲染器和编辑器
        rightTopTable.getColumnModel().getColumn(0).setCellRenderer(new RadioButtonRenderer());
        rightTopTable.getColumnModel().getColumn(0).setCellEditor(new RadioButtonEditor(new JCheckBox()));

        // 添加鼠标点击监听器
        rightTopTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = rightTopTable.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    selectRightTopRow(row);  // 选中该行的 Radio 按钮
                }
            }
        });

        // 创建上方表格的工具栏（使用 Lambda 表达式简化代码）
        ToolbarDecorator topDecorator = ToolbarDecorator.createDecorator(rightTopTable)
                .setAddAction(button -> addRightTopRow())
                .setRemoveAction(button -> removeRightTopRow())
                .setEditAction(button -> editRightTopRow());

        // 创建上方表格的容器面板，添加边框标题
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder("配置项"));
        topPanel.add(topDecorator.createPanel(), BorderLayout.CENTER);

        // ========== 创建右侧下方表格 ==========

        // rightBottomButtonGroup = new ButtonGroup(); // 预留的单选按钮组

        // 创建下方表格的数据模型
        rightBottomTableModel = new DefaultTableModel(new Object[]{"属性"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;  // 不可直接编辑
            }
        };

        // 创建下方表格
        rightBottomTable = new JBTable(rightBottomTableModel);
        rightBottomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rightBottomTable.setRowHeight(25);
        // 设置 Radio 按钮渲染器和编辑器
        rightBottomTable.getColumnModel().getColumn(0).setCellRenderer(new RadioButtonRenderer());
        rightBottomTable.getColumnModel().getColumn(0).setCellEditor(new RadioButtonEditor(new JCheckBox()));

        // 添加鼠标点击监听器
        rightBottomTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = rightBottomTable.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    selectRightBottomRow(row);  // 选中该行的 Radio 按钮
                }
            }
        });

        // 创建下方表格的工具栏
        ToolbarDecorator bottomDecorator = ToolbarDecorator.createDecorator(rightBottomTable)
                .setAddAction(button -> addRightBottomRow())
                .setRemoveAction(button -> removeRightBottomRow())
                .setEditAction(button -> editRightBottomRow());

        // 创建下方表格的容器面板，添加边框标题
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createTitledBorder("属性"));
        bottomPanel.add(bottomDecorator.createPanel(), BorderLayout.CENTER);

        // 将上下两个表格面板添加到右侧面板
        panel.add(topPanel);
        panel.add(bottomPanel);

        return panel;
    }

    // ========== 自定义渲染器和编辑器 ==========

    /**
     * Radio 按钮渲染器
     * 作用：在表格的每一行前面显示一个 Radio 按钮
     * 原理：继承 JRadioButton 并实现 TableCellRenderer 接口
     */
    class RadioButtonRenderer extends JRadioButton implements TableCellRenderer {

        RadioButtonRenderer() {
            // 设置 Radio 按钮左对齐
            setHorizontalAlignment(JLabel.LEFT);
        }

        /**
         * 渲染表格单元格
         * @param table 表格对象
         * @param value 单元格的值（文本内容）
         * @param isSelected 该行是否被表格选中（鼠标点击时的高亮状态）
         * @param hasFocus 该单元格是否获得焦点
         * @param row 行索引
         * @param column 列索引
         * @return 返回渲染后的组件（Radio 按钮）
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            // 设置 Radio 按钮的文本为单元格的值
            setText(value != null ? value.toString() : "");

            // 根据不同的表格，判断该行的 Radio 是否应该被选中
            if (table == leftTable) {
                // 左侧表格：检查当前行是否是选中的行
                setSelected(row == leftSelectedIndex);
            } else if (table == rightTopTable) {
                // 右侧上方表格：从 dataMap 中获取对应的数据，检查是否选中
                TableData data = dataMap.get(leftSelectedIndex);
                setSelected(data != null && row == data.topSelectedIndex);
            } else if (table == rightBottomTable) {
                // 右侧下方表格：从 dataMap 中获取对应的数据，检查是否选中
                TableData data = dataMap.get(leftSelectedIndex);
                setSelected(data != null && row == data.bottomSelectedIndex);
            }

            // 设置背景色和前景色（根据表格行是否被选中）
            if (isSelected) {
                setBackground(table.getSelectionBackground());  // 选中行的背景色
                setForeground(table.getSelectionForeground());  // 选中行的前景色
            } else {
                setBackground(table.getBackground());  // 正常行的背景色
                setForeground(table.getForeground());  // 正常行的前景色
            }

            return this;  // 返回 Radio 按钮组件
        }
    }

    /**
     * Radio 按钮编辑器
     * 作用：处理表格单元格的编辑事件（虽然实际上不可编辑，但需要编辑器来处理点击）
     */
    class RadioButtonEditor extends DefaultCellEditor {
        private JRadioButton button;

        RadioButtonEditor(JCheckBox checkBox) {
            super(checkBox);  // 调用父类构造函数
            button = new JRadioButton();
            button.setHorizontalAlignment(JLabel.LEFT);
        }

        /**
         * 获取编辑器组件
         * @return 返回 Radio 按钮组件
         */
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            button.setText(value != null ? value.toString() : "");
            return button;
        }
    }

    // ========== 选中行的处理方法 ==========

    /**
     * 选中左侧表格的某一行
     * 重要：这个方法会触发右侧表格内容的切换
     *
     * @param row 要选中的行索引
     */
    private void selectLeftRow(int row) {
        // 参数校验：行索引必须在有效范围内
        if (row < 0 || row >= leftTableModel.getRowCount()) {
            return;
        }

        // 步骤1：保存当前右侧表格的数据到 dataMap
        // 原因：切换左侧选项前，需要先保存当前右侧表格的修改
        saveCurrentTableData();

        // 步骤2：更新左侧表格的选中索引
        leftSelectedIndex = row;

        // 步骤3：重绘左侧表格，使 Radio 按钮的选中状态生效
        leftTable.repaint();

        // 步骤4：加载新选中行对应的右侧表格数据
        // 原因：不同的左侧选项有不同的右侧配置
        loadTableDataForRow(row);
    }

    /**
     * 选中右侧上方表格的某一行
     *
     * @param row 要选中的行索引
     */
    private void selectRightTopRow(int row) {
        // 如果左侧未选中任何行，或者行索引无效，则不处理
        if (leftSelectedIndex < 0 || row < 0) {
            return;
        }

        // 获取当前左侧选中行对应的数据对象
        TableData data = dataMap.get(leftSelectedIndex);
        if (data != null) {
            // 更新上方表格的选中索引
            data.topSelectedIndex = row;
            // 重绘表格，使 Radio 按钮的选中状态生效
            rightTopTable.repaint();
        }
    }

    /**
     * 选中右侧下方表格的某一行
     *
     * @param row 要选中的行索引
     */
    private void selectRightBottomRow(int row) {
        // 如果左侧未选中任何行，或者行索引无效，则不处理
        if (leftSelectedIndex < 0 || row < 0) {
            return;
        }

        // 获取当前左侧选中行对应的数据对象
        TableData data = dataMap.get(leftSelectedIndex);
        if (data != null) {
            // 更新下方表格的选中索引
            data.bottomSelectedIndex = row;
            // 重绘表格，使 Radio 按钮的选中状态生效
            rightBottomTable.repaint();
        }
    }

    // ========== 数据初始化和管理 ==========

    /**
     * 初始化示例数据
     * 作用：在第一次打开设置面板时，显示一些预设的数据
     */
    private void initSampleData() {
        // 添加左侧表格的示例数据（3 个选项）
        leftTableModel.addRow(new Object[]{"默认规则"});
        leftTableModel.addRow(new Object[]{"生成规则"});
        leftTableModel.addRow(new Object[]{"单测示例"});

        // 为每个左侧选项初始化对应的右侧表格数据
        for (int i = 0; i < 3; i++) {
            TableData data = new TableData();

            if (i == 0) {
                // "默认规则" 的数据
                data.topItems.add("配置1");
                data.topItems.add("配置2");
                data.bottomItems.add("属性A");
                data.topSelectedIndex = 0;  // 默认选中第一个配置项
            } else if (i == 1) {
                // "生成规则" 的数据
                data.topItems.add("生成配置1");
                data.bottomItems.add("生成属性A");
                data.bottomItems.add("生成属性B");
                data.bottomSelectedIndex = 0;  // 默认选中第一个属性
            } else {
                // "单测示例" 的数据
                data.topItems.add("测试配置1");
                data.bottomItems.add("测试属性A");
                // 注意：这里没有设置默认选中项
            }

            // 将数据存储到 dataMap 中
            dataMap.put(i, data);
        }

        // 默认选中左侧表格的第一行
        if (leftTableModel.getRowCount() > 0) {
            selectLeftRow(0);
        }
    }

    /**
     * 保存当前右侧表格的数据到 dataMap
     * 调用时机：切换左侧选项前 / 点击 Apply 按钮时
     */
    private void saveCurrentTableData() {
        // 如果左侧没有选中任何行，则无需保存
        if (leftSelectedIndex < 0) {
            return;
        }

        // 获取当前左侧选中行对应的数据对象
        TableData data = dataMap.get(leftSelectedIndex);
        if (data == null) {
            // 如果数据对象不存在，创建一个新的
            data = new TableData();
            dataMap.put(leftSelectedIndex, data);
        }

        // 保存右侧上方表格的数据
        data.topItems.clear();  // 清空旧数据
        for (int i = 0; i < rightTopTableModel.getRowCount(); i++) {
            // 逐行读取表格数据，添加到列表中
            data.topItems.add((String) rightTopTableModel.getValueAt(i, 0));
        }

        // 保存右侧下方表格的数据
        data.bottomItems.clear();  // 清空旧数据
        for (int i = 0; i < rightBottomTableModel.getRowCount(); i++) {
            // 逐行读取表格数据，添加到列表中
            data.bottomItems.add((String) rightBottomTableModel.getValueAt(i, 0));
        }
    }

    /**
     * 加载指定行对应的右侧表格数据
     * 调用时机：切换左侧选项时
     *
     * @param row 左侧表格的行索引
     */
    private void loadTableDataForRow(int row) {
        // 获取指定行对应的数据对象
        TableData data = dataMap.get(row);
        if (data == null) {
            // 如果数据对象不存在，创建一个新的（空数据）
            data = new TableData();
            dataMap.put(row, data);
        }

        // 清空右侧表格的当前数据
        rightTopTableModel.setRowCount(0);
        rightBottomTableModel.setRowCount(0);

        // 加载上方表格的数据
        for (String item : data.topItems) {
            rightTopTableModel.addRow(new Object[]{item});
        }

        // 加载下方表格的数据
        for (String item : data.bottomItems) {
            rightBottomTableModel.addRow(new Object[]{item});
        }

        // 刷新表格显示（使 Radio 按钮的选中状态生效）
        rightTopTable.repaint();
        rightBottomTable.repaint();
    }

    // ========== 左侧表格的增删改操作 ==========

    /**
     * 添加左侧表格的一行
     * 操作：弹出输入框，让用户输入选项名称
     */
    private void addLeftRow() {
        // 弹出输入对话框
        String name = JOptionPane.showInputDialog(mainPanel, "请输入选项名称:", "添加选项", JOptionPane.PLAIN_MESSAGE);

        // 如果用户输入了内容（不为空）
        if (name != null && !name.trim().isEmpty()) {
            // 在表格中添加新行
            leftTableModel.addRow(new Object[]{name.trim()});

            // 为新行创建一个空的数据对象
            dataMap.put(leftTableModel.getRowCount() - 1, new TableData());
        }
    }

    /**
     * 删除左侧表格的选中行
     * 操作：弹出确认对话框，确认后删除
     */
    private void removeLeftRow() {
        int selectedRow = leftTable.getSelectedRow();  // 获取当前选中的行

        if (selectedRow >= 0) {
            // 弹出确认对话框
            int confirm = Messages.showYesNoDialog(
                    mainPanel,
                    "确定要删除该选项吗？",
                    "确认删除",
                    Messages.getQuestionIcon()
            );

            // 如果用户点击了 "Yes"
            if (confirm == Messages.YES) {
                // 从表格中删除该行
                leftTableModel.removeRow(selectedRow);

                // 从 dataMap 中删除对应的数据
                dataMap.remove(selectedRow);

                // 重新映射 dataMap 的索引（因为删除了一行，后面的行索引都要减 1）
                Map<Integer, TableData> newMap = new HashMap<>();
                for (int i = 0; i < leftTableModel.getRowCount(); i++) {
                    // 如果当前行在被删除行之前，索引不变
                    // 如果当前行在被删除行之后，索引需要加 1（因为原来的数据在 dataMap 中）
                    if (dataMap.containsKey(i < selectedRow ? i : i + 1)) {
                        newMap.put(i, dataMap.get(i < selectedRow ? i : i + 1));
                    }
                }
                dataMap = newMap;  // 替换为新的 dataMap

                // 更新左侧选中索引
                if (leftSelectedIndex == selectedRow) {
                    // 如果删除的正好是当前选中的行，清空右侧表格
                    leftSelectedIndex = -1;
                    rightTopTableModel.setRowCount(0);
                    rightBottomTableModel.setRowCount(0);
                } else if (leftSelectedIndex > selectedRow) {
                    // 如果删除的行在当前选中行之前，选中索引减 1
                    leftSelectedIndex--;
                }

                // 重绘左侧表格
                leftTable.repaint();
            }
        } else {
            // 如果没有选中任何行，提示用户
            Messages.showWarningDialog(mainPanel, "请先选择一行!", "警告");
        }
    }

    /**
     * 编辑左侧表格的选中行
     * 操作：弹出输入框，显示当前值，让用户修改
     */
    private void editLeftRow() {
        int selectedRow = leftTable.getSelectedRow();  // 获取当前选中的行

        if (selectedRow >= 0) {
            // 获取当前行的值
            String currentName = (String) leftTableModel.getValueAt(selectedRow, 0);

            // 弹出输入对话框，并显示当前值
            String newName = (String) JOptionPane.showInputDialog(
                    mainPanel,
                    "请输入新的选项名称:",
                    "编辑选项",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    currentName  // 默认值
            );

            // 如果用户输入了新值（不为空）
            if (newName != null && !newName.trim().isEmpty()) {
                // 更新表格中的值
                leftTableModel.setValueAt(newName.trim(), selectedRow, 0);
                // 重绘表格
                leftTable.repaint();
            }
        } else {
            // 如果没有选中任何行，提示用户
            Messages.showWarningDialog(mainPanel, "请先选择一行!", "警告");
        }
    }

    // ========== 右侧上方表格的增删改操作 ==========

    /**
     * 添加右侧上方表格的一行
     * 前提：左侧必须已经选中了一个选项
     */
    private void addRightTopRow() {
        // 检查左侧是否已选中
        if (leftSelectedIndex < 0) {
            Messages.showWarningDialog(mainPanel, "请先在左侧选择一个选项!", "警告");
            return;
        }

        // 弹出输入对话框
        String value = JOptionPane.showInputDialog(mainPanel, "请输入配置项:", "添加配置", JOptionPane.PLAIN_MESSAGE);

        // 如果用户输入了内容
        if (value != null && !value.trim().isEmpty()) {
            // 在表格中添加新行
            rightTopTableModel.addRow(new Object[]{value.trim()});
            // 重绘表格
            rightTopTable.repaint();
        }
    }

    /**
     * 删除右侧上方表格的选中行
     */
    private void removeRightTopRow() {
        int selectedRow = rightTopTable.getSelectedRow();

        if (selectedRow >= 0) {
            // 从表格中删除该行
            rightTopTableModel.removeRow(selectedRow);

            // 更新 dataMap 中的选中索引
            TableData data = dataMap.get(leftSelectedIndex);
            if (data != null) {
                if (data.topSelectedIndex == selectedRow) {
                    // 如果删除的是选中的行，清除选中状态
                    data.topSelectedIndex = -1;
                } else if (data.topSelectedIndex > selectedRow) {
                    // 如果删除的行在选中行之前，选中索引减 1
                    data.topSelectedIndex--;
                }
            }

            // 重绘表格
            rightTopTable.repaint();
        } else {
            Messages.showWarningDialog(mainPanel, "请先选择一行!", "警告");
        }
    }

    /**
     * 编辑右侧上方表格的选中行
     */
    private void editRightTopRow() {
        int selectedRow = rightTopTable.getSelectedRow();

        if (selectedRow >= 0) {
            // 获取当前值
            String currentValue = (String) rightTopTableModel.getValueAt(selectedRow, 0);

            // 弹出输入对话框
            String newValue = (String) JOptionPane.showInputDialog(
                    mainPanel,
                    "请输入新的配置项:",
                    "编辑配置",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    currentValue
            );

            // 如果用户输入了新值
            if (newValue != null && !newValue.trim().isEmpty()) {
                // 更新表格中的值
                rightTopTableModel.setValueAt(newValue.trim(), selectedRow, 0);
                // 重绘表格
                rightTopTable.repaint();
            }
        } else {
            Messages.showWarningDialog(mainPanel, "请先选择一行!", "警告");
        }
    }

    // ========== 右侧下方表格的增删改操作 ==========

    /**
     * 添加右侧下方表格的一行
     * 前提：左侧必须已经选中了一个选项
     */
    private void addRightBottomRow() {
        // 检查左侧是否已选中
        if (leftSelectedIndex < 0) {
            Messages.showWarningDialog(mainPanel, "请先在左侧选择一个选项!", "警告");
            return;
        }

        // 弹出输入对话框
        String value = JOptionPane.showInputDialog(mainPanel, "请输入属性:", "添加属性", JOptionPane.PLAIN_MESSAGE);

        // 如果用户输入了内容
        if (value != null && !value.trim().isEmpty()) {
            // 在表格中添加新行
            rightBottomTableModel.addRow(new Object[]{value.trim()});
            // 重绘表格
            rightBottomTable.repaint();
        }
    }

    /**
     * 删除右侧下方表格的选中行
     */
    private void removeRightBottomRow() {
        int selectedRow = rightBottomTable.getSelectedRow();

        if (selectedRow >= 0) {
            // 从表格中删除该行
            rightBottomTableModel.removeRow(selectedRow);

            // 更新 dataMap 中的选中索引
            TableData data = dataMap.get(leftSelectedIndex);
            if (data != null) {
                if (data.bottomSelectedIndex == selectedRow) {
                    // 如果删除的是选中的行，清除选中状态
                    data.bottomSelectedIndex = -1;
                } else if (data.bottomSelectedIndex > selectedRow) {
                    // 如果删除的行在选中行之前，选中索引减 1
                    data.bottomSelectedIndex--;
                }
            }

            // 重绘表格
            rightBottomTable.repaint();
        } else {
            Messages.showWarningDialog(mainPanel, "请先选择一行!", "警告");
        }
    }

    /**
     * 编辑右侧下方表格的选中行
     */
    private void editRightBottomRow() {
        int selectedRow = rightBottomTable.getSelectedRow();

        if (selectedRow >= 0) {
            // 获取当前值
            String currentValue = (String) rightBottomTableModel.getValueAt(selectedRow, 0);

            // 弹出输入对话框
            String newValue = (String) JOptionPane.showInputDialog(
                    mainPanel,
                    "请输入新的属性:",
                    "编辑属性",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    currentValue
            );

            // 如果用户输入了新值
            if (newValue != null && !newValue.trim().isEmpty()) {
                // 更新表格中的值
                rightBottomTableModel.setValueAt(newValue.trim(), selectedRow, 0);
                // 重绘表格
                rightBottomTable.repaint();
            }
        } else {
            Messages.showWarningDialog(mainPanel, "请先选择一行!", "警告");
        }
    }

    // ========== Configurable 接口的其他方法 ==========

    /**
     * 检查设置是否被修改
     * 返回 true 时，Apply 和 OK 按钮会变为可用状态
     *
     * @return 是否被修改
     */
    @Override
    public boolean isModified() {
        // TODO: 可以在这里实现实际的修改检测逻辑
        // 例如：比较当前数据和保存的数据是否一致
        return false;
    }

    /**
     * 应用设置（保存数据）
     * 当用户点击 Apply 或 OK 按钮时调用
     */
    @Override
    public void apply() {
        // 步骤1：保存当前右侧表格的数据
        saveCurrentTableData();

        // 步骤2：实际的持久化保存逻辑（可选）
        // 例如：保存到 PropertiesComponent 或 PersistentStateComponent
        // PropertiesComponent.getInstance().setValue("mySettings", serializeData());

        // 步骤3：打印调试信息（实际应用中可以删除）
        System.out.println("=== 保存的数据 ===");
        System.out.println("左侧选中行: " + leftSelectedIndex);
        for (Map.Entry<Integer, TableData> entry : dataMap.entrySet()) {
            TableData data = entry.getValue();
            System.out.println("左侧行 " + entry.getKey() + ":");
            System.out.println("  上方选中: " + data.topSelectedIndex);
            System.out.println("  下方选中: " + data.bottomSelectedIndex);
        }
    }

    /**
     * 重置设置（恢复到保存的状态）
     * 当用户点击 Reset 按钮时调用
     */
    @Override
    public void reset() {
        // TODO: 实现重置逻辑
        // 例如：从持久化存储中重新加载数据
        // String data = PropertiesComponent.getInstance().getValue("mySettings");
        // deserializeData(data);
    }
}
