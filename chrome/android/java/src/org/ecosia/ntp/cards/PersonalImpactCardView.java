package org.ecosia.ntp.cards;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityOptionsCompat;

import org.chromium.base.IntentUtils;
import org.chromium.chrome.browser.IntentHandler;
import org.chromium.ui.base.PageTransition;

public class PersonalImpactCardView extends CardView implements View.OnClickListener {
    private TextView mTopTextView;
    private TextView mBottomTextView;
    private TextView mRightTextView;
    private String mRightTextUrl;
    private Context mContext;

    public PersonalImpactCardView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public PersonalImpactCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PersonalImpactCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;

        inflate(getContext(), org.chromium.chrome.R.layout.ecosia_ntp_personal_impact_card, this);

        mTopTextView = findViewById(org.chromium.chrome.R.id.personal_impact_top_text_view);
        mBottomTextView = findViewById(org.chromium.chrome.R.id.personal_impact_bottom_text_view);
        mRightTextView = findViewById(org.chromium.chrome.R.id.right_text_view);
        mRightTextView.setOnClickListener(this);
    }

    public void setTopText(String text) {
        mTopTextView.setText(text);
    }

    public void setBottomText(String text) {
        mBottomTextView.setText(text);
    }

    public void setRightText(String text, String url) {
        mRightTextView.setText(text);
        mRightTextUrl = url;
    }

    @Override
    public void onClick(View view) {
        Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mRightTextUrl));
        Bundle startActivityOptions = ActivityOptionsCompat.makeCustomAnimation(
                mContext, org.chromium.chrome.R.anim.abc_fade_in, org.chromium.chrome.R.anim.abc_fade_out).toBundle();
        myIntent.setPackage(mContext.getPackageName());
        myIntent.putExtra(IntentHandler.EXTRA_PAGE_TRANSITION_TYPE, PageTransition.LINK);
        myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        IntentUtils.addTrustedIntentExtras(myIntent);
        mContext.startActivity(myIntent, startActivityOptions);
    }
}
