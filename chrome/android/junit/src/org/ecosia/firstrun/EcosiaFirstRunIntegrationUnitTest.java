/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */


package org.ecosia.firstrun;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserManager;
import java.util.List;
import java.util.ArrayList;

import org.chromium.chrome.browser.firstrun.FirstRunActivity;
import org.chromium.chrome.browser.firstrun.TabbedModeFirstRunActivity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowApplication;

import org.chromium.base.test.BaseRobolectricTestRunner;
import org.chromium.chrome.browser.ChromeTabbedActivity;
import org.chromium.chrome.browser.ShortcutHelper;
import org.chromium.chrome.browser.document.ChromeLauncherActivity;
import org.chromium.chrome.browser.init.BrowserParts;
import org.chromium.chrome.browser.init.ChromeBrowserInitializer;
import org.chromium.chrome.browser.locale.LocaleManager;
import org.chromium.chrome.browser.searchwidget.SearchActivity;
import org.chromium.chrome.browser.webapps.WebappActivity;
import org.chromium.chrome.browser.webapps.WebappLauncherActivity;
import org.chromium.components.webapk.lib.client.WebApkValidator;
import org.chromium.webapk.lib.common.WebApkConstants;
import org.chromium.components.webapk.lib.common.WebApkMetaDataKeys;
import org.chromium.webapk.test.WebApkTestHelper;
import org.chromium.base.ContextUtils;
import org.chromium.chrome.R;

import android.util.Log;

import androidx.browser.customtabs.CustomTabsIntent;

/** JUnit tests for first run triggering code. */
@RunWith(BaseRobolectricTestRunner.class)
@Config(manifest = Config.NONE,
        shadows = {EcosiaFirstRunIntegrationUnitTest.MockChromeBrowserInitializer.class})
public final class EcosiaFirstRunIntegrationUnitTest {
    /** Do nothing version of {@link ChromeBrowserInitializer}. */
    @Implements(ChromeBrowserInitializer.class)
    public static class MockChromeBrowserInitializer {
        @Implementation
        public void __constructor__() {}

        @Implementation
        public void handlePreNativeStartup(final BrowserParts parts) {}
    }

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    private Context mContext;
    private ShadowApplication mShadowApplication;

    @Before
    public void setUp() throws InterruptedException {
        mContext = RuntimeEnvironment.application;
        mShadowApplication = ShadowApplication.getInstance();

        UserManager userManager = Mockito.mock(UserManager.class);
        Mockito.when(userManager.isDemoUser()).thenReturn(false);
        mShadowApplication.setSystemService(Context.USER_SERVICE, userManager);

        // TODO: FirstRunStatus.setFirstRunFlowComplete(false);
        WebApkValidator.setDisableValidationForTesting(true);
    }

    /** Checks that the intent component targets the passed-in class. */
    private boolean checkIntentComponentClass(Intent intent, Class componentClass) {
        return checkIntentComponentClassOneOf(intent, new Class[] {componentClass});
    }

    /** Checks that the intent component is one of the provided classes. */
    private boolean checkIntentComponentClassOneOf(Intent intent, Class[] componentClassOptions) {
        if (intent == null || intent.getComponent() == null) return false;

        String componentClassName = intent.getComponent().getClassName();
        for (Class componentClassOption : componentClassOptions) {
            if (componentClassOption.getName().equals(componentClassName)) return true;
        }
        return false;
    }

    /**
     * Checks that intent is either for {@link EcosiaFirstRunActivity}
     */
    private boolean checkIntentIsForFre(Intent intent) {
        return checkIntentComponentClassOneOf(
                intent, new Class[] {EcosiaFirstRunActivity.class,});
    }

    /** Builds activity using the component class name from the provided intent. */
    @SuppressWarnings("unchecked")
    private static void buildActivityWithClassNameFromIntent(Intent intent) {
        Class<? extends Activity> activityClass = null;
        try {
            activityClass =
                    (Class<? extends Activity>) Class.forName(intent.getComponent().getClassName());
        } catch (ClassNotFoundException e) {
            Assert.fail();
        }
        Robolectric.buildActivity(activityClass, intent).create();
    }

    /**
     * Checks that either {@link FirstRunActivity} or {@link TabbedModeFirstRunActivity}
     * was launched.
     */
    private void assertFirstRunActivityLaunched() {
        Intent launchedIntent = mShadowApplication.getNextStartedActivity();
        Assert.assertNotNull(launchedIntent);

        Assert.assertTrue(checkIntentIsForFre(launchedIntent));
    }

    @Ignore("Ecosia : TODO - verify the scenario if the test is valid")
    @Test
    public void testGenericViewIntentGoesToFirstRun() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://test.com"));
        intent.setPackage(mContext.getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity launcherActivity =
                Robolectric.buildActivity(ChromeLauncherActivity.class, intent).create().get();
        assertFirstRunActivityLaunched();
        Assert.assertTrue(launcherActivity.isFinishing());
    }

    @Test
    public void testRedirectCustomTabActivityToFirstRun() {
        CustomTabsIntent customTabIntent = new CustomTabsIntent.Builder().build();
        customTabIntent.intent.setPackage(mContext.getPackageName());
        customTabIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        customTabIntent.launchUrl(mContext, Uri.parse("http://test.com"));
        Intent launchedIntent = mShadowApplication.getNextStartedActivity();
        Assert.assertNotNull(launchedIntent);

        Activity launcherActivity =
                Robolectric.buildActivity(ChromeLauncherActivity.class, launchedIntent)
                        .create()
                        .get();
        assertFirstRunActivityLaunched();
        Assert.assertTrue(launcherActivity.isFinishing());
    }

    @Ignore("Ecosia : TODO - verify the scenario if the test is valid")
    @Test
    public void testRedirectChromeTabbedActivityToFirstRun() {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity tabbedActivity =
                Robolectric.buildActivity(ChromeTabbedActivity.class, intent).create().get();
        assertFirstRunActivityLaunched();
        Assert.assertTrue(tabbedActivity.isFinishing());
    }

    @Ignore("Ecosia : TODO - verify the scenario if the test is valid")
    @Test
    public void testRedirectSearchActivityToFirstRun() {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity searchActivity =
                Robolectric.buildActivity(SearchActivity.class, intent).create().get();
        assertFirstRunActivityLaunched();
        Assert.assertTrue(searchActivity.isFinishing());
    }
}
