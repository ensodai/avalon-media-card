# Project-specific R8/ProGuard rules.
#
# Keep this file minimal until release shrinking surfaces concrete issues.
# The default optimize config is already applied from the Android Gradle plugin.

# kotlinx-rpc
# Сохраняем аннотации и метаданные, необходимые для работы RPC и сериализации
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep @interface kotlinx.rpc.annotations.Rpc

# Сохраняем все интерфейсы, помеченные @Rpc, и их методы
-keep @kotlinx.rpc.annotations.Rpc interface * {
    *;
}

# Сохраняем сгенерированные дескрипторы и стабы (в 0.10.2 они часто имеют префикс __Rpc)
-keep class **Descriptor { *; }
-keep class **Stub { *; }
-keep class **__Rpc*Descriptor { *; }
-keep class **__Rpc*Stub { *; }

# Сохраняем все пакеты проекта, так как там лежат RPC сервисы, стабы, компаньоны и DTO
-keep class org.ensodai.avalonmediacard.** { *; }
-keep class org.ensodai.avalonmediacard.shared.** { *; }

# Сохраняем внутренние классы библиотеки для корректной регистрации сервисов
-keep class kotlinx.rpc.** { *; }

# Сохраняем Serializable классы, которые используются в RPC методах
-keep @kotlinx.serialization.Serializable class * {
    *;
}
-keepclassmembers class * {
    *** Companion;
}
-keepnames class kotlinx.serialization.json.** { *; }
