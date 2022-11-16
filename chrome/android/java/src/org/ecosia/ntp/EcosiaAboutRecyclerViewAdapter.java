/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.ecosia.ntp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.chromium.base.IntentUtils;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.IntentHandler;
import org.chromium.ui.base.PageTransition;

import java.util.ArrayList;

public class EcosiaAboutRecyclerViewAdapter extends RecyclerView.Adapter<EcosiaAboutRecyclerViewAdapter.AboutEcosiaViewHolder> {

    private Context mContext;
    private ArrayList<AboutSection> mAboutContents;
    private LinearLayout mPrevLayout;
    private ImageView mPrevBtn;
    private View mPrevView;
    private int mPrevClickPos = -1;

    public EcosiaAboutRecyclerViewAdapter(Context context, ArrayList<AboutSection> aboutContents) {
        mContext = context;
        mAboutContents = aboutContents;
    }

    @NonNull
    @Override
    public AboutEcosiaViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int pos) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.ecosia_about_card, viewGroup, false);
        AboutEcosiaViewHolder viewHolder = new AboutEcosiaViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull AboutEcosiaViewHolder ecosiaAboutViewHolder, int pos) {
        ecosiaAboutViewHolder.mImage.setImageDrawable(mAboutContents.get(pos).getImage());
        ecosiaAboutViewHolder.mTitle.setText(mAboutContents.get(pos).getTitle());
        ecosiaAboutViewHolder.mText.setText(mAboutContents.get(pos).getText());
        ecosiaAboutViewHolder.mView.setVisibility(pos == (mAboutContents.size()-1) ? View.GONE : View.VISIBLE);
    }

    public void openURL(String url) {

        Bundle startActivityOptions = ActivityOptionsCompat.makeCustomAnimation(
                mContext, org.chromium.chrome.R.anim.abc_fade_in, org.chromium.chrome.R.anim.abc_fade_out).toBundle();
        Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        myIntent.setPackage(mContext.getApplicationContext().getPackageName());
        myIntent.putExtra(IntentHandler.EXTRA_PAGE_TRANSITION_TYPE, PageTransition.LINK);
        myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        IntentUtils.addTrustedIntentExtras(myIntent);
        mContext.startActivity(myIntent, startActivityOptions);
    }

    @Override
    public int getItemCount() {
        return mAboutContents.size();
    }

    public class AboutEcosiaViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView mImage;
        TextView mTitle;
        ImageView mbtn;
        LinearLayout mExpandLayout;
        TextView mText;
        TextView mLearnMore;
        View mView;
        RelativeLayout mParentLayout;

        public AboutEcosiaViewHolder(@NonNull View itemView) {
            super(itemView);
            mParentLayout = itemView.findViewById(R.id.parent_card);
            mImage = itemView.findViewById(R.id.thumbnail_about_ecosia);
            mTitle = itemView.findViewById(R.id.text_about_ecosia);
            mbtn = itemView.findViewById(R.id.btn_about_ecosia);
            mExpandLayout = itemView.findViewById(R.id.expanding_layout);
            mText = itemView.findViewById(R.id.about_text);
            mLearnMore = itemView.findViewById(R.id.text_learn_more);
            mView = itemView.findViewById(R.id.view_about_card);
            mExpandLayout.setVisibility(View.GONE);
            mParentLayout.setOnClickListener(this);
            mLearnMore.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mLearnMore.equals(v)) {
                AboutSection about = mAboutContents.get(getAbsoluteAdapterPosition());
                openURL(about.getTargetUrl());
            } else {
                boolean isSameItem = (mPrevClickPos == getAbsoluteAdapterPosition() ? true : false);
                resetPrevBtn(isSameItem);
                if(isSameItem && mAboutContents.get(getAbsoluteAdapterPosition()).isExpand()) {
                    mAboutContents.get(getAbsoluteAdapterPosition()).setExpand(false);
                    return;
                }
                AboutSection aboutSection = mAboutContents.get(getAbsoluteAdapterPosition());
                aboutSection.setExpand(!aboutSection.isExpand());
                mExpandLayout.setVisibility(View.VISIBLE);
                mView.setVisibility(View.GONE);
                mbtn.setBackground(mContext.getDrawable(R.drawable.ecosia_collapse_arrow));
                setCurPos(getAbsoluteAdapterPosition());
            }
        }

        boolean hideLastView(int pos) {
            return (pos == mAboutContents.size()-1);
        }

        private void resetPrevBtn(boolean isSameItemClicked) {
            if ((mPrevClickPos > -1) || isSameItemClicked) {
                mPrevLayout.setVisibility(View.GONE);
                mPrevBtn.setBackground(mContext.getDrawable((R.drawable.ecosia_expand_arrow)));
                mPrevView.setVisibility(hideLastView(mPrevClickPos) ? View.GONE : View.VISIBLE);
                mAboutContents.get(mPrevClickPos).setExpand(isSameItemClicked ? mAboutContents.get(mPrevClickPos).isExpand() :
                        !mAboutContents.get(mPrevClickPos).isExpand());
            }
        }

        private void setCurPos(int pos){
            mPrevClickPos = pos;
            mPrevLayout = mExpandLayout;
            mPrevBtn = mbtn;
            mPrevView = mView;
        }
    }

}
