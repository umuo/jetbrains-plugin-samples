# Kotlin Layout Demo Plugin

这是一个JetBrains IntelliJ IDEA平台的插件，展示了多种布局管理器的使用方法，包括Java和Kotlin的实现方式。

## 功能特点

- 提供多种布局演示，包括BorderLayout、FlowLayout、BoxLayout、GridLayout、GridBagLayout等
- 展示了Java和Kotlin在IntelliJ插件开发中的混合使用
- 包含UI DSL (Kotlin) 演示，展示现代化的UI构建方式

## 插件信息

- **插件ID**: `cn.lacknb.blog.kotlin-layout-demo`
- **插件名称**: Kotlin-layout-demo
- **依赖**: `com.intellij.modules.platform`

## 布局演示

插件提供了以下布局管理器的演示:

1. **BorderLayout (Java)** - 使用Java实现的BorderLayout演示
2. **UI DSL (Kotlin)** - 使用Kotlin UI DSL的现代化UI构建方式
3. **Flow** - FlowLayout布局演示
4. **Box** - BoxLayout布局演示
5. **Grid** - GridLayout布局演示
6. **GridBag** - GridBagLayout布局演示

## UI DSL 特性

`UiDslDemoPanel.kt` 展示了Kotlin UI DSL的强大功能：

- 文本字段的双向数据绑定
- 动态表单构建
- 分组和折叠功能
- 交互式按钮和复选框

## 工具窗口配置

- **ID**: `myToolWindowFactory`
- **锚点**: `right` (右侧停靠)
- **类型**: 次要工具窗口 (`secondary="true"`)
- **图标**: `AllIcons.General.Modified`

## 安装与使用

1. 编译插件: `./gradlew buildPlugin`
2. 在IntelliJ IDEA中安装插件
3. 从右侧工具窗口栏打开 "myToolWindowFactory" 窗口
4. 在工具窗口中查看不同布局的演示

## 项目结构

```
src/main/
├── java/cn/lacknb/blog/kotlinlayoutdemo/toolwindow/tabs
│   ├── BorderLayoutDemoPanel.java
│   ├── BoxLayoutDemoPanel.java
│   ├── FlowLayoutDemoPanel.java
│   ├── GridBagLayoutDemoPanel.java
│   └── GridLayoutDemoPanel.java
├── kotlin/cn/lacknb/blog/kotlinlayoutdemo/toolwindow
│   ├── tabs
│   │   └── UiDslDemoPanel.kt
│   └── MyToolWindowFactory.kt
└── resources/META-INF
    └── plugin.xml
```

## 技术栈

- Java (用于传统布局演示)
- Kotlin (用于现代化UI DSL)
- IntelliJ Platform UI组件
- Gradle构建系统

## 开发

此插件演示了在IntelliJ插件开发中如何同时使用Java和Kotlin，并展示了不同的UI构建方法，为学习IntelliJ插件开发提供了很好的参考示例。