package com.roni.library.crashguard;

import org.junit.Assert;
import org.junit.Test;

public class CrashGuardConfigTest {

    @Test
    public void testDefaultConfigValues() {
        CrashGuardConfig config = CrashGuardConfig.defaultConfig();
        Assert.assertTrue(config.isEnabled());
        Assert.assertTrue(config.isDebugMode());
        Assert.assertTrue(config.isShowCrashActivity());
        Assert.assertFalse(config.isAutoRestart());
        Assert.assertTrue(config.isEnableBreadcrumbs());
        Assert.assertTrue(config.isFilterSensitiveData());
        Assert.assertTrue(config.getMaxStoredReports() > 0);
    }

    @Test
    public void testCustomBuilderSettings() {
        CrashGuardConfig config = CrashGuardConfig.newBuilder()
            .setEnabled(true)
            .setDebugMode(false)
            .setAutoRestart(true)
            .setRestartDelayMs(2000L)
            .setMaxStoredReports(15)
            .setMaxBreadcrumbs(30)
            .setFilterSensitiveData(false)
            .build();

        Assert.assertTrue(config.isEnabled());
        Assert.assertFalse(config.isDebugMode());
        Assert.assertTrue(config.isAutoRestart());
        Assert.assertEquals(2000L, config.getRestartDelayMs());
        Assert.assertEquals(15, config.getMaxStoredReports());
        Assert.assertEquals(30, config.getMaxBreadcrumbs());
        Assert.assertFalse(config.isFilterSensitiveData());
    }
}
