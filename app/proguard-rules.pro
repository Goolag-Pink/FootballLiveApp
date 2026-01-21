-dontwarn
-dontpreverify
-dontoptimize

-keep public class com.example.footballlive.MainActivity
-keep public class com.example.footballlive.PlayerActivity

-keep class com.google.gson.Gson { *; }
-keep class com.example.animeapp.models.HostListModel { *; }

-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class com.bumptech.glide.** { *; }
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule


# ExoPlayer Rules
# ==========================================
-keep class com.google.android.exoplayer2.** { *; }
-keepclassmembers class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**

# Keep ExoPlayer UI
-keep class com.google.android.exoplayer2.ui.** { *; }
-keep interface com.google.android.exoplayer2.ui.** { *; }

# Keep ExoPlayer's MediaSource classes
-keep class * implements com.google.android.exoplayer2.source.MediaSource { *; }

# Keep ExoPlayer's DataSource classes
-keep class * implements com.google.android.exoplayer2.upstream.DataSource { *; }


# OkHttp (if ExoPlayer uses it internally)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

