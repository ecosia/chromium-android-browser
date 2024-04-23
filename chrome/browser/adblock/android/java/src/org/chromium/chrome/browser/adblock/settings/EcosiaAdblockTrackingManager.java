/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.chromium.chrome.browser.adblock.settings;

import com.snowplowanalytics.snowplow.Snowplow;
import com.snowplowanalytics.snowplow.event.Structured;
import com.snowplowanalytics.snowplow.controller.TrackerController;
import android.content.Context;

// TODO merge with TrackingManager
public class EcosiaAdblockTrackingManager {

    private static final String CATEGORY_ADBLOCK = "adblock";
    public static final String ACTION_CLICK = "click";
    private static final String LABEL_CHANGE_ADBLOCK_STATUS="change_adblock_status";
    private static final String LABEL_CHANGE_ACCEPTABLE_ADS_STATUS = "change_acceptable_ads_status";
    private static final String LABEL_ENABLE = "enable";
    private static final String LABEL_DISABLE = "disable";

    private static volatile EcosiaAdblockTrackingManager sInstance;

    private EcosiaAdblockTrackingManager(final Context context) {
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
        TrackerController tracker = Snowplow.getDefaultTracker();
        if (tracker == null) { return; };

        tracker.track(
                new Structured(CATEGORY_ADBLOCK, ACTION_CLICK)
                        .label(label)
                        .property(getPrefChangeStatus(bResult))
        );
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
