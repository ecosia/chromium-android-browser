package org.ecosia.ntp.cards;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.ecosia.utils.UrlHelpers;

public class AboutCard {
    private final RelativeLayout mParentLayout;
    private final Context mContext;
    private final ImageView mButton;
    private final LinearLayout mExpandLayout;
    private boolean mIsExpanded;

    public AboutCard(RelativeLayout layout, String title, Drawable img, String text, String url, Context context) {
        mContext = context;
        mIsExpanded = false;

        mParentLayout = layout.findViewById(org.chromium.chrome.R.id.parent_card);
        ImageView thumbnailView = layout.findViewById(org.chromium.chrome.R.id.thumbnail_about_ecosia);
        TextView titleView = layout.findViewById(org.chromium.chrome.R.id.text_about_ecosia);
        TextView textView = layout.findViewById(org.chromium.chrome.R.id.about_text);
        TextView learnMore = layout.findViewById(org.chromium.chrome.R.id.text_learn_more);
        mButton = layout.findViewById(org.chromium.chrome.R.id.btn_about_ecosia);
        mExpandLayout = layout.findViewById(org.chromium.chrome.R.id.expanding_layout);
        mExpandLayout.setVisibility(View.GONE);

        thumbnailView.setImageDrawable(img);
        titleView.setText(title);
        textView.setText(text);

        learnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UrlHelpers.openUrl(mContext, url);
            }
        });
    }

    public void setOnClickListener(View.OnClickListener listener) {
        mParentLayout.setOnClickListener(listener);
    }

    public boolean isExpanded() {
        return mIsExpanded;
    }

    public void collapseInfo () {
        mExpandLayout.setVisibility(View.GONE);
        mButton.setBackground(mContext.getDrawable(org.chromium.chrome.R.drawable.ecosia_expand_arrow));
        mIsExpanded = false;
    }

    public void expandInfo() {
        mExpandLayout.setVisibility(View.VISIBLE);
        mButton.setBackground(mContext.getDrawable(org.chromium.chrome.R.drawable.ecosia_collapse_arrow));
        mIsExpanded = true;
    }
}
