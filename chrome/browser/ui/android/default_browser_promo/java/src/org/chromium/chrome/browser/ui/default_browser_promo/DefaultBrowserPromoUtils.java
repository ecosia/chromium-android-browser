// Copyright 2020 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.ui.default_browser_promo;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ResolveInfo;

import androidx.annotation.IntDef;

import org.chromium.chrome.browser.preferences.ChromePreferenceKeys;
import org.chromium.chrome.browser.preferences.SharedPreferencesManager;
import org.chromium.ui.base.WindowAndroid;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A utility class providing information regarding states of default browser.
 */
public class DefaultBrowserPromoUtils {
    @IntDef({DefaultBrowserState.ECOSIA_DEFAULT, DefaultBrowserState.NO_DEFAULT,
            DefaultBrowserState.OTHER_DEFAULT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DefaultBrowserState {
        int NO_DEFAULT = 0;
        int OTHER_DEFAULT = 1;
        /**
         * CHROME_DEFAULT means the currently running Chrome as opposed to
         * #isCurrentDefaultBrowserChrome() which looks for any Chrome.
         */
        int ECOSIA_DEFAULT = 2;
        int NUM_ENTRIES = 3;
    }

    /**
     * Determine whether a promo dialog should be displayed or not. And prepare related logic to
     * launch promo if a promo dialog has been decided to display.
     *
     * @param activity The context.
     * @param windowAndroid The {@link WindowAndroid} for sending an intent.
     * @param ignoreMaxCount Whether the promo dialog should be shown irrespective of whether it has
     *         been shown before
     * @return True if promo dialog will be displayed.
     */
    public static boolean prepareLaunchPromoIfNeeded(
            Activity activity, WindowAndroid windowAndroid, boolean ignoreMaxCount,
            boolean isRestricted /* Ecosia: check restricted google market (MOB-1858) */) {
        DefaultBrowserPromoDeps deps = DefaultBrowserPromoDeps.getInstance();
        if (!shouldShowPromo(deps, activity, ignoreMaxCount)) return false;
        deps.incrementPromoCount();

        deps.recordPromoTime();
        DefaultBrowserPromoManager manager = new DefaultBrowserPromoManager(
                activity, windowAndroid, deps.getCurrentDefaultBrowserState());
        manager.promoByRoleManager(isRestricted);   // Ecosia: check restricted google market (MOB-1858)
        return true;
    }

        /**
     * This decides whether the dialog should be promoed.
     * Returns false if any of following criteria is met:
     *      1. A promo dialog has been displayed before, unless {@code ignoreMaxCount} is true.
     *      2. Not enough sessions have been started before.
     *      3. Any chrome, including pre-stable, has been set as default.
     *      4. On Chrome stable while no default browser is set and multiple chrome channels
     *         are installed.
     *      5. Less than the promo interval if re-promoing.
     *      6. A browser other than chrome channel is default and default app setting is not
     *         available in the current system.
     */
    public static boolean shouldShowPromo(
            DefaultBrowserPromoDeps deps, Context context, boolean ignoreMaxCount) {
        if (!deps.isFeatureEnabled() || !deps.isRoleAvailable(context)) {
            return false;
        }
        // Criteria 1, 2, 5
        // Ecosia : Default browser promo shown to new users on launch or existing users on 3rd app launch
        if (!ignoreMaxCount
                && (deps.getPromoCount() >= deps.getMaxPromoCount()
                        || (deps.getSessionCount() > 1 && deps.getSessionCount() < deps.getMinSessionCount())
                        || deps.getLastPromoInterval() < deps.getMinPromoInterval())) {
            return false;
        }

        ResolveInfo info = deps.getDefaultWebBrowserActivityResolveInfo();
        if (info == null) {
            return false;
        }

        int state = deps.getCurrentDefaultBrowserState(info);
        if (state == DefaultBrowserState.ECOSIA_DEFAULT) { // Ecosia: Rebranding
            return false;
        }
        /* Ecosia : Bug fix where default browser pop up was not shown when
                    chrome browser is set as default
        else if (state == DefaultBrowserState.NO_DEFAULT) {
            // Criteria 4
            return !deps.isChromeStable() || !deps.isChromePreStableInstalled();
        } else { // other default
            // Criteria 3
            return !deps.isCurrentDefaultBrowserChrome(info);
        }
        */
        return true;
    }

    /**
     * Increment session count for triggering feature in the future.
     */
    public static void incrementSessionCount() {
        SharedPreferencesManager.getInstance().incrementInt(
                ChromePreferenceKeys.DEFAULT_BROWSER_PROMO_SESSION_COUNT);
    }

    public static int getDefaultBrowserSet() {
        return DefaultBrowserPromoDeps.getInstance().getCurrentDefaultBrowserState();
    }
}
