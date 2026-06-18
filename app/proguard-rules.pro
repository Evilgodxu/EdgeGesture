# 保留行号信息用于崩溃堆栈分析
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin Serialization（官方推荐规则）
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    static ** Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-dontnote kotlinx.serialization.**
-dontwarn kotlinx.serialization.internal.ClassValueReferences
-keepclassmembers public class **$$serializer {
    private ** descriptor;
}

# Koin 依赖注入
-keep class * extends org.koin.core.module.Module { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.* *;
}

# DataStore
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite* {
    <fields>;
}

# WorkManager
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# Shizuku AIDL/Binder IPC
-keep class com.byss.jh.service.** { *; }
-keep class * extends rikka.shizuku.** { *; }

# model 类
-keep class com.byss.jh.data.gesture.** { *; }
