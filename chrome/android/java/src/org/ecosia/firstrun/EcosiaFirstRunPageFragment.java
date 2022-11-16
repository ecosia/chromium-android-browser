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
import org.ecosia.incentives.SearchIncentivesManager;

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

    public EcosiaFirstRunPageFragment(final Page page) {
        mPage = page;
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater layoutInflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        final ViewGroup hostView = (ViewGroup) layoutInflater.inflate(R.layout.ecosia_firstrun_page, null);
        final FrameLayout frameLayout = hostView.findViewById(R.id.ecosia_firstrun_page_content_bg);
        layoutInflater.inflate(mPage.mLayoutId, frameLayout, true);
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
        boolean isRestricted = SearchIncentivesManager.getInstance(getContext()).isRestricted();
        switch (mPage) {
            case PROOF_1:
                if (isRestricted) {
                    headline.setText(R.string.onboarding_page_1_headline_gm);
                    body.setText(R.string.onboarding_page_1_body_gm);
                } else {
                    headline.setText(R.string.onboarding_page_1_headline);
                    body.setText(R.string.onboarding_page_1_body);
                }
                break;

            case PROOF_2:
                headline.setText(R.string.onboarding_page_2_headline);
                body.setText(R.string.onboarding_page_2_body);
                break;

            case PROOF_3:
                headline.setText(R.string.onboarding_page_3_headline);
                body.setText(R.string.onboarding_page_3_body);
                break;

            case PROOF_4:
                if (isRestricted) {
                    headline.setText(R.string.onboarding_page_4_headline_gm);
                    body.setText(R.string.onboarding_page_4_body_gm);
                } else {
                    headline.setText(R.string.onboarding_page_4_headline);
                    body.setText(R.string.onboarding_page_4_body);
                }
                break;
        }
    }

    private void setViews(@NonNull final View view) {
        boolean isRestricted = SearchIncentivesManager.getInstance(getContext()).isRestricted();
        switch (mPage) {
            case PROOF_1:
                if (isRestricted) {
                    ImageView cardBImage = view.findViewById(R.id.ecosia_firstrun_proof_1_card_b_image);
                    TextView cardBTitle = view.findViewById(R.id.ecosia_firstrun_proof_1_card_b_title);
                    TextView cardBText = view.findViewById(R.id.ecosia_firstrun_proof_1_card_b_text);
                    TextView cardBTreeCounter = view.findViewById(R.id.ecosia_firstrun_proof_1_card_b_tree_counter);
                    ImageView cardBImageGm = view.findViewById(R.id.ecosia_firstrun_proof_1_card_b_image_gm);

                    cardBImage.setVisibility(View.INVISIBLE);
                    cardBTitle.setVisibility(View.INVISIBLE);
                    cardBText.setVisibility(View.INVISIBLE);
                    cardBTreeCounter.setVisibility(View.INVISIBLE);
                    cardBImageGm.setVisibility(View.VISIBLE);
                }
                break;

            case PROOF_4:
                Context context = getContext();
                if (context != null && isRestricted) {
                    ImageView proof4Background = view.findViewById(R.id.ecosia_firstrun_fragment_bg);
                    Drawable drawable = ContextCompat.getDrawable(context, R.drawable.onboarding_proof_4_bg_gm);
                    proof4Background.setImageDrawable(drawable);
                }
                break;
        }
    }

    private void setCards(@NonNull final View view) {
        boolean isRestricted = SearchIncentivesManager.getInstance(getContext()).isRestricted();
        if (mPage == Page.PROOF_4 && isRestricted) {
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
