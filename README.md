# JetBrains 插件多模块示例仓库

本仓库使用 Gradle 多模块结构，包含两个可独立运行/打包的 JetBrains 插件示例。通过它你可以学习如何：
- 基于 org.jetbrains.intellij Gradle 插件开发与调试插件
- 在插件中注册 Action、工具窗口、行标记等功能
- 打包并发布插件

## 环境要求
- JDK: 11（本项目在 Java 11 下构建，子模块已声明 source/target 兼容性）
- Gradle: 使用仓库自带的 Gradle Wrapper 即可
- IntelliJ 平台版本: 2022.2（由子模块的 intellij.version 配置指定）
- 推荐开发 IDE: IntelliJ IDEA 2022.2+（Community 或 Ultimate 均可）

## 目录结构
- plugin-sample-01
  - 在编辑器右键菜单中新增一个“Show Greeting”动作，展示如何注册简单的 Action。
- plugin-sample-02（AI Coding Assistant）
  - 右侧工具窗口：简易的“AI Assistant”对话界面
  - 方法行标记：在 Java 方法左侧显示灯泡图标，并提供弹出操作组
  - Tools 菜单动作：动态编译并运行 JUnit 测试用例的演示对话框

## 快速开始
1. 克隆仓库
   git clone <your-repo-url>
   cd jetbrain-plugin-samples

2. 选择要运行的子模块启动沙箱 IDE（务必在子模块上执行）
   # 运行示例 01
   ./gradlew :plugin-sample-01:runIde

   # 运行示例 02（AI Coding Assistant）
   ./gradlew :plugin-sample-02:runIde

3. 离线运行（如需）
   ./gradlew :plugin-sample-02:runIde --offline

提示：不建议在根项目直接执行 runIde，否则可能尝试在多个子模块上同时启动 IDE。

## 如何体验各示例
- 示例 01（plugin-sample-01）
  - 在沙箱 IDE 中打开任意文本/Java 文件
  - 选中部分文本，右键选择“Show Greeting”
  - 将弹出对话框并显示你的选中文本

- 示例 02（plugin-sample-02，AI Coding Assistant）
  1) 工具窗口
     - 右侧边栏打开“AI Assistant”工具窗口
     - 可在输入框中输入文本并点击“发送”（演示 UI，无真实 AI 接入）
  2) 方法行标记与动作组
     - 打开一个 Java 文件，将光标置于某个方法上
     - 在方法左侧会看到一个灯泡图标，点击可弹出包含“解释代码/生成注释/生成单元测试”的操作菜单（演示逻辑，行为由 TODO 占位）
  3) 动态编译并运行 JUnit 测试
     - 菜单栏 Tools → Dynamic Compile Java Code
     - 在弹窗中粘贴/编写一个包含 @Test 方法的 JUnit4 测试类（默认示例已给出）
     - 点击“编译”，成功后会检测到 @Test 方法并激活“运行测试”按钮
     - 点击“运行测试”即可在 IDE 的 Run 窗口中执行该测试

注意：编译与运行在“当前打开项目”的模块与类路径下完成，请确保沙箱 IDE 中已打开一个标准的 Java 项目，且存在可写的源码根目录（例如 src/main/java）。

## 打包
- 在目标子模块上执行打包任务：
  ./gradlew :plugin-sample-01:buildPlugin
  ./gradlew :plugin-sample-02:buildPlugin

- 产物位置：<模块>/build/distributions/<plugin-name>-<version>.zip

打包前检查清单：
- plugin.xml/patchPluginXml 中的 id、version、sinceBuild、untilBuild 等已正确配置
- intellij {} 配置的 platform 版本与你的 since/untilBuild 兼容
- 能正常执行 :<module>:runIde
- 若使用 Kotlin，请在依赖中引入 kotlin-stdlib

## 常见问题与提示
- 运行哪个模块的沙箱 IDE？
  - 使用 Gradle 的限定路径语法 :<module>:runIde 指定模块；避免在根项目直接运行 runIde
- 平台版本与兼容范围
  - 本仓库示例使用 2022.2，对应 sinceBuild 通常为 222；如需更高版本，请同步调整 intellij.version 与 since/untilBuild
- 依赖插件
  - 示例根据需要声明了 'java'、'junit' 等依赖，请确保 plugin.xml 与 Gradle 配置一致

## 开发指引：新增一个插件子模块
1. 在 settings.gradle 中加入 include 'plugin-xxx'
2. 在新模块中添加 build.gradle，应用 org.jetbrains.intellij 插件并配置 intellij {}
3. 在 src/main/resources/META-INF 下创建 plugin.xml，注册你的扩展点与动作
4. 在 src/main/java 下编写你的插件代码
5. 使用 :plugin-xxx:runIde 进行本地调试

示例 build.gradle 关键片段：
plugins {
    id 'java'
    id 'org.jetbrains.intellij' version '1.15.0'
}

intellij {
    version.set("2022.2")
    plugins = ['java']
    updateSinceUntilBuild = false
}

tasks {
    patchPluginXml {
        sinceBuild = '222'
        untilBuild = '222.*'
    }
}

## 许可证
本仓库用于学习和演示，若要在生产中使用请根据你的实际需求完善配置与代码。
