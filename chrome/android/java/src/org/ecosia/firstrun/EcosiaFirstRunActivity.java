/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.ecosia.firstrun;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;

import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.support.constraint.ConstraintLayout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.view.View;

import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.util.Log;
import android.widget.TextView;

import org.chromium.chrome.R;
import org.chromium.chrome.browser.firstrun.FirstRunStatus;
import org.chromium.chrome.browser.init.AsyncInitializationActivity;
import org.chromium.base.IntentUtils;
import org.ecosia.mmp.Singular;
import org.ecosia.tracking.TrackingManager;
import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;

public final class EcosiaFirstRunActivity extends AsyncInitializationActivity
        implements ViewPager.OnPageChangeListener {

    private static final String TAG = "org.ecosia.firstrun.EcosiaFirstRunActivity";
    private static final String EXTRA_PENDING_INTENT = "ecosia_pending_intent";
    private static final int ANIMATION_DURATION_DELAY_MS = 300;
    private static final int ANIMATION_DURATION_TRANSITION_MS = 1400;
    private static final float ANIMATION_TEXT_TRANSLATION_POSITION_DP = -30f;
    private static final float LOGO_SMALL_FINAL_POSITION = 30f;
    private static final float LOGO_SMALL_HEIGHT_DP = 48f;
    private static final float LOGO_SMALL_WIDTH_DP = 98.82f;

    private TrackingManager mTrackingManager;
    private ViewPager mPager;
    private LinearLayout loadingView1;
    private LinearLayout loadingView2;
    private ConstraintLayout loadingView3;

    @Override
    protected void triggerLayoutInflation() {
        setContentView(R.layout.ecosia_firstrun_page_host);

        mTrackingManager = TrackingManager.getInstance(this);
        EcosiaFirstRunAdapter mAdapter = new EcosiaFirstRunAdapter(getSupportFragmentManager());

        mPager = findViewById(R.id.ecosia_firstrun_page_view_pager);
        mPager.setAdapter(mAdapter);
        mPager.addOnPageChangeListener(this);

        final TabLayout tabLayout = findViewById(R.id.ecosia_firstrun_page_tab_layout);
        tabLayout.setupWithViewPager(mPager, true); onInitialLayoutInflationComplete();

        mTrackingManager.sendIntroDisplayEvent(0);

        ConstraintLayout pagerContainer = findViewById(R.id.ecosia_firstrun_pager_container);
        pagerContainer.setVisibility(View.GONE);
        displayLoadingAnimation();

        TextView firstRunContinueButton = findViewById(R.id.firstrun_continue_button);
        firstRunContinueButton.setOnClickListener(view -> {
            loadingView3.setVisibility(View.GONE);
            pagerContainer.setVisibility(View.VISIBLE);
        });

        TextView firstRunSkipButton = findViewById(R.id.firstrun_skip_button);
        firstRunSkipButton.setOnClickListener(view -> skip());
    }

    private void displayLoadingAnimation() {
        loadingView1 = findViewById(R.id.ecosia_firstrun_loading_1);
        loadingView2 = findViewById(R.id.ecosia_firstrun_loading_2);
        loadingView3 = findViewById(R.id.ecosia_firstrun_loading_3);

        loadingView1.setVisibility(View.VISIBLE);
        loadingView2.setVisibility(View.GONE);
        loadingView3.setVisibility(View.GONE);

        animateLogoDelay();
    }

    private void animateLogoDelay() {
        loadingView1.animate()
                .alpha(1f)
                .setDuration(ANIMATION_DURATION_DELAY_MS)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        animateFadeInBackgroundReveal();
                    }
                });
    }

    private void animateFadeInBackgroundReveal() {
        loadingView2.setAlpha(0f);
        loadingView2.setVisibility(View.VISIBLE);
        loadingView2.animate()
                .alpha(1f)
                .setDuration(ANIMATION_DURATION_TRANSITION_MS)
                .setListener(null);

        loadingView1.animate()
                .alpha(0f)
                .setDuration(ANIMATION_DURATION_TRANSITION_MS)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        animateBackgroundRevealDelay();
                    }
                });
    }

    private void animateBackgroundRevealDelay() {
        loadingView1.animate()
                .alpha(1f)
                .setDuration(ANIMATION_DURATION_DELAY_MS)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        loadingView1.setVisibility(View.GONE);
                        animateOnboardingText();
                    }
                });
    }

    private void animateOnboardingText() {
        loadingView2.setVisibility(View.GONE);
        loadingView3.setVisibility(View.VISIBLE);

        formatTextWithImages();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;
        float screenHeightDp = convertPixelsToDp(screenHeight);

        Rect rectangle = new Rect();
        Window window = getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
        int statusBarHeight = rectangle.top;
        float statusBarHeightDp = convertPixelsToDp(statusBarHeight);

        ImageView ecosiaLogo = findViewById(R.id.firstrun_logo);

        float translationDp = statusBarHeightDp + LOGO_SMALL_FINAL_POSITION - screenHeightDp / 2f;
        float logoBigHeightDp = convertPixelsToDp(ecosiaLogo.getLayoutParams().height);
        float logoBigWidthDp = convertPixelsToDp(ecosiaLogo.getLayoutParams().width);
        float scaleX = LOGO_SMALL_WIDTH_DP / logoBigWidthDp;
        float scaleY = LOGO_SMALL_HEIGHT_DP / logoBigHeightDp;

        ecosiaLogo.animate()
                .scaleX(scaleX)
                .scaleY(scaleY)
                .translationY(convertDpToPixel(translationDp))
                .setDuration(ANIMATION_DURATION_DELAY_MS);

        animateTextView(findViewById(R.id.firstrun_text));
        animateTextView(findViewById(R.id.firstrun_continue_button));
        animateTextView(findViewById(R.id.firstrun_skip_button));
    }

    private void formatTextWithImages() {
        TextView firstRunTextView = findViewById(R.id.firstrun_text);
        // workaround for image span not recognizing new line characters
        String firstRunText = firstRunTextView.getText().toString().replace("\n", "@");
        SpannableString spannableString = new SpannableString(firstRunText);

        int index1 = spannableString.toString().indexOf("@");
        int index2 = spannableString.toString().indexOf("@", index1 + 1);
        Drawable image1 = getResources().getDrawable(R.drawable.firstrun_text_tree_yellow);
        Drawable image2 = getResources().getDrawable(R.drawable.firstrun_text_tree_green);
        addImage(spannableString, index1, image1);
        addImage(spannableString, index2, image2);

        firstRunTextView.setText(spannableString);
    }

    private void addImage(SpannableString string, int startIndex, Drawable drawable) {
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        ImageSpan span = new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);
        string.setSpan(span, startIndex, startIndex + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    }

    private void animateTextView(TextView textView) {
        textView.setAlpha(0f);
        textView.animate()
                .alpha(1f)
                .translationY(convertDpToPixel(ANIMATION_TEXT_TRANSLATION_POSITION_DP))
                .setDuration(ANIMATION_DURATION_DELAY_MS);
    }

    private float convertPixelsToDp(float px){
        Resources resources = getApplicationContext().getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return px / ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    private float convertDpToPixel(float dp){
        Resources resources = getApplicationContext().getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    @Override
    public boolean shouldStartGpuProcess() {
        return true;
    }

    @Override
    protected boolean requiresFirstRunToBeCompleted(final Intent intent) {
        return false;
    }

    void finishOnboarding() {
        // Set Chromium FirstRunFlow complete. We don't want to show it.
        FirstRunStatus.setFirstRunFlowComplete(true);
        installTracking();
        finish();
        final PendingIntent pendingIntent =
                IntentUtils.safeGetParcelableExtra(getIntent(), EXTRA_PENDING_INTENT);

        if (pendingIntent != null) {
            try {
                pendingIntent.send(Activity.RESULT_OK);
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, "Exception: " + e);
            }
        }
    }

    private void installTracking() {
        //Ecosia : InstallReferrer is called once based on first run setting
        InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(this).build();
        referrerClient.startConnection(new InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {
                switch (responseCode) {
                    case InstallReferrerClient.InstallReferrerResponse.OK:
                        try {
                            ReferrerDetails response = referrerClient.getInstallReferrer();
                            String referrerUrl = response.getInstallReferrer();
                            TrackingManager.getInstance(getApplicationContext()).install(referrerUrl);
                            Singular.getInstance(getApplicationContext()).setInstallRef(response);
                        } catch (RemoteException e) {
                            org.chromium.base.Log.e(TAG, "" + e.getMessage());
                        }
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                        org.chromium.base.Log.e(TAG, "InstallReferrer client connection feature not supported error");
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                        org.chromium.base.Log.e(TAG, "InstallReferrer service unavailable error");
                        break;
                }
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {
                org.chromium.base.Log.e(TAG, "InstallReferrer service disconnected error");
            }
        });
    }

    void next() {
        mTrackingManager.sendIntroNextClickEvent(mPager.getCurrentItem());
        nextPage();
    }

    void skip() {
        mTrackingManager.sendIntroSkipClickEvent(mPager.getCurrentItem());
        finishOnboarding();
    }

    void nextPage() {
        if (mPager.getAdapter() == null) {
            return;
        }

        final int max = mPager.getAdapter().getCount();
        final int next = mPager.getCurrentItem() + 1;

        if (next < max) {
            mPager.setCurrentItem(next);
        } else {
            finishOnboarding();
        }
    }

    @Override
    public void onPageSelected(final int position) {
        if (position > 0) {
            mTrackingManager.sendIntroDisplayEvent(position);
        }
    }

    @Override
    public void onPageScrolled(final int position, final float positionOffset,
                               final int positionOffsetPixels) {
        // not implemented
    }

    @Override
    public void onPageScrollStateChanged(final int position) {
        // not implemented
    }

    public static boolean launch(final Context context, final Intent fromIntent) {
        if (FirstRunStatus.getFirstRunFlowComplete()) {
            return false;
        }
        final Intent intent = new Intent(context, EcosiaFirstRunActivity.class);
        final int pendingIntentFlags =
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE;

        final PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, fromIntent, pendingIntentFlags);

        intent.putExtra(EXTRA_PENDING_INTENT, pendingIntent);
        IntentUtils.safeStartActivity(context, intent);
        return true;
    }
}
