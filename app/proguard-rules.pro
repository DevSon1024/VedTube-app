# ============================================================================
# VedTube Production ProGuard / R8 Rules
# ============================================================================

# Preserve line numbers and source files for meaningful crash stacktraces
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ----------------------------------------------------------------------------
# Kotlinx Serialization
# ----------------------------------------------------------------------------
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Serializable <fields>;
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keep class * implements kotlinx.serialization.KSerializer { *; }
-dontwarn kotlinx.serialization.**

# ----------------------------------------------------------------------------
# NewPipe Extractor & Rhino JavaScript Engine & Jsoup
# ----------------------------------------------------------------------------
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter { *; }
-dontwarn org.mozilla.javascript.**
-dontwarn org.mozilla.javascript.tools.**
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# ----------------------------------------------------------------------------
# AndroidX Media3 / ExoPlayer
# ----------------------------------------------------------------------------
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.datasource.** { *; }
-keep class androidx.media3.ui.** { *; }
-keep class androidx.media3.extractor.** { *; }
-keep class androidx.media3.decoder.** { *; }
-dontwarn androidx.media3.**

# ----------------------------------------------------------------------------
# Retrofit, OkHttp, Okio
# ----------------------------------------------------------------------------
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ----------------------------------------------------------------------------
# AndroidX Room & SQLite
# ----------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * implements androidx.sqlite.db.SupportSQLiteOpenHelper$Factory
-keep class * extends androidx.room.migration.Migration

# ----------------------------------------------------------------------------
# Coil Image Loader
# ----------------------------------------------------------------------------
-keep class coil.** { *; }
-dontwarn coil.**

# ----------------------------------------------------------------------------
# VedTube Models & DTOs
# ----------------------------------------------------------------------------
-keep class com.devson.vedtube.domain.model.** { *; }
-keep class com.devson.vedtube.core.database.model.** { *; }
-keep class com.devson.vedtube.data.model.** { *; }
-keep class com.devson.vedtube.data.provider.**.** { *; }
-keep class com.devson.vedtube.core.datastore.model.** { *; }