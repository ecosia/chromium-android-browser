package org.ecosia.tracking;

import static org.ecosia.utils.SharedPreferencesHelpers.PREF_ECCC_VALUE;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.Snowplow;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.SubjectConfiguration;
import com.snowplowanalytics.snowplow.configuration.EmitterConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.configuration.PlatformContextProperty;
import com.snowplowanalytics.snowplow.event.SelfDescribing;
import com.snowplowanalytics.snowplow.event.Structured;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.network.HttpMethod;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;

import org.chromium.base.ContextUtils;
import org.chromium.base.IntentUtils;
import org.chromium.base.Log;
import org.chromium.base.version_info.VersionInfo;
import org.ecosia.utils.DateHolder;
import org.ecosia.utils.SettingsHelpers;
import org.ecosia.utils.SharedPreferencesHelpers;
import org.ecosia.utils.UserHelper;

public class TrackingManager {

    private static final String ACTIVITY_KEY = "activity";
    private static final String LAUNCH_KEY = "launch";
    private static final String RESUME_KEY = "resume";

    private static final String INAPP_ORIGIN = "inapp";
    public static final String OPEN_ORIGIN = "open";
    public static final String WIDGET_ORIGIN = "widget";
    public static final String BOOKMARKS_ORIGIN = "bookmarks";

    private static final String NAMESPACE = "chromium";
    private static final String RECEIVER_URL = "sp.ecosia.org";
    private static final String RECEIVER_URL_STAGING = "org-ecosia-prod1.mini.snplow.net";

    private static final String INSTALL_SCHEMA = "iglu:org.ecosia/android_install_event/jsonschema/1-0-0";
    private static final String CONSENT_SCHEMA = "iglu:org.ecosia/eccc_context/jsonschema/1-0-2";
    
    private static final String PARAM_TYPETAG = "tt";
    private static final String PARAM_CAMPAIGN = "utm_campaign";
    private static final String PARAM_CONTENT = "utm_content";
    private static final String PARAM_MEDIUM = "utm_medium";
    private static final String PARAM_SOURCE = "utm_source";
    private static final String PARAM_TERM = "utm_term";
    private static final String PARAM_SP_USER_ID = "_sp";

    private static final String REFERRAL_KEY = "app_referral";
    private static final String TYPETAG_KEY = "typetag";
    private static final String[] UTM_PARAMS = {
            PARAM_CAMPAIGN,
            PARAM_CONTENT,
            PARAM_MEDIUM,
            PARAM_SOURCE,
            PARAM_TERM,
    };
    
    private static final String CATEGORY_ENGAGEMENT = "engagement";
    private static final String CATEGORY_INTRO = "intro";
    private static final String CATEGORY_NOTIFICATION = "notification";
    private static final String CATEGORY_ADBLOCK = "adblock";
    private static final String CATEGORY_SETTINGS = "settings";
    private static final String CATEGORY_INVITATIONS = "invitations";
    private static final String CATEGORY_NTP = "ntp";
    public static final String ACTION_CLICK = "click";
    public static final String ACTION_DISPLAY = "display";
    private static final String ACTION_CHANGE = "change";
    public static final String ACTION_CLAIM = "claim";
    public static final String ACTION_SEND = "send";
    public static final String ACTION_VIEW = "view";
    public static final String ACTION_OPEN = "open";
    private static final String LABEL_ADBLOCK_MENU = "show_adblock_menu";
    private static final String LABEL_ADBLOCK_POPUP = "show_adblock_popup";
    private static final String LABEL_CHANGE_ADBLOCK_STATUS = "change_adblock_status";
    private static final String LABEL_DISPLAY_POPUP = "display_adblock_popup";
    private static final String LABEL_CHANGE_ACCEPTABLE_ADS_STATUS = "change_acceptable_ads_status";
    public static final String LABEL_NEWS = "news";
    private static final String LABEL_SKIP = "skip";
    private static final String LABEL_NEXT = "next";
    public static final String LABEL_DEFAULT_BROWSER_CHANGE = "default_browser_change";
    private static final String LABEL_DISMISS = "dismiss";
    private static final String LABEL_ENABLE = "enable";
    private static final String LABEL_DISABLE = "disable";
    private static final String LABEL_ANALYTICS = "analytics";
    public static final String LABEL_INVITE = "invite";
    public static final String LABEL_PROMO = "promo";
    public static final String LABEL_INVITE_SCREEN = "invite_screen";
    public static final String LABEL_MENU = "menu";
    public static final String LABEL_LINK_COPYING = "link_copying";
    public static final String LABEL_TOP_SITES = "top_sites";
    public static final String LABEL_IMPACT = "impact";
    public static final String LABEL_ABOUT = "about";
    public static final String LABEL_CUSTOMIZE = "customize";
    private static final String SCREEN_START = "start";
    private static final String SCREEN_SEARCH = "search";
    private static final String SCREEN_GREEN_SEARCH = "green_search";
    private static final String SCREEN_PROFITS = "profits";
    private static final String SCREEN_ACTION = "action";
    private static final String SCREEN_PRIVACY = "privacy";
    private static final String SCREEN_TRANSPARENT_FINANCES= "transparent_finances";
    private static final String PROPERTY_DEFAULT_BROWSER_SET = "default_browser_set";
    private static final String TAG = TrackingManager.class.getSimpleName();
    
