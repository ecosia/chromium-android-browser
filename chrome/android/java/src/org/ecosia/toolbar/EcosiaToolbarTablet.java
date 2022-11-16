/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.ecosia.toolbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import org.chromium.base.ApiCompatibilityUtils;
import org.chromium.base.Log;
import org.chromium.chrome.browser.ui.theme.BrandedColorScheme;
import org.chromium.components.adblock.AdblockController;
import org.chromium.chrome.browser.app.ChromeActivity;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.toolbar.top.ToolbarTablet;
import org.chromium.components.adblock.settings.EcosiaAdblockPreferences;
import org.chromium.components.embedder_support.util.UrlConstants;
import org.chromium.url.GURL;
import org.ecosia.adblock.AdblockButton;
import org.ecosia.adblock.AdblockPopupWindow;

public class EcosiaToolbarTablet extends ToolbarTablet {

    private static final String TAG = EcosiaToolbarTablet.class.getSimpleName();
    private final Context mContext;
    protected @Nullable AdblockButton mAdblockButton;
    private boolean mAdblockerIsEnabled;
    private AdblockPopupWindow mPopupWindow;
    private boolean isAdblockInitialized;

    public EcosiaToolbarTablet(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        isAdblockInitialized = false;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mAdblockButton = findViewById(org.chromium.chrome.R.id.adblock_button);
    }

    @Override
    public void onNativeLibraryReady() {
        super.onNativeLibraryReady();
        if (mAdblockButton != null) {
            mAdblockButton.setOnClickListener(this);
            mPopupWindow = new AdblockPopupWindow(mContext);
        }
        isAdblockInitialized = true;
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        if (mAdblockButton != null && mAdblockButton == v) {
            mPopupWindow.showAsDropdown(mAdblockButton);
        }
    }

    private void updateAdblockIcon() {
        int adblockButtonIcon;
        if (AdblockController.getInstance().isEnabled()) {
            adblockButtonIcon =  org.chromium.chrome.R.drawable.adblocker_button;
            mAdblockerIsEnabled = true;
        } else {
            adblockButtonIcon = org.chromium.chrome.R.drawable.adblocker_button_pause;
            mAdblockerIsEnabled = false;
        }
        mAdblockButton.setImageDrawable(ContextCompat.getDrawable(mContext, adblockButtonIcon));
    }

    private void refreshPage() {
        Tab activityTab = ((ChromeActivity) getContext()).getActivityTab();
        if (activityTab != null) {
            activityTab.reload();
        }
    }

    @Override
    public void onTintChanged(ColorStateList tint, @BrandedColorScheme int brandedColorScheme) {
        super.onTintChanged(tint, brandedColorScheme);
        if (mAdblockButton != null) {
            ImageViewCompat.setImageTintList(mAdblockButton, tint);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (isAdblockInitialized && adblockerStatusChanged()) {
            if (shouldShowAdblockerButton()) {
                showAdblockerButton();
            } else {
                hideAdblockerButton();
            }
            updateAdblockIcon();
            refreshPage();
        }
    }

    private boolean adblockerStatusChanged() {
        boolean previousState = mAdblockerIsEnabled;
        boolean currentState = previousState;
        try {
            currentState = AdblockController.getInstance().isEnabled();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return previousState != currentState;
    }

    @Override
    public void updateButtonVisibility() {
        if (isNtp()) {
            removeHomeButton();
        } else {
            addHomeButton();
        }
    }

    private void removeHomeButton() {
        if (mHomeButton != null) {
            mHomeButton.setVisibility(GONE);
        }
    }

    private void addHomeButton() {
        if (mHomeButton != null) {
            mHomeButton.setVisibility(VISIBLE);
        }
    }

    private boolean isNtp() {
        Tab activityTab = ((ChromeActivity) getContext()).getActivityTab();
        if (activityTab != null) {
            GURL url = activityTab.getUrl();
            return url.equals(new GURL(UrlConstants.NTP_URL));
        }
        return false;
    }

    private boolean shouldShowAdblockerButton() {
        return EcosiaAdblockPreferences.getAdblockButtonVisibility(getContext());
    }

    private void showAdblockerButton() {
        if (mAdblockButton != null) {
            mAdblockButton.setVisibility(VISIBLE);
        }
    }

    private void hideAdblockerButton() {
        if (mAdblockButton != null) {
            mAdblockButton.setVisibility(GONE);
        }
    }
}
