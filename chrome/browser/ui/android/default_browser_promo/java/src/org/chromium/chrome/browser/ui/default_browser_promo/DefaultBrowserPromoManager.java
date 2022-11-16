// Copyright 2020 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.ui.default_browser_promo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.chromium.chrome.browser.ui.default_browser_promo.DefaultBrowserPromoUtils.DefaultBrowserState;
import org.chromium.ui.base.WindowAndroid;

import static org.chromium.chrome.browser.ui.default_browser_promo.EcosiaDefaultBrowserPromoDialog.DEFAULT_BROWSER_ACCEPT;
import static org.chromium.chrome.browser.ui.default_browser_promo.EcosiaDefaultBrowserPromoDialog.DEFAULT_BROWSER_DISMISS;

/**
 * Manage all types of default browser promo dialogs and listen to the activity state change to
 * trigger dialogs.
 */
public class DefaultBrowserPromoManager {
    private final Activity mActivity;
    private EcosiaDefaultBrowserPromoDialog mDialog;
    private @DefaultBrowserState int mCurrentState;
    private WindowAndroid mWindowAndroid;

    /**
     * @param activity Activity to show promo dialogs.
     * @param windowAndroid The {@link WindowAndroid} for sending an intent.
     * @param currentState The current {@link DefaultBrowserState} in the system.
     */
    public DefaultBrowserPromoManager(
            Activity activity, WindowAndroid windowAndroid, @DefaultBrowserState int currentState) {
        mActivity = activity;
        mWindowAndroid = windowAndroid;
        mCurrentState = currentState;
    }

    @SuppressLint({"WrongConstant", "NewApi"})
    void promoByRoleManager(boolean isRestricted) { // Ecosia: check restricted google market (MOB-1858)
        final int mDialogStyle;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mDialogStyle = EcosiaDefaultBrowserPromoDialog.DialogStyle.ROLE_MANAGER;
        } else {
            mDialogStyle = EcosiaDefaultBrowserPromoDialog.DialogStyle.SYSTEM_SETTINGS;
        }

        Runnable onOK = () -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager =
                        (RoleManager) mActivity.getSystemService(Context.ROLE_SERVICE);

                Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER);
                //Ecosia : Tracking event for default browser - send response on cancel in Browser options popup
                mWindowAndroid.showCancelableIntent(intent, (resultCode, data) -> {
                    if (mDialog != null) {
                        if (resultCode == 0) {
                            mDialog.sendBroadcast(DEFAULT_BROWSER_DISMISS);
                        } else {
                            mDialog.sendBroadcast(DEFAULT_BROWSER_ACCEPT);
                        }
                    }
                }, null);
                mDialog.dismiss();
            } else {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_APP_BROWSER);
                mActivity.startActivity(intent);

                if (DefaultBrowserPromoUtils.getDefaultBrowserSet() == DefaultBrowserState.ECOSIA_DEFAULT) {
                    mDialog.sendBroadcast(DEFAULT_BROWSER_ACCEPT);
                } else {
                    mDialog.sendBroadcast(DEFAULT_BROWSER_DISMISS);
                }
            }
        };
        showDialog(mDialogStyle, onOK, isRestricted);   // Ecosia: check restricted google market (MOB-1858)
    }

    private void showDialog(@EcosiaDefaultBrowserPromoDialog.DialogStyle int style, Runnable okCallback,
                            boolean isRestricted /* Ecosia: check restricted google market (MOB-1858) */) {
        mDialog = EcosiaDefaultBrowserPromoDialog.createDialog(mActivity, style, okCallback, () -> {
            mDialog.dismiss();
        }, isRestricted);   // Ecosia: check restricted google market (MOB-1858)
        mDialog.show();
    }
}
