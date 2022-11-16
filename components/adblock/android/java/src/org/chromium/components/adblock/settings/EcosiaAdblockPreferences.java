/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.chromium.components.adblock.settings;

import android.content.Context;

public class EcosiaAdblockPreferences {

    private final static String PREF_ADBLOCK_BUTTON_VISIBILITY = "PREF_ADBLOCK_BUTTON_VISIBILITY";

    public static void setAdblockButtonVisibility(Context context, boolean visible) {
        EcosiaAdblockSharedPreferencesHelpers.putBoolean(context, PREF_ADBLOCK_BUTTON_VISIBILITY, visible);
    }

    public static boolean getAdblockButtonVisibility(Context context) {
        return EcosiaAdblockSharedPreferencesHelpers.getBoolean(context, PREF_ADBLOCK_BUTTON_VISIBILITY, true);
    }
}
