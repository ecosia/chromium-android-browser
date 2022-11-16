package org.ecosia.tracking;

import android.content.Context;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import org.chromium.base.test.BaseRobolectricTestRunner;

@RunWith(BaseRobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public final class TrackingTest {

    private static final String SEARCH_URL = "https://www.ecosia.org/search?q=trees&addon=opensearch";
    private static final String REFERRER_URL = "utm_source=google-play&utm_medium=organic";
    private static final String INCOGNITO_PARSED_URL = "https://www.ecosia.org/search?q=trees&addon=opensearch" +
            "&utm_medium=organic&utm_source=google-play&_sp=00000000-0000-0000-0000-000000000000";

    private Context mContext;
    private TrackingManager mTrackingManager;

    @Before
    public void setUp() throws InterruptedException {
        mContext = RuntimeEnvironment.application;
        mTrackingManager = TrackingManager.getInstance(mContext);
        mTrackingManager.install(REFERRER_URL);
    }

    @Test
    public void testAnonymusUserIdForIncongito() {
        boolean isIncognito = true;
        String result = mTrackingManager.ecosify(SEARCH_URL, isIncognito);
        System.out.println("Result: " + result);
        Assert.assertEquals(result, INCOGNITO_PARSED_URL);
    }

    @Test
    public void testUserIdForNonIncognito() {
        boolean isIncognito = false;
        String result = mTrackingManager.ecosify(SEARCH_URL, isIncognito);
        System.out.println("Result: " + result);
        Assert.assertFalse(result.equals(INCOGNITO_PARSED_URL));
    }
}
