package org.ecosia.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.net.Uri;

import android.util.Log;

import javax.annotation.Nullable;

public class SettingsHelpers {

    private static final String LOG_TAG = SettingsHelpers.class.getSimpleName();
    private static String sAppCenterKey = "com.appcenter.android.appSecret";
    private static String sCloudflareClientIdKey = "org.ecosia.cloudflare.clientId";
    private static String sCloudflareClientSecretKey = "org.ecosia.cloudflare.clientSecret";

    public static boolean isEcosiaDefaultBrowser(final Context context) {
        return isEcosiaDefaultBrowser(context, false);
    }

    public static boolean isEcosiaDefaultBrowser(final Context context, final boolean require) {
        Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://"));
        ResolveInfo resolveInfo = context.getPackageManager().
                resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY);

        if (resolveInfo == null || resolveInfo.activityInfo == null) {
            Log.d(LOG_TAG, "Activity info couldn't be resolved.");
            if (require) {
                Log.d(LOG_TAG, " - Will proceed as if we are an optional browser.");
                return false;
            } else {
                Log.d(LOG_TAG, " - Will proceed as if we are the default browser.");
                return true;
            }
        }

        String defaultBrowserPackageName = resolveInfo.activityInfo.packageName;
        String ecosiaPackageName = context.getPackageName();
        return defaultBrowserPackageName.equals(ecosiaPackageName);
    }

    @Nullable
    private static String getManifestValueFromKey(Context context, String key) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo app = packageManager.getApplicationInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA);
            Bundle bundle = app.metaData;
            return bundle.getString(key);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    public static String getAppCenterSecret(Context context) {
        return getManifestValueFromKey(context, sAppCenterKey);
    }

    @Nullable
    public static String getCloudflareClientId(Context context) {
        return getManifestValueFromKey(context, sCloudflareClientIdKey);
    }

    @Nullable
    public static String getCloudflareClientSecret(Context context) {
        return getManifestValueFromKey(context, sCloudflareClientSecretKey);
    }
}