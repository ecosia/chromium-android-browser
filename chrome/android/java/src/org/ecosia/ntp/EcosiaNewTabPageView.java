/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.ecosia.ntp;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import org.chromium.base.task.AsyncTask;
import org.chromium.chrome.R;
import org.chromium.base.TraceEvent;
import androidx.annotation.VisibleForTesting;
import org.chromium.chrome.browser.app.ChromeActivity;
import org.chromium.chrome.browser.native_page.ContextMenuManager;
import org.chromium.chrome.browser.offlinepages.OfflinePageBridge;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.suggestions.SiteSuggestion;
import org.chromium.chrome.browser.suggestions.SuggestionsConfig;
import org.chromium.chrome.browser.suggestions.SuggestionsDependencyFactory;
import org.chromium.chrome.browser.suggestions.SuggestionsUiDelegate;
import org.chromium.chrome.browser.suggestions.tile.Tile;
import org.chromium.chrome.browser.suggestions.tile.TileGroup;
import org.chromium.chrome.browser.suggestions.tile.TileRenderer;
import org.chromium.chrome.browser.tab.Tab;

import org.chromium.components.browser_ui.widget.displaystyle.UiConfig;
import org.chromium.components.embedder_support.util.UrlConstants;
import org.chromium.components.favicon.IconType;
import org.chromium.components.favicon.LargeIconBridge;
import org.chromium.ui.base.ViewUtils;

import org.chromium.content_public.browser.LoadUrlParams;
import org.chromium.url.GURL;
import org.ecosia.cookies.EcosiaCookieObserver;
import org.ecosia.incentives.SearchIncentivesManager;
import org.ecosia.ntp.cards.ImpactCardContainer;
import org.ecosia.ntp.cards.GlobalImpactCardView;
import org.ecosia.ntp.cards.NewsCard;
import org.ecosia.ntp.cards.PersonalImpactCardView;
import org.ecosia.tracking.TrackingManager;
import org.ecosia.utils.RetrieveDataTask;
import org.ecosia.utils.RetrieveDelegate;
import org.ecosia.utils.SharedPreferencesHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.ecosia.ntp.YourImpactTooltip.NTP_IMPACT_TOOLTIP_DISMISSED;
import static org.ecosia.ntp.cards.CardPosition.BOTTOM;
import static org.ecosia.ntp.cards.CardPosition.MIDDLE;
import static org.ecosia.ntp.cards.CardPosition.TOP;

public class EcosiaNewTabPageView extends FrameLayout implements TileGroup.Observer {

    private int mSnapshotWidth;
    private int mSnapshotHeight;
    private int mSnapshotScrollY;

    private ScrollView mScrollView;
    private List<NewsCard> mNewsCards;

    private Context mContext;
    private EcosiaStatisticsManager mStatisticsManager;
    private NewsManager mNewsManager;
    private TrackingManager mTrackingManager;
    private TileGroup mTileGroup;
    private UiConfig mUiConfig;
    public String CHROMIUM_URL = "https://www.chromium.org/";
    private boolean mTilesLoaded;
    private float mUrlFocusChangePercent;
    private int mScreenHeight;
    private YourImpactTooltip mYourImpactTooltip;
    private int mLastOrientation;
    private LargeIconBridge mLargeIconBridge;
    private TileRenderer mTileRenderer;
    private GlobalImpactCardView mTreeCounterCard;
    private GlobalImpactCardView mFinancesCard;

    /** Flag used to request some layout changes after the next layout pass is completed. */
    private boolean mTileCountChanged;
    private boolean mSnapshotTileGridChanged;
    public static final String TAG = "EcosiaNewTabPageView";
    public ContextMenuManager mContextMenuManager;
    private RecyclerView mRecyclerView;
    private EcosiaNewTabPageRecyclerViewAdapter mRecyclerAdapter;
    private ArrayList<Drawable> mSiteIcons;
    private ArrayList<String> mSiteText;
    private ArrayList<String> mSiteURL;
    private ArrayList<SiteSuggestion> mSuggestion;
    private TileGroup.Delegate mTileGroupDelegate;
    private final int NUM_TILES_DISPLAY = 8;
    private boolean mNewsAvailable;
    private static final long FADE_IN_GAP_MILLISECONDS = 1600;
    private static final long ANIM_DURATION_MILLISECONDS = 800;

