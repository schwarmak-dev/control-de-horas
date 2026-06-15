# Keep data classes used by Room
-keep class com.schwarmakdev.controldehoras.data.entity.** { *; }
-keep class com.schwarmakdev.controldehoras.data.dao.** { *; }
-keep class com.schwarmakdev.controldehoras.data.database.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
