# RyNotes Proguard Rules

# Keep ViewModel classes as they are often accessed via reflection/factories
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# Keep DataStore preference keys if they were accessed via reflection (not the case here, but good practice)
-keepclassmembers class **.R$* {
    public static <fields>;
}

# R8 handles Compose well, but if you had custom LayoutInspector issues:
#-keep @androidx.compose.runtime.Composable class *