    private EcosiaAboutRecyclerViewAdapter mRecyclerAboutSection;
    private ArrayList<AboutSection> mAboutContent;
    private RecyclerView mRecyclerAboutView;

    private static final String URL_ECOSIA_FINANCIAL_REPORTS_EN = "https://blog.ecosia.org/ecosia-financial-reports-tree-planting-receipts/";
    private static final String URL_ECOSIA_FINANCIAL_REPORTS_DE = "https://de.blog.ecosia.org/ecosia-finanzberichte-baumplanzbelege/";
    private static final String URL_ECOSIA_FINANCIAL_REPORTS_FR = "https://fr.blog.ecosia.org/rapports-financiers-recus-de-plantations-arbres/";
    private static final String URL_ECOSIA_TREES = "https://blog.ecosia.org/tag/where-does-ecosia-plant-trees/";
    private static final String URL_ECOSIA_PRIVACY = "https://www.ecosia.org/privacy";
    private static final String URL_HELP_INFO = "https://ecosia.helpscoutdocs.com/article/369-impact-counter";

    /**
     * Manages the view interaction with the rest of the system.
     */
    public interface EcosiaNewTabPageManager extends SuggestionsUiDelegate {
        /** @return Whether the location bar is shown in the NTP. */
        boolean isLocationBarShownInNTP();

        /** @return Whether voice search is enabled and the microphone should be shown. */
        boolean isVoiceSearchEnabled();

        /**
         * Animates the search box up into the omnibox and bring up the keyboard.
         * @param beginVoiceSearch Whether to begin a voice search.
         * @param pastedText Text to paste in the omnibox after it's been focused. May be null.
         */
        void focusSearchBox(boolean beginVoiceSearch, String pastedText);

        /**
         * @return whether the {@link EcosiaNewTabPage} associated with this manager is the current page
         * displayed to the user.
         */
        boolean isCurrentPage();

        /**
         * Called when the NTP has completely finished loading (all views will be inflated
         * and any dependent resources will have been loaded).
         */
        void onLoadingComplete();
    }

    public EcosiaNewTabPageView(Context context, AttributeSet attrs) {
        super(context);
        mContext = context;
        mLastOrientation = getResources().getConfiguration().orientation;

        mStatisticsManager = new EcosiaStatisticsManager(mContext);
        mStatisticsManager.start();

        mNewsManager = new NewsManager(mContext);
        mNewsManager.scheduleNewsDownload();
        mTrackingManager = TrackingManager.getInstance(mContext);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        resizeContentViews();
        if (screenRotated()) {
            if (mYourImpactTooltip != null) {
                mYourImpactTooltip.dismiss();
                showTooltipWindow();
            }
        }
    }

    private boolean screenRotated() {
        int orientation = getResources().getConfiguration().orientation;
        if (mLastOrientation != orientation) {
            mLastOrientation = orientation;
            return true;
        }
        return false;
    }

    private void resizeContentViews() {
        final Resources resources = getResources();
        final boolean tabletLikeScreenSize = resources.getBoolean(R.bool.device_screen_size_is_tablet_like);
        if (tabletLikeScreenSize) {
            final int maxWidth = resolveMaxWidth(resources);
            final View ntpAlignmentView = findViewById(R.id.ecosia_ntp_alignment_view);
            final ViewGroup.LayoutParams layoutParams = ntpAlignmentView.getLayoutParams();
            if (layoutParams.width > maxWidth || layoutParams.width <= 0) {
                layoutParams.width = maxWidth;
                ntpAlignmentView.setLayoutParams(layoutParams);
            }
        }
    }

