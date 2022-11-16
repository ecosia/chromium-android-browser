// Copyright 2017 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.components.browser_ui.widget;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import org.chromium.components.browser_ui.widget.PromoDialog.DialogParams;

/**
 * Lays out a promo dialog that is shown when Clank starts up.
 *
 * Because of the versatility of dialog content and screen sizes, this layout exhibits a bunch of
 * specific behaviors (see go/snowflake-dialogs for details):
 *
 * + It hides controls when their resources are not specified by the {@link DialogParams}.
 *   The only two required components are the header text and the primary button label.
 *
 * + When the width is greater than the height, the promo content switches from vertical to
 *   horizontal and moves the illustration from the top of the text to the side of the text.
 *
 * + The buttons are always locked to the bottom of the dialog and stack when there isn't enough
 *   room to display them on one row.
 *
 * + If there is no promo illustration, the header text becomes locked to the top of the dialog and
 *   doesn't scroll away.
 */
public final class PromoDialogLayout extends BoundedLinearLayout {
    /** Content in the dialog that will flip orientation when the screen is wide. */
    private LinearLayout mFlippableContent;

    /** The scrolling container for the scrollable content. */
    private ViewGroup mScrollingContainer;

    /** Content in the dialog that can be scrolled. */
    private LinearLayout mScrollableContent;

    /** Illustration that teases the thing being promoted. */
    private ImageView mIllustrationView;

    /** View containing the header of the promo. */
    private TextView mHeaderView;

    /** View containing the header of the promo. */
    private TextView mFooterView;

    /** View containing text explaining the promo. */
    private TextView mSubheaderView;
    /*Ecosia : Custom views for promos */
    private TextView mSubheaderView2;

    private TextView mSubheaderTitle;

    private TextView mSubheaderTitle2;

    private ImageView mSubheaderImgCheck;

    private ImageView mSubheader2ImgCheck;

    private LinearLayout mSubheaderLayout;
    /*Ecosia <--> */

    /** Paramters used to build the promo. */
    private DialogParams mParams;

    public PromoDialogLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onFinishInflate() {
        mFlippableContent = (LinearLayout) findViewById(R.id.full_promo_content);
        mScrollingContainer = (ViewGroup) findViewById(R.id.promo_container);
        mScrollableContent = (LinearLayout) findViewById(R.id.scrollable_promo_content);
        mIllustrationView = (ImageView) findViewById(R.id.illustration);
        mHeaderView = (TextView) findViewById(R.id.header);
        //Ecosia : Custom views added for promos
        mSubheaderView = (TextView) findViewById(R.id.subheader_desc);
        mSubheaderView2 = (TextView) findViewById(R.id.subheader2_desc);
        mSubheaderTitle = (TextView) findViewById(R.id.subheader);
        mSubheaderTitle2 = (TextView) findViewById(R.id.subheader2);
        mSubheaderImgCheck = (ImageView) findViewById(R.id.subheader_check);
        mSubheader2ImgCheck = (ImageView) findViewById(R.id.subheader2_check);
        mSubheaderLayout = (LinearLayout) findViewById(R.id.subheader_layout);
        super.onFinishInflate();
    }

