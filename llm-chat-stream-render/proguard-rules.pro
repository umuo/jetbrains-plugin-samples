-dontoptimize
-dontshrink
-dontpreverify
-dontwarn com.intellij.**
-dontwarn org.jetbrains.**

# Aggressive obfuscation (keep only what is required for IDE entry points).
-overloadaggressively
-useuniqueclassmembernames
-flattenpackagehierarchy a
-repackageclasses a
-adaptclassstrings
-renamesourcefileattribute obf

-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature

-keeppackagenames cn.lacknb.blog.llm.stream

# Classes referenced in plugin.xml must keep their names.
-keep class cn.lacknb.blog.llm.stream.LLMChatToolWindowFactory { *; }
-keep class cn.lacknb.blog.llm.stream.MethodActionLineMarkerProvider { *; }

# Obfuscate everything else, including member names.