    private int resolveMaxWidth(final Resources resources) {
        final int maxWidthFromResources = resources.getDimensionPixelSize(R.dimen.ecosia_ntp_tablet_max_width);
        final View recyclerView = findViewById(R.id.site_recycler_view);
        final int recyclerViewWidth = recyclerView.getWidth();
        return Math.min(recyclerViewWidth, maxWidthFromResources);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        startProgress();
        mScrollView = findViewById(R.id.ntp_scrollview);

        boolean isRestricted = SearchIncentivesManager.getInstance(mContext).isRestricted();
        if (isRestricted) {
            ImpactCardContainer personalImpactCard = findViewById(R.id.personal_impact_card_container);
            personalImpactCard.setVisibility(GONE);
        } else {
            addPersonalImpactCard();
        }

        addGlobalImpactCards();

        updateTreeCounterView();
        updateInvestmentsAmountView();

        if (shouldDisplayImpactTooltip()) {
            mYourImpactTooltip = new YourImpactTooltip(mContext);
            showTooltipWindow();
        }

        TextView moreNews = findViewById(R.id.label_news_load_more);
        moreNews.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                openUrl(getResources().getString(R.string.ecosia_news_url));
            }
        });

        mNewsCards = new ArrayList<>();
        mNewsCards.add(new NewsCard(findViewById(R.id.news_card_1), TOP));
        mNewsCards.add(new NewsCard(findViewById(R.id.news_card_2), MIDDLE));
        mNewsCards.add(new NewsCard(findViewById(R.id.news_card_3), BOTTOM));
        refreshNews();

        mRecyclerView = findViewById(R.id.site_recycler_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(mContext, getMaxTileColumns()));
        mSiteIcons = new ArrayList<>();
        mSiteText = new ArrayList<>();
        mSiteURL = new ArrayList<>();
        mSuggestion = new ArrayList<>();

        mRecyclerAboutView = findViewById(org.chromium.chrome.R.id.card_recycler_view);
        mRecyclerAboutView.setLayoutManager(new LinearLayoutManager(mContext));
        mAboutContent = new ArrayList<>();
        String locale = getResources().getConfiguration().locale.toString().substring(0,2);
        String url_finance = locale.equalsIgnoreCase("de") ? URL_ECOSIA_FINANCIAL_REPORTS_DE : locale.equalsIgnoreCase("fr") ? URL_ECOSIA_FINANCIAL_REPORTS_FR : URL_ECOSIA_FINANCIAL_REPORTS_EN;
        mAboutContent.add(new AboutSection(getResources().getString(org.chromium.chrome.R.string.financial_reports),
                getResources().getDrawable(org.chromium.chrome.R.drawable.ecosia_reports),
                getResources().getString(org.chromium.chrome.R.string.financial_reports_help),
                url_finance ));
        mAboutContent.add(new AboutSection(getResources().getString(org.chromium.chrome.R.string.ecosia_trees),
                getResources().getDrawable(org.chromium.chrome.R.drawable.ecosia_trees),
                getResources().getString(org.chromium.chrome.R.string.ecosia_trees_help),
                URL_ECOSIA_TREES ));
        mAboutContent.add(new AboutSection(getResources().getString(org.chromium.chrome.R.string.prefs_privacy_menu_item),
                getResources().getDrawable(org.chromium.chrome.R.drawable.ecosia_privacy),
                getResources().getString(org.chromium.chrome.R.string.ecosia_privacy_help),
                URL_ECOSIA_PRIVACY ));

        mRecyclerAboutSection = new EcosiaAboutRecyclerViewAdapter(mContext, mAboutContent);
        mRecyclerAboutView.setAdapter(mRecyclerAboutSection);

    }

    private void addPersonalImpactCard() {
        ImpactCardContainer personalImpactContainer = findViewById(R.id.personal_impact_card_container);
        PersonalImpactCardView personalImpactCard = new PersonalImpactCardView(mContext);
        TreeCounterProgressbar progressBar = personalImpactCard.findViewById((R.id.tree_progress));

        personalImpactCard.setRightText(getResources().getString(R.string.ecosia_how_it_works), URL_HELP_INFO);
        RetrieveDelegate retrieveDelegate = new RetrieveDelegate<String>() {
            @Override
            public String doInBackground() {
                int counter = EcosiaCookieObserver.getInstance().getCookie().getPersonalCounter();
                return Integer.toString(counter);
            }

            @Override
            public void onPostExecute(String result) {
                int noOfSearches = result.isEmpty() ? 0 : Integer.parseInt(result);
                progressBar.CalculateProgress(noOfSearches);
                personalImpactCard.setTopText(Integer.toString(progressBar.getNoOfTrees()));
                int searchId = R.string.ecosia_counter_search;
                int searchesId = R.string.ecosia_counter_searches;
                String searchText = getResources().getString(noOfSearches == 1 ? searchId : searchesId);
                String string = String.format(Locale.getDefault(), "%d %s", noOfSearches, searchText);
                personalImpactCard.setBottomText(string);
            }
        };
        new RetrieveDataTask(retrieveDelegate).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        personalImpactContainer.addImpactCard(personalImpactCard);
    }

    private void addGlobalImpactCards() {
        ImpactCardContainer globalImpactContainer = findViewById(R.id.global_impact_card_container);

        mTreeCounterCard = new GlobalImpactCardView(mContext);
        mTreeCounterCard.setImage(R.drawable.impact_card_global_counter);
        mTreeCounterCard.setBottomText(getResources().getString(R.string.ntp_impact_card_tree_counter_label));

        mFinancesCard = new GlobalImpactCardView(mContext);
        mFinancesCard.setImage(R.drawable.impact_card_financial_reports);
        mFinancesCard.setBottomText(getResources().getString(R.string.ntp_impact_card_investment_label));

        globalImpactContainer.addImpactCard(mTreeCounterCard);
        globalImpactContainer.addImpactCard(mFinancesCard);
    }

    private void showTooltipWindow() {
        mYourImpactTooltip.showTooltipWindow(this, findViewById(R.id.section_impact));
    }

    public void initialize(EcosiaNewTabPageView.EcosiaNewTabPageManager manager, Tab tab, TileGroup.Delegate tileGroupDelegate,
                           long constructedTimeNs, ChromeActivity activity) {
        TraceEvent.begin(TAG + ".initialize()");
        measureScreenHeight();
        adjustLogoSpacing();
        mUiConfig = new UiConfig(this);
        mTileGroupDelegate = tileGroupDelegate;
        // Don't store a direct reference to the activity, because it might change later if the tab
        // is reparented.
        Runnable closeContextMenuCallback = activity::closeContextMenu;
        mContextMenuManager = new ContextMenuManager(manager.getNavigationDelegate(),
                null, closeContextMenuCallback,
                EcosiaNewTabPage.CONTEXT_MENU_USER_ACTION_PREFIX); // Needs to be modified
        tab.getWindowAndroid().addContextMenuCloseListener(mContextMenuManager);

        mRecyclerAdapter = new EcosiaNewTabPageRecyclerViewAdapter(mContext, mSiteIcons, mSiteText, mSiteURL, mSuggestion);
        mRecyclerView.setAdapter(mRecyclerAdapter);

        Profile profile = Profile.getLastUsedRegularProfile();
        OfflinePageBridge offlinePageBridge =
                SuggestionsDependencyFactory.getInstance().getOfflinePageBridge(profile);

        mLargeIconBridge = new LargeIconBridge(Profile.getLastUsedRegularProfile());

        mTileRenderer = new TileRenderer(activity, SuggestionsConfig.getTileStyle(mUiConfig),
                        getTileTitleLines(), manager.getImageFetcher());
        mTileGroup = new TileGroup(mTileRenderer, manager, mContextMenuManager, mTileGroupDelegate,
                /* observer = */ this, offlinePageBridge);
        mTileGroup.startObserving(getMaxTileRows() * getMaxTileColumns());
    }

    private void measureScreenHeight() {
        final WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        final Display defaultDisplay = windowManager.getDefaultDisplay();
        final DisplayMetrics metrics = new DisplayMetrics();
        defaultDisplay.getMetrics(metrics);
        mScreenHeight = metrics.heightPixels;
    }

    private void adjustLogoSpacing() {
        final int upperSpacing = mScreenHeight/10;
        if (upperSpacing > 0) {
            final View logoView = findViewById(R.id.ecosia_logo);
            final ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) logoView.getLayoutParams();
            layoutParams.setMargins(layoutParams.leftMargin, upperSpacing, layoutParams.rightMargin, layoutParams.bottomMargin);
            logoView.setLayoutParams(layoutParams);
            logoView.requestLayout();
        }
    }

    private boolean shouldDisplayImpactTooltip() {
        return !SharedPreferencesHelpers.getBoolean(mContext, NTP_IMPACT_TOOLTIP_DISMISSED, false);
    }

    public void dismissImpactTolltip() {
        if(mYourImpactTooltip != null)
            mYourImpactTooltip.dismiss();
    }

    protected void updateTreeCounterView() {
        TotalTreeCounter totalTreeCounter = mStatisticsManager.getTotalTreeCounter();
        long computedCount = totalTreeCounter.getComputedCount();
        mTreeCounterCard.setTopText(mStatisticsManager.getFormattedTreeCount(computedCount));
    }

    protected void updateInvestmentsAmountView() {
        InvestmentsAmount investmentsAmount = mStatisticsManager.getInvestmentsAmount();
        long computedCount = investmentsAmount.getComputedCount();
        mFinancesCard.setTopText(mStatisticsManager.getFormattedInvestmentsAmount(computedCount));
    }

    public void refreshNews() {
        List<News> newsList = mNewsManager.getStoredNews();
        if (newsList != null) {
            mNewsAvailable = true;
            for (int i = 0; i < newsList.size(); i++) {
                News article = newsList.get(i);
                String url = article.getTargetUrl().toString();
                mNewsCards.get(i).initialize(article, i, cardClickListener(url, TrackingManager.LABEL_NEWS, i), getResources());
            }
        } else {
            mNewsAvailable = false;
        }
        if(findViewById(R.id.news_container).getVisibility() == GONE) {
            stopProgress();
        }
    }

    private void startProgress() {
        findViewById(R.id.loading_progress).setVisibility(VISIBLE);
        hideNewsSection();
    }

    private void stopProgress() {
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(ANIM_DURATION_MILLISECONDS);

        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setDuration(ANIM_DURATION_MILLISECONDS);

        AnimationSet set = new AnimationSet(true);
        set.setInterpolator(new AccelerateInterpolator());

        Animation slideUp = AnimationUtils.loadAnimation(mContext, R.anim.ecosia_slide_in_up);
        set.addAnimation(slideUp);
        set.addAnimation(fadeIn);

        //Time gap to start animation 0.8s
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(findViewById(R.id.loading_progress).getVisibility() == VISIBLE) {
                    findViewById(R.id.loading_progress).startAnimation(fadeOut);
                }
            }
        }, ANIM_DURATION_MILLISECONDS);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.loading_progress).setVisibility(GONE);
                if(mNewsAvailable) {
                    findViewById(R.id.section_news).startAnimation(set);
                    findViewById(R.id.news_container).startAnimation(set);
                    showNewsSection();
                } else {
                    hideNewsSection();
                }
            }
        }, FADE_IN_GAP_MILLISECONDS);

    }

    private void showNewsSection() {
        findViewById(R.id.section_news).setVisibility(VISIBLE);
        findViewById(R.id.news_container).setVisibility(VISIBLE);
    }

    private void hideNewsSection() {
        findViewById(R.id.section_news).setVisibility(GONE);
        findViewById(R.id.news_container).setVisibility(GONE);
    }

    private OnClickListener urlClickListener(String url) {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl(url);
            }
        };
    }

    private OnClickListener cardClickListener(String url, String label, int rank) {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl(url);
                mTrackingManager.engagementEvent(label, rank);
            }
        };
    }

    private void openUrl(String url) {
        LoadUrlParams params = new LoadUrlParams(url);
        ((ChromeActivity) mContext).getActivityTab().loadUrl(params);
    }

    // Chromium required methods

    boolean shouldCaptureThumbnail() {
        if (getWidth() == 0 || getHeight() == 0) return false;

        return getWidth() != mSnapshotWidth
                || getHeight() != mSnapshotHeight
                || mScrollView.getScrollY() != mSnapshotScrollY;
    }

    void captureThumbnail(Canvas canvas) {
        ViewUtils.captureBitmap(this, canvas);
        mSnapshotWidth = getWidth();
        mSnapshotHeight = getHeight();
        mSnapshotScrollY = mScrollView.getScrollY();
    }

    public void onTilesLoaded() {
        if (mTilesLoaded) return;
        mTilesLoaded = true;
    }

    @Override
    public void onTileDataChanged() {
        updateTileGridPlaceholderVisibility();
        mSnapshotTileGridChanged = true;
    }

    @Override
    public void onTileCountChanged() {
        // If the number of tile rows change while the URL bar is focused, the icons'
        // position will be wrong. Schedule the translation to be updated.
        if (mUrlFocusChangePercent == 1f) mTileCountChanged = true;
        updateTileGridPlaceholderVisibility();
    }

    @Override
    public void onTileIconChanged(Tile tile) {
        // Updating icons for all tiles other than Top Sites
        if (!tile.getUrl().getSpec().equals(UrlConstants.EXPLORE_URL)) {
            mRecyclerAdapter.setIcon(tile.getIcon(), tile.getUrl().getSpec());
            mRecyclerAdapter.notifyDataSetChanged();
        }
        mSnapshotTileGridChanged = true;
    }

    @Override
    public void onTileOfflineBadgeVisibilityChanged(Tile tile) {
        mSnapshotTileGridChanged = true;
    }

    @VisibleForTesting
    public TileGroup getTileGroup() {
        return mTileGroup;
    }

    private void updateTileGridPlaceholderVisibility() {
        resetRecyclerData();
        SparseArray<List<Tile>> data = getTileGroup().getTileSections();
        for (int index = 0; index < data.size(); index++) {
            List<Tile> tiles = data.valueAt(index);
            for (int pos = 0; pos < tiles.size() && mSiteText.size() < NUM_TILES_DISPLAY; pos++) {
                mSiteText.add(tiles.get(pos).getTitle());
                mSiteIcons.add(tiles.get(pos).getIcon());
                mSiteURL.add(tiles.get(pos).getUrl().getSpec());
                mSuggestion.add(tiles.get(pos).getData());

                int finalPos = pos;
                LargeIconBridge.LargeIconCallback callback = new LargeIconBridge.LargeIconCallback() {
                    @Override
                    public void onLargeIconAvailable(Bitmap icon, int fallbackColor,
                                                     boolean isFallbackColorDefault, @IconType int iconType) {
                        if (icon == null) {
                            GURL url = new GURL(mSiteURL.get(finalPos));
                            mSiteIcons.set(finalPos, mTileRenderer.getTileIconFromColor(url, fallbackColor, isFallbackColorDefault));
                        } else {
                            mSiteIcons.set(finalPos, mTileRenderer.getTileIconFromBitmap(icon));
                        }
                    }
                };
                int iconSize = (int) getResources().getDimension(R.dimen.default_favicon_min_size);
                mLargeIconBridge.getLargeIconForUrl(new GURL(mSiteURL.get(pos)), iconSize, callback);
            }
        }

        mRecyclerAdapter.setData(mSiteIcons, mSiteText, mSiteURL, mSuggestion);
        mRecyclerAdapter.setTileGroupData(mTileGroup, mTileGroupDelegate);
        mRecyclerAdapter.notifyDataSetChanged();
    }

    private void resetRecyclerData() {
        mSiteText.clear();
        mSiteURL.clear();
        mSiteIcons.clear();
        mSuggestion.clear();
    }

    // Max tiles shown will be set to 8
    private static int getMaxTileRows() {
        return 3;
    }

    private int getMaxTileColumns() {
        return 4;
    }

    private static int getTileTitleLines() {
        return 1;
    }
}
