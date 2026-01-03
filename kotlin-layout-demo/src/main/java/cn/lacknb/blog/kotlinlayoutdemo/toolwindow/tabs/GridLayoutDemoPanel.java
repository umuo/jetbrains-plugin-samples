package cn.lacknb.blog.kotlinlayoutdemo.toolwindow.tabs;

import com.intellij.ui.components.JBPanel;

import javax.swing.*;
import java.awt.*;

/**
 * <h2> 网格布局 </h2>
 *
 * @description: 容器分割成大小相等的矩形网格。注意：所有单元格大小必须一样（取决于最大的那个组件）。
 * @menu
 * @author: gitsilence
 * @description:
 * @date: 2026/1/3 10:58
 **/
public class GridLayoutDemoPanel {

    public JPanel getPanel() {
        // 3行 2列, 垂直间间距为 5, 水平间距为 5
        JBPanel<JBPanel<?>> panel = new JBPanel<>(new GridLayout(3, 2, 5, 5));
        panel.add(new JLabel("Label 1"));
        panel.add(new JTextField("Text 1"));

        panel.add(new JLabel("Label 2"));
        panel.add(new JTextField("Text 2"));

        panel.add(new JButton("Cancel"));
        panel.add(new JButton("OK"));

        return panel;

    }

}
