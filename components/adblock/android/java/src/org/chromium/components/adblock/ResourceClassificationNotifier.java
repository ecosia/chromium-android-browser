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

import androidx.annotation.UiThread;

import org.chromium.base.ContextUtils;
import org.chromium.base.ThreadUtils;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.NativeMethods;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @brief Allows observing notifications from ongoing resource classification.
 * Classification combines filters from all FilteringConfigurations.
 * Lives on UI thread, not thread-safe.
 */
public final class ResourceClassificationNotifier {
    private static final String TAG = ResourceClassificationNotifier.class.getSimpleName();

    private static ResourceClassificationNotifier sInstance;
    private final Set<ResourceFilteringObserver> mResourceFilteringObservers = new HashSet<>();
    // TODO(mpawlowski) in the future, we can consider adding filter hit
    // notifications here as well (DPD-1233)

    public interface ResourceFilteringObserver {
        /**
         * "Request allowed" event for a request which would be blocked but there
         * was an allowlisting filter found.
         *
         * It should not block the UI thread for too long.
         * @param info contains auxiliary information about the resource.
         */
        @UiThread
        void onRequestAllowed(ResourceFilteringCounters.ResourceInfo info);

        /**
         * "Request blocked" event for a request which was blocked.
         *
         * It should not block the UI thread for too long.
         * @param info contains auxiliary information about the resource.
         */
        @UiThread
        void onRequestBlocked(ResourceFilteringCounters.ResourceInfo info);

        /**
         * "Page allowed" event for an allowlisted domain (page).
         *
         * It should not block the UI thread for too long.
         * @param info contains auxiliary information about the resource.
         */
        @UiThread
        void onPageAllowed(ResourceFilteringCounters.ResourceInfo info);

        /**
         * "Popup allowed" event for a popup which would be blocked but there
         * was an allowlisting filter found.
         *
         * It should not block the UI thread for too long.
         * @param info contains auxiliary information about the resource.
         */
        @UiThread
        void onPopupAllowed(ResourceFilteringCounters.ResourceInfo info);

        /**
         * "Popup blocked" event for a popup which was blocked.
         *
         * It should not block the UI thread for too long.
         * @param info contains auxiliary information about the resource.
         */
        @UiThread
        void onPopupBlocked(ResourceFilteringCounters.ResourceInfo info);
    }

    // DEPRECATED and will be removed in version v121.
    @Deprecated
    public interface AdBlockedObserver extends ResourceFilteringObserver {}

    private ResourceClassificationNotifier() {
        ResourceClassificationNotifierJni.get().bind(this);
    }

    public static ResourceClassificationNotifier getInstance() {
        ThreadUtils.assertOnUiThread();
        if (sInstance == null) {
            sInstance = new ResourceClassificationNotifier();
        }
        return sInstance;
    }

    @UiThread
    public void addResourceFilteringObserver(final ResourceFilteringObserver observer) {
        mResourceFilteringObservers.add(observer);
    }

    @UiThread
    public void removeResourceFilteringObserver(final ResourceFilteringObserver observer) {
        mResourceFilteringObservers.remove(observer);
    }

    @CalledByNative
    private void requestMatchedCallback(final String requestUrl, boolean wasBlocked,
            final String[] parentFrameUrls, final String subscriptionUrl,
            final String configurationName, final int contentType, final int tabId) {
        ThreadUtils.assertOnUiThread();
        final List<String> parentsList = Arrays.asList(parentFrameUrls);
        final ResourceFilteringCounters.ResourceInfo resourceInfo =
                new ResourceFilteringCounters.ResourceInfo(requestUrl, parentsList, subscriptionUrl,
                        configurationName, contentType, tabId);
        Log.d(TAG,
                "eyeo: requestMatchedCallback() notifies " + mResourceFilteringObservers.size()
                        + " listeners about " + resourceInfo.toString()
                        + (wasBlocked ? " getting blocked" : " being allowed"));
        for (final ResourceFilteringObserver observer : mResourceFilteringObservers) {
            if (wasBlocked) {
                observer.onRequestBlocked(resourceInfo);
            } else {
                observer.onRequestAllowed(resourceInfo);
            }
        }
    }

    @CalledByNative
    private void pageAllowedCallback(final String requestUrl, final String subscriptionUrl,
            final String configurationName, final int tabId) {
        ThreadUtils.assertOnUiThread();
        final ResourceFilteringCounters.ResourceInfo resourceInfo =
                new ResourceFilteringCounters.ResourceInfo(requestUrl, new ArrayList(),
                        subscriptionUrl, configurationName,
                        ContentType.CONTENT_TYPE_OTHER.getValue(), tabId);
        Log.d(TAG,
                "eyeo: pageAllowedCallback() notifies " + mResourceFilteringObservers.size()
                        + " listeners about " + resourceInfo.toString());
        for (final ResourceFilteringObserver observer : mResourceFilteringObservers) {
            observer.onPageAllowed(resourceInfo);
        }
    }

    @CalledByNative
    private void popupMatchedCallback(final String requestUrl, boolean wasBlocked,
            final String openerUrl, final String subscription, final String configurationName,
            final int tabId) {
        ThreadUtils.assertOnUiThread();
        final List<String> parentsList = Arrays.asList(openerUrl);
        final ResourceFilteringCounters.ResourceInfo resourceInfo =
                new ResourceFilteringCounters.ResourceInfo(requestUrl, parentsList, subscription,
                        configurationName, ContentType.CONTENT_TYPE_OTHER.getValue(), tabId);
        Log.d(TAG,
                "eyeo: popupMatchedCallback() notifies " + mResourceFilteringObservers.size()
                        + " listeners about " + resourceInfo.toString()
                        + (wasBlocked ? " getting blocked" : " being allowed"));
        for (final ResourceFilteringObserver observer : mResourceFilteringObservers) {
            if (wasBlocked) {
                observer.onPopupBlocked(resourceInfo);
            } else {
                observer.onPopupAllowed(resourceInfo);
            }
        }
    }

    @NativeMethods
    interface Natives {
        void bind(ResourceClassificationNotifier caller);
    }
}
