package org.ecosia.unleash;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Pair;

import org.chromium.base.Log;
import org.chromium.base.task.AsyncTask;
import org.chromium.components.version_info.VersionInfo;
import org.ecosia.utils.Requests;
import org.ecosia.utils.RetrieveDataTask;
import org.ecosia.utils.RetrieveDelegate;
import org.ecosia.utils.SettingsHelpers;
import org.ecosia.utils.SharedPreferencesHelpers;
import org.ecosia.utils.UserHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

public class Unleash implements IUnleash {
    private static final String TAG = "Unleash";
    private static final String PREF_UNLEASH_TOGGLES_LAST_SYNC_DATA = "com.ecosia.PREF_UNLEASH_TOGGLES_LAST_SYNC_DATA";
    private static final String PREF_UNLEASH_TOGGLES_LAST_SYNC_ETAG = "com.ecosia.PREF_UNLEASH_TOGGLES_LAST_SYNC_ETAG";
    private static final String PREF_UNLEASH_TOGGLES_LAST_SYNC_TIMESTAMP = "com.ecosia.PREF_UNLEASH_TOGGLES_LAST_SYNC_TIMESTAMP";
    private static final String PREF_UNLEASH_TOGGLES_LAST_SYNC_APP_VERSION = "com.ecosia.PREF_UNLEASH_TOGGLES_LAST_SYNC_APP_VERSION";
    private static final String PREF_UNLEASH_TOGGLES_LAST_SYNC_DEVICE_REGION = "com.ecosia.PREF_UNLEASH_TOGGLES_LAST_SYNC_DEVICE_REGION";

    private Context mContext;
    private Handler mHandler;
    private Runnable mRunnable;
    private final List<WeakReference<UnleashCallback>> mCallbacks = new ArrayList<>();
    private List<Toggle> mToggles;
    private static Unleash sInstance;

    private Unleash(Context context) {
        mContext = context;
        mHandler = new Handler();
        mRunnable = () -> retrieve();
        mToggles = null;
    }

    public static Unleash getInstance(final Context context) {
        if (sInstance == null) {
            synchronized (Unleash.class) {
                if (sInstance == null) {
                    sInstance = new Unleash(context);
                    sInstance.restoreToggles();
                }
            }
        }
        return sInstance;
    }

    @Override
    public void addListener(UnleashCallback callback) {
        if (callback == null) return;

        synchronized (mCallbacks) {
            boolean needsAdd = true;
            Iterator<WeakReference<UnleashCallback>> refIter = mCallbacks.iterator();
            while (refIter.hasNext()) {
                UnleashCallback storedCallback = refIter.next().get();
                if (null == storedCallback) {
                    refIter.remove();
                } else if (storedCallback == callback) {
                    needsAdd = false;
                }
            }
            if (needsAdd) {
                mCallbacks.add(new WeakReference<>(callback));
            }
        }
    }

    @Override
    public void removeListener(UnleashCallback callback) {
        if (callback == null) return;

        synchronized (mCallbacks) {
            Iterator<WeakReference<UnleashCallback>> refIter = mCallbacks.iterator();
            while (refIter.hasNext()) {
                UnleashCallback storedCallback = refIter.next().get();
                if (null == storedCallback) {
                    refIter.remove();
                } else if (storedCallback == callback) {
                    refIter.remove();
                }
            }
        }
    }

    private void signalUnleashReady() {
        synchronized (mCallbacks) {
            Iterator<WeakReference<UnleashCallback>> refIter = mCallbacks.iterator();
            while (refIter.hasNext()) {
                UnleashCallback callback = refIter.next().get();
                if (null == callback) {
                    refIter.remove();
                } else {
                    callback.onUnleashReady();
                }
            }
        }
    }

    @Override
    public boolean isReady() {
        return mToggles != null;
    }

    @Override
    public boolean isEnabled(Toggle.Name toggle) {
        if (!isReady()) {
            return false;
        }
        Optional<Toggle> firstToggleOpt = findFirstToggleByName(toggle);
        return firstToggleOpt.map(value -> value.enabled).orElse(false);
    }

    @Override
    @Nullable
    public Toggle.Variant getVariant(Toggle.Name toggle) {
        Optional<Toggle> firstToggleOpt = findFirstToggleByName(toggle);
        return firstToggleOpt.map(value -> value.variant).orElse(null);
    }

    private Optional<Toggle> findFirstToggleByName(Toggle.Name toggle) {
        return mToggles.stream().filter(tgl -> Objects.equals(tgl.name, toggle.getName())).findFirst();
    }

