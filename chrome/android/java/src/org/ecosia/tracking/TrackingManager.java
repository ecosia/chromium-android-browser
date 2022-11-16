package org.ecosia.tracking;

import com.snowplowanalytics.snowplow.tracker.Emitter;
import com.snowplowanalytics.snowplow.tracker.Subject;
import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestSecurity;
import com.snowplowanalytics.snowplow.tracker.events.SelfDescribing;
import com.snowplowanalytics.snowplow.tracker.events.Structured;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import org.chromium.components.version_info.VersionInfo;
import org.chromium.base.IntentUtils;
import org.ecosia.utils.SharedPreferencesHelpers;
import org.ecosia.utils.SettingsHelpers;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

import org.chromium.base.IntentUtils;
import org.chromium.base.Log;
import org.chromium.components.version_info.VersionInfo;
import org.ecosia.incentives.SearchIncentivesManager;
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
    private static final String RECEIVER_URL_STAGING = "org-ecosia.mini.snplow.net";

    private static final String INSTALL_SCHEMA = "iglu:org.ecosia/android_install_event/jsonschema/1-0-0";

    private static final String EVENT_SCHEME = "se";
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
    
    private static final String PREF_ANALYTICS_ENABLED = "com.ecosia.PREF_ANALYTICS_ENABLED";
    private static final String CATEGORY_ENGAGEMENT = "engagement";
    private static final String CATEGORY_INTRO = "intro";
    private static final String CATEGORY_NOTIFICATION = "notification";
    private static final String CATEGORY_ADBLOCK = "adblock";
    private static final String CATEGORY_SETTINGS = "settings";
    public static final String ACTION_CLICK = "click";
    public static final String ACTION_DISPLAY = "display";
    private static final String ACTION_CHANGE = "change";
    private static final String LABEL_ADBLOCK_MENU = "show_adblock_menu";
    private static final String LABEL_ADBLOCK_POPUP = "show_adblock_popup";
    private static final String LABEL_CHANGE_ADBLOCK_STATUS = "change_adblock_status";
    private static final String LABEL_DISPLAY_POPUP = "display_adblock_popup";
    private static final String LABEL_CHANGE_ACCEPTABLE_ADS_STATUS = "change_acceptable_ads_status";
    public static final String LABEL_NEWS = "news";
    private static final String LABEL_SKIP = "skip";
    private static final String LABEL_NEXT = "next";
    private static final String LABEL_FIRST_PAGE = "first_page";
    public static final String LABEL_DEFAULT_BROWSER_CHANGE = "default_browser_change";
    private static final String LABEL_DISMISS = "dismiss";
    private static final String LABEL_ENABLE = "enable";
    private static final String LABEL_DISABLE = "disable";
    private static final String LABEL_ANALYTICS = "analytics";
    private static final String SCREEN_START = "start";
    private static final String SCREEN_SEARCH = "search";
    private static final String SCREEN_GREEN_SEARCH = "green_search";
    private static final String SCREEN_PROFITS = "profits";
    private static final String SCREEN_ACTION = "action";
    private static final String SCREEN_PRIVACY = "privacy";
    private static final String SCREEN_TRANSPARENT_FINANCES= "transparent_finances";
    private static final String PROPERTY_DEFAULT_BROWSER_SET = "default_browser_set";
    private static final String TAG = TrackingManager.class.getSimpleName();

    private static final String TAG_FILTER_PLUGIN_CONFIG = ".filterPluginConfiguration";

    private static final String SHARED_PREF_LAST_RESUME_EVENT_PREFIX = TrackingManager.class.getSimpleName();
    private static final String SHARED_PREF_LAST_RESUME_EVENT_SUFFIX = ".lastResumeEvent";

    private static final String EVENT_PAYLOAD_LABEL = "se_la";

    private static final String EVENT_PAYLOAD_CATEGORY = "se_ca";

    private static final String EVENT_PAYLOAD_ACTION = "se_ac";

    private static volatile TrackingManager sInstance;
    private final Tracker mTracker;
    private boolean mPaused;
    private final Context mContext;
    private boolean mIsTrackingEnabled;

    private long mLastResumeEventTimestamp = 0L;

    private TrackingManager(final Context context) {
        String receiverUrl = VersionInfo.isOfficialBuild() ? RECEIVER_URL : RECEIVER_URL_STAGING;
        Emitter emitter = new Emitter.EmitterBuilder(receiverUrl, context).security(RequestSecurity.HTTPS).build();
        String versionCode;
        try {
            versionCode = Integer.toString(
                    context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            versionCode = "";
        }
        Subject subject = new Subject.SubjectBuilder().build();
        subject.setUserId(UserHelper.getUserId(context));

        Tracker.TrackerBuilder builder = new Tracker.TrackerBuilder(emitter, NAMESPACE, versionCode, context);
        builder.mobileContext(true).applicationCrash(true).subject(subject);
        mTracker = Tracker.init(builder.build());
        mPaused = false;
        mContext = context;
        mIsTrackingEnabled = SharedPreferencesHelpers.getBoolean(mContext, PREF_ANALYTICS_ENABLED, true);
    }

    /* MOB-1982 deactivated filtering until we upgrade back to SP 5.x
    private PluginConfiguration createFilterPlugin() {
        final String tag = TAG + TAG_FILTER_PLUGIN_CONFIG;
        final PluginConfiguration pluginConfiguration = new PluginConfiguration(tag);
        final List<String> schemas = Collections.singletonList(EVENT_SCHEME);
        pluginConfiguration.filter(schemas, (inspectableEvent -> shouldTrack(inspectableEvent)));
        return pluginConfiguration;
    }

    private boolean shouldTrack(final InspectableEvent event) {
        final Map<String, Object> payload = event.getPayload();
        final Object label = payload.get(EVENT_PAYLOAD_LABEL);
        final Object category = payload.get(EVENT_PAYLOAD_CATEGORY);
        final Object action = payload.get(EVENT_PAYLOAD_ACTION);

        if (allObjectsAreOfTypeString(label, category, action)) {
            final boolean isLabelInApp = INAPP_ORIGIN.equalsIgnoreCase((String) label);
            final boolean isCategoryActivity = ACTIVITY_KEY.equalsIgnoreCase((String) category);
            final boolean isActionResume = RESUME_KEY.equalsIgnoreCase((String) action);
            if (isLabelInApp && isCategoryActivity && isActionResume) {
                return atLeastOneDayElapsedSinceLastResumeEvent();
            }
        }

        return true;
    }

    private boolean allObjectsAreOfTypeString(final Object... objects) {
        for (Object object : objects) {
            if (!(object instanceof String)) {
                return false;
            }
        }
        return true;
    }

    private boolean atLeastOneDayElapsedSinceLastResumeEvent() {
        final Calendar calendar = Calendar.getInstance();
        final TrackingDateHolder now = new TrackingDateHolder(calendar);

        restoreLastTrackedTimestampIfNeeded();
        calendar.setTimeInMillis(mLastResumeEventTimestamp);

        final TrackingDateHolder lastTracked = new TrackingDateHolder(calendar);
        return now.isAtLeastOneDayLaterAs(lastTracked);
    }

    private void restoreLastTrackedTimestampIfNeeded() {
        if (mLastResumeEventTimestamp <= 0) {
            mLastResumeEventTimestamp = SharedPreferencesHelpers.getLong(mContext, SHARED_PREF_LAST_RESUME_EVENT_PREFIX + SHARED_PREF_LAST_RESUME_EVENT_SUFFIX, 0);
        }
    }
    */

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

    public void install(String referrer) {
        Uri parsedReferrerUrl = Uri.parse(String.format("?%s", referrer));
        Map<String, Object> installEvent = new HashMap<>();
        addConditionally(REFERRAL_KEY, referrer, installEvent);
        extractAddAndSave(parsedReferrerUrl, TYPETAG_KEY, PARAM_TYPETAG, installEvent);
        for (String key : UTM_PARAMS) {
            extractAddAndSave(parsedReferrerUrl, key, key, installEvent);
        }

        SelfDescribingJson installData = new SelfDescribingJson(INSTALL_SCHEMA, installEvent);
        SelfDescribing installEevent = SelfDescribing.builder().eventData(installData).build();
        sendEvent(installEevent);
    }

    private void addConditionally(String key, Object value, Map<String, Object> map) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private void extractAddAndSave(Uri parsedUrl, String key, String param, Map<String, Object> map) {
        String value = parsedUrl.getQueryParameter(param);
        addConditionally(key, value, map);
        SharedPreferencesHelpers.putString(mContext, prefFromParam(param), value);
    }

    public void trackOriginEvent(Intent intent) {
        String origin = IntentUtils.safeGetStringExtra(intent, OPEN_ORIGIN);
        launchSessionEvent(origin == null ? INAPP_ORIGIN : origin);
        intent.removeExtra(OPEN_ORIGIN);
    }

    private void launchSessionEvent(final String origin) {
        if (mPaused) {
            sessionEvent(RESUME_KEY, origin);
            mPaused = false;
        } else {
            sessionEvent(LAUNCH_KEY, origin);
        }
    }

    private void sessionEvent(final String action, final String origin) {
        sendEvent(
                Structured.builder()
                .category(ACTIVITY_KEY)
                .action(action)
                .label(origin)
                .property(getDefaultBrowserProperty())
                .build()
        );
    }

    public void engagementEvent(final String label, final double rank) {
        sendEvent(
                Structured.builder()
                .category(CATEGORY_ENGAGEMENT)
                .action(ACTION_CLICK)
                .label(label)
                .value(rank)
                .build()
        );
    }

    private Uri.Builder appendParamFromPref(Uri.Builder builder, String param) {
        String val = SharedPreferencesHelpers.getString(mContext, prefFromParam(param), "");
        if (!val.isEmpty()) {
            builder.appendQueryParameter(param, val);
        }
        return builder;
    }

    private static String prefFromParam(String param) {
        return "PREF_PARAM_" + param.toUpperCase(Locale.ENGLISH);
    }

    public String ecosify(final String url, final boolean isIncognito) {
        Uri.Builder urlBuilder = Uri.parse(url).buildUpon();
        appendParamFromPref(urlBuilder, PARAM_TYPETAG);
        for (String param : UTM_PARAMS) {
            appendParamFromPref(urlBuilder, param);
        }

        // patch all ecosia.org urls with userId to enable x-platform tracking
        if (mContext != null) {
            String userId = isIncognito ? new UUID(0, 0).toString() : UserHelper.getUserId(mContext);
            urlBuilder.appendQueryParameter(PARAM_SP_USER_ID, userId);
        }

        return urlBuilder.build().toString();
    }

    private String getDefaultBrowserProperty() {
        return SettingsHelpers.isEcosiaDefaultBrowser(mContext) ? PROPERTY_DEFAULT_BROWSER_SET : "";
    }

    // Intro tracking

    private void sendIntroClickEvent(final String label, final int page) {
        sendEvent(
                Structured.builder()
                .category(CATEGORY_INTRO)
                .action(ACTION_CLICK)
                .label(label)
                .property(getIntroScreenName(page))
                .value((double) page)
                .build()
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
                Structured.builder()
                .category(CATEGORY_INTRO)
                .action(ACTION_DISPLAY)
                .property(getIntroScreenName(page))
                .value((double) page)
                .build()
        );
    }

    private String getIntroScreenName(final int page) {
        boolean isRestricted = SearchIncentivesManager.getInstance(mContext).isRestricted();
        switch (page) {
            case 0:
                return SCREEN_START;
            case 1:
                return isRestricted ? SCREEN_GREEN_SEARCH : SCREEN_SEARCH;
            case 2:
                return SCREEN_PROFITS;
            case 3:
                return SCREEN_ACTION;
            case 4:
                return isRestricted ? SCREEN_TRANSPARENT_FINANCES : SCREEN_PRIVACY;
            default:
                return "";
        }
    }

    // AdBlock tracking

    private void adblockChangeEvents(String label, boolean bResult) {
        sendEvent(
                Structured.builder()
                .category(CATEGORY_ADBLOCK)
                .action(ACTION_CLICK)
                .label(label)
                .property(getPrefChangeStatus(bResult))
                .build()
        );
    }

    public void displayAdblockMenuEvent() {
        sendEvent(
                Structured.builder()
                .category(CATEGORY_ADBLOCK)
                .action(ACTION_DISPLAY)
                .label(LABEL_ADBLOCK_MENU)
                .build()
        );
    }

    //This event refers to Adblock Settings from popup
    public void displayAdblockPopupWindowEvent() {
        sendEvent(
                Structured.builder()
                .category(CATEGORY_ADBLOCK)
                .action(ACTION_CLICK)
                .label(LABEL_ADBLOCK_POPUP)
                .build()
        );
    }

    //This event refers to Adblock popup in NTP page
    public void showAdblockPopupDialog() {
        sendEvent(
                Structured.builder()
                .category(CATEGORY_ADBLOCK)
                .action(ACTION_CLICK)
                .label(LABEL_DISPLAY_POPUP)
                .build()
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
                Structured.builder()
                .category(CATEGORY_NOTIFICATION) 
                .action(ACTION_CLICK)
                .label(LABEL_DEFAULT_BROWSER_CHANGE)
                .build()
        );
    }

    public void defaultBrowserDismissEvent() {
        sendEvent(
                Structured.builder()
                .category(CATEGORY_NOTIFICATION)
                .action(ACTION_CLICK)
                .label(LABEL_DISMISS)
                .build()
        );
    }

    public boolean isTrackingEnabled() {
        mIsTrackingEnabled = SharedPreferencesHelpers.getBoolean(mContext, PREF_ANALYTICS_ENABLED, true);
        return mIsTrackingEnabled;
    }

    public void setIsTrackingEnabled(boolean bResult) {
        //Sending event in both enable/disable cases of opt out switch
        mTracker.track(
                Structured.builder()
                .category(CATEGORY_SETTINGS) 
                .action(ACTION_CHANGE)
                .label(LABEL_ANALYTICS)
                .property(getPrefChangeStatus(bResult))
                .build()
        );
        SharedPreferencesHelpers.putBoolean(mContext, PREF_ANALYTICS_ENABLED, bResult);
    }

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
