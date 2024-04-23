/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.ecosia.firstrun;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.chromium.chrome.R;
import org.ecosia.ntp.EcosiaStatisticsManager;

import java.lang.ref.WeakReference;

public class EcosiaFirstRunPageFragment extends Fragment {

    public enum Page {
        PROOF_1(R.layout.ecosia_firstrun_proof_1),
        PROOF_2(R.layout.ecosia_firstrun_proof_2),
        PROOF_3(R.layout.ecosia_firstrun_proof_3),
        PROOF_4(R.layout.ecosia_firstrun_proof_4);

        private final int mLayoutId;

        Page(final int layoutId) {
            mLayoutId = layoutId;
        }

        public int getLayoutId() {
            return mLayoutId;
        }
    }

    private final Page mPage;
    private EcosiaStatisticsManager mStatisticsManager;
    private WeakReference<TextView> mTreeCountTextView;

    public EcosiaFirstRunPageFragment(final Page page) {
        mPage = page;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getContext();
        mStatisticsManager = EcosiaStatisticsManager.getInstance(context);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater layoutInflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        final ViewGroup hostView = (ViewGroup) layoutInflater.inflate(R.layout.ecosia_firstrun_page, null);
        final FrameLayout frameLayout = hostView.findViewById(R.id.ecosia_firstrun_page_content_bg);
        layoutInflater.inflate(mPage.mLayoutId, frameLayout, true);
        final TextView treeCountTextView = hostView.findViewById(R.id.onboarding_page_3_card_1_headline_text);
        mTreeCountTextView = new WeakReference<>(treeCountTextView);
        return hostView;
    }

    @Override
    public void onViewCreated(@NonNull final View view, final Bundle savedInstanceState) {
        setText(view);
        setViews(view);
        setCards(view);
        setOnClickListeners(view);
    }

    private void setText(@NonNull final View view) {
        final TextView headline = view.findViewById(R.id.ecosia_firstrun_page_headline);
        final TextView body = view.findViewById(R.id.ecosia_firstrun_page_text);
        switch (mPage) {
            case PROOF_1:
                headline.setText(R.string.onboarding_page_1_headline_gm);
                body.setText(R.string.onboarding_page_1_body_gm);
                break;

            case PROOF_2:
                headline.setText(R.string.onboarding_page_2_headline);
                body.setText(R.string.onboarding_page_2_body);
                break;

            case PROOF_3:
                headline.setText(R.string.onboarding_page_3_headline);
                body.setText(R.string.onboarding_page_3_body);
                updateNumberOfTreesDisplayed();
                break;

            case PROOF_4:
                headline.setText(R.string.onboarding_page_4_headline_gm);
                body.setText(R.string.onboarding_page_4_body_gm);
                break;
        }
    }

    private void updateNumberOfTreesDisplayed() {
        final TextView textView = mTreeCountTextView.get();
        if (textView == null) {
            return;
        }

        final String formattedTreeCount = mStatisticsManager.getOnboardingTreeCount();
        textView.setText(formattedTreeCount);
    }

    private void setViews(@NonNull final View view) {
        switch (mPage) {
            case PROOF_4:
                Context context = getContext();
                if (context != null) {
                    ImageView proof4Background = view.findViewById(R.id.ecosia_firstrun_fragment_bg);
                    Drawable drawable = ContextCompat.getDrawable(context, R.drawable.onboarding_proof_4_bg_gm);
                    proof4Background.setImageDrawable(drawable);
                }
                break;
        }
    }

    private void setCards(@NonNull final View view) {
        if (mPage == Page.PROOF_4) {
            ConstraintLayout proof4CardsLayout = view.findViewById(R.id.ecosia_firstrun_proof_4_cards_gm);
            proof4CardsLayout.setVisibility(View.VISIBLE);
        }
    }

    private void setOnClickListeners(final View view) {
        final View skipButton = view.findViewById(R.id.ecosia_firstrun_skip_button);
        if (skipButton != null) {
            skipButton.setOnClickListener((v) -> skipButtonAction());
        }

        final View continueButton = view.findViewById(R.id.ecosia_firstrun_continue_button);
        if (continueButton != null) {
            continueButton.setOnClickListener((v) -> continueButtonAction());
        }
    }

    private void continueButtonAction() {
        if (getActivity() instanceof EcosiaFirstRunActivity) {
            final EcosiaFirstRunActivity activity = (EcosiaFirstRunActivity) getActivity();
            activity.next();
        }
    }

    private void skipButtonAction() {
        if (getActivity() instanceof EcosiaFirstRunActivity) {
            final EcosiaFirstRunActivity activity = (EcosiaFirstRunActivity) getActivity();
            activity.skip();
        }
    }
}
