package cn.lacknb.blog.kotlinlayoutdemo.toolwindow.tabs;

import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;

/**
 * <h2>  </h2>
 *
 * @description:
 * @menu
 * @author: gitsilence
 * @description:
 * @date: 2026/1/3 09:30
 **/
public class BorderLayoutDemoPanel {

    public JPanel getPanel() {
        JBPanel<JBPanel<?>> mainPanel = new JBPanel<>(new BorderLayout());

        // 1. 北 Top 通常放状态栏和确认按钮
        JButton topBtn = new JButton("Top (North) - Search Bar");
        mainPanel.add(topBtn, BorderLayout.NORTH);

        // 2. 南 Bottom 通常放状态栏和确认按钮
        JLabel bottomLabel = new JLabel("Bottom (South) - Status Area");
        // 水平对齐方式为居中对齐
        bottomLabel.setHorizontalAlignment(SwingConstants.CENTER);
        // 添加 蚀(shi)刻边框  - 过绘制凹陷或凸起的线条来增强组件的视觉层次
        bottomLabel.setBorder(BorderFactory.createEtchedBorder());
        mainPanel.add(bottomLabel, BorderLayout.SOUTH);

        // 3. 西 West 侧边栏
        mainPanel.add(new JButton("Left"), BorderLayout.WEST);

        // 4. 东 辅助栏
        mainPanel.add(new JButton("Right"), BorderLayout.EAST);

        // 5. 中: 主内容区(会自动填充剩余空间)
        JTextArea centerArea = new JTextArea("Center Area\n\nResize the window to see how I occupy all remaining space!");
        mainPanel.add(new JBScrollPane(centerArea), BorderLayout.CENTER);
        return mainPanel;
    }

}
