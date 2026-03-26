# Keep Kotlin metadata
-keepclassmembers class kotlin.Metadata { *; }

# Keep ViewBinding classes
-keepclassmembers class **Binding {
    public static ** inflate(...);
    public static ** bind(...);
}

# Keep launcher-related classes and models
-keep class com.nerf.launcher.** { *; }