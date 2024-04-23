package org.ecosia.ntp;

import android.content.Context;

import org.chromium.base.test.BaseRobolectricTestRunner;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ApplicationProvider;
@RunWith(BaseRobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class TotalTreeCounterUnitTest {

    private static final long EXAMPLE_ABSOLUTE_TREE_COUNT = 204266700;
    private static final long EXPECTED_NUMBER = 200000000;

    private Context mContext;

    @Before
    public void setUp() throws InterruptedException {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testGetRawTreeCounterForOnboarding() {
        final TotalTreeCounter totalTreeCounter = new TotalTreeCounter(mContext);

        final long calculatedCount = totalTreeCounter.getRawTreeCounterForOnboarding(EXAMPLE_ABSOLUTE_TREE_COUNT);
        Assert.assertEquals(EXPECTED_NUMBER, calculatedCount);
    }
}
