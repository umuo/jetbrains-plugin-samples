package cn.lacknb.blog.kotlinlayoutdemo.toolwindow.tabs;

import com.intellij.ui.components.JBPanel;

import javax.swing.*;

/**
 * <h2> 盒子布局 </h2>
 *
 * @description:    把组件像堆积木一样，垂直（从上到下）或 水平（从左到右）单行排列。
 * @menu
 * @author: gitsilence
 * @description:
 * @date: 2026/1/3 10:50
 **/
public class BoxLayoutDemoPanel {

    public JPanel getPanel() {
        JBPanel<JBPanel<?>> panel = new JBPanel<>();

        // 设置布局为 BoxLayout
        // BoxLayout 需要绑定在一个具体的 panel 对象上，并指定方向
        // BoxLayout.Y_AXIS 表示垂直布局
        // BoxLayout.X_AXIS 表示水平布局
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(new JButton("Button 1"));
        // 增加一个 10px 的固定垂直间距
        panel.add(Box.createVerticalStrut(10));

        panel.add(new JButton("Button 2 (After 10 px gap)"));
        // 增加一个弹簧（Glue）, 它会占据所有剩余的空间
        // 这会导致下面的按钮被顶到最底部
        panel.add(Box.createVerticalGlue());
        panel.add(new JButton("I am at the bottom because of Glue."));

        return panel;
    }

}
