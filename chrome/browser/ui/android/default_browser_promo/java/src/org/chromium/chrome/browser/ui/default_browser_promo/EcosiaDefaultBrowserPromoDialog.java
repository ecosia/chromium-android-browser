/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.chromium.chrome.browser.ui.default_browser_promo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import org.chromium.ui.base.WindowAndroid;

/**
 * The promo dialog guiding how to set Chrome as the default browser.
 */
public class EcosiaDefaultBrowserPromoDialog {

    // Actions for the broadcast in DefaultBrowserActionReceiver
    public static final String DEFAULT_BROWSER_ACTION = "com.ecosia.DEFAULT_BROWSER_ACTION";
    public static final String DEFAULT_BROWSER_DATA = "com.ecosia.DEFAULT_BROWSER_DATA";
    public static final String DEFAULT_BROWSER_ACCEPT = "com.ecosia.DEFAULT_BROWSER_ACCEPT";
    public static final String DEFAULT_BROWSER_DISMISS = "com.ecosia.DEFAULT_BROWSER_DISMISS";
    public static final String DEFAULT_BROWSER_LAUNCH = "com.ecosia.DEFAULT_BROWSER_LAUNCH";

    private final AlertDialog mDialog;
    private final Context mContext;
    private Runnable mOnAcceptCallback;
    private boolean mAccepted = false;

    public EcosiaDefaultBrowserPromoDialog(final Context context) {
        mContext = context;
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.ecosia_promo_dialog_layout, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);

        mDialog = builder.create();
        mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (mAccepted) {
                    mAccepted = false;
                    // Tracking is handled by the runnable
                } else {
                    sendDismissBroadcast();
                }
            }
        });

        Button buttonOk = dialogView.findViewById(R.id.button_primary);
        Button buttonCancel = dialogView.findViewById(R.id.button_secondary);

        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnAcceptCallback != null) {
                    mOnAcceptCallback.run();
                }
                mAccepted = true;
                mDialog.dismiss();
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });
    }

    public void setOnAcceptCallback(final Runnable onAcceptCallback) {
        mOnAcceptCallback = onAcceptCallback;
    }

    public void show() {
        sendLaunchBroadcast();
        mDialog.show();
    }

    public void sendLaunchBroadcast() {
        sendBroadcast(DEFAULT_BROWSER_LAUNCH);
    }

    public void sendAcceptBroadcast() {
        sendBroadcast(DEFAULT_BROWSER_ACCEPT);
    }

    public void sendDismissBroadcast() {
        sendBroadcast(DEFAULT_BROWSER_DISMISS);
    }

    // Send broadcasts for tracking from a different module
    public void sendBroadcast(String event) {
        Intent intent = new Intent(DEFAULT_BROWSER_ACTION);
        intent.putExtra(DEFAULT_BROWSER_DATA, event);
        mContext.getApplicationContext().sendBroadcast(intent);
    }

    public static void showPopup(Activity activity, WindowAndroid windowAndroid) {
        DefaultBrowserPromoUtils.prepareLaunchPromoIfNeeded(activity, windowAndroid, false);
    }
}
