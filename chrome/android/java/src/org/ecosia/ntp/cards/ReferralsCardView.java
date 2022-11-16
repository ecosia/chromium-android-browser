package org.ecosia.ntp.cards;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import org.chromium.chrome.R;

public class ReferralsCardView extends CardView implements View.OnClickListener {
    private TextView mTopTextView;
    private TextView mBottomTextView;
    private TextView mRightTextView;
    private Context mContext;

    @Nullable
    private OnClickListener mButtonAction;

    public ReferralsCardView(@NonNull Context context, int layout) {
        super(context);
        init(context, layout);
    }

    public ReferralsCardView(@NonNull Context context, @Nullable AttributeSet attrs, int layout) {
        super(context, attrs);
        init(context, layout);
    }

    public ReferralsCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int layout) {
        super(context, attrs, defStyleAttr);
        init(context, layout);
    }

    private void init(Context context, int layout) {
        mContext = context;

        inflate(getContext(), layout, this);

        mTopTextView = findViewById(R.id.referrals_card_top_text_view);
        mBottomTextView = findViewById(R.id.referrals_card_bottom_text_view);
        mRightTextView = findViewById(R.id.right_text_view);
        mRightTextView.setOnClickListener(this);
    }

    public void setTopText(String text) {
        mTopTextView.setText(text);
    }

    public void setBottomText(String text) {
        mBottomTextView.setText(text);
    }

    public void setButton(String text, OnClickListener action) {
        mButtonAction = action;
        mRightTextView.setText(text);
        mRightTextView.setVisibility(VISIBLE);
    }

    @Override
    public void onClick(View view) {
        if (view == mRightTextView && mButtonAction != null) {
            mButtonAction.onClick(view);
        }
    }
}