    /** Initializes the dialog contents using the given params.  Should only be called once. */
    void initialize(DialogParams params) {
        assert mParams == null && params != null;
        assert params.headerStringResource != 0 || params.headerCharSequence != null;
        assert params.primaryButtonStringResource != 0 || params.primaryButtonCharSequence != null;
        mParams = params;

        if (mParams.drawableInstance != null) {
            mIllustrationView.setImageDrawable(mParams.drawableInstance);
        } else if (mParams.vectorDrawableResource != 0) {
            mIllustrationView.setImageDrawable(VectorDrawableCompat.create(
                    getResources(), mParams.vectorDrawableResource, getContext().getTheme()));
        } else if (mParams.drawableResource != 0) {
            mIllustrationView.setImageResource(mParams.drawableResource);
        } else {
            // Dialogs with no illustration make the header stay visible at all times instead of
            // scrolling off on small screens.
            ((ViewGroup) mIllustrationView.getParent()).removeView(mIllustrationView);
        }

        // Create the header.
        if (mParams.headerCharSequence != null) {
            mHeaderView.setText(mParams.headerCharSequence);
        } else {
            mHeaderView.setText(mParams.headerStringResource);
        }

        //Ecosia : text alignment

        if(params.subheaderTitle != null) {
            mSubheaderTitle.setText(params.subheaderTitle);
            mSubheaderTitle.setVisibility(VISIBLE);
        } else {
            mSubheaderTitle.setVisibility(GONE);
        }

        if(params.subheader2Title != null) {
            mSubheaderTitle2.setText(params.subheader2Title);
            mSubheaderTitle2.setVisibility(VISIBLE);
        } else {
            mSubheaderTitle2.setVisibility(GONE);
        }

        // Set up the subheader text.
        if (mParams.subheaderCharSequence != null) {
            // Ecosia : Custom content
            if(mParams.subheaderCharSequence2 == null) { //Ecosia : Set match parent width if only one subheader is found
                mSubheaderLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                mHeaderView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
            }
            mSubheaderView.setText(mParams.subheaderCharSequence);
            if (mParams.subheaderIsLink) {
                mSubheaderView.setMovementMethod(LinkMovementMethod.getInstance());
            }
        } else if (mParams.subheaderStringResource == 0) {
            ((ViewGroup) mSubheaderView.getParent()).removeView(mSubheaderView);
        } else {
            mSubheaderView.setText(mParams.subheaderStringResource);
        }

        // Ecosia : Set up the subheader2 text.
        if (mParams.subheaderCharSequence2 != null) {
            mSubheaderView2.setText(mParams.subheaderCharSequence2);
            if (mParams.subheaderIsLink) {
                mSubheaderView2.setMovementMethod(LinkMovementMethod.getInstance());
            }
        } else if (mParams.subheaderStringResource == 0) {
            ((ViewGroup) mSubheaderView2.getParent()).removeView(mSubheaderView2);
        } else {
            mSubheaderView2.setText(mParams.subheaderStringResource2);
        }

        // Create the footer.
        ViewStub footerStub = (ViewStub) findViewById(R.id.footer_stub);
        if (mParams.footerStringResource == 0) {
            ((ViewGroup) footerStub.getParent()).removeView(footerStub);
        } else {
            mFooterView = (TextView) footerStub.inflate();
            mFooterView.setText(mParams.footerStringResource);
        }

        // Create the buttons.
        DualControlLayout buttonBar = (DualControlLayout) findViewById(R.id.button_bar);
        String primaryString = mParams.primaryButtonCharSequence != null
                ? mParams.primaryButtonCharSequence.toString()
                : getResources().getString(mParams.primaryButtonStringResource);

        /* |-> Ecosia : Adding custom layout style and theme for buttons */
        buttonBar.setAlignment(DualControlLayout.DualControlLayoutAlignment.APART);
        final Button buttonForLayout;
        if (params.primaryButtonBackgroundDrawableResource > 0) {
            buttonForLayout = DualControlLayout.createButtonForLayout(getContext(), true, primaryString, null,
                    params.primaryButtonBackgroundDrawableResource, params.themePrimaryBtn);
        } else {
            buttonForLayout = DualControlLayout.createButtonForLayout(getContext(), true, primaryString, null);
        }

        buttonBar.addView(buttonForLayout);

        if (mParams.secondaryButtonStringResource != 0) {
            String secondaryString =
                    getResources().getString(mParams.secondaryButtonStringResource);
            if(mParams.themeSecondaryBtn > 0) {
                buttonBar.addView(DualControlLayout.createButtonForLayout(
                        getContext(), false, secondaryString, null, 0, mParams.themeSecondaryBtn));
            } else {
                buttonBar.addView(DualControlLayout.createButtonForLayout(
                        getContext(), false, secondaryString, null));
            }

        }

        if(params.isPrimaryCheckShown) {
            mSubheaderImgCheck.setImageResource(params.primaryDescIcon);
            mSubheaderImgCheck.setVisibility(VISIBLE);
        } else {
            mSubheaderImgCheck.setVisibility(GONE);
        }
        if(params.isSecondaryCheckShown) {
            mSubheader2ImgCheck.setImageResource(params.secondaryDescIcon);
            mSubheader2ImgCheck.setVisibility(VISIBLE);
        } else {
            mSubheader2ImgCheck.setVisibility(GONE);
        }

        /* Ecosia ->| */
    }

    /**
     * Determines whether the header layout needs to be adjusted to ensure the scrollable content
     * is usable in small form factors.
     *
     * @return Whether the layout needed to be adjusted.
     */
    private boolean fixupHeader() {
        if (mParams.drawableResource != 0 || mParams.vectorDrawableResource != 0
                || mParams.drawableInstance != null) {
            return false;
        }

        int minScrollHeight =
                getResources().getDimensionPixelSize(R.dimen.promo_dialog_min_scrollable_height);
        boolean shouldHeaderScroll = mScrollingContainer.getMeasuredHeight() < minScrollHeight;
        ViewGroup desiredParent;
        boolean applyHeaderPadding;
        if (shouldHeaderScroll) {
            desiredParent = mScrollableContent;
            applyHeaderPadding = false;
        } else {
            desiredParent = this;
            applyHeaderPadding = true;
        }
        if (mHeaderView.getParent() == desiredParent) return false;
        ((ViewGroup) mHeaderView.getParent()).removeView(mHeaderView);
        desiredParent.addView(mHeaderView, 0);

        int startEndPadding = applyHeaderPadding
                ? getResources().getDimensionPixelSize(R.dimen.promo_dialog_padding)
                : 0;
        ViewCompat.setPaddingRelative(mHeaderView, startEndPadding, 0, startEndPadding, 0);
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int availableWidth = MeasureSpec.getSize(widthMeasureSpec);
        int availableHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (availableWidth > availableHeight * 1.5) {
            mFlippableContent.setOrientation(LinearLayout.HORIZONTAL);
        } else {
            mFlippableContent.setOrientation(LinearLayout.VERTICAL);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (fixupHeader()) super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /** Adds a View to the layout within the scrollable area. */
    void addControl(View control) {
        mScrollableContent.addView(
                control, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }
}