    private String getUrl() {
        return getApiBaseUrl() + getApiPath()
                + "?userId=" + URLEncoder.encode(UserHelper.getUserId(mContext))
                + "&appName=Android"
                + "&appVersion=" + URLEncoder.encode(getAppVersion(mContext))
                + "&environment=" + URLEncoder.encode(getEnvironment())
                + "&deviceRegion=" + URLEncoder.encode(getDeviceRegion(mContext));
    }

    private String getAppVersion(Context context) {
        String versionCode;
        try {
            versionCode = Integer.toString(
                    context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            versionCode = "";
        }
        return versionCode;
    }

    private String getEnvironment() {
        return VersionInfo.isOfficialBuild() ? "prod" : "staging";
    }

    private String getDeviceRegion(Context context) {
        Locale locale = getRelevantLocale(context);
        String region = locale.getCountry().toLowerCase();
        return region;
    }

    private Locale getRelevantLocale(Context context) {
        Locale currentLocale = context.getResources().getConfiguration().getLocales().get(0);
        if (currentLocale == null) {
            return Locale.US;
        }
        return currentLocale;
    }

    private String getApiBaseUrl() {
        if (getEnvironment().equals("staging")) {
            return "https://api.ecosia-staging.xyz";
        }
        return "https://api.ecosia.org";
    }

    private String getApiPath() {
        return "/v2/toggles";
    }

    private HashMap<String, String> getApiRequestHeaders() {
        return new HashMap<>() {{
            if (getEnvironment().equals("staging")) {
                put("CF-Access-Client-Id", SettingsHelpers.getCloudflareClientId(mContext));
                put("CF-Access-Client-Secret", SettingsHelpers.getCloudflareClientSecret(mContext));
            }
            String etag = SharedPreferencesHelpers.getString(mContext, PREF_UNLEASH_TOGGLES_LAST_SYNC_ETAG, "");
            if (!etag.isEmpty()) {
                put("If-None-Match", etag);
            }
        }};
    }

    private boolean areCriteriaForRetrieveMatched() {
        // did device region change?
        String lastDeviceRegion = SharedPreferencesHelpers.getString(mContext, PREF_UNLEASH_TOGGLES_LAST_SYNC_DEVICE_REGION, "");
        if (!lastDeviceRegion.equals(getDeviceRegion(mContext))) {
            return true;
        }

        // did app version change?
        String lastAppVersion = SharedPreferencesHelpers.getString(mContext, PREF_UNLEASH_TOGGLES_LAST_SYNC_APP_VERSION, "");
        if (!lastAppVersion.equals(getAppVersion(mContext))) {
            return true;
        }

        // did last fetch happen on the day before today?
        long lastFetchMillis = SharedPreferencesHelpers.getLong(mContext, PREF_UNLEASH_TOGGLES_LAST_SYNC_TIMESTAMP, 0);

        Calendar lastFetchCalendar = Calendar.getInstance();
        lastFetchCalendar.setTimeInMillis(lastFetchMillis);
        Calendar todayCalendar = Calendar.getInstance();

        if (todayCalendar.get(Calendar.YEAR) > lastFetchCalendar.get(Calendar.YEAR)) {
            return true;
        }

        return todayCalendar.get(Calendar.YEAR) == lastFetchCalendar.get(Calendar.YEAR)  &&
                todayCalendar.get(Calendar.DAY_OF_YEAR) > lastFetchCalendar.get(Calendar.DAY_OF_YEAR);

    }

    @Override
    public void onStartOrResume() {
        boolean shouldFetchNewToggles = areCriteriaForRetrieveMatched();
        Log.d(TAG, "Criteria for fetching new toggles met:" + shouldFetchNewToggles);
        if (shouldFetchNewToggles) {
            mHandler.post(mRunnable);
        } else if (mToggles != null) {
            signalUnleashReady();
        } else {
            restoreToggles();
        }
    }

    private void retrieve() {
        RetrieveDelegate retrieveDelegate = new RetrieveDelegate<Pair<String, Map<String, List<String>>>>() {
            @Override
            public Pair<String, Map<String, List<String>>> doInBackground() {
                try {
                    String url = getUrl();
                    return new Requests().downloadContentAndHeaders(url, getApiRequestHeaders());

                } catch (IOException | RuntimeException e) {
                    if (e.getMessage() != null ) {
                        Log.d(TAG, "Fetch failed: " + e.getMessage());
                    }
                    return new Pair<>("", new HashMap<>());
                }
            }

            @Override
            public void onPostExecute(Pair<String, Map<String, List<String>>> result) {
                Log.d(TAG, "Result" + result);
                String response = result.first;

                if (response == null || response.isEmpty()) {
                    Log.d(TAG, "Received empty result. Bailing out.");
                    return;
                }

                try {
                    Log.d(TAG, "Unleash is ready with toggles:" + mToggles);
                    mToggles = decodeTogglesJsonFromString(response);

                    List<String> etags = result.second.get("ETag");
                    if (etags == null || etags.isEmpty()) {
                        Log.e(TAG, "ETag not found. Not persisting toggles.");
                    } else {
                        String etag = etags.get(0);
                        persistToggles(response, etag);
                    }

                    signalUnleashReady();
                } catch (JSONException e) {
                    Log.e(TAG, "Could not deserialize JSON" + e.getMessage());
                }
            }
        };

        new RetrieveDataTask(retrieveDelegate).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private List<Toggle> decodeTogglesJsonFromString(String string) throws JSONException {
        JSONObject jsonObject = new JSONObject(string);
        JSONArray togglesArray = jsonObject.getJSONArray("toggles");
        List<Toggle> toggleList = new ArrayList<>();
        for (int i = 0; i < togglesArray.length(); i++)  {
            JSONObject toggleObject = togglesArray.optJSONObject(i);
            if (toggleObject == null) continue;

            // Toggle
            Toggle toggle = new Toggle();
            toggle.name = toggleObject.getString("name");
            toggle.enabled = toggleObject.getBoolean("enabled");

            // Variant
            JSONObject variantObject = toggleObject.optJSONObject("variant");
            if (variantObject != null) {
                Toggle.Variant variant = new Toggle.Variant();
                variant.enabled = variantObject.getBoolean("enabled");
                variant.name = variantObject.getString("name");

                // Variant Payload
                JSONObject payloadObject = variantObject.optJSONObject("payload");
                if (payloadObject != null) {
                    Toggle.Payload payload = new Toggle.Payload();
                    payload.type = payloadObject.getString("type");
                    payload.value = payloadObject.getString("value");
                    variant.payload = payload;
                }

                toggle.variant = variant;
            }

            toggleList.add(toggle);
        }

        return toggleList;
    }

    private void persistToggles(String togglesJson, String etag) {
        if (togglesJson.isBlank()) {
            Log.e(TAG, "Toggles JSON is blank. No need to persist.");
            return;
        }
        SharedPreferencesHelpers.putString(mContext, PREF_UNLEASH_TOGGLES_LAST_SYNC_DATA, togglesJson);
        SharedPreferencesHelpers.putString(mContext, PREF_UNLEASH_TOGGLES_LAST_SYNC_ETAG, etag);
        SharedPreferencesHelpers.putString(mContext, PREF_UNLEASH_TOGGLES_LAST_SYNC_DEVICE_REGION, getDeviceRegion(mContext));
        SharedPreferencesHelpers.putString(mContext, PREF_UNLEASH_TOGGLES_LAST_SYNC_APP_VERSION, getAppVersion(mContext));
        SharedPreferencesHelpers.putLong(mContext, PREF_UNLEASH_TOGGLES_LAST_SYNC_TIMESTAMP, System.currentTimeMillis());
    }

    private void restoreToggles() {
        Log.d(TAG, "Trying to restore existing Toggles");
        String toggles = SharedPreferencesHelpers.getString(mContext, PREF_UNLEASH_TOGGLES_LAST_SYNC_DATA, "");
        if (toggles.isEmpty()) {
            Log.d(TAG, "No existing Toggles found");
            mToggles = null;
            return;
        }
        try {
            mToggles = decodeTogglesJsonFromString(toggles);
            Log.d(TAG, "Unleash is ready with toggles:" + mToggles);
            signalUnleashReady();
        } catch (JSONException e) {
            Log.e(TAG, "Could not deserialize JSON" + e.getMessage());
            resetStoredToggles();
        }
    }

    private void resetStoredToggles() {
        SharedPreferencesHelpers.putString(mContext, PREF_UNLEASH_TOGGLES_LAST_SYNC_DATA, "");
        SharedPreferencesHelpers.putString(mContext, PREF_UNLEASH_TOGGLES_LAST_SYNC_ETAG, "");
        SharedPreferencesHelpers.putLong(mContext, PREF_UNLEASH_TOGGLES_LAST_SYNC_TIMESTAMP, 0);
    }
}
