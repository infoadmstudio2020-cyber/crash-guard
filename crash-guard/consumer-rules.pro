# Consumer ProGuard rules for CrashGuard
# Retain public API methods and model classes for serialization
-keep class com.roni.library.crashguard.CrashGuard { *; }
-keep class com.roni.library.crashguard.CrashGuardConfig** { *; }
-keep class com.roni.library.crashguard.CrashReport { *; }
-keep class com.roni.library.crashguard.Breadcrumb { *; }
-keep class com.roni.library.crashguard.DefaultCrashActivity { *; }
-keep interface com.roni.library.crashguard.CrashListener { *; }
