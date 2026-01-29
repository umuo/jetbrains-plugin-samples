# 禁用混淆：只做复制，不改名
# -dontobfuscate

# ProGuard配置文件 - 用于混淆和优化代码
# 注意：此配置适用于JetBrains插件项目

# 禁用优化 - 避免对代码进行复杂优化，防止破坏功能
-dontoptimize
# 禁用压缩 - 保留所有类和成员，不删除任何未使用的代码
-dontshrink
# 禁用预验证 - 在某些平台上可提高兼容性
# -dontpreverify
# 明确告诉 ProGuard 目标 Java 版本 (IDEA 插件通常是 11, 17 或 21)
# 这能确保生成的 StackMapTable 格式是正确的
-target 17

# 告诉 ProGuard：虽然有警告，但请继续干活，不要停！
# -ignorewarnings

# 忽略常见的三方库警告（建议加上）
-dontwarn javax.annotation.**
-dontwarn sun.misc.**
-dontwarn java.lang.invoke.**
# 忽略警告 - 对于IntelliJ和JetBrains相关API不发出警告
-dontwarn com.intellij.**
-dontwarn org.jetbrains.**

# 激进混淆策略（仅保留IDE入口点所需的内容）
# 启用激进的方法重载，允许不同类中的方法具有相同名称
#-overloadaggressively


# 使用唯一的类成员名称，避免混淆时产生冲突
-useuniqueclassmembernames
# 将包层次结构扁平化到单个级别
#-flattenpackagehierarchy a
# 将类重新打包到指定的单级目录中
#-repackageclasses a
# 根据新的类名调整字符串常量池中的类名
-adaptclassstrings
# 将源文件属性重命名为"obf"，隐藏原始源文件名
-renamesourcefileattribute obf

# 现在的 IDEA 插件必须保留这个，否则 ComponentManager 无法实例化类
-keep class kotlin.Metadata { *; }

# --- 【核弹测试】 ---
# 暂时无条件保留你的包下所有类和成员，先确保能跑起来！
-keep class cn.lacknb.blog.llm.** { *; }



# 保留重要属性 - 保留注解、内部类、封闭方法、签名等信息
#-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature,Exceptions,SourceFile,LineNumberTable

# 保留特定包名 - 防止指定包的包名被混淆
#-keeppackagenames cn.lacknb.blog.llm.stream

# 插件入口点保护 - 以下类在plugin.xml中引用，必须保持原有名称和结构
# ToolWindow工厂类 - 提供LLM聊天窗口功能
# 修改后 (更强力的保留)：
-keep public class cn.lacknb.blog.llm.stream.LLMChatToolWindowFactory {
    public <init>();  # 显式保留无参构造函数
    *;                # 保留其他方法
}
# 行标记提供者类 - 为代码行添加动作标记
# 同样的，对 LineMarkerProvider 也做同样处理
-keep public class cn.lacknb.blog.llm.stream.MethodActionLineMarkerProvider {
    public <init>();
    *;
}
# 其他所有内容都将被混淆，包括类成员名称
# 这有助于减小最终插件包的大小并提供基本保护
