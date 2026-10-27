package com.roni.library.crashguard;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main public entry point for the CrashGuard Android crash defense and telemetry library.
 *
 * <p>Usage:</p>
 * <pre>
 * CrashGuard.install(this, CrashGuardConfig.newBuilder()
 *     .setDebugMode(true)
 *     .setAutoRestart(false)
 *     .setMaxStoredReports(20)
 *     .build());
 * </pre>
 */
public final class CrashGuard {

    private static final String TAG = "CrashGuard";

    private static volatile CrashGuard instance;
    private static final Object INIT_LOCK = new Object();

    private final Context context;
    private final CrashGuardConfig config;
    private final CrashReportStorage storage;
    private final BreadcrumbTracker breadcrumbTracker;
    private final Map<String, String> customData = new ConcurrentHashMap<>();
    private final CrashHandler crashHandler;

    private CrashGuard(Context context, CrashGuardConfig config) {
        this.context = context.getApplicationContext();
        this.config = config != null ? config : CrashGuardConfig.defaultConfig();
        this.storage = new CrashReportStorage(this.context, this.config.getMaxStoredReports());
        this.breadcrumbTracker = new BreadcrumbTracker(this.config.getMaxBreadcrumbs());

        this.crashHandler = new CrashHandler(
            this.context,
            this.config,
            this.storage,
            this.breadcrumbTracker,
            this.customData
        );

        // Register default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(this.crashHandler);

        // Seed initial breadcrumbs
        addBreadcrumb("CrashGuard initialized (v1.0.0)", Breadcrumb.Type.SYSTEM, "init");
    }

    /**
     * Initializes CrashGuard with default configuration settings.
     *
     * @param context Application or Activity context.
     * @return Initialized CrashGuard instance.
     */
    public static CrashGuard install(Context context) {
        return install(context, CrashGuardConfig.defaultConfig());
    }

    /**
     * Initializes CrashGuard with custom configuration options.
     *
     * @param context Application or Activity context.
     * @param config Custom CrashGuardConfig builder options.
     * @return Initialized CrashGuard instance.
     */
    public static CrashGuard install(Context context, CrashGuardConfig config) {
        if (instance == null) {
            synchronized (INIT_LOCK) {
                if (instance == null) {
                    if (context == null) {
                        throw new IllegalArgumentException("Context cannot be null when installing CrashGuard.");
                    }
                    instance = new CrashGuard(context, config);
                    Log.i(TAG, "CrashGuard initialized successfully.");
                }
            }
        }
        return instance;
    }

    /**
     * Returns the singleton instance if installed, or null.
     */
    public static CrashGuard getInstance() {
        return instance;
    }

    /**
     * Checks if CrashGuard has been installed in the current process.
     */
    public static boolean isInstalled() {
        return instance != null;
    }

    /**
     * Appends a user or diagnostic breadcrumb to the sliding window ring buffer.
     */
    public static void addBreadcrumb(String message, Breadcrumb.Type type, String category) {
        if (instance != null && instance.config.isEnableBreadcrumbs()) {
            instance.breadcrumbTracker.add(message, type, category);
        }
    }

    /**
     * Convenience method to append a user action breadcrumb.
     */
    public static void addBreadcrumb(String message) {
        addBreadcrumb(message, Breadcrumb.Type.USER_ACTION, "user");
    }

    /**
     * Sets a key-value diagnostic metadata tag to be included in subsequent crash reports.
     */
    public static void setCustomKey(String key, String value) {
        if (instance != null && key != null) {
            if (value == null) {
                instance.customData.remove(key);
            } else {
                instance.customData.put(key, value);
            }
        }
    }

    /**
     * Captures and stores a non-fatal, caught exception without terminating the application.
     */
    public static void logHandledException(Throwable throwable) {
        if (instance == null || throwable == null) return;

        CrashReport report = new CrashReport();
        report.setThreadName(Thread.currentThread().getName());
        report.setThreadId(Thread.currentThread().getId());
        report.setMainThread(Looper.getMainLooper() != null && Looper.getMainLooper().getThread() == Thread.currentThread());
        report.setExceptionClass(throwable.getClass().getName());
        report.setExceptionMessage("[HANDLED] " + (throwable.getMessage() != null ? throwable.getMessage() : ""));
        report.setStackTrace(CrashReport.getStackTraceString(throwable));

        report.setAppPackage(instance.context.getPackageName());
        report.setAppVersionName(DeviceUtils.getAppVersionName(instance.context));
        report.setAppVersionCode(DeviceUtils.getAppVersionCode(instance.context));
        report.setFreeRamMb(DeviceUtils.getFreeRamMb(instance.context));
        report.setTotalRamMb(DeviceUtils.getTotalRamMb(instance.context));
        report.setBreadcrumbs(instance.breadcrumbTracker.getAll());
        report.setCustomData(instance.customData);

        instance.storage.saveReport(report);
        addBreadcrumb("Logged handled exception: " + throwable.getClass().getSimpleName(), Breadcrumb.Type.SYSTEM, "error");
    }

    /**
     * Retrieves all saved crash reports persisted on disk.
     */
    public static List<CrashReport> getStoredReports() {
        if (instance != null) {
            return instance.storage.getAllReports();
        }
        return Collections.emptyList();
    }

    /**
     * Deletes a specific stored crash report.
     */
    public static boolean deleteReport(String reportId) {
        if (instance != null) {
            return instance.storage.deleteReport(reportId);
        }
        return false;
    }

    /**
     * Clears all crash reports from disk storage.
     */
    public static void clearReports() {
        if (instance != null) {
            instance.storage.clearAllReports();
        }
    }

    public CrashGuardConfig getConfig() {
        return config;
    }

    public CrashReportStorage getStorage() {
        return storage;
    }

    public BreadcrumbTracker getBreadcrumbTracker() {
        return breadcrumbTracker;
    }
}
