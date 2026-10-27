package com.roni.library.crashguard;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class CrashReportTest {

    @Test
    public void testSerializationAndDeserialization() {
        CrashReport report = new CrashReport();
        report.setExceptionClass("java.lang.NullPointerException");
        report.setExceptionMessage("Attempt to invoke virtual method on a null object reference");
        report.setStackTrace("java.lang.NullPointerException\n\tat com.roni.demo.MainActivity.onClick(MainActivity.java:42)");
        report.setThreadName("main");
        report.setThreadId(1);
        report.setMainThread(true);
        report.setAppPackage("com.roni.crashguard.demo");
        report.setAppVersionName("1.0.1");
        report.setDeviceModel("Pixel 8 Pro");
        report.setDeviceManufacturer("Google");

        JSONObject json = report.toJson();
        Assert.assertNotNull(json);

        CrashReport parsed = CrashReport.fromJson(json);
        Assert.assertNotNull(parsed);
        Assert.assertEquals(report.getReportId(), parsed.getReportId());
        Assert.assertEquals(report.getExceptionClass(), parsed.getExceptionClass());
        Assert.assertEquals(report.getExceptionMessage(), parsed.getExceptionMessage());
        Assert.assertEquals(report.getThreadName(), parsed.getThreadName());
        Assert.assertEquals(report.getAppPackage(), parsed.getAppPackage());
        Assert.assertEquals(report.getDeviceModel(), parsed.getDeviceModel());
        Assert.assertTrue(parsed.isMainThread());
    }

    @Test
    public void testStackTraceExtraction() {
        Throwable throwable = new IllegalArgumentException("Invalid test parameter provided");
        String trace = CrashReport.getStackTraceString(throwable);
        Assert.assertTrue(trace.contains("IllegalArgumentException"));
        Assert.assertTrue(trace.contains("Invalid test parameter provided"));
    }
}
