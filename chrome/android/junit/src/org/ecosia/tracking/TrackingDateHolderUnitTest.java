package org.ecosia.tracking;

import org.mockito.Mockito;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import org.chromium.base.test.BaseRobolectricTestRunner;

import java.util.Calendar;

@RunWith(BaseRobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class TrackingDateHolderUnitTest {

    @Test
    public void testSameDayComparison() {
        final Calendar calendar = Calendar.getInstance();
        final TrackingDateHolder now = new TrackingDateHolder(calendar);

        final long currentTimeMillis = calendar.getTimeInMillis();
        final long msAgo = 1000L * 60L * 60L * 5L; // 5 Hours
        calendar.setTimeInMillis(currentTimeMillis - msAgo);

        final TrackingDateHolder fiveHoursAgo = new TrackingDateHolder(calendar);
        final boolean atLeastOneDayLater = now.isAtLeastOneDayLaterAs(fiveHoursAgo);
        Assert.assertFalse(atLeastOneDayLater);
    }

    @Test
    public void testNextDayComparison() {
        final Calendar calendar = Calendar.getInstance();
        final TrackingDateHolder now = new TrackingDateHolder(calendar);

        final long currentTimeMillis = calendar.getTimeInMillis();
        final long msEarlier = 1000L * 60L * 60L * 36L; // 36 Hours
        calendar.setTimeInMillis(currentTimeMillis - msEarlier);

        final TrackingDateHolder thirtySixHoursAgo = new TrackingDateHolder(calendar);
        final boolean atLeastOneDayLater = now.isAtLeastOneDayLaterAs(thirtySixHoursAgo);
        Assert.assertTrue(atLeastOneDayLater);
    }

    @Test
    public void testNextYear() {
        final Calendar now = Mockito.mock(Calendar.class);
        Mockito.when(now.get(Calendar.YEAR)).thenReturn(2024);
        Mockito.when(now.get(Calendar.DAY_OF_YEAR)).thenReturn(42);
        final TrackingDateHolder february2024 = new TrackingDateHolder(now);

        final Calendar beginningOfNextYear = Mockito.mock(Calendar.class);
        Mockito.when(beginningOfNextYear.get(Calendar.YEAR)).thenReturn(2025);
        Mockito.when(beginningOfNextYear.get(Calendar.DAY_OF_YEAR)).thenReturn(2);
        final TrackingDateHolder january2025 = new TrackingDateHolder(beginningOfNextYear);

        final boolean atLeastOneYearLater = january2025.isAtLeastOneDayLaterAs(february2024);
        Assert.assertTrue(atLeastOneYearLater);
    }

}
