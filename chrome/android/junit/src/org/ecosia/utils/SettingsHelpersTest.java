package org.ecosia.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.chromium.base.test.BaseRobolectricTestRunner;
import org.chromium.base.test.BaseJUnit4ClassRunner;

import org.robolectric.annotation.Config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(BaseRobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SettingsHelpersTest {

    private static final String ECOSIA_PACKAGE_NAME = "org.ecosia";

    @Mock
    private Context context;

    @Mock
    private PackageManager packageManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(context.getPackageName()).thenReturn(ECOSIA_PACKAGE_NAME);
        when(context.getPackageManager()).thenReturn(packageManager);
    }

    @Test
    public void testIsEcosiaDefaultBrowserHappyPath() {
        final ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = ECOSIA_PACKAGE_NAME;

        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = activityInfo;

        when(packageManager.resolveActivity(any(Intent.class), eq(PackageManager.MATCH_DEFAULT_ONLY))).thenReturn(resolveInfo);
        final boolean ecosiaIsDefaultBrowser = SettingsHelpers.isEcosiaDefaultBrowser(context, true);

        Assert.assertTrue(ecosiaIsDefaultBrowser);
    }

    @Test
    public void testEcosiaIsNotTheDefaultBrowserButRequired() {
        final ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = "some.other.browser";

        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = activityInfo;

        when(packageManager.resolveActivity(any(Intent.class), eq(PackageManager.MATCH_DEFAULT_ONLY))).thenReturn(resolveInfo);
        final boolean ecosiaIsDefaultBrowser = SettingsHelpers.isEcosiaDefaultBrowser(context, true);

        Assert.assertFalse(ecosiaIsDefaultBrowser);
    }

    @Test
    public void testEcosiaIsNeitherTheDefaultBrowserNorRequired() {
        final ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = "some.other.browser";

        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = activityInfo;

        when(packageManager.resolveActivity(any(Intent.class), eq(PackageManager.MATCH_DEFAULT_ONLY))).thenReturn(resolveInfo);
        final boolean ecosiaIsDefaultBrowser = SettingsHelpers.isEcosiaDefaultBrowser(context, false);

        Assert.assertFalse(ecosiaIsDefaultBrowser);
    }

    @Test
    public void testUnableToResolveTheDefaultBrowser() {
        // Activity info is null
        final ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = "org.browser.other.some";

        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = activityInfo;

        when(packageManager.resolveActivity(any(Intent.class), eq(PackageManager.MATCH_DEFAULT_ONLY))).thenReturn(resolveInfo);
        boolean ecosiaIsDefaultBrowser = SettingsHelpers.isEcosiaDefaultBrowser(context, true);
        Assert.assertFalse(ecosiaIsDefaultBrowser);

        // Resolver info is null
        final ResolveInfo nullResolverInfo = null;
        when(packageManager.resolveActivity(any(Intent.class), eq(PackageManager.MATCH_DEFAULT_ONLY))).thenReturn(nullResolverInfo);
        ecosiaIsDefaultBrowser = SettingsHelpers.isEcosiaDefaultBrowser(context, true);
        Assert.assertFalse(ecosiaIsDefaultBrowser);
    }
}
