# JVM 字符串加密与 ASM 插桩详解

## 1. 为什么要做字符串加密
字符串在 Java 字节码里通常以常量池形式出现，反编译或字符串扫描非常容易。字符串加密的目标是：

1. 提高静态分析成本
2. 降低关键字和密钥被“搜索到”的概率

这是一种反逆向手段，但不是绝对安全。

## 2. 字节码层面的字符串位置

### 2.1 常量池 + LDC
普通字符串字面量会被编译为常量池项，运行时通过 LDC 指令加载。

Java 代码：

```java
String x = "abc";
```

字节码大致是：

```
LDC "abc"
```

ASM 中体现为 LdcInsnNode，cst 为 String。

### 2.2 static final String 常量
`public static final String X = "abc";` 这类常量通常放在字段的 ConstantValue 属性里，而不是 LDC 中。

因此：

1. 字段自身持有常量值
2. 调用方类可能被编译器内联

这也是“成员变量字符串没加密”的根本原因。

## 3. 解决成员变量字符串加密的核心思路

### 3.1 处理 Field 常量值
ASM 中 FieldNode.value 就是 ConstantValue。可以识别：

1. `FieldNode.value instanceof String`
2. `FieldNode.desc == "Ljava/lang/String;"`

处理方式：

1. 置空字段常量值 `fn.value = null`
2. 在 `<clinit>` 中插入赋值逻辑：
   `field = decrypt(encryptedBytes, iv)`

这样常量值不会以明文存在于常量池里。

### 3.2 <clinit> 是什么
`<clinit>` 是类的静态初始化方法，JVM 在类加载时自动调用。

给 static 字段赋值，最稳妥的方式是写入 `<clinit>`。

## 4. 只加密指定包或注解

常见策略：

1. 包名匹配，只加密目标包
2. 注解控制，`@EncryptStrings` 和 `@NoStringEncrypt`
3. 排除类列表，避免破坏解密自身逻辑

这样比全项目加密更安全、更易排错。

## 5. 运行时解密策略

### 5.1 AES/GCM
AES/GCM 的优势：

1. 同时提供加密和完整性校验
2. 无需填充
3. 现代安全性更好

缺点：

1. 解密开销高于简单 XOR
2. 需要 IV

### 5.2 为什么不用 AES/CTR
CTR 更轻量，但没有校验，安全性低一档。

如果目标是提高反逆向难度，AES/GCM 更稳妥。

## 6. 加密流程（构建期）

1. 编译 class
2. ASM 遍历 class
3. 找到字符串字面量（LDC 或 Field.value）
4. 生成随机 IV
5. 用 AES/GCM 加密
6. 替换原字符串为解密调用
7. 输出修改后的 class

## 7. 解密流程（运行时）

调用：

```java
StringDecryptor.d(encryptedBytes, iv)
```

流程：

1. 解密 bytes
2. new String(bytes, UTF-8)

## 8. 为什么常量会被内联

Java 编译器会在编译期把 `static final String` 常量替换到调用方类。

因此：

1. 即使定义类加密了
2. 调用方类仍可能有明文

解决办法：

1. 不用 `static final`
2. 让调用方也进入加密范围

## 9. 常见风险点

### 9.1 反射和资源键
IntelliJ 插件大量使用字符串做 key：

1. Action ID
2. extension point
3. resource bundle key
4. ServiceLoader

这些字符串不应加密，否则会造成运行错误。

解决办法：

1. 用 `@NoStringEncrypt`
2. 排除特定包或类

### 9.2 性能影响
字符串解密会增加运行时开销，尤其是高频调用路径。

建议：

1. 只加密敏感字符串
2. 对热点路径加 `@NoStringEncrypt`

## 10. 安全性边界

字符串加密只能提高静态逆向成本，不是安全边界。攻击者仍可：

1. Hook 解密方法
2. Dump 内存
3. 逆向解密逻辑

真正的安全依赖：

1. 服务端校验
2. 安全的 API Key 管理

## 11. 调试建议

1. 只对一个类启用加密
2. 先测试加密后的 jar
3. 反编译对比是否明文还在
4. 若崩溃，检查是否加密了资源 key 或反射字符串

## 12. 你项目中的落地结构

1. 构建任务：`encryptStrings`
2. 输出目录：`$buildDir/encrypted-classes`
3. 解密类：`StringDecryptor`（构建时生成）
4. 注解控制：`@EncryptStrings` / `@NoStringEncrypt`
