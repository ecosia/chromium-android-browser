/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.ecosia.ntp;

import android.app.Activity;
import android.content.BroadcastReceiver;

import static org.ecosia.ntp.YourImpactTooltip.NTP_IMPACT_TOOLTIP_DISMISSED;
import static org.ecosia.ntp.cards.CardPosition.BOTTOM;
import static org.ecosia.ntp.cards.CardPosition.MIDDLE;
import static org.ecosia.ntp.cards.CardPosition.TOP;
import static org.ecosia.tracking.TrackingManager.ACTION_CLAIM;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.chromium.chrome.R;

import org.chromium.chrome.browser.tabbed_mode.TabbedRootUiCoordinator;
import org.chromium.chrome.browser.ui.default_browser_promo.EcosiaDefaultBrowserPromoDialog;

import org.ecosia.accessibility.AccessibilityHelper;

import org.ecosia.cookies.ECFGCookie;
import org.ecosia.cookies.EcosiaCookieObserver;
import org.ecosia.ntp.cards.AboutCard;
import org.ecosia.ntp.cards.GlobalImpactCardView;
import org.ecosia.ntp.cards.ImpactCardContainer;
import org.ecosia.ntp.cards.NewsCard;
import org.ecosia.ntp.cards.ReferralsCardView;
import org.ecosia.preferences.EcosiaHomepagePreferencesHelper;
import org.ecosia.referrals.Referrals;
import org.ecosia.referrals.ReferralsActivity;
import org.ecosia.tracking.TrackingManager;
import org.ecosia.utils.BroadcastReceiverHelper;
import org.ecosia.utils.SharedPreferencesHelpers;
import org.ecosia.utils.UrlHelpers;

import java.util.ArrayList;
import java.util.List;

import static org.ecosia.ntp.NewsManager.ACTION_NEWS_UPDATE;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

public class EcosiaNewTabPageView extends FrameLayout {

    private List<NewsCard> mNewsCards;

    private final Context mContext;
    private final EcosiaStatisticsManager mStatisticsManager;
    private final NewsManager mNewsManager;
    private final TrackingManager mTrackingManager;
    private YourImpactTooltip mYourImpactTooltip;
    private ReferralsSuccessTooltip mReferralsSuccessTooltip;
    private int mLastOrientation;
    private boolean mLastReferralsSuccessTooltipIsInviter;
    private int mLastReferralsSuccessTooltipClaimsCount;
    private GlobalImpactCardView mTreeCounterCard;
    private GlobalImpactCardView mFinancesCard;
    private ReferralsCardView mReferralsCard;

    public static final String TAG = "EcosiaNewTabPageView";
    private boolean mNewsAvailable;
    private static final long FADE_IN_GAP_MILLISECONDS = 1600;
    private static final long ANIM_DURATION_MILLISECONDS = 800;

    private static final String URL_ECOSIA_FINANCIAL_REPORTS_EN = "https://blog.ecosia.org/ecosia-financial-reports-tree-planting-receipts/";
    private static final String URL_ECOSIA_FINANCIAL_REPORTS_DE = "https://de.blog.ecosia.org/ecosia-finanzberichte-baumplanzbelege/";
    private static final String URL_ECOSIA_FINANCIAL_REPORTS_FR = "https://fr.blog.ecosia.org/rapports-financiers-recus-de-plantations-arbres/";
    private static final String URL_ECOSIA_TREES = "https://blog.ecosia.org/tag/where-does-ecosia-plant-trees/";
    private static final String URL_ECOSIA_PRIVACY = "https://www.ecosia.org/privacy";

