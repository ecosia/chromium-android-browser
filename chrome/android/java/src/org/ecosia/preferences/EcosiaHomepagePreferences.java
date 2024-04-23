package org.ecosia.preferences;

import static org.ecosia.preferences.EcosiaHomepagePreferencesHelper.PREF_ABOUT_ECOSIA;
import static org.ecosia.preferences.EcosiaHomepagePreferencesHelper.PREF_CLIMATE_IMPACT;
import static org.ecosia.preferences.EcosiaHomepagePreferencesHelper.PREF_ECOSIA_NEWS;
import static org.ecosia.preferences.EcosiaHomepagePreferencesHelper.PREF_TOP_SITES;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import org.chromium.chrome.R;
import org.chromium.components.browser_ui.settings.SettingsUtils;
import org.ecosia.tracking.TrackingManager;

public class EcosiaHomepagePreferences extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(@Nullable Bundle bundle, @Nullable String s) {
        if (getActivity() != null) {
            getActivity().setTitle(R.string.prefs_custom_ntp_title);
            SettingsUtils.addPreferencesFromResource(this, R.xml.ecosia_homepage_preferences);

            SwitchPreference topSitesPref = findPreference(PREF_TOP_SITES);
            if (topSitesPref != null) {
                topSitesPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(@NonNull Preference preference, Object object) {
                        boolean newValue = (Boolean) object;
                        TrackingManager.getInstance(getContext()).sendTopSitesChangeEvent(newValue);
                        return true;
                    }
                });
            }

            SwitchPreference climateImpactPref = findPreference(PREF_CLIMATE_IMPACT);
            if (climateImpactPref != null) {
                climateImpactPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(@NonNull Preference preference, Object object) {
                        boolean newValue = (Boolean) object;
                        TrackingManager.getInstance(getContext()).sendImpactChangeEvent(newValue);
                        return true;
                    }
                });
            }

            SwitchPreference ecosiaNewsPref = findPreference(PREF_ECOSIA_NEWS);
            if (ecosiaNewsPref != null) {
                ecosiaNewsPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(@NonNull Preference preference, Object object) {
                        boolean newValue = (Boolean) object;
                        TrackingManager.getInstance(getContext()).sendNewsChangeEvent(newValue);
                        return true;
                    }
                });
            }

            SwitchPreference aboutEcosiaPref = findPreference(PREF_ABOUT_ECOSIA);
            if (aboutEcosiaPref != null) {
                aboutEcosiaPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(@NonNull Preference preference, Object object) {
                        boolean newValue = (Boolean) object;
                        TrackingManager.getInstance(getContext()).sendAboutChangeEvent(newValue);
                        return true;
                    }
                });
            }
        }
    }
}
