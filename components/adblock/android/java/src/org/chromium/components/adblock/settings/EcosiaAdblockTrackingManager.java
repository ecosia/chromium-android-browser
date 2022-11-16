/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.chromium.components.adblock.settings;

import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.events.Structured;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

// TODO merge with TrackingManager
public class EcosiaAdblockTrackingManager {
    private static final String PREF_ANALYTICS_ENABLED = "com.ecosia.PREF_ANALYTICS_ENABLED";

    private static final String CATEGORY_ADBLOCK = "adblock";
    public static final String ACTION_CLICK = "click";
    private static final String LABEL_CHANGE_ADBLOCK_STATUS="change_adblock_status";
    private static final String LABEL_CHANGE_ACCEPTABLE_ADS_STATUS = "change_acceptable_ads_status";
    private static final String LABEL_ENABLE = "enable";
    private static final String LABEL_DISABLE = "disable";

    private static volatile EcosiaAdblockTrackingManager sInstance;
    private Context mContext;

    private EcosiaAdblockTrackingManager(final Context context) {
        mContext = context;
    }

    public static EcosiaAdblockTrackingManager getInstance(final Context context) {
        if (sInstance == null) {
            synchronized (EcosiaAdblockTrackingManager.class) {
                if (sInstance == null) {
                    sInstance = new EcosiaAdblockTrackingManager(context);
                }
            }
        }
        return sInstance;
    }

    private void adblockChangeEvents(String label, boolean bResult) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        Boolean isTrackingEnabled = preferences.getBoolean(PREF_ANALYTICS_ENABLED, true);

        if (isTrackingEnabled && Tracker.instance() != null) {
            Tracker.instance().track(Structured.builder()
                .category(CATEGORY_ADBLOCK)
                .action(ACTION_CLICK)
                .label(label)
                .property(getPrefChangeStatus(bResult))
                .build());
        }
    }

    public void changeAdblockEvent(boolean bResult) {
        adblockChangeEvents(LABEL_CHANGE_ADBLOCK_STATUS, bResult);
    }

    public void changeAcceptableAdsEvent(boolean bResult) {
        adblockChangeEvents(LABEL_CHANGE_ACCEPTABLE_ADS_STATUS, bResult);
    }

    private String getPrefChangeStatus(boolean bResult) {
        return bResult ? LABEL_ENABLE : LABEL_DISABLE;
    }
}
