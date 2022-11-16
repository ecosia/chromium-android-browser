/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */


package org.chromium.chrome.browser.ui.default_browser_promo;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;

import org.chromium.components.browser_ui.widget.PromoDialog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The promo dialog guiding how to set Chrome as the default browser.
 */
public class EcosiaDefaultBrowserPromoDialog extends PromoDialog {

    // Ecosia: actions for the broadcast in DefaultBrowserActionReceiver
    public static final String DEFAULT_BROWSER_ACTION = "com.ecosia.DEFAULT_BROWSER_ACTION";
    public static final String DEFAULT_BROWSER_DATA = "com.ecosia.DEFAULT_BROWSER_DATA";
    public static final String DEFAULT_BROWSER_ACCEPT = "com.ecosia.DEFAULT_BROWSER_ACCEPT";
    public static final String DEFAULT_BROWSER_DISMISS = "com.ecosia.DEFAULT_BROWSER_DISMISS";
    public static final String DEFAULT_BROWSER_LAUNCH = "com.ecosia.DEFAULT_BROWSER_LAUNCH";

    @IntDef({DialogStyle.ROLE_MANAGER, DialogStyle.DISAMBIGUATION_SHEET,
            DialogStyle.SYSTEM_SETTINGS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DialogStyle {
        int ROLE_MANAGER = 0;
        int DISAMBIGUATION_SHEET = 1;
        int SYSTEM_SETTINGS = 2;
    }

    private final int mDialogStyle;
    private final Runnable mOnOK;
    private Runnable mOnCancel;
    private final boolean mIsRestricted;

    /**
     * Building a {@link EcosiaDefaultBrowserPromoDialog}.
     * @param activity The activity to display dialog.
     * @param dialogStyle The type of dialog.
     * @param onOK The {@link Runnable} on user's agreeing to change default.
     * @param onCancel The {@link Runnable} on user's refusing or dismissing the dialog.
     * @return
     */
    public static EcosiaDefaultBrowserPromoDialog createDialog(
            Activity activity, @DialogStyle int dialogStyle, Runnable onOK, Runnable onCancel, boolean isRestricted) {
        return new EcosiaDefaultBrowserPromoDialog(activity, dialogStyle, onOK, onCancel, isRestricted);
    }

    private EcosiaDefaultBrowserPromoDialog(
            Activity activity, @DialogStyle int style, Runnable onOK, Runnable onCancel, boolean isRestricted) {
        super(activity);
        mDialogStyle = style;
        mOnOK = onOK;
        mOnCancel = onCancel;
        mIsRestricted = isRestricted;
        setOnDismissListener(this);
        sendBroadcast(DEFAULT_BROWSER_LAUNCH);
        setDialogHeightToWrapContent();
    }

    @Override
    @VisibleForTesting
    public DialogParams getDialogParams() {
        DialogParams params = new DialogParams();

        Activity activity = getOwnerActivity();
        assert activity != null;

        params.drawableResource = R.drawable.default_promo;
        // Ecosia: change title and description
        params.headerCharSequence = activity.getString(R.string.default_browser_title);
        String desc;
        if (mIsRestricted) {
            desc = activity.getString(R.string.default_browser_desc_gm);
        } else {
            desc = activity.getString(R.string.default_browser_desc);
        }
        String steps;
        String primaryButtonText;
        if (mDialogStyle == DialogStyle.ROLE_MANAGER) {
            // Ecosia: change choose button text and steps
            steps = activity.getString(R.string.default_browser_steps_manager);
            primaryButtonText = activity.getString(R.string.default_browser_button_settings);
        } else {
            assert mDialogStyle == DialogStyle.SYSTEM_SETTINGS;
            // Ecosia: change choose button text and steps
            steps = activity.getString(R.string.default_browser_steps_disambiguation);
            primaryButtonText = activity.getString(R.string.default_browser_button_settings);
        }

        params.subheaderCharSequence = desc ;
        params.subheaderCharSequence2 = steps;
        params.primaryButtonCharSequence = primaryButtonText;
        // Ecosia: change dismiss button text
        params.secondaryButtonStringResource = R.string.default_browser_button_dismiss;
        params.primaryButtonBackgroundDrawableResource = R.drawable.default_browser_promo_bg;
        params.isPrimaryCheckShown = true;
        params.isSecondaryCheckShown = true;
        params.primaryDescIcon = org.chromium.chrome.browser.ui.default_browser_promo.R.drawable.ecosia_check;
        params.secondaryDescIcon = org.chromium.chrome.browser.ui.default_browser_promo.R.drawable.ecosia_check;
        params.themePrimaryBtn = org.chromium.chrome.browser.ui.default_browser_promo.R.style.EcosiaButtonStyle;
        params.themeSecondaryBtn = org.chromium.chrome.browser.ui.default_browser_promo.R.style.EcosiaDialogBtnStyle;

        return params;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        // Can be dismissed by pressing the back button.
        if (mOnCancel != null) {
            sendBroadcast(DEFAULT_BROWSER_DISMISS); // Ecosia: send dismiss broadcast
            mOnCancel.run();
        }
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        int id = view.getId();
        if (id == R.id.button_primary) {
            mOnCancel = null;
            mOnOK.run();
            dismiss();
        } else if (id == R.id.button_secondary) {
            if (mOnCancel != null) {
                sendBroadcast(DEFAULT_BROWSER_DISMISS); // Ecosia: send dismiss broadcast
                mOnCancel.run();
            }
            mOnCancel = null;
            dismiss();
        }
    }

    // Ecosia: send broadcasts for tracking from a different module
    public void sendBroadcast(String event) {
        Intent intent = new Intent(DEFAULT_BROWSER_ACTION);
        intent.putExtra(DEFAULT_BROWSER_DATA, event);
        getContext().getApplicationContext().sendBroadcast(intent);
    }
}
