# CrashGuard 🛡️
> Production-Ready Android Crash Interceptor, Process Isolation & Telemetry Library in Pure Java

[![JitPack](https://jitpack.io/v/infoadmstudio2020-cyber/crash-guard.svg)](https://jitpack.io/#infoadmstudio2020-cyber/crash-guard)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)

---

## 🌟 Key Features

- **Isolated Process Crash Screen (`:crash_guard_process`)**:
  When your main application thread crashes or suffers memory exhaustion, CrashGuard launches the diagnostic activity in an independent Linux process so Android does not kill the crash dialog.
- **Pure Java 17 (Zero Kotlin Overhead)**:
  Ultra-lightweight, 100% pure Java codebase without heavy runtime dependencies or Kotlin stdlib requirements.
- **Ring-Buffer Breadcrumbs Tracking**:
  Captures a configurable sliding window (default 50) of navigation changes, user taps, network calls, and lifecycle events leading up to the failure.
- **Comprehensive Hardware & Environment Telemetry**:
  Automatically extracts device manufacturer, model, free/total RAM, available internal flash storage, root status, OS release, and SDK level.
- **Sensitive Data Redaction**:
  Automated regex scrubbing filters out JWT tokens, Bearer authorizations, passwords, and sensitive keys from stack traces and messages.
- **Persistent Disk Storage**:
  Thread-safe FIFO JSON storage retains up to *N* historical crash reports with eviction safeguards.
- **Customizable UI & Callbacks**:
  Support for custom branded crash activities, auto-restart triggers, background notification alerts, and external forwarders (Sentry, Crashlytics, custom REST endpoints).

---

## 📦 Gradle Dependency Coordinates

### 1. Add the JitPack repository
In your root `settings.gradle` (or `settings.gradle.kts`):

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### 2. Add the dependency to your module `build.gradle`
```groovy
dependencies {
    implementation 'com.github.infoadmstudio2020-cyber:crash-guard:1.0.0'
}
```

Or with Kotlin DSL (`build.gradle.kts`):
```kotlin
dependencies {
    implementation("com.github.infoadmstudio2020-cyber:crash-guard:1.0.0")
}
```

---

## 🚀 Quick Start Guide

### Step 1: Initialize in your `Application` class

```java
package com.example.app;

import android.app.Application;
import com.roni.library.crashguard.CrashGuard;
import com.roni.library.crashguard.CrashGuardConfig;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // One-line default initialization:
        CrashGuard.install(this);

        // Or with fluent configuration:
        CrashGuardConfig config = CrashGuardConfig.newBuilder()
            .setEnabled(true)
            .setDebugMode(true)
            .setShowCrashActivity(true)
            .setAutoRestart(false)
            .setMaxStoredReports(25)
            .setEnableBreadcrumbs(true)
            .setMaxBreadcrumbs(50)
            .setFilterSensitiveData(true)
            .setShowNotificationOnBackgroundCrash(true)
            .build();

        CrashGuard.install(this, config);
    }
}
```

### Step 2: Register in `AndroidManifest.xml`

```xml
<application
    android:name=".MyApplication"
    android:allowBackup="true"
    android:theme="@style/Theme.AppCompat.Light">
    ...
</application>
```

---

## 🛠️ Advanced Telemetry & Breadcrumbs

### Log User Breadcrumbs
```java
// Record custom user or navigation breadcrumb
CrashGuard.addBreadcrumb("User tapped checkout button", Breadcrumb.Type.USER_ACTION, "checkout");
CrashGuard.addBreadcrumb("Navigated to CartFragment", Breadcrumb.Type.NAVIGATION, "ui");
CrashGuard.addBreadcrumb("GET /api/v1/cart HTTP/1.1 -> 200 OK", Breadcrumb.Type.NETWORK, "api");
```

### Attach Custom Metadata
```java
CrashGuard.setCustomKey("user_id", "usr_9918237");
CrashGuard.setCustomKey("plan", "premium");
CrashGuard.setCustomKey("environment", "production");
```

### Log Handled (Non-Fatal) Exceptions
```java
try {
    performDangerousOperation();
} catch (Exception e) {
    CrashGuard.logHandledException(e);
}
```

### Inspect Stored Historical Reports
```java
List<CrashReport> reports = CrashGuard.getStoredReports();
for (CrashReport report : reports) {
    Log.d("CrashGuard", report.getFormattedDate() + " - " + report.getExceptionClass());
}

// Clear all reports
CrashGuard.clearReports();
```

---

## 🔬 Project Architecture & Modules

```
CrashGuard/
├── crash-guard/                     # Android Library Module (AAR)
│   ├── src/main/AndroidManifest.xml # Declares DefaultCrashActivity in :crash_guard_process
│   ├── src/main/java/com/roni/library/crashguard/
│   │   ├── CrashGuard.java          # Public API Singleton
│   │   ├── CrashGuardConfig.java    # Fluent Builder Configuration
│   │   ├── CrashHandler.java        # UncaughtExceptionHandler implementation
│   │   ├── CrashReport.java         # Diagnostic Data Model (JSON serializable)
│   │   ├── CrashReportStorage.java  # Persistent FIFO disk storage
│   │   ├── BreadcrumbTracker.java   # Thread-safe ring buffer
│   │   ├── Breadcrumb.java          # Breadcrumb model
│   │   ├── DefaultCrashActivity.java# Isolated recovery UI
│   │   ├── CrashNotificationManager.java # Background crash notification
│   │   └── DeviceUtils.java         # Hardware, memory & root inspection
│   └── src/test/                    # Automated JUnit tests
│
├── demo/                            # Standalone Demo Application Module
│   ├── src/main/java/com/roni/crashguard/demo/
│   │   ├── DemoApplication.java     # Configures CrashGuard
│   │   ├── MainActivity.java        # Interactive Crash Triggers & Tests
│   │   ├── CustomCrashActivity.java # Custom recovery screen example
│   │   └── ReportsHistoryActivity.java # Stored reports viewer
│
├── .github/workflows/               # GitHub Actions CI/CD
├── jitpack.yml                      # JitPack OpenJDK 17 build specification
└── LICENSE                          # Apache-2.0 License
```

---

## 📄 License

```
Copyright 2026 MD Roni Hossain (infoadmstudio2020-cyber)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
