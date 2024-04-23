package org.ecosia.accessibility;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import org.chromium.chrome.browser.accessibility.AccessibilityTabHelper;

public class AccessibilityHelper {

    private static AccessibilityHelper sInstance;

    private final Context mContext;

    private AccessibilityHelper(final Context context) {
        mContext = context;
    }

    public static AccessibilityHelper getAccessibilityHelper(final Context context) {
        if (sInstance == null) {
            sInstance = new AccessibilityHelper(context);
        }
        return sInstance;
    }

    public boolean animationsEnabled() {
        boolean animationDurationIsZero = false;
        boolean transitionAnimationsScaleIsZero = false;
        boolean windowAnimationsScaleIsZero = false;

        try {
            final ContentResolver contentResolver = mContext.getContentResolver();
            animationDurationIsZero = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE) == 0f;
            transitionAnimationsScaleIsZero = Settings.Global.getFloat(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE) == 0f;
            windowAnimationsScaleIsZero = Settings.Global.getFloat(contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE) == 0f;
        } catch (Settings.SettingNotFoundException ignore) {
            return true;
        }

        return !(animationDurationIsZero && transitionAnimationsScaleIsZero && windowAnimationsScaleIsZero);
    }

}
