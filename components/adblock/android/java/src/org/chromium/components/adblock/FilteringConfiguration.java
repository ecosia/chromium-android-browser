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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @brief Represents an independent configuration of filters, filter lists,
 * allowed domains and other settings that influence resource filtering and
 * content blocking.
 * Multiple Filtering Configurations can co-exist and be controlled separately.
 * A network resource is blocked if any enabled Filtering Configuration
 * determines it should be, through its filters.
 * Elements on websites are hidden according to a superset of element-hiding
 * selectors from all enabled Filtering Configurations.
 * Lives on UI thread, not thread-safe.
 */
public class FilteringConfiguration {
    private static final String TAG = FilteringConfiguration.class.getSimpleName();

    private final Set<ConfigurationChangeObserver> mConfigurationChangeObservers = new HashSet<>();
    protected final Set<SubscriptionUpdateObserver> mSubscriptionUpdateObservers = new HashSet<>();
    private final String mName;
    private final static Set<FilteringConfiguration> mConfigurations = new HashSet<>();

    public interface ConfigurationChangeObserver {
        /**
         * Triggered when the FilteringConfiguration becomes disabled or enabled.
         */
        @UiThread
        void onEnabledStateChanged();

        /**
         * Triggered when the collection of installed filter lists changes.
         */
        @UiThread
        void onFilterListsChanged();

        /**
         * Triggered when the set of allowed domain changes.
         */
        @UiThread
        void onAllowedDomainsChanged();

        /**
         * Triggered when the set of custom filters changes.
         */
        @UiThread
        void onCustomFiltersChanged();
    }

    public interface SubscriptionUpdateObserver {
        @UiThread
        void onSubscriptionDownloaded(final URL url);
    }

    @UiThread
    public void addObserver(final ConfigurationChangeObserver observer) {
        mConfigurationChangeObservers.add(observer);
    }

    @UiThread
    public void removeObserver(final ConfigurationChangeObserver observer) {
        mConfigurationChangeObservers.remove(observer);
    }

    @UiThread
    public void addSubscriptionUpdateObserver(final SubscriptionUpdateObserver observer) {
        mSubscriptionUpdateObservers.add(observer);
    }

    @UiThread
    public void removeSubscriptionUpdateObserver(final SubscriptionUpdateObserver observer) {
        mSubscriptionUpdateObservers.remove(observer);
    }

    /**
     * @throws IllegalStateException when called on removed FilteringConfiguration.
     */
    @UiThread
    public void setEnabled(boolean enabled) throws IllegalStateException {
        FilteringConfigurationJni.get().setEnabled(mName, enabled);
    }

    /**
     * @throws IllegalStateException when called on removed FilteringConfiguration.
     */
    @UiThread
    public boolean isEnabled() throws IllegalStateException {
        return FilteringConfigurationJni.get().isEnabled(mName);
    }

    /**
     * @throws IllegalStateException when called on removed FilteringConfiguration.
     */
    @UiThread
    public void addFilterList(final URL url) throws IllegalStateException {
        FilteringConfigurationJni.get().addFilterList(mName, url.toString());
    }

    /**
     * @throws IllegalStateException when called on removed FilteringConfiguration.
     */
    @UiThread
    public void removeFilterList(final URL url) throws IllegalStateException {
        FilteringConfigurationJni.get().removeFilterList(mName, url.toString());
    }

    /**
     * @throws IllegalStateException when called on removed FilteringConfiguration.
     */
    @UiThread
    public List<URL> getFilterLists() throws IllegalStateException {
        List<String> filterListsStr =
                Arrays.asList(FilteringConfigurationJni.get().getFilterLists(mName));
        List<URL> filterLists = new ArrayList<URL>();
        for (String url : filterListsStr) {
            try {
                filterLists.add(new URL(url));
            } catch (MalformedURLException e) {
                Log.e(TAG, "Received invalid subscription URL from C++: " + url);
            }
        }
        return filterLists;
    }

    @UiThread
    public String getAcceptableAdsUrl() {
        return FilteringConfigurationJni.get().getAcceptableAdsUrl();
    }

    /**
     * @throws IllegalStateException when called on removed FilteringConfiguration.
     */
    @UiThread
    public void addAllowedDomain(final String domain) throws IllegalStateException {
        String sanitizedDomain = sanitizeSite(domain);
        if (sanitizedDomain == null) return;
        FilteringConfigurationJni.get().addAllowedDomain(mName, sanitizedDomain);
    }

    /**
     * @throws IllegalStateException when called on removed FilteringConfiguration.
     */

    @UiThread
    public void removeAllowedDomain(final String domain) throws IllegalStateException {
        String sanitizedDomain = sanitizeSite(domain);
        if (sanitizedDomain == null) return;
        FilteringConfigurationJni.get().removeAllowedDomain(mName, sanitizedDomain);
    }

    /**
     * @throws IllegalStateException when called on removed FilteringConfiguration.
     */
    @UiThread
    public List<String> getAllowedDomains() throws IllegalStateException {
        List<String> allowedDomains =
                Arrays.asList(FilteringConfigurationJni.get().getAllowedDomains(mName));
        Collections.sort(allowedDomains);
        return allowedDomains;
    }

    /**
     * @throws IllegalStateException when called on removed FilteringConfiguration.
     */
    @UiThread
    public void addCustomFilter(final String filter) throws IllegalStateException {
        FilteringConfigurationJni.get().addCustomFilter(mName, filter);
    }

