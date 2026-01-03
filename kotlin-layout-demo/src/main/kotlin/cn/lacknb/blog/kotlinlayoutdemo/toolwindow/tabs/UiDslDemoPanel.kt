package cn.lacknb.blog.kotlinlayoutdemo.toolwindow.tabs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.lateinitVal

/**
 * <h2>  </h2>
 * @description:
 * @menu
 * @author: gitsilence
 * @description:
 * @date: 2026/1/3 09:30
 **/
class UiDslDemoPanel(private val project: Project) {

    fun createPanel(): DialogPanel {
        var userName = ""
        // 使用 lateinit 是因为我们在定义 panel 内部就要用到这个变量()闭包引用
        lateinit var myPanel: DialogPanel
        myPanel = panel {
            // 第一行 普通的 Label
            row {
                label("Welcome to Kotlin in DSL").bold()
            }

            // 分割线
            separator()

            // 表单行：两列布局
            row("UserName: ") {
                textField().bindText({ userName }, { userName = it })  // 双向绑定变量
                    .comment("Enter your name here.")
            }

            // 按钮行
            row {
                button("Submit") {
                    // 关键步骤 - 手动应用更改
                    myPanel.apply()
                    // 点击事件
                    println("User clicked submit with name: $userName")
                }
                checkBox("I agree to terms.")
            }

            // 折叠组
            group("Advanced Options") {
                row("API KEY: ") {
                    passwordField()
                }
            }
        }
        return myPanel
    }

}