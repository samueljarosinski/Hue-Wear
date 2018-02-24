### BuildConfig
-keep class io.github.samueljarosinski.huewear.BuildConfig { *; }

### Lines numbers
-keepattributes SourceFile, LineNumberTable, *Annotation*
-keep public class * extends java.lang.Exception

### LogCat
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** w(...);
    public static *** i(...);
    public static *** v(...);
}

### Android Support Libraries
-keep class android.support.v4.** { *; }
-keep interface android.support.v4.** { *; }

# Design
-dontwarn android.support.design.**
-keep class android.support.design.** { *; }
-keep interface android.support.design.** { *; }
-keep public class android.support.design.R$* { *; }

# AppComapt
-keep public class android.support.v7.widget.** { *; }
-keep public class android.support.v7.internal.widget.** { *; }
-keep public class android.support.v7.internal.view.menu.** { *; }

-keep public class * extends android.support.v4.view.ActionProvider {
    public <init>(android.content.Context);
}