    public EcosiaNewTabPageView(Context context, AttributeSet attrs) {
        super(context);
        mContext = context;
        mLastOrientation = getResources().getConfiguration().orientation;

        BroadcastReceiverHelper broadcastReceiverHelper = new BroadcastReceiverHelper(mContext);

        StatsUpdateReceiver statsUpdateReceiver = new StatsUpdateReceiver();
        IntentFilter statsFilter = new IntentFilter(EcosiaStatisticsManager.ACTION_ECOSIA_STATS_UPDATE);
        broadcastReceiverHelper.registerUnexportedReceiver(statsUpdateReceiver, statsFilter);
        mStatisticsManager = EcosiaStatisticsManager.getInstance(context);

        NewsReceiver newsReceiver = new NewsReceiver();
        IntentFilter mNewsFilter = new IntentFilter(ACTION_NEWS_UPDATE);
        broadcastReceiverHelper.registerUnexportedReceiver(newsReceiver, mNewsFilter);
        mNewsManager = new NewsManager(mContext);
        mNewsManager.scheduleNewsDownload();
        mTrackingManager = TrackingManager.getInstance(mContext);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (screenRotated()) {
            if (mYourImpactTooltip != null) {
                mYourImpactTooltip.dismiss();
                showTooltipWindow();
            }
            if (mReferralsSuccessTooltip != null) {
                mReferralsSuccessTooltip.dismiss();
                showReferralsSuccessWindow(mLastReferralsSuccessTooltipIsInviter, mLastReferralsSuccessTooltipClaimsCount);
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

    private void claimReferral() {
        Referrals.getInstance(mContext).claimInstallationFromReferrer(new Referrals.ClaimReferralCallback() {
            @Override
            public void onClaimed() {
                mTrackingManager.invitationsEvent(ACTION_CLAIM, null);
                refreshReferralsCount();
                showReferralsSuccessWindow(false, 1);
            }

            @Override
            public void onError (Referrals.ClaimError error) {
                switch (error) {
                    case ILLEGALLY_FORMATTED_REFERRAL_CODE:
                        // Referrer code start with something else than `friends-`, let's bail out
                        break;
                    case ALREADY_CLAIMED:
                    case INVALID_LINK:
                        new AlertDialog.Builder(mContext).setTitle(mContext.getString(R.string.ecosia_referrals_error_link_invalid_title))
                                .setMessage(mContext.getString(R.string.ecosia_referrals_error_link_invalid_message))
                                .setNeutralButton(mContext.getString(R.string.ecosia_referrals_error_alert_ok_button), (dialog, which) -> { })
                                .create()
                                .show();
                        break;
                    case NETWORK_ERROR:
                        new AlertDialog.Builder(mContext).setTitle(mContext.getString(R.string.ecosia_referrals_error_network_title))
                                .setMessage(mContext.getString(R.string.ecosia_referrals_error_network_message))
                                .setNeutralButton(mContext.getString(R.string.ecosia_referrals_error_alert_ok_button), (dialog, which) -> { })
                                .setPositiveButton(mContext.getString(R.string.ecosia_referrals_error_alert_retry_button), (dialog, which) -> claimReferral())
                                .create()
                                .show();
                        break;
                }
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        addReferralsCard();
        addGlobalImpactCards();
        updateTreeCounterView();
        updateInvestmentsAmountView();

        if (shouldDisplayImpactTooltip()) {
            mYourImpactTooltip = new YourImpactTooltip(mContext);
            showTooltipWindow();
        }

        addNewsCards();
        addAboutCards();
        claimReferral();
        checkDefaultBrowserPromoLaunch();
    }

    private void checkDefaultBrowserPromoLaunch() {
        ECFGCookie cookie = EcosiaCookieObserver.getInstance().getEcfgCookie();
        if(cookie != null && cookie.getPersonalCounter() >= 50) { // Launch when cookie counter reaches 50
            EcosiaDefaultBrowserPromoDialog.showPopup((Activity)mContext, TabbedRootUiCoordinator.getWindowAndroid());
        }
    }

    private void addReferralsCard() {
        ImpactCardContainer personalImpactContainer = findViewById(R.id.personal_impact_card_container);
        mReferralsCard = new ReferralsCardView(mContext, R.layout.ecosia_ntp_referrals_card);
        mReferralsCard.setButton(mContext.getString(R.string.ecosia_referrals_ntp_invite_friends), v -> {
            Intent intent = new Intent(mContext, ReferralsActivity.class);
            mContext.startActivity(intent);
        });
        personalImpactContainer.addImpactCard(mReferralsCard);
        updateReferralsCardCount(0);
        refreshReferralsCount();
    }

    private void updateReferralsCardCount(int claimsCount) {
        if (mReferralsCard != null) {
            mReferralsCard.setTopText(String.valueOf(claimsCount));
            mReferralsCard.setBottomText(mContext.getResources().getQuantityString(R.plurals.ecosia_referrals_ntp_x_friends_invited, claimsCount, claimsCount));
        }
    }

    public void refreshReferralsCount() {
        Referrals.getInstance(mContext).getCurrentClaimsCount(false, new Referrals.ReferralCodeCallback() {
            @Override
            public void onReady(@Nullable String referralCode, int currentClaimsCount, int previousClaimsCount) {
                updateReferralsCardCount(currentClaimsCount);
                if (currentClaimsCount > previousClaimsCount) {
                    showReferralsSuccessWindow(true, currentClaimsCount - previousClaimsCount);
                }
            }

            @Override
            public void onError(Referrals.Error error) {
                // no-op
            }
        });
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

    private void addNewsCards() {
        TextView moreNews = findViewById(R.id.label_news_load_more);
        moreNews.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                UrlHelpers.openUrl(mContext, getResources().getString(R.string.ecosia_news_url));
                // Hide Your Impact Tooltip in case Referrals Success will be shown,
                // but don't persist dismissal so it will be shown next time
                if(mYourImpactTooltip != null) {
                    mYourImpactTooltip.dismiss();
                }
            }
        });

        mNewsCards = new ArrayList<>();
        mNewsCards.add(new NewsCard(findViewById(R.id.news_card_1), TOP));
        mNewsCards.add(new NewsCard(findViewById(R.id.news_card_2), MIDDLE));
        mNewsCards.add(new NewsCard(findViewById(R.id.news_card_3), BOTTOM));
        refreshNews();
    }

    private void addAboutCards() {
        String locale = getResources().getConfiguration().locale.toString().substring(0,2);
        String url_finance = locale.equalsIgnoreCase("de") ?
                URL_ECOSIA_FINANCIAL_REPORTS_DE : locale.equalsIgnoreCase("fr") ?
                URL_ECOSIA_FINANCIAL_REPORTS_FR : URL_ECOSIA_FINANCIAL_REPORTS_EN;
        AboutCard aboutCard1 = new AboutCard(findViewById(R.id.about_card_1),
                getResources().getString(org.chromium.chrome.R.string.financial_reports),
                getResources().getDrawable(org.chromium.chrome.R.drawable.ecosia_reports),
                getResources().getString(org.chromium.chrome.R.string.financial_reports_help),
                url_finance, mContext);

        AboutCard aboutCard2 = new AboutCard(findViewById(R.id.about_card_2),
                getResources().getString(org.chromium.chrome.R.string.ecosia_trees),
                getResources().getDrawable(org.chromium.chrome.R.drawable.ecosia_trees),
                getResources().getString(org.chromium.chrome.R.string.ecosia_trees_help),
                URL_ECOSIA_TREES, mContext);

        AboutCard aboutCard3 = new AboutCard(findViewById(R.id.about_card_3),
                getResources().getString(org.chromium.chrome.R.string.prefs_privacy_menu_item),
                getResources().getDrawable(org.chromium.chrome.R.drawable.ecosia_privacy),
                getResources().getString(org.chromium.chrome.R.string.ecosia_privacy_help),
                URL_ECOSIA_PRIVACY, mContext);

        aboutCard1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (aboutCard1.isExpanded()) {
                    aboutCard1.collapseInfo();
                } else {
                    aboutCard2.collapseInfo();
                    aboutCard3.collapseInfo();
                    aboutCard1.expandInfo();
                }
            }
        });

        aboutCard2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (aboutCard2.isExpanded()) {
                    aboutCard2.collapseInfo();
                } else {
                    aboutCard1.collapseInfo();
                    aboutCard3.collapseInfo();
                    aboutCard2.expandInfo();
                }
            }
        });

        aboutCard3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (aboutCard3.isExpanded()) {
                    aboutCard3.collapseInfo();
                } else {
                    aboutCard1.collapseInfo();
                    aboutCard2.collapseInfo();
                    aboutCard3.expandInfo();
                }
            }
        });
    }

    private void showTooltipWindow() {
        mYourImpactTooltip.showTooltipWindow(this, findViewById(R.id.section_impact));
    }

    private boolean shouldDisplayImpactTooltip() {
        return !SharedPreferencesHelpers.getBoolean(mContext, NTP_IMPACT_TOOLTIP_DISMISSED, false);
    }

    public void dismissImpactTolltip() {
        if(mYourImpactTooltip != null)
            mYourImpactTooltip.dismiss();
    }

    protected void updateTreeCounterView() {
        final long computedCount = mStatisticsManager.getComputedTreeCount();
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
            setNewsAvailability(true);
            for (int i = 0; i < newsList.size(); i++) {
                News article = newsList.get(i);
                String url = article.getTargetUrl().toString();
                mNewsCards.get(i).initialize(article, i, cardClickListener(url, TrackingManager.LABEL_NEWS, i), getResources());
            }
        } else {
            setNewsAvailability(false);
        }
        if(findViewById(R.id.news_container).getVisibility() == GONE) {
            animateNews();
        }
    }

    private void setNewsAvailability(final boolean value) {
        mNewsAvailable = value;
        EcosiaHomepagePreferencesHelper.setEcosiaNewsAvailable(mContext, value);
    }

    private void showReferralsSuccessWindow(boolean isInviter, int newClaimsCount) {
        if (mYourImpactTooltip != null) {
            // Hide Your Impact Tooltip in case Referrals Success will be shown,
            // but don't persist dismissal so it will be shown next time
            mYourImpactTooltip.dismiss();
        }
        mLastReferralsSuccessTooltipIsInviter = isInviter;
        mLastReferralsSuccessTooltipClaimsCount = newClaimsCount;
        mReferralsSuccessTooltip = new ReferralsSuccessTooltip(mContext);
        mReferralsSuccessTooltip.showTooltipWindow(this, findViewById(R.id.section_impact), mLastReferralsSuccessTooltipIsInviter , mLastReferralsSuccessTooltipClaimsCount);
    }

    private void animateNews() {
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(ANIM_DURATION_MILLISECONDS);

        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setDuration(ANIM_DURATION_MILLISECONDS);

        AnimationSet set = new AnimationSet(true);
        set.setInterpolator(new AccelerateInterpolator());

        Animation slideUp = AnimationUtils.loadAnimation(mContext, R.anim.ecosia_slide_in_up);
        set.addAnimation(slideUp);
        set.addAnimation(fadeIn);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mNewsAvailable) {
                    findViewById(R.id.news_container).startAnimation(set);
                    showNewsSection();
                } else {
                    hideNewsSection();
                }
            }
        }, FADE_IN_GAP_MILLISECONDS);
    }

    private void showNewsSection() {
        findViewById(R.id.news_container).setVisibility(VISIBLE);
    }

    private void hideNewsSection() {
        findViewById(R.id.news_container).setVisibility(GONE);
    }

    private OnClickListener cardClickListener(String url, String label, int rank) {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                UrlHelpers.openUrl(mContext, url);
                mTrackingManager.engagementEvent(label, rank);
            }
        };
    }

    private class NewsReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            refreshNews();
        }
    }

    private class StatsUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final AccessibilityHelper accessibilityHelper = AccessibilityHelper.getAccessibilityHelper(context);
            if (accessibilityHelper.animationsEnabled()) {
                updateTreeCounterView();
                updateInvestmentsAmountView();
            }
        }
    }
}
