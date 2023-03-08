/*
 * This file is part of eyeo Chromium SDK,
 * Copyright (C) 2006-present eyeo GmbH
 *
 * eyeo Chromium SDK is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * eyeo Chromium SDK is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with eyeo Chromium SDK.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.chromium.components.adblock;

import android.support.test.InstrumentationRegistry;
import android.util.Log;

import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.chromium.base.test.util.CallbackHelper;
import org.chromium.base.test.util.CommandLineFlags;
import org.chromium.base.test.util.DisabledTest;
import org.chromium.base.test.util.Feature;
import org.chromium.base.test.util.IntegrationTest;
import org.chromium.chrome.browser.flags.ChromeSwitches;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.test.ChromeBrowserTestRule;
import org.chromium.chrome.test.ChromeJUnit4ClassRunner;
import org.chromium.chrome.test.ChromeTabbedActivityTestRule;
import org.chromium.content_public.browser.test.util.JavaScriptUtils;
import org.chromium.content_public.browser.test.util.TestThreadUtils;
import org.chromium.content_public.common.ContentSwitches;
import org.chromium.net.test.EmbeddedTestServer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(ChromeJUnit4ClassRunner.class)
@CommandLineFlags.Add({ChromeSwitches.DISABLE_FIRST_RUN_EXPERIENCE,
        ContentSwitches.HOST_RESOLVER_RULES + "=MAP * 127.0.0.1"})
public class ResourceClassificationNotifierTest {
    @Rule
    public final ChromeBrowserTestRule mBrowserTestRule = new ChromeBrowserTestRule();
    @Rule
    public ChromeTabbedActivityTestRule mActivityTestRule = new ChromeTabbedActivityTestRule();
    private CallbackHelper mHelper = new CallbackHelper();
    public FilteringConfiguration mConfiguration;
    public TestAdBlockedObserver mAdBlockedObserver = new TestAdBlockedObserver();

    private EmbeddedTestServer mTestServer;
    private String mTestUrl;

    public void loadTestUrl() throws InterruptedException {
        mActivityTestRule.loadUrl(mTestUrl, 5);
    }

    @Before
    public void setUp() throws TimeoutException {
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            mConfiguration = new FilteringConfiguration("a");
            ResourceClassificationNotifier.getInstance().addOnAdBlockedObserver(mAdBlockedObserver);
            mHelper.notifyCalled();
        });
        mHelper.waitForCallback(0, 1, 10, TimeUnit.SECONDS);
        mActivityTestRule.startMainActivityOnBlankPage();
        mTestServer = EmbeddedTestServer.createAndStartServer(InstrumentationRegistry.getContext());
        mTestUrl = mTestServer.getURLWithHostName(
                "test.org", "/chrome/test/data/adblock/innermost_frame.html");
    }

    @After
    public void tearDown() {
        mTestServer.stopAndDestroyServer();
    }

    @Test
    @IntegrationTest
    @LargeTest
    @Feature({"adblock"})
    public void noNotificationWithoutBlocking() throws Exception {
        loadTestUrl();

        Assert.assertTrue(mAdBlockedObserver.blockedInfos.isEmpty());
        Assert.assertTrue(mAdBlockedObserver.allowedInfos.isEmpty());
        Assert.assertTrue(mAdBlockedObserver.allowedPageInfos.isEmpty());
    }

    @Test
    @IntegrationTest
    @LargeTest
    @Feature({"adblock"})
    public void resourceBlockedByFilter() throws Exception {
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            mConfiguration.addCustomFilter("resource.png");
            mHelper.notifyCalled();
        });
        mHelper.waitForCallback(0, 1, 10, TimeUnit.SECONDS);
        loadTestUrl();
        // Observer was notified about the blocking
        Assert.assertTrue(mAdBlockedObserver.isBlocked("resource.png"));
        Assert.assertTrue(mAdBlockedObserver.allowedInfos.isEmpty());
        Assert.assertTrue(mAdBlockedObserver.allowedPageInfos.isEmpty());
    }

    @Test
    @IntegrationTest
    @LargeTest
    @Feature({"adblock"})
    public void pageAllowed() throws Exception {
        TestThreadUtils.runOnUiThreadBlocking(() -> {
            mConfiguration.addCustomFilter("resource.png");
            mConfiguration.addAllowedDomain("test.org");
            mHelper.notifyCalled();
        });
        mHelper.waitForCallback(0, 1, 10, TimeUnit.SECONDS);
        loadTestUrl();
        // Observer was notified about the allowed resource
        Assert.assertTrue(mAdBlockedObserver.blockedInfos.isEmpty());
        Assert.assertTrue(mAdBlockedObserver.isAllowed("resource.png"));

        // TODO(mpawlowski): The observer could also be notified about the entire domain being
        // allowed: Assert.assertTrue(mAdBlockedObserver.isPageAllowed("test.org")); However this is
        // currently broken with multiple FilteringConfigurations (DPD-1729).
    }
}
