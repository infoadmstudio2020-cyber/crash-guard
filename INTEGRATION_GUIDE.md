# CrashGuard Integration & Developer Guide 📖

This guide walks you through integrating the `CrashGuard` Android library into any existing or new Android application.

---

## 1. System Requirements

| Parameter | Value |
|---|---|
| Language | Java (17 or higher) or Kotlin |
| Minimum Android SDK (`minSdk`) | API 21 (Android 5.0 Lollipop) |
| Compile SDK (`compileSdk`) | API 36 (Android 16) |
| Target SDK (`targetSdk`) | API 36 |
| Android Gradle Plugin | 8.1.0+ (Tested on 8.10.1) |
| Gradle Version | 8.0+ (Configured for 8.13) |

---

## 2. Configuration Options Breakdown

`CrashGuardConfig.Builder` provides fine-grained control:

```java
CrashGuardConfig config = CrashGuardConfig.newBuilder()
    // Master switch to turn interception on/off
    .setEnabled(true)

    // Logs detailed diagnostics into logcat under "CrashGuard"
    .setDebugMode(true)

    // Whether to launch the crash screen on uncaught exceptions
    .setShowCrashActivity(true)

    // Optional: supply a custom Activity class to display instead of the default one
    .setCustomCrashActivityClass(MyCustomCrashActivity.class)

    // Optional: supply target activity to relaunch when the user taps 'Restart'
    .setRestartActivityClass(SplashActivity.class)

    // Automatically reboot the app without showing the crash UI
    .setAutoRestart(false)

    // Delay in milliseconds before restarting
    .setRestartDelayMs(1500L)

    // Maximum number of JSON reports kept in internal disk cache (FIFO pruning)
    .setMaxStoredReports(30)

    // Enable circular breadcrumb tracking
    .setEnableBreadcrumbs(true)

    // Number of recent breadcrumbs stored in ring-buffer
    .setMaxBreadcrumbs(50)

    // Masks passwords, JWTs, and authorization tokens in stack traces
    .setFilterSensitiveData(true)

    // Show high-priority status bar notification if background crash occurs
    .setShowNotificationOnBackgroundCrash(true)

    // Register callback for third-party forwarders (e.g. your backend, Slack webhook)
    .setCrashListener(new CrashListener() {
        @Override
        public void onCrash(Thread thread, Throwable throwable, CrashReport report) {
            // Forward report.toJson() to your telemetry server
        }
    })
    .build();

CrashGuard.install(this, config);
```

---

## 3. Handling Custom Recovery Screen

If you set `.setCustomCrashActivityClass(MyCustomCrashActivity.class)`:

Remember to declare your activity in your app's `AndroidManifest.xml` with `:crash_guard_process`:

```xml
<activity
    android:name=".MyCustomCrashActivity"
    android:process=":crash_guard_process"
    android:exported="false"
    android:launchMode="singleInstance"
    android:excludeFromRecents="true" />
```

Inside `MyCustomCrashActivity.onCreate`:
```java
CrashReport report = (CrashReport) getIntent().getSerializableExtra(DefaultCrashActivity.EXTRA_CRASH_REPORT);
if (report == null) {
    CrashReportStorage storage = new CrashReportStorage(this, 10);
    report = storage.getLatestReport();
}
```

---

## 4. ProGuard / R8 Rules

If your application enables code shrinking (`minifyEnabled true`), the library automatically bundles consumer rules:

```proguard
-keep class com.roni.library.crashguard.CrashGuard { *; }
-keep class com.roni.library.crashguard.CrashGuardConfig** { *; }
-keep class com.roni.library.crashguard.CrashReport { *; }
-keep class com.roni.library.crashguard.Breadcrumb { *; }
-keep class com.roni.library.crashguard.DefaultCrashActivity { *; }
-keep interface com.roni.library.crashguard.CrashListener { *; }
```

---

## 5. Security & Privacy Safeguards

CrashGuard respects user privacy:
- Redacts regex patterns matching `(?i)(password|secret|bearer)`
- Does not require any dangerous runtime permissions (`CAMERA`, `LOCATION`, `STORAGE` permissions are NOT needed)
- Reports are persisted to private app-internal sandbox storage (`context.getFilesDir()`)
