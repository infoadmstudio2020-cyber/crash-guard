package com.roni.library.crashguard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread uncaught exception interceptor that captures diagnostic state,
 * persists reports, invokes listener callbacks, and routes to isolated crash UI.
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashGuard";

    private final Context context;
    private final CrashGuardConfig config;
    private final CrashReportStorage storage;
    private final BreadcrumbTracker breadcrumbTracker;
    private final Map<String, String> customData;
    private final Thread.UncaughtExceptionHandler defaultHandler;
    private final AtomicBoolean isHandlingCrash = new AtomicBoolean(false);

    public CrashHandler(Context context,
                        CrashGuardConfig config,
                        CrashReportStorage storage,
                        BreadcrumbTracker breadcrumbTracker,
                        Map<String, String> customData) {
        this.context = context.getApplicationContext();
        this.config = config;
        this.storage = storage;
        this.breadcrumbTracker = breadcrumbTracker;
        this.customData = customData;
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        if (!config.isEnabled()) {
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
            return;
        }

        // Prevent recursive loops if crash handling fails
        if (isHandlingCrash.compareAndSet(false, true)) {
            try {
                handleCrash(thread, throwable);
            } catch (Throwable handlerError) {
                Log.e(TAG, "Critical failure inside CrashGuard handler", handlerError);
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, throwable);
                }
            } finally {
                terminateCurrentProcess();
            }
        } else {
            // Already handling a crash on another thread
            terminateCurrentProcess();
        }
    }

    private void handleCrash(Thread thread, Throwable throwable) {
        boolean isMain = Looper.getMainLooper() != null && Looper.getMainLooper().getThread() == thread;

        CrashReport report = buildCrashReport(thread, throwable, isMain);

        if (config.isDebugMode()) {
            Log.e(TAG, "Intercepted uncaught exception on thread: " + thread.getName(), throwable);
        }

        // Persist to internal storage
        storage.saveReport(report);

        // Notify user callback
        if (config.getCrashListener() != null) {
            try {
                config.getCrashListener().onCrash(thread, throwable, report);
            } catch (Throwable listenerError) {
                Log.e(TAG, "Error in user CrashListener", listenerError);
            }
        }

        // If background crash and notification configured
        if (!isMain && config.isShowNotificationOnBackgroundCrash()) {
            CrashNotificationManager.showCrashNotification(context, report);
        }

        // Auto-restart or Launch Crash Activity
        if (config.isAutoRestart()) {
            executeAutoRestart();
        } else if (config.isShowCrashActivity()) {
            launchCrashActivity(report);
        }
    }

    private CrashReport buildCrashReport(Thread thread, Throwable throwable, boolean isMain) {
        CrashReport report = new CrashReport();
        report.setThreadName(thread != null ? thread.getName() : "unknown");
        report.setThreadId(thread != null ? thread.getId() : 0);
        report.setMainThread(isMain);

        String exClass = throwable != null ? throwable.getClass().getName() : "UnknownThrowable";
        String exMessage = throwable != null && throwable.getMessage() != null ? throwable.getMessage() : "";

        if (config.isFilterSensitiveData()) {
            exMessage = sanitizeSensitiveInfo(exMessage);
        }

        report.setExceptionClass(exClass);
        report.setExceptionMessage(exMessage);

        String stackTrace = CrashReport.getStackTraceString(throwable);
        if (config.isFilterSensitiveData()) {
            stackTrace = sanitizeSensitiveInfo(stackTrace);
        }
        report.setStackTrace(stackTrace);

        if (throwable != null && throwable.getCause() != null) {
            report.setCause(throwable.getCause().toString());
        }

        // Environment & Device metrics
        report.setAppPackage(context.getPackageName());
        report.setAppVersionName(DeviceUtils.getAppVersionName(context));
        report.setAppVersionCode(DeviceUtils.getAppVersionCode(context));

        report.setAndroidVersion(Build.VERSION.RELEASE);
        report.setSdkInt(Build.VERSION.SDK_INT);
        report.setDeviceManufacturer(Build.MANUFACTURER);
        report.setDeviceModel(Build.MODEL);
        report.setDeviceProduct(Build.PRODUCT);
        report.setRooted(DeviceUtils.isDeviceRooted());

        report.setFreeRamMb(DeviceUtils.getFreeRamMb(context));
        report.setTotalRamMb(DeviceUtils.getTotalRamMb(context));
        report.setFreeStorageMb(DeviceUtils.getAvailableInternalStorageMb());
        report.setTotalStorageMb(DeviceUtils.getTotalInternalStorageMb());

        if (breadcrumbTracker != null) {
            report.setBreadcrumbs(breadcrumbTracker.getAll());
        }

        if (customData != null) {
            report.setCustomData(customData);
        }

        return report;
    }

    private String sanitizeSensitiveInfo(String input) {
        if (input == null) return "";
        // Mask JWT tokens, passwords, bearer authorization tokens
        String sanitized = input.replaceAll("(?i)(password|passwd|pwd|secret)\\s*=\\s*[^&,\\s]+", "$1=***REDACTED***");
        sanitized = sanitized.replaceAll("(?i)Bearer\\s+[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*", "Bearer ***REDACTED***");
        return sanitized;
    }

    private void launchCrashActivity(CrashReport report) {
        Class<? extends Activity> targetClass = config.getCustomCrashActivityClass();
        if (targetClass == null) {
            targetClass = DefaultCrashActivity.class;
        }

        Intent intent = new Intent(context, targetClass);
        intent.putExtra(DefaultCrashActivity.EXTRA_CRASH_REPORT, report);
        if (config.getRestartActivityClass() != null) {
            intent.putExtra(DefaultCrashActivity.EXTRA_RESTART_ACTIVITY, config.getRestartActivityClass().getName());
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP);

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start crash activity", e);
        }
    }

    private void executeAutoRestart() {
        try {
            Intent restartIntent = null;
            if (config.getRestartActivityClass() != null) {
                restartIntent = new Intent(context, config.getRestartActivityClass());
            } else {
                restartIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            }

            if (restartIntent != null) {
                restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(restartIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed auto-restart", e);
        }
    }

    private void terminateCurrentProcess() {
        try {
            // Small grace period for disk sync
            Thread.sleep(150);
        } catch (InterruptedException ignored) {}

        Process.killProcess(Process.myPid());
        System.exit(10);
    }
}
