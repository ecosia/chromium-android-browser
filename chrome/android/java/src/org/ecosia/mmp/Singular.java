package org.ecosia.mmp;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

import com.android.installreferrer.api.ReferrerDetails;

import org.chromium.base.Log;
import org.chromium.base.task.AsyncTask;
import org.chromium.base.version_info.VersionInfo;
import org.ecosia.tracking.TrackingManager;
import org.ecosia.utils.Requests;
import org.ecosia.utils.SettingsHelpers;
import org.ecosia.utils.UserHelper;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Singular {

    public static class Events {
        private static final String URL_SUFFIX = "v2/attribution/event";
        private static final String EVENT_NAME_KEY = "n";
        public static final String ONBOARDING_START = "onboarding_start";
        public static final String ONBOARDING_COMPLETE = "onboarding_complete";
        public static final String SEARCH_1 = "first_search";
        public static final String SEARCH_5 = "fifth_search";
        public static final String SEARCH_10 = "tenth_search";
    }

    public static final String TAG = Singular.class.getSimpleName();
    public static final String URL_PROD = "https://api.ecosia.org";
    public static final String URL_STAGING = "https://api.ecosia-staging.xyz";
    public static final String GET = "v2/attribution/launch";
    private final String PLATFORM = "android";
    private final String REF_SOURCE = "service";
    private final String HTTP_STATUS_OK = "\"status\":\"ok\"";
    private final String FB_INSTALL_REFERRER_PROVIDER_PACKAGE_NAME = "com.facebook.katana.provider.InstallReferrerProvider";
    private final String IG_INSTALL_REFERRER_PROVIDER_PACKAGE_NAME = "com.instagram.contentprovider.InstallReferrerProvider";
    private final String META_INSTALL_REFERRER_COLUMN_NAME = "install_referrer";
    private final String META_IS_CT_COLUMN_NAME = "is_ct";
    private final String META_ACTUAL_TIMESTAMP_COLUMN_NAME = "actual_timestamp";
    private final Context mContext;
    private static volatile Singular sInstance;
    private JSONObject mInstallParams;
    private JSONObject mMetaRefParams;
    private String mUUID;
    private String mUrl;
    private Map<String, String> mParams;

    private Singular(final Context context) {
        mUrl = VersionInfo.isOfficialBuild() ? URL_PROD : URL_STAGING;
        mContext = context;
        mUUID = UserHelper.getUserId(mContext); //UUID same as TrackingManager
    }

    public static Singular getInstance(final Context context) {
        if (sInstance == null) {
            synchronized (Singular.class) {
                if (sInstance == null) {
                    sInstance = new Singular(context);
                }
            }
        }
        return sInstance;
    }

    public void setInstallRef(ReferrerDetails installRef) {
        try {
            mInstallParams = new JSONObject();
            mInstallParams.put(Parameters.INSTALL_TIMESTAMP.toString(), installRef.getInstallBeginTimestampServerSeconds());
            mInstallParams.put(Parameters.REFERRER.toString(), installRef.getInstallReferrer());
            mInstallParams.put(Parameters.CLICK_TIMESTAMP.toString(), installRef.getReferrerClickTimestampSeconds());
            mInstallParams.put(Parameters.REFERRER_SOURCE.toString(), REF_SOURCE);
            mInstallParams.put(Parameters.CURRENT_DEVICE_TIMESTAMP.toString(), Calendar.getInstance().getTimeInMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        queryAndSetMetaRef();
        updateParams();
    }

    private void queryAndSetMetaRef() {
        String fbAppId = SettingsHelpers.getFacebookAppId(mContext);
        if (fbAppId == null) {
            Log.e(TAG, "Singular Meta Referrer Error: Facebook App Id not set");
            return;
        }
        Cursor cursor = null;
        try {
            String [] projection = { META_INSTALL_REFERRER_COLUMN_NAME, META_IS_CT_COLUMN_NAME, META_ACTUAL_TIMESTAMP_COLUMN_NAME };
            Uri providerUri;

            if (mContext.getPackageManager().resolveContentProvider(
                    FB_INSTALL_REFERRER_PROVIDER_PACKAGE_NAME, 0) != null) {
                providerUri = Uri.parse(
                        "content://" + FB_INSTALL_REFERRER_PROVIDER_PACKAGE_NAME + "/" + fbAppId);
            } else if (mContext.getPackageManager().resolveContentProvider(
                    IG_INSTALL_REFERRER_PROVIDER_PACKAGE_NAME, 0) != null) {
                providerUri = Uri.parse(
                        "content://" + IG_INSTALL_REFERRER_PROVIDER_PACKAGE_NAME + "/" + fbAppId);
            } else {
                Log.e(TAG, "Could not resolve content provider");
                return;
            }

            cursor = mContext.getContentResolver().query(providerUri, projection, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                Log.e(TAG, "Could not acquire cursor");
                return;
            }

            int installReferrerIndex = cursor.getColumnIndex(META_INSTALL_REFERRER_COLUMN_NAME);
            int timestampIndex = cursor.getColumnIndex(META_ACTUAL_TIMESTAMP_COLUMN_NAME);
            int isCTIndex = cursor.getColumnIndex(META_IS_CT_COLUMN_NAME);

            if (installReferrerIndex == -1 || timestampIndex == -1 || isCTIndex == -1) {
                Log.e(TAG, "Failed to query column for required value");
                return;
            }

            String installReferrer = cursor.getString(installReferrerIndex); // serialized and encrypted attribution details
            long actualTimestamp = cursor.getLong(timestampIndex); // timestamp in seconds for click/impression
            int isCT = cursor.getInt(isCTIndex); // VT (View-Through-) or CT (Click-Through-) Installation, 0 = VT, 1 = CT

            mMetaRefParams = new JSONObject();
            mMetaRefParams.put(MetaRefParams.IS_CT.toString(), isCT);
            mMetaRefParams.put(MetaRefParams.INSTALL_REFERRER.toString(), installReferrer);
            mMetaRefParams.put(MetaRefParams.ACTUAL_TIMESTAMP.toString(), actualTimestamp);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void updateParams() {
        mParams = new HashMap<>();
        mParams.put(Parameters.PLATFORM.toString(), PLATFORM);
        mParams.put(Parameters.PACKAGE_NAME.toString(), mContext.getPackageName());
        mParams.put(Parameters.CLIENT_NAME.toString(), mUUID);
        mParams.put(Parameters.COUNTRY.toString(), mContext.getResources()
                .getConfiguration().locale.getCountry());
        mParams.put(Parameters.OS_VERSION.toString(), String.valueOf(Build.VERSION.SDK_INT));
        if(mInstallParams == null) {
            mParams.put(Parameters.INSTALL_REF.toString(), "");
        } else {
            mParams.put(Parameters.INSTALL_REF.toString(), mInstallParams.toString());
        }
        if (mMetaRefParams != null) {
            mParams.put(Parameters.META_REF.toString(), mMetaRefParams.toString());
        }
        mParams.put(Parameters.DEVICE_BRAND.toString(), Build.BRAND);
        mParams.put(Parameters.DEVICE_MODEL.toString(),  Build.MODEL);
        mParams.put(Parameters.DEVICE_LANGUAGE.toString(), Locale.getDefault().toString());
        mParams.put(Parameters.DEVICE_BUILD.toString(), Build.DISPLAY);

        try {
            PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            mParams.put(Parameters.APP_VERSION.toString(), packageInfo.versionName);
            mParams.put(Parameters.INSTALL_TIME.toString(), String.valueOf(packageInfo.firstInstallTime));
            mParams.put(Parameters.UPDATE_TIME.toString(), String.valueOf(packageInfo.lastUpdateTime));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Singular param response error : " + e.getMessage());
        }
    }

    public void sendSessionInfo(){
        if(!TrackingManager.getInstance(mContext).isTrackingEnabled()) {
            return;
        }

        if (mParams == null) {
            updateParams();
        }

        new AsyncTask<String>() {
            @Override
            protected String doInBackground() {
                try {
                    String response = createAndRunSessionUrlRequest();
                    if (!response.contains(HTTP_STATUS_OK))
                        Log.e(TAG, "Singular response error : " + response + "space");
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(String response) {
                // Not implemented
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    String createAndRunSessionUrlRequest() {
        try {
            final Uri parsedUrl = Uri.parse(mUrl);
            final Uri.Builder urlBuilder = parsedUrl.buildUpon();
            urlBuilder.appendEncodedPath(GET);
            for (Map.Entry < String, String > param: mParams.entrySet()) {
                //Encoding JSON objects are not required as per Singular (it's encoded twice)
                if(param.getKey().equals(Parameters.INSTALL_REF.toString())) {
                    urlBuilder.appendQueryParameter(param.getKey(),
                            (String) param.getValue());
                } else {
                    if (Build.VERSION.SDK_INT >= 33) {
                        urlBuilder.appendQueryParameter(URLEncoder.encode(param.getKey(), StandardCharsets.UTF_8),
                                URLEncoder.encode((String) param.getValue(), StandardCharsets.UTF_8));
                    } else {
                        urlBuilder.appendQueryParameter(URLEncoder.encode(param.getKey(), "UTF-8"),
                                URLEncoder.encode((String) param.getValue(), "UTF-8"));
                    }
                }
            }
            return new Requests().downloadContent(urlBuilder.build().toString());

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return "";
    }

    public void trackEvent(final String event) {
        if (!TrackingManager.getInstance(mContext).isTrackingEnabled()) {
            return;
        }

        new AsyncTask<String>() {
            protected String doInBackground() {
                try {
                    final Uri.Builder eventTrackingBuilder = Uri.parse(mUrl)
                            .buildUpon()
                            .appendEncodedPath(Events.URL_SUFFIX);

                    // We only need a subset of Parameters
                    eventTrackingBuilder.appendQueryParameter(Events.EVENT_NAME_KEY, event);
                    eventTrackingBuilder.appendQueryParameter(Parameters.CLIENT_NAME.toString(), mUUID);
                    eventTrackingBuilder.appendQueryParameter(Parameters.PLATFORM.toString(), PLATFORM);
                    eventTrackingBuilder.appendQueryParameter(Parameters.PACKAGE_NAME.toString(), mContext.getPackageName());
                    eventTrackingBuilder.appendQueryParameter(Parameters.OS_VERSION.toString(), String.valueOf(Build.VERSION.SDK_INT));

                    final Uri eventTrackingUri = eventTrackingBuilder.build();
                    final String eventTrackingUrl = eventTrackingUri.toString();
                    final String eventTrackingResponse = new Requests()
                            .downloadContent(eventTrackingUrl);
                    if (!eventTrackingResponse.contains(HTTP_STATUS_OK))
                        Log.e(TAG, "Singular response error : " + eventTrackingResponse + " ");
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(String response) {
                // Not implemented
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    enum Parameters {
        SDK_KEY,
        PLATFORM,
        PACKAGE_NAME,
        CLIENT_NAME,
        IP,
        COUNTRY,
        OS_VERSION,
        INSTALL_REF,
        META_REF,
        DEVICE_BRAND,
        DEVICE_MODEL,
        DEVICE_LANGUAGE,
        DEVICE_BUILD,
        APP_VERSION,
        INSTALL_TIME,
        UPDATE_TIME,
        REFERRER,
        REFERRER_SOURCE,
        CLICK_TIMESTAMP,
        INSTALL_TIMESTAMP,
        CURRENT_DEVICE_TIMESTAMP;

        public String toString(){
            switch (this) {
                case SDK_KEY: return "a";
                case PLATFORM: return "p";
                case PACKAGE_NAME: return "i";
                case CLIENT_NAME: return "sing";
                case IP: return "use_ip";
                case COUNTRY: return "country";
                case OS_VERSION: return "ve";
                case INSTALL_REF: return "install_ref";
                case META_REF: return "meta_ref";
                case DEVICE_BRAND: return "ma";
                case DEVICE_MODEL: return "mo";
                case DEVICE_LANGUAGE: return "lc";
                case DEVICE_BUILD: return "bd";
                case APP_VERSION: return "app_v";
                case INSTALL_TIME: return "install_time";
                case UPDATE_TIME: return "update_time";
                case REFERRER: return "referrer";
                case REFERRER_SOURCE: return "referrer_source";
                case CLICK_TIMESTAMP: return "clickTimestampSeconds";
                case INSTALL_TIMESTAMP: return "installBeginTimestampSeconds";
                case CURRENT_DEVICE_TIMESTAMP: return "current_device_time";
            }
            return null;
        }
    };

    enum MetaRefParams {
        IS_CT,
        INSTALL_REFERRER,
        ACTUAL_TIMESTAMP;

        public String toString() {
            switch (this) {
                case IS_CT: return "is_ct";
                case INSTALL_REFERRER: return "install_referrer";
                case ACTUAL_TIMESTAMP: return "actual_timestamp";
            }
            return null;
        }
    }

}


