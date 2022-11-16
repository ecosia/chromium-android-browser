package org.ecosia.mmp;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import com.android.installreferrer.api.ReferrerDetails;

import org.chromium.base.Log;
import org.chromium.base.task.AsyncTask;
import org.chromium.components.version_info.VersionInfo;
import org.ecosia.tracking.TrackingManager;
import org.ecosia.utils.Requests;
import org.ecosia.utils.SharedPreferencesHelpers;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Singular {

    public static final String TAG = Singular.class.getSimpleName();
    public static final String URL_PROD = "https://api.ecosia.org";
    public static final String URL_STAGING = "https://api.ecosia-staging.xyz";
    public static final String GET = "v2/attribution/launch";
    private final String PREF_USER_ID =  "com.ecosia.PREF_USER_ID";
    private final String PLATFORM = "android";
    private final String REF_SOURCE = "service";
    private final String RESULT = "\"status\":\"ok\"";

    private final Context mContext;
    private static volatile Singular sInstance;
    private JSONObject mInstallParams;
    private String mUUID;
    private String mUrl;
    private Map<String, String> mParams;

    private Singular(final Context context) {
        mUrl = VersionInfo.isOfficialBuild() ? URL_PROD : URL_STAGING;
        mContext = context;
        mUUID = SharedPreferencesHelpers.getString(context, PREF_USER_ID, ""); //UUID same as TrackingManager
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

    }

    public void initParams() {
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
        if(TrackingManager.getInstance(mContext).isTrackingEnabled()) {
            new AsyncTask<String>() {
                @Override
                protected String doInBackground() {
                    if (mParams == null) {
                        initParams();
                    }

                    try {
                        String response = createUrlStructure();
                        if (!response.contains(RESULT))
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
    }

    String createUrlStructure() {
        try {
            Uri parseUrl = Uri.parse(mUrl);
            Uri.Builder urlBuilder = parseUrl.buildUpon();
            urlBuilder.appendEncodedPath(GET);

            for (Map.Entry < String, String > param: mParams.entrySet()) {
                //Encoding JSON objects are not required as per Singular (it's encoded twice)
                if(param.getKey().equals(Parameters.INSTALL_REF.toString())) {
                    urlBuilder.appendQueryParameter(param.getKey(),
                            (String) param.getValue());
                } else {
                    urlBuilder.appendQueryParameter(URLEncoder.encode(param.getKey(), "UTF-8"),
                            URLEncoder.encode((String) param.getValue(), "UTF-8"));
                }
            }

            return new Requests().downloadContent(urlBuilder.build().toString());

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return "";
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

}


