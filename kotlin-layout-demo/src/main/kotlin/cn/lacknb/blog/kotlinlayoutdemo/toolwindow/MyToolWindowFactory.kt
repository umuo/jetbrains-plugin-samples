package cn.lacknb.blog.kotlinlayoutdemo.toolwindow

import cn.lacknb.blog.kotlinlayoutdemo.toolwindow.tabs.BorderLayoutDemoPanel
import cn.lacknb.blog.kotlinlayoutdemo.toolwindow.tabs.BoxLayoutDemoPanel
import cn.lacknb.blog.kotlinlayoutdemo.toolwindow.tabs.FlowLayoutDemoPanel
import cn.lacknb.blog.kotlinlayoutdemo.toolwindow.tabs.GridBagLayoutDemoPanel
import cn.lacknb.blog.kotlinlayoutdemo.toolwindow.tabs.GridLayoutDemoPanel
import cn.lacknb.blog.kotlinlayoutdemo.toolwindow.tabs.UiDslDemoPanel
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory

/**
 * <h2>  </h2>
 * @description:
 * @menu
 * @author: gitsilence
 * @description:
 * @date: 2026/1/3 09:23
 **/
class MyToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow
    ) {
        // JBTabbedPane 用处： 创建多个Tab
        val tabs = JBTabbedPane()
        tabs.addTab("BorderLayout(Java)", BorderLayoutDemoPanel().panel)
        tabs.addTab("UI DSL (Kotlin)", UiDslDemoPanel(project).createPanel())
        tabs.addTab("Flow", FlowLayoutDemoPanel().panel)
        tabs.add("Box", BoxLayoutDemoPanel().panel)
        tabs.addTab("Grid", GridLayoutDemoPanel().panel)
        tabs.addTab("GridBag", GridBagLayoutDemoPanel().panel)

        // 将选项卡容器注入到 ToolWindow 中
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(tabs, "Dis", false)
        toolWindow.contentManager.addContent(content)
    }
}