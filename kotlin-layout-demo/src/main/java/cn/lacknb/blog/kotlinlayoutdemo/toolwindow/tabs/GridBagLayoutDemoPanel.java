package cn.lacknb.blog.kotlinlayoutdemo.toolwindow.tabs;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * <h2>  </h2>
 *
 * @description:
 * @menu
 * @author: gitsilence
 * @description:
 * @date: 2026/1/3 11:10
 **/
public class GridBagLayoutDemoPanel {

    public JPanel getPanel() {
        JBPanel<JBPanel<?>> panel = new JBPanel<>(new GridBagLayout());

        // 创建 约束对象(遥控器)
        // 之后的每一个组件添加 都要修改这个对象
        GridBagConstraints c = new GridBagConstraints();

        // --- 全局设置 ----
        // inserts 设置组件之间的间距(上、左、下、右) 支持高分屏缩放
        c.insets = JBUI.insets(5);

        // anchor 当组件比格子小时，靠哪里对齐? (LINE_START 靠左)
        c.anchor = GridBagConstraints.LINE_START;

        c.gridx = 0;  // 第0列
        c.gridy = 0;  // 第0行
        c.weightx = 0.0;  // 权重为0：不想被拉伸，有多少字占多少地
        c.fill = GridBagConstraints.NONE;  // 不填充
        panel.add(new JBLabel("Hot Address: "), c);

        c.gridx = 1;  // 第1列
        c.gridy = 0;  // 第0行
        c.weightx = 1.0;  // 权重为1， 表示水平方向上多余的空白都归我
        c.fill = GridBagConstraints.HORIZONTAL;  // 水平拉伸 填满格子
        panel.add(new JBTextField("127.0.0.1"), c);

        c.gridx = 0;  // 第0列
        c.gridy = 1;  // 第1行
        c.weightx = 0.0;
        c.fill = GridBagConstraints.NONE;
        panel.add(new JBLabel("Port: "), c);

        c.gridx = 1;
        c.gridy = 1;
        // 这里不再设置 weightx 它沿用了上面的 1.0，所以也会被拉伸
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JBTextField("8080"), c);

        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0.0;
        c.gridwidth = 2;  // 我们要跨两列显示标题
        panel.add(new JBLabel("Configuration Details (Logs): "));

        // TextArea 带滚动条
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;  // 也是跨两列
        c.weightx = 1.0;  // 水平方向 拉伸权重
        c.weighty = 1.0;  // 垂直权重， 表示窗口拉高时，这个区域会变高
        c.fill = GridBagConstraints.BOTH;  // 水平和垂直都拉伸
        JTextArea area = new JTextArea("Connecting to server...\nWaiting for response...");
        panel.add(new JScrollPane(area), c);

        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        c.weighty = 0.0;  // 按钮不需要垂直拉伸，保持原高度
        c.fill = GridBagConstraints.NONE;  // 不要拉伸按钮
        c.anchor = GridBagConstraints.LINE_END;  // 靠右对齐
        panel.add(new JButton("Test connection"), c);
        return panel;
    }
}
