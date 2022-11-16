package org.ecosia.preferences;

import java.util.Locale;

import org.chromium.base.Log;
import org.chromium.chrome.R;
import org.chromium.components.version_info.VersionInfo;
import org.chromium.chrome.browser.about_settings.AboutChromeSettings;
import org.chromium.base.BuildInfo;
import org.chromium.chrome.browser.about_settings.AboutSettingsBridge;
import org.chromium.components.browser_ui.settings.SettingsUtils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.preference.Preference;
import android.text.format.DateUtils;

public class EcosiaAboutPreferences extends AboutChromeSettings {
    private static final String PREF_FEEDBACK = "feedback";
    private static final String PREF_RATE_APP = "rate_app";
    private static final String TAG = "EcosiaAboutPreferences";
    private static String mAppVersion;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        getActivity().setTitle(R.string.prefs_about_ecosia);
        SettingsUtils.addPreferencesFromResource(this, R.xml.ecosia_about_preferences);

        String appVersionString = BuildInfo.getInstance().versionName;


        Preference appVersionPref = findPreference(PREF_APPLICATION_VERSION);
        String appVersionSummary = getAppVersionSummary(getActivity(), appVersionString);
        appVersionPref.setSummary(appVersionSummary);
        appVersionPref.setOnPreferenceClickListener(this);

        Preference osVersionPref = findPreference(PREF_OS_VERSION);
        osVersionPref.setSummary(AboutSettingsBridge.getOSVersion());

        Preference legalPref = findPreference(PREF_LEGAL_INFORMATION);
        legalPref.setSummary(getString(R.string.prefs_legal_information_summary));

        Preference prefFeedback = findPreference(PREF_FEEDBACK);
        prefFeedback.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String address = getResources().getString(R.string.ecosia_feedback_address);
                // Getting Ecosia App and Build version for feedback
                String subject = getFeedbackSubject(getActivity(), mAppVersion);
                
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:"));
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{address});
                intent.putExtra(Intent.EXTRA_SUBJECT, subject);
                try {
                    startActivity(intent);
                } catch (android.content.ActivityNotFoundException ex) {
                    Log.e(TAG, "Failed to start intent to mail feedback " + ex);
                }
                return true;
            }
        });

        Preference prefRate = findPreference(PREF_RATE_APP);
        prefRate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                rateApp(getActivity());
                return true;
            }
        });
    }

    public static String getAppVersionSummary(Context context, String baseVersion) {
        String versionName = "1.0";
        int versionCode = 0;
        String packageName = context.getApplicationContext().getPackageName();
        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            versionName = pInfo.versionName;
            versionCode = pInfo.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        String version = String.format(Locale.getDefault(), "%s.%d (%s)", versionName, versionCode, baseVersion);
        // Ecosia : Getting Ecosia App and Build version for feedback
        mAppVersion = String.format(Locale.getDefault(), "%s.%d", versionName, versionCode);
        if (VersionInfo.isOfficialBuild()) {
            return version;
        }

        // For developer builds, show how recently the app was installed/updated.
        if (pInfo == null) {
            return version;
        }
        CharSequence updateTimeString = DateUtils.getRelativeTimeSpanString(pInfo.lastUpdateTime,
                System.currentTimeMillis(), 0);
        return context.getString(R.string.version_with_update_time, version, updateTimeString);
    }

    private String getFeedbackSubject(Context context, String appVersion) {
        return context.getResources().getString(R.string.ecosia_feedback_subject) + " " + appVersion;
    }

    private void rateApp(Context context) {
        String packageName = context.getPackageName();
        try {
            Intent rateIntent = rateIntentForUrl("market://details", packageName);
            context.startActivity(rateIntent);
        } catch (ActivityNotFoundException e) {
            Intent rateIntent = rateIntentForUrl("https://play.google.com/store/apps/details", packageName);
            context.startActivity(rateIntent);
        }
    }

    private Intent rateIntentForUrl(String url, String packageName) {
        Uri formattedUrl = Uri.parse(String.format("%s?id=%s", url, packageName));
        Intent intent = new Intent(Intent.ACTION_VIEW, formattedUrl);
        int flags = Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
        if (Build.VERSION.SDK_INT >= 21) {
            flags |= Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
        } else {
            // noinspection deprecation
            flags |= Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET;
        }
        intent.addFlags(flags);
        return intent;
    }
}
