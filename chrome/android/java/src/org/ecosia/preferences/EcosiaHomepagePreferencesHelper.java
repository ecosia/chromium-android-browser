package org.ecosia.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.ecosia.utils.SharedPreferencesHelpers;

public class EcosiaHomepagePreferencesHelper {

    public static final String PREF_TOP_SITES = "top_sites";
    public static final String PREF_CLIMATE_IMPACT = "climate_impact";
    public static final String PREF_ECOSIA_NEWS = "ecosia_news";
    public static final String PREF_ECOSIA_NEWS_AVAILABLE = "ecosia_news_available";
    public static final String PREF_ABOUT_ECOSIA = "about_ecosia";

    public static boolean isTopSitesEnabled(final Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(PREF_TOP_SITES, true);
    }

    public static boolean isClimateImpactEnabled(final Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(PREF_CLIMATE_IMPACT, true);
    }

    public static boolean isEcosiaNewsEnabled(final Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(PREF_ECOSIA_NEWS, true);
    }

    public static boolean isAboutEcosiaEnabled(final Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(PREF_ABOUT_ECOSIA, true);
    }

    public static boolean isEcosiaNewsAvailable(final Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(PREF_ECOSIA_NEWS_AVAILABLE, false);
    }

    public static void setEcosiaNewsAvailable(final Context context, final boolean value) {
        SharedPreferencesHelpers.putBoolean(context, PREF_ECOSIA_NEWS_AVAILABLE, value);
    }

}
