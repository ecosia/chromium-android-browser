/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.ecosia.ntp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import androidx.core.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;

import org.chromium.chrome.R;
import org.chromium.chrome.browser.app.ChromeActivity;
import org.chromium.chrome.browser.compositor.layouts.content.InvalidationAwareThumbnailProvider;
import org.chromium.chrome.browser.ui.native_page.NativePageHost;

import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.ui.messages.snackbar.SnackbarManager;
import org.chromium.chrome.browser.suggestions.SuggestionsDependencyFactory;
import org.chromium.chrome.browser.suggestions.SuggestionsNavigationDelegate;
import org.chromium.chrome.browser.suggestions.SuggestionsUiDelegateImpl;
import org.chromium.chrome.browser.suggestions.tile.Tile;
import org.chromium.chrome.browser.suggestions.tile.TileGroup;
import org.chromium.chrome.browser.suggestions.tile.TileGroupDelegateImpl;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.tabmodel.TabModelSelector;
import org.chromium.chrome.browser.ui.native_page.BasicNativePage;
import org.chromium.chrome.browser.util.BrowserUiUtils;

import java.util.List;

import static org.ecosia.ntp.NewsManager.ACTION_NEWS_UPDATE;

public class EcosiaNewTabPage extends BasicNativePage implements InvalidationAwareThumbnailProvider {

    private EcosiaNewTabPageView mEcosiaNewTabPageView;
    private final String mNTPUrl;
    private final String mNTPHost;
    private final Context mContext;
    private final NewsReceiver mNewsReceiver;
    private final StatsUpdateReceiver mStatsUpdateReceiver;
    protected final EcosiaNewTabPage.ManagerImpl mEcosiaNewTabPageManager;
    protected final Tab mTab;
    protected final TileGroup.Delegate mTileGroupDelegate;
    public static final String CONTEXT_MENU_USER_ACTION_PREFIX = "Suggestions";

    public EcosiaNewTabPage(ChromeActivity activity, NativePageHost host, String ntpUrl, String ntpHost, TabModelSelector tabModelSelector, Tab tab) {
        super(host);
        initialize(activity, host);
        mNTPUrl = ntpUrl;
        mNTPHost = ntpHost;

        mContext = activity.getApplicationContext();

        mStatsUpdateReceiver = new StatsUpdateReceiver();
        IntentFilter statsFilter = new IntentFilter(EcosiaStatisticsManager.ACTION_ECOSIA_STATS_UPDATE);
        mContext.registerReceiver(mStatsUpdateReceiver, statsFilter);

        mNewsReceiver = new NewsReceiver();
        IntentFilter mNewsFilter = new IntentFilter(ACTION_NEWS_UPDATE);
        mContext.registerReceiver(mNewsReceiver, mNewsFilter);


        mTab = tab;
        Profile profile = Profile.fromWebContents(mTab.getWebContents());

        SuggestionsDependencyFactory depsFactory = SuggestionsDependencyFactory.getInstance();

        SuggestionsNavigationDelegate navigationDelegate = new SuggestionsNavigationDelegate(
                activity, profile, host, tabModelSelector, mTab);
        mTileGroupDelegate = new EcosiaNewTabPage.TileGroupDelegate(activity, profile, navigationDelegate);
        mEcosiaNewTabPageManager = new EcosiaNewTabPage.ManagerImpl(navigationDelegate, profile, host,
                activity.getSnackbarManager());

        mEcosiaNewTabPageView.initialize(mEcosiaNewTabPageManager, mTab, mTileGroupDelegate, 0, activity);
    }

    protected void initialize(ChromeActivity activity, final NativePageHost host) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        mEcosiaNewTabPageView = (EcosiaNewTabPageView) inflater.inflate(R.layout.ecosia_new_tab_page, null);
    }

    // NativePage overrides

    @Override
    public void destroy() {
        assert !ViewCompat
                .isAttachedToWindow(getView()) : "Destroy called before removed from window";

        mContext.unregisterReceiver(mNewsReceiver);
        mContext.unregisterReceiver(mStatsUpdateReceiver);
        mEcosiaNewTabPageView.dismissImpactTolltip();

        super.destroy();
    }

    @Override
    public String getUrl() {
        return mNTPUrl;
    }

    @Override
    public int getBackgroundColor() {
        return 0;
    }

    @Override
    public String getTitle() {
        return "";
    }

    @Override
    public boolean needsToolbarShadow() {
        return true;
    }

    @Override
    public View getView() {
        return mEcosiaNewTabPageView;
    }

    @Override
    public String getHost() {
        return mNTPHost;
    }

    @Override
    public void updateForUrl(String url) {
    }

    // InvalidationAwareThumbnailProvider

    @Override
    public boolean shouldCaptureThumbnail() {
        return mEcosiaNewTabPageView.shouldCaptureThumbnail();
    }

    @Override
    public void captureThumbnail(Canvas canvas) {
        mEcosiaNewTabPageView.captureThumbnail(canvas);
    }

    // Ecosia classes

    class NewsReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            mEcosiaNewTabPageView.refreshNews();
        }
    }

    class StatsUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            mEcosiaNewTabPageView.updateTreeCounterView();
            mEcosiaNewTabPageView.updateInvestmentsAmountView();
        }
    }

    protected class ManagerImpl
            extends SuggestionsUiDelegateImpl implements EcosiaNewTabPageView.EcosiaNewTabPageManager {
        public ManagerImpl(SuggestionsNavigationDelegate navigationDelegate, Profile profile,
                                           NativePageHost nativePageHost,
                                           SnackbarManager snackbarManager) {
            super(navigationDelegate, profile, nativePageHost,
                     snackbarManager);
        }

        @Override
        public boolean isLocationBarShownInNTP() {
            return false;
        }

        @Override
        public boolean isVoiceSearchEnabled() {
            return false;
        }

        @Override
        public void focusSearchBox(boolean beginVoiceSearch, String pastedText) {
            // Enable animation on search box up into the omnibox and bring up the keyboard
            // when Voice Search is on (Currently disabled)
        }

        @Override
        public boolean isCurrentPage() {
            return false;
        }

        @Override
        public void onLoadingComplete() {

        }
    }

    /**
     * Extends {@link TileGroupDelegateImpl} to add metrics logging that is specific to
     * {@link EcosiaNewTabPage}.
     */
    private class TileGroupDelegate extends TileGroupDelegateImpl {
        private TileGroupDelegate(ChromeActivity activity, Profile profile,
                                                  SuggestionsNavigationDelegate navigationDelegate) {
            super(activity.getApplicationContext(), profile, navigationDelegate, activity.getSnackbarManager(),
                    BrowserUiUtils.HostSurface.NEW_TAB_PAGE);
        }

        @Override
        public void onLoadingComplete(List<Tile> tiles) {
            super.onLoadingComplete(tiles);
            mEcosiaNewTabPageView.onTilesLoaded();
        }
    }
}
