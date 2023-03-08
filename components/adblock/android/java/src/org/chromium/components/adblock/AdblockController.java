/*
 * This file is part of eyeo Chromium SDK,
 * Copyright (C) 2006-present eyeo GmbH
 *
 * eyeo Chromium SDK is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * eyeo Chromium SDK is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with eyeo Chromium SDK.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.chromium.components.adblock;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import org.chromium.base.ContextUtils;
import org.chromium.base.ThreadUtils;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.NativeMethods;
import org.chromium.chrome.browser.preferences.Pref;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.components.user_prefs.UserPrefs;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @brief Main access point for java UI code to control ad filtering.
 * It calls its native counter part also AdblockController.
 * It lives in UI thread on the browser process.
 */
public final class AdblockController extends FilteringConfiguration {
    private static final String TAG = AdblockController.class.getSimpleName();

    private static AdblockController sInstance;

    private URL mAcceptableAds;
    private final Set<SubscriptionUpdateObserver> mSubscriptionUpdateObservers;

    private AdblockController() {
        super("adblock");
        try {
            mAcceptableAds =
                    new URL("https://easylist-downloads.adblockplus.org/exceptionrules.txt");
        } catch (java.net.MalformedURLException e) {
            mAcceptableAds = null;
        }
        mSubscriptionUpdateObservers = new CopyOnWriteArraySet<>();
    }

    /**
     * @return The singleton object.
     */
    public static AdblockController getInstance() {
        ThreadUtils.assertOnUiThread();
        if (sInstance == null) {
            sInstance = new AdblockController();
            AdblockControllerJni.get().bind(sInstance);
        }
        return sInstance;
    }

    // TODO(mpawlowski) move to FilteringConfiguration: DPD-1754
    public interface SubscriptionUpdateObserver {
        @UiThread
        void onSubscriptionDownloaded(final URL url);
    }

    public static class Subscription {
        private URL mUrl;
        private String mTitle;
        private String[] mLanguages = {};

        public Subscription(final URL url, final String title) {
            this.mUrl = url;
            this.mTitle = title;
        }

        @CalledByNative("Subscription")
        public Subscription(final URL url, final String title, final String[] languages) {
            this.mUrl = url;
            this.mTitle = title;
            this.mLanguages = languages;
        }

        public String title() {
            return mTitle;
        }

        public URL url() {
            return mUrl;
        }

        public String[] languages() {
            return mLanguages;
        }

        @Override
        public boolean equals(final Object object) {
            if (object == null) return false;
            if (getClass() != object.getClass()) return false;

            Subscription other = (Subscription) object;
            return url().equals(other.url());
        }
    }

    @UiThread
    public void setAcceptableAdsEnabled(boolean enabled) {
        if (enabled)
            addFilterList(mAcceptableAds);
        else
            removeFilterList(mAcceptableAds);
    }

    @UiThread
    public boolean isAcceptableAdsEnabled() {
        return getFilterLists().contains(mAcceptableAds);
    }

    @UiThread
    public void setMoreOptionsEnabled(boolean enabled) {
        UserPrefs.get(Profile.getLastUsedRegularProfile())
                .setBoolean(Pref.ADBLOCK_MORE_OPTIONS_ENABLED, enabled);
    }

    @UiThread
    public boolean areMoreOptionsEnabled() {
        return UserPrefs.get(Profile.getLastUsedRegularProfile())
                .getBoolean(Pref.ADBLOCK_MORE_OPTIONS_ENABLED);
    }

    @UiThread
    public List<Subscription> getRecommendedSubscriptions(Context context) {
        final List<Subscription> recommendedSubscriptions =
                (List<Subscription>) (List<?>) Arrays.asList(
                        AdblockControllerJni.get().getRecommendedSubscriptions());
        final Map<String, String> localeToTitle = getLocaleToTitleMap(context);
        for (int i = 0; i < recommendedSubscriptions.size(); ++i) {
            for (final String language : recommendedSubscriptions.get(i).languages()) {
                final String title = localeToTitle.get(language);
                if (title != null && !title.isEmpty()) {
                    recommendedSubscriptions.set(i,
                            new Subscription(recommendedSubscriptions.get(i).url(), title,
                                    recommendedSubscriptions.get(i).languages()));
                    break;
                }
            }
        }
        return recommendedSubscriptions;
    }

