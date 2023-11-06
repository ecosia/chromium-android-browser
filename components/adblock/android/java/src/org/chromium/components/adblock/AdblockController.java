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
import android.util.Log;
import android.webkit.URLUtil;

import androidx.annotation.UiThread;

import org.chromium.base.ThreadUtils;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.NativeMethods;
import org.chromium.components.adblock.controller.R;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DEPRECATED, please use {@link FilteringConfiguration} instead.
 *
 * @brief Main access point for java UI code to control ad filtering.
 * It calls its native counter part also AdblockController.
 * It lives in UI thread on the browser process.
 */
@Deprecated
public final class AdblockController extends FilteringConfiguration {
    private static final String TAG = AdblockController.class.getSimpleName();

    private static AdblockController sInstance;

    private URL mAcceptableAds;

    private AdblockController() {
        super("adblock");
        try {
            mAcceptableAds =
                    new URL("https://easylist-downloads.adblockplus.org/exceptionrules.txt");
        } catch (java.net.MalformedURLException e) {
            mAcceptableAds = null;
        }
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

    public static class Subscription {
        private URL mUrl;
        private String mTitle;
        private String mVersion = "";
        private String[] mLanguages = {};

        public Subscription(final URL url, final String title, final String version) {
            this.mUrl = url;
            this.mTitle = title;
            this.mVersion = version;
        }

        @CalledByNative("Subscription")
        public Subscription(
                final URL url, final String title, final String version, final String[] languages) {
            this.mUrl = url;
            this.mTitle = title;
            this.mVersion = version;
            this.mLanguages = languages;
        }

        public String title() {
            return mTitle;
        }

        public URL url() {
            return mUrl;
        }

        public String version() {
            return mVersion;
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
    public List<Subscription> getRecommendedSubscriptions() {
        return (List<Subscription>) (List<?>) Arrays.asList(
                AdblockControllerJni.get().getRecommendedSubscriptions());
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

    // DEPRECATED, please use {@link ResourceClassificationNotifier#ResourceFilteringObserver}
    // instead. This interface will be removed in version v121.
    @Deprecated
    public interface AdBlockedObserver extends ResourceClassificationNotifier.AdBlockedObserver {
        @UiThread
        void onAdAllowed(AdblockCounters.ResourceInfo info);

        @UiThread
        void onAdBlocked(AdblockCounters.ResourceInfo info);

        @UiThread
        void onPopupAllowed(AdblockCounters.ResourceInfo info);

        @UiThread
        void onPopupBlocked(AdblockCounters.ResourceInfo info);

        @UiThread
        void onPageAllowed(AdblockCounters.ResourceInfo info);

        // Pass-through to the legacy method
        @UiThread
        @Override
        default void onRequestAllowed(ResourceFilteringCounters.ResourceInfo info) {
            onAdAllowed(new AdblockCounters.ResourceInfo(info));
        }

        // Pass-through to the legacy method
        @UiThread
        @Override
        default void onRequestBlocked(ResourceFilteringCounters.ResourceInfo info) {
            onAdBlocked(new AdblockCounters.ResourceInfo(info));
        }

        // Pass-through to the legacy method
        @UiThread
        @Override
        default void onPopupAllowed(ResourceFilteringCounters.ResourceInfo info) {
            onPopupAllowed(new AdblockCounters.ResourceInfo(info));
        }

        // Pass-through to the legacy method
        @UiThread
        @Override
        default void onPopupBlocked(ResourceFilteringCounters.ResourceInfo info) {
            onPopupBlocked(new AdblockCounters.ResourceInfo(info));
        }

        // Pass-through to the legacy method
        @UiThread
        @Override
        default void onPageAllowed(ResourceFilteringCounters.ResourceInfo info) {
            onPageAllowed(new AdblockCounters.ResourceInfo(info));
        }
    }

    // DEPRECATED, please use {@link
    // ResourceClassificationNotifier#addResourceFilteringObserver(ResourceClassificationNotifier.ResourceFilteringObserver)}
    // instead. This method will be removed in version v121.
    @Deprecated
    @UiThread
    public void addOnAdBlockedObserver(
            final ResourceClassificationNotifier.AdBlockedObserver observer) {
        ResourceClassificationNotifier.getInstance().addResourceFilteringObserver(observer);
    }

    // DEPRECATED, please use {@link
    // ResourceClassificationNotifier#removeResourceFilteringObserver(ResourceClassificationNotifier.ResourceFilteringObserver)}
    // instead. This method will be removed in version v121.
    @Deprecated
    @UiThread
    public void removeOnAdBlockedObserver(
            final ResourceClassificationNotifier.AdBlockedObserver observer) {
        ResourceClassificationNotifier.getInstance().removeResourceFilteringObserver(observer);
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
    }
}