    /**
     * @throws IllegalStateException when called on removed FilteringConfiguration.
     */
    @UiThread
    public void removeCustomFilter(final String filter) throws IllegalStateException {
        FilteringConfigurationJni.get().removeCustomFilter(mName, filter);
    }

    /**
     * @throws IllegalStateException when called on removed FilteringConfiguration.
     */
    @UiThread
    public List<String> getCustomFilters() throws IllegalStateException {
        return Arrays.asList(FilteringConfigurationJni.get().getCustomFilters(mName));
    }

    @UiThread
    public static FilteringConfiguration createConfiguration(final String configuration_name) {
        FilteringConfiguration configuration = findConfigurationByName(configuration_name);
        if (configuration == null) {
            configuration = new FilteringConfiguration(configuration_name);
        }
        return configuration;
    }

    @UiThread
    public static void removeConfiguration(final String configuration_name) {
        final FilteringConfiguration configuration = findConfigurationByName(configuration_name);
        if (configuration != null) {
            mConfigurations.remove(configuration);
        }
        // Even if on Java side there is no such a FilteringConfiguration object, it can exist
        // on native side.
        FilteringConfigurationJni.get().removeConfiguration(configuration_name);
    }

    @UiThread
    public static List<FilteringConfiguration> getConfigurations() {
        // Get all existing (on C++ side) configurations, if there is no matching Java
        // instance present in mConfigurations set then create one and add.
        final String[] existing_configurations_names =
                FilteringConfigurationJni.get().getConfigurations();
        final List<FilteringConfiguration> configurations_to_return =
                new ArrayList<FilteringConfiguration>();
        // If mConfigurations contains configurations which are not present on the list returned
        // from FilteringConfigurationJni.get().getConfigurations() then we need to remove them
        // as it means that a configuration has been removed on native side (not via Java API).
        mConfigurations.removeIf(filteringConfiguration -> {
            return !Arrays.asList(existing_configurations_names)
                            .contains(filteringConfiguration.mName);
        });
        for (final String configuration_name : existing_configurations_names) {
            FilteringConfiguration configuration = findConfigurationByName(configuration_name);
            if (configuration == null) {
                configuration = new FilteringConfiguration(configuration_name);
                mConfigurations.add(configuration);
            }
            configurations_to_return.add(configuration);
        }
        Collections.sort(configurations_to_return, new Comparator<FilteringConfiguration>() {
            @Override
            public int compare(
                    final FilteringConfiguration object1, final FilteringConfiguration object2) {
                return object1.mName.compareTo(object2.mName);
            }
        });
        return configurations_to_return;
    }

    // TODO(kzlomek): Make it private once we remove AdblockController (DPD-2120).
    protected FilteringConfiguration(final String configuration_name) {
        mName = configuration_name;
        mConfigurations.add(this);
        FilteringConfigurationJni.get().bind(mName, this);
    }

    @UiThread
    private static FilteringConfiguration findConfigurationByName(final String configuration_name) {
        for (final FilteringConfiguration configuration : mConfigurations) {
            if (configuration.mName.equals(configuration_name)) {
                return configuration;
            }
        }
        return null;
    }

    private String sanitizeSite(String site) {
        // |site| is raw user input. We expect it to be either a domain or a URL.
        try {
            URL candidate = new URL(URLUtil.guessUrl(site));
            return candidate.getHost();
        } catch (java.net.MalformedURLException e) {
        }
        // Could not parse |site| as URL or domain.
        return null;
    }

    @CalledByNative
    private void enabledStateChanged() {
        ThreadUtils.assertOnUiThread();
        for (final ConfigurationChangeObserver observer : mConfigurationChangeObservers) {
            observer.onEnabledStateChanged();
        }
    }

    @CalledByNative
    private void filterListsChanged() {
        ThreadUtils.assertOnUiThread();
        for (final ConfigurationChangeObserver observer : mConfigurationChangeObservers) {
            observer.onFilterListsChanged();
        }
    }

    @CalledByNative
    private void allowedDomainsChanged() {
        ThreadUtils.assertOnUiThread();
        for (final ConfigurationChangeObserver observer : mConfigurationChangeObservers) {
            observer.onAllowedDomainsChanged();
        }
    }

    @CalledByNative
    private void customFiltersChanged() {
        ThreadUtils.assertOnUiThread();
        for (final ConfigurationChangeObserver observer : mConfigurationChangeObservers) {
            observer.onCustomFiltersChanged();
        }
    }

    @NativeMethods
    interface Natives {
        void bind(String configuration_name, FilteringConfiguration caller);
        void removeConfiguration(String configuration_name);
        String[] getConfigurations();
        void setEnabled(String configuration_name, boolean enabled);
        boolean isEnabled(String configuration_name);
        void addAllowedDomain(String configuration_name, String domain);
        void removeAllowedDomain(String configuration_name, String domain);
        String[] getAllowedDomains(String configuration_name);
        void addCustomFilter(String configuration_name, String filter);
        void removeCustomFilter(String configuration_name, String filter);
        String[] getCustomFilters(String configuration_name);
        void addFilterList(String configuration_name, String url);
        void removeFilterList(String configuration_name, String url);
        String[] getFilterLists(String configuration_name);
        String getAcceptableAdsUrl();
    }
}
