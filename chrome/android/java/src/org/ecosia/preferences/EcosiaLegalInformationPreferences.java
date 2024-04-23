/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.ecosia.preferences;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

import org.chromium.chrome.R;
import org.chromium.components.browser_ui.settings.SettingsUtils;

public class EcosiaLegalInformationPreferences extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String s) {
        SettingsUtils.addPreferencesFromResource(this, R.xml.ecosia_legal_information_preferences);
        getActivity().setTitle(R.string.legal_information_title);
    }
}
