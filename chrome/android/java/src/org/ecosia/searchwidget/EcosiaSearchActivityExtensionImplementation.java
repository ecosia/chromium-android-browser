/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.ecosia.searchwidget;

import android.content.Intent;

import androidx.annotation.NonNull;

import org.ecosia.tracking.TrackingManager;

public class EcosiaSearchActivityExtensionImplementation implements EcosiaSearchActivityExtension {

    private static final String WIDGET_ORIGIN = "android_default_widget";

    @NonNull
    private final TrackingManager trackingManager;

    public EcosiaSearchActivityExtensionImplementation(@NonNull TrackingManager trackingManager) {
        this.trackingManager = trackingManager;
    }

    @Override
    public void onSearchWidgetSearchMade() {
        final Intent originEvemt = new Intent();
        originEvemt.putExtra(TrackingManager.OPEN_ORIGIN, WIDGET_ORIGIN);
        trackingManager.trackOriginEvent(originEvemt);
    }
}
