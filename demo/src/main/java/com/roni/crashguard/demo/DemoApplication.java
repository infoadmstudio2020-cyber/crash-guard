package com.roni.crashguard.demo;

import android.app.Application;
import android.util.Log;

import com.roni.library.crashguard.Breadcrumb;
import com.roni.library.crashguard.CrashGuard;
import com.roni.library.crashguard.CrashGuardConfig;
import com.roni.library.crashguard.CrashListener;
import com.roni.library.crashguard.CrashReport;

/**
 * Initializes CrashGuard with enterprise telemetry, custom keys, and crash callbacks.
 */
public class DemoApplication extends Application {

    private static final String TAG = "CrashGuardDemo";

    @Override
    public void onCreate() {
        super.onCreate();

        CrashGuardConfig config = CrashGuardConfig.newBuilder()
            .setEnabled(true)
            .setDebugMode(true)
            .setShowCrashActivity(true)
            .setRestartActivityClass(MainActivity.class)
            .setAutoRestart(false)
            .setMaxStoredReports(30)
            .setEnableBreadcrumbs(true)
            .setMaxBreadcrumbs(60)
            .setFilterSensitiveData(true)
            .setShowNotificationOnBackgroundCrash(true)
            .setCrashListener(new CrashListener() {
                @Override
                public void onCrash(Thread thread, Throwable throwable, CrashReport report) {
                    Log.w(TAG, "Crash intercepted by CrashListener callback on thread: " + thread.getName());
                    // Here developers can send report to Sentry, Firebase Crashlytics, or custom REST endpoint
                }
            })
            .build();

        CrashGuard.install(this, config);

        // Set contextual runtime diagnostics
        CrashGuard.setCustomKey("environment", "production-demo");
        CrashGuard.setCustomKey("user_tier", "enterprise");
        CrashGuard.setCustomKey("build_flavor", "demoRelease");

        CrashGuard.addBreadcrumb("DemoApplication launched and configured", Breadcrumb.Type.SYSTEM, "lifecycle");
    }
}
