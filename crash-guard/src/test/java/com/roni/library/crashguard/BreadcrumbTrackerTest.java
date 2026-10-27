package com.roni.library.crashguard;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class BreadcrumbTrackerTest {

    @Test
    public void testRingBufferCapacityLimit() {
        BreadcrumbTracker tracker = new BreadcrumbTracker(5);

        for (int i = 1; i <= 10; i++) {
            tracker.add("Action " + i, Breadcrumb.Type.USER_ACTION, "test");
        }

        Assert.assertEquals(5, tracker.size());
        List<Breadcrumb> list = tracker.getAll();
        Assert.assertEquals(5, list.size());

        // First item should now be Action 6
        Assert.assertEquals("Action 6", list.get(0).getMessage());
        // Last item should be Action 10
        Assert.assertEquals("Action 10", list.get(4).getMessage());
    }

    @Test
    public void testClearTracker() {
        BreadcrumbTracker tracker = new BreadcrumbTracker(10);
        tracker.add("Test Breadcrumb", Breadcrumb.Type.NAVIGATION, "ui");
        Assert.assertEquals(1, tracker.size());

        tracker.clear();
        Assert.assertEquals(0, tracker.size());
    }
}
