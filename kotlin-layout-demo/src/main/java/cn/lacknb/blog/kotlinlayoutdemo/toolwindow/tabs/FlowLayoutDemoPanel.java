package cn.lacknb.blog.kotlinlayoutdemo.toolwindow.tabs;

import com.intellij.ui.components.JBPanel;

import javax.swing.*;
import java.awt.*;

/**
 * <h2> 流式布局 </h2>
 *
 * @description: 组件像写文章的文字一样，从左到右排列，一行满了就换到下一行。
 * @menu
 * @author: gitsilence
 * @description:
 * @date: 2026/1/3 10:46
 **/
public class FlowLayoutDemoPanel {
    public JPanel getPanel() {
        // 创建一个 FlowLayout 布局, 左对齐, 水平和垂直间距为 20
        JBPanel<JBPanel<?>> panel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 20, 20));

        // 添加一堆按钮 看看效果
        for (int i = 0; i < 20; i++) {
            panel.add(new JButton("Button " + i));
        }
        panel.add(new JButton("I am a very long button to west mapping ."));
        return panel;
    }
}