    // TODO(kolam) deprecate and move to FilteringConfiguration or add version
    // field to Subscription: DPD-1794
    @UiThread
    public String getSelectedSubscriptionVersion(final Subscription subscription) {
        return AdblockControllerJni.get().getSelectedSubscriptionVersion(
                subscription.url().toString());
    }

    @UiThread
    public void installSubscription(final URL url) {
        addFilterList(url);
    }

    @UiThread
    public void uninstallSubscription(final URL url) {
        removeFilterList(url);
    }

    @UiThread
    public List<Subscription> getInstalledSubscriptions() {
        return (List<Subscription>) (List<?>) Arrays.asList(
                AdblockControllerJni.get().getInstalledSubscriptions());
    }

    // TODO(mpawlowski) temporary pass-through, to enable gradual deprecation.
    public interface AdBlockedObserver extends ResourceClassificationNotifier.AdBlockedObserver {}
    // TODO(mpawlowski) deprecate and remove, use ResourceClassificationNotifier directly.
    @UiThread
    public void addOnAdBlockedObserver(
            final ResourceClassificationNotifier.AdBlockedObserver observer) {
        ResourceClassificationNotifier.getInstance().addOnAdBlockedObserver(observer);
    }

    // TODO(mpawlowski) deprecate and remove, use ResourceClassifier directly.
    @UiThread
    public void removeOnAdBlockedObserver(
            final ResourceClassificationNotifier.AdBlockedObserver observer) {
        ResourceClassificationNotifier.getInstance().removeOnAdBlockedObserver(observer);
    }

    @UiThread
    public void addSubscriptionUpdateObserver(final SubscriptionUpdateObserver observer) {
        mSubscriptionUpdateObservers.add(observer);
    }

    @UiThread
    public void removeSubscriptionUpdateObserver(final SubscriptionUpdateObserver observer) {
        mSubscriptionUpdateObservers.remove(observer);
    }

    @UiThread
    public void composeFilterSuggestions(@NonNull final AdblockElement element,
            @NonNull final AdblockComposeFilterSuggestionsCallback callback) {
        AdblockControllerJni.get().composeFilterSuggestions(element, callback);
    }

    private List<URL> transform(String[] urls) {
        if (urls == null) return null;

        List<URL> result = new ArrayList<URL>();
        for (String url : urls) {
            try {
                result.add(new URL(URLUtil.guessUrl(url)));
            } catch (MalformedURLException e) {
                Log.e(TAG, "Error parsing url: " + url);
            }
        }

        return result;
    }

    private Map<String, String> getLocaleToTitleMap(final Context context) {
        final Resources resources = context.getResources();
        final String[] locales =
                resources.getStringArray(R.array.fragment_adblock_general_locale_title);
        final String separator = resources.getString(R.string.fragment_adblock_general_separator);
        final Map<String, String> localeToTitle = new HashMap<>(locales.length);
        for (final String localeAndTitlePair : locales) {
            // in `String.split()` separator is a regexp, but we want to treat it as a string
            final int separatorIndex = localeAndTitlePair.indexOf(separator);
            final String locale = localeAndTitlePair.substring(0, separatorIndex);
            final String title = localeAndTitlePair.substring(separatorIndex + 1);
            localeToTitle.put(locale, title);
        }

        return localeToTitle;
    }

    @CalledByNative
    private void subscriptionUpdatedCallback(final String url) {
        ThreadUtils.assertOnUiThread();
        try {
            URL subscriptionUrl = new URL(URLUtil.guessUrl(url));
            for (final SubscriptionUpdateObserver observer : mSubscriptionUpdateObservers) {
                observer.onSubscriptionDownloaded(subscriptionUrl);
            }
        } catch (MalformedURLException e) {
            Log.e(TAG, "Error parsing subscription url: " + url);
        }
    }

    @NativeMethods
    interface Natives {
        void bind(AdblockController caller);
        Object[] getInstalledSubscriptions();
        Object[] getRecommendedSubscriptions();
        String getSelectedSubscriptionVersion(String subscription);
        void composeFilterSuggestions(
                AdblockElement element, AdblockComposeFilterSuggestionsCallback callback);
    }
}
