-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Kotlin
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Koin DI
-keep class io.insert.koin.** { *; }

# DataStore
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite* {
    <fields>;
}

# WorkManager
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# Keep model classes
-keep class com.byss.jh.data.gesture.** { *; }

# Tests
-keep class * extends org.junit.** { *; }
-keep class androidx.test.** { *; }