    private static final String PREF_ANALYTICS_ENABLED = "com.ecosia.PREF_ANALYTICS_ENABLED";
    private static final String PREF_INSTALL_TRACKED = "com.ecosia.PREF_INSTALL_TRACKED";
    private static final String SHARED_PREF_LAST_RESUME_EVENT_SUFFIX = ".lastResumeEvent";

    private static volatile TrackingManager sInstance;
    private final TrackerController mTracker;
    private boolean mPaused;
    private final Context mContext;
    private boolean mIsTrackingEnabled;

    private TrackingManager(final Context context) {
        String receiverUrl = VersionInfo.isOfficialBuild() ? RECEIVER_URL : RECEIVER_URL_STAGING;

        NetworkConfiguration networkConfig = new NetworkConfiguration(
                receiverUrl,
                HttpMethod.POST
        );

        String versionCode;
        try {
            versionCode = Integer.toString(
                    context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            versionCode = "";
        }

        TrackerConfiguration trackerConfig = new TrackerConfiguration()
                .appId(versionCode)
                .sessionContext(true)
                .applicationContext(true)
                .platformContext(true)
                .platformContextProperties(Arrays.asList(PlatformContextProperty.APP_SET_ID, PlatformContextProperty.APP_SET_ID_SCOPE)) //only track app set id from device properties
                .geoLocationContext(true)
                .deepLinkContext(false)
                .screenContext(false)
                .installAutotracking(false)
                .screenViewAutotracking(false)
                .lifecycleAutotracking(false)
                .exceptionAutotracking(false)
                .diagnosticAutotracking(false)
                .userAnonymisation(false);

        final SubjectConfiguration subjectConfig = new SubjectConfiguration();
        subjectConfig.userId(UserHelper.getUserId(context));

        EmitterConfiguration emitterConfig = new EmitterConfiguration()
                .retryFailedRequests(false);

        TrackerController trackerController = Snowplow.createTracker(
                context,
                NAMESPACE,
                networkConfig,
                trackerConfig,
                subjectConfig,
                emitterConfig
        );

        Snowplow.setTrackerAsDefault(trackerController);

        mTracker = trackerController;
        mPaused = false;
        mContext = context;
        mIsTrackingEnabled = SharedPreferencesHelpers.getBoolean(mContext, PREF_ANALYTICS_ENABLED, true);
    }

    public static TrackingManager getInstance(final Context context) {
        if (sInstance == null) {
            synchronized (TrackingManager.class) {
                if (sInstance == null) {
                    sInstance = new TrackingManager(context);
                }
            }
        }
        return sInstance;
    }

    public void onPause() {
        mPaused = true;
    }

    public void trackInstall(@Nullable String referrer) {
        // Persist referrer details for later usages like search url annotation
        extractAndPersistInstallReferrerDetails(referrer);

        // Make sure this only executed once per lifetime
        if (SharedPreferencesHelpers.getBoolean(mContext, PREF_INSTALL_TRACKED, false)) {
            return;
        }

        // Build the tracking event
        Map<String, Object> installEventMap = getInstallDataHashMap(referrer);
        SelfDescribingJson installData = new SelfDescribingJson(INSTALL_SCHEMA, installEventMap);
        SelfDescribing installEevent = new SelfDescribing(installData);
        sendEvent(installEevent);

        SharedPreferencesHelpers.putBoolean(mContext, PREF_INSTALL_TRACKED, true);
    }

    private void extractAndPersistInstallReferrerDetails(@Nullable String referrer) {
        if (referrer != null && !referrer.isEmpty()) {
            Uri parsedReferrerUrl = Uri.parse(String.format("?%s", referrer));
            extractAddAndSave(parsedReferrerUrl, TYPETAG_KEY, PARAM_TYPETAG);

            for (String key : UTM_PARAMS) {
                extractAddAndSave(parsedReferrerUrl, key, key);
            }
        }
    }

    private @NonNull Map<String, Object> getInstallDataHashMap(@Nullable String referrer) {

        Map<String, Object> installEventMap = new HashMap<>();

        if (referrer != null && !referrer.isEmpty()) {
            // adding the complete referrer
            putKeyAndValueToMap(REFERRAL_KEY, referrer, installEventMap);

            Uri parsedReferrerUrl = Uri.parse(String.format("?%s", referrer));

            // extract and add type-tags and utm parameters
            extractToMap(parsedReferrerUrl, TYPETAG_KEY, PARAM_TYPETAG, installEventMap);
            for (String key : UTM_PARAMS) {
                extractToMap(parsedReferrerUrl, key, key, installEventMap);
            }
        }
        return installEventMap;
    }

    private void putKeyAndValueToMap(String key, Object value, Map<String, Object> map) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private Map<String, Object> extractToMap(Uri parsedUrl, String key, String param, Map<String, Object> map) {
        String value = parsedUrl.getQueryParameter(param);
        putKeyAndValueToMap(key, value, map);
        return map;
    }

    private void extractAddAndSave(Uri parsedUrl, String key, String param) {
        String value = parsedUrl.getQueryParameter(param);
        SharedPreferencesHelpers.putString(mContext, prefFromParam(param), value);
    }

    private static String prefFromParam(String param) {
        return "PREF_PARAM_" + param.toUpperCase(Locale.ENGLISH);
    }


    public void trackOriginEvent(Intent intent) {
        String origin = IntentUtils.safeGetStringExtra(intent, OPEN_ORIGIN);
        launchSessionEvent(origin == null ? INAPP_ORIGIN : origin);
        intent.removeExtra(OPEN_ORIGIN);
    }

    private void launchSessionEvent(final String origin) {
        if (mPaused) {
            if (hasDayPassedSinceLastResumeEvent()) {
                updateLastResumeEventTimestamp();
                sessionEvent(RESUME_KEY, origin);
            }
            mPaused = false;
        } else {
            sessionEvent(LAUNCH_KEY, origin);
        }
    }

    private boolean hasDayPassedSinceLastResumeEvent() {
        String key = TAG + SHARED_PREF_LAST_RESUME_EVENT_SUFFIX;
        long lastCheckTime = SharedPreferencesHelpers.getLong(mContext, key, 0);
        long currentTime = System.currentTimeMillis();

        if (lastCheckTime == 0) {
            return true;
        }

        Calendar lastCheckCalendar = Calendar.getInstance();
        lastCheckCalendar.setTimeInMillis(lastCheckTime);

        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTimeInMillis(currentTime);

        DateHolder lastCheckDateHolder = new DateHolder(lastCheckCalendar);
        DateHolder currentDateHolder = new DateHolder(currentCalendar);

        return currentDateHolder.isAtLeastOneDayLaterAs(lastCheckDateHolder);
    }

    private void updateLastResumeEventTimestamp() {
        String key = TAG + SHARED_PREF_LAST_RESUME_EVENT_SUFFIX;
        long currentTime = System.currentTimeMillis();
        SharedPreferencesHelpers.putLong(mContext, key, currentTime);
    }

    private void sessionEvent(final String action, final String origin) {
        Structured event = new Structured(ACTIVITY_KEY, action)
                .label(origin)
                .property(getDefaultBrowserProperty());
        String ecccValue = SharedPreferencesHelpers.getString(ContextUtils.getApplicationContext(), PREF_ECCC_VALUE, null);
        if (ecccValue != null) {
            SelfDescribingJson json = new SelfDescribingJson(CONSENT_SCHEMA, Map.of("cookie_consent", ecccValue));
            event.setEntities(List.of(json));
        }
        sendEvent(event);
    }

    public void engagementEvent(final String label, final double rank) {
        sendEvent(
                new Structured(CATEGORY_ENGAGEMENT, ACTION_CLICK)
                        .label(label)
                        .value(rank)
        );
    }

    public void invitationsEvent(final String action, @Nullable final String label) {
        sendEvent(
                new Structured(CATEGORY_INVITATIONS, action)
                        .label(label)
        );
    }


    public String ecosify(final String url, final boolean isZeroUuid) {
        Uri.Builder urlBuilder = Uri.parse(url).buildUpon();
        appendParamFromPref(urlBuilder, PARAM_TYPETAG);
        for (String param : UTM_PARAMS) {
            appendParamFromPref(urlBuilder, param);
        }

        // patch all ecosia.org urls with userId to enable x-platform tracking
        if (mContext != null && !(urlBuilder.build().getQueryParameterNames().contains(PARAM_SP_USER_ID))) {
            String userId = isZeroUuid ? new UUID(0, 0).toString() : UserHelper.getUserId(mContext);
            urlBuilder.appendQueryParameter(PARAM_SP_USER_ID, userId);
        }

        return urlBuilder.build().toString();
    }

    private void appendParamFromPref(Uri.Builder builder, String param) {
        String val = SharedPreferencesHelpers.getString(mContext, prefFromParam(param), "");

        // only append key and value if not existing yet
        if (!val.isEmpty() && !(builder.build().getQueryParameterNames().contains(param))) {
            builder.appendQueryParameter(param, val);
        }
    }


    private String getDefaultBrowserProperty() {
        return SettingsHelpers.isEcosiaDefaultBrowser(mContext) ? PROPERTY_DEFAULT_BROWSER_SET : "";
    }

    // Intro tracking

    private void sendIntroClickEvent(final String label, final int page) {
        sendEvent(
                new Structured(CATEGORY_INTRO, ACTION_CLICK)
                        .label(label)
                        .property(getIntroScreenName(page))
                        .value((double) page)
        );
    }

    public void sendIntroNextClickEvent(final int page) {
        sendIntroClickEvent(LABEL_NEXT, page);
    }

    public void sendIntroSkipClickEvent(final int page) {
        sendIntroClickEvent(LABEL_SKIP, page);
    }

    public void sendIntroDisplayEvent(final int page) {
        sendEvent(
                new Structured(CATEGORY_INTRO, ACTION_DISPLAY)
                        .property(getIntroScreenName(page))
                        .value((double) page)
        );
    }

    private String getIntroScreenName(final int page) {
        switch (page) {
            case 0:
                return SCREEN_START;
            case 1:
                return SCREEN_GREEN_SEARCH;
            case 2:
                return SCREEN_PROFITS;
            case 3:
                return SCREEN_ACTION;
            case 4:
                return SCREEN_TRANSPARENT_FINANCES;
            default:
                return "";
        }
    }

    // AdBlock tracking

    private void adblockChangeEvents(String label, boolean bResult) {
        sendEvent(
                new Structured(CATEGORY_ADBLOCK, ACTION_CLICK)
                        .label(label)
                        .property(getPrefChangeStatus(bResult))
        );
    }

    public void displayAdblockMenuEvent() {
        sendEvent(
                new Structured(CATEGORY_ADBLOCK, ACTION_DISPLAY)
                        .label(LABEL_ADBLOCK_MENU)
        );
    }

    //This event refers to Adblock Settings from popup
    public void displayAdblockPopupWindowEvent() {
        sendEvent(
                new Structured(CATEGORY_ADBLOCK, ACTION_CLICK)
                        .label(LABEL_ADBLOCK_POPUP)
        );
    }

    //This event refers to Adblock popup in NTP page
    public void showAdblockPopupDialog() {
        sendEvent(
                new Structured(CATEGORY_ADBLOCK, ACTION_CLICK)
                        .label(LABEL_DISPLAY_POPUP)
        );
    }

    public void changeAdblockEvent(boolean bResult) {
        adblockChangeEvents(LABEL_CHANGE_ADBLOCK_STATUS, bResult);
    }

    public void changeAcceptableAdsEvent(boolean bResult) {
        adblockChangeEvents(LABEL_CHANGE_ACCEPTABLE_ADS_STATUS, bResult);
    }

    private String getPrefChangeStatus(boolean bResult) {
        return bResult ? LABEL_ENABLE : LABEL_DISABLE;
    }

    // Default browser tracking

    public void defaultBrowserChangeEvent() {
        sendEvent(
                new Structured(CATEGORY_NOTIFICATION, ACTION_CLICK)
                        .label(LABEL_DEFAULT_BROWSER_CHANGE)
        );
    }

    public void defaultBrowserDismissEvent() {
        sendEvent(
                new Structured(CATEGORY_NOTIFICATION, ACTION_CLICK)
                        .label(LABEL_DISMISS)
        );
    }

    public boolean isTrackingEnabled() {
        mIsTrackingEnabled = SharedPreferencesHelpers.getBoolean(mContext, PREF_ANALYTICS_ENABLED, true);
        return mIsTrackingEnabled;
    }

    public void setIsTrackingEnabled(boolean bResult) {
        //Sending event in both enable/disable cases of opt out switch
        mTracker.track(
                new Structured(CATEGORY_SETTINGS, ACTION_CHANGE)
                        .label(LABEL_ANALYTICS)
                        .property(getPrefChangeStatus(bResult))
        );
        SharedPreferencesHelpers.putBoolean(mContext, PREF_ANALYTICS_ENABLED, bResult);
    }

    // NTP Customization

    public void sendTopSitesChangeEvent(boolean action) {
        sendEvent(
                new Structured(CATEGORY_NTP, getPrefChangeStatus(action))
                        .label(LABEL_TOP_SITES)
        );
    }

    public void sendImpactChangeEvent(boolean action) {
        sendEvent(
                new Structured(CATEGORY_NTP, getPrefChangeStatus(action))
                        .label(LABEL_IMPACT)
        );
    }

    public void sendNewsChangeEvent(boolean action) {
        sendEvent(
                new Structured(CATEGORY_NTP, getPrefChangeStatus(action))
                        .label(LABEL_NEWS)
        );
    }

    public void sendAboutChangeEvent(boolean action) {
        sendEvent(
                new Structured(CATEGORY_NTP, getPrefChangeStatus(action))
                        .label(LABEL_ABOUT)
        );
    }

    public void sendCustomizeNewTabPageButtonClickEvent() {
        sendEvent(
                new Structured(CATEGORY_NTP, ACTION_CLICK)
                        .label(LABEL_CUSTOMIZE)
        );
    }

    // Events dispatch

    private void sendEvent(SelfDescribing event) {
        if(mIsTrackingEnabled) {
            mTracker.track(event);
        } else {
            Log.e(TAG, "Tracking event is disabled");
        }
    }

    private void sendEvent(Structured event) {
        if(mIsTrackingEnabled) {
            mTracker.track(event);
        } else {
            Log.e(TAG, "Tracking event is disabled");
        }
    }

}
