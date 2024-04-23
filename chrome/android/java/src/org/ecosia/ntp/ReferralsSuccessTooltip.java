package org.ecosia.ntp;

import org.chromium.chrome.R;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ReferralsSuccessTooltip {

    private final Context mContext;
    private PopupWindow mPopupWindow;

    public ReferralsSuccessTooltip(final Context context) {
        mContext = context;
    }

    public void showTooltipWindow(final View view, final View anchor, boolean isInviter, int newClaimsCount) {
        LayoutInflater inflater =
                (LayoutInflater) view.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup parent = ((ViewGroup) view.getParent());
        View popupView = inflater.inflate(R.layout.ecosia_ntp_referrals_success_tooltip, parent, false);
        TextView textView = popupView.findViewById(R.id.tooltip_text);
        if (textView != null) {
            if (isInviter) {
                textView.setText(mContext.getResources().getQuantityString(R.plurals.ecosia_referrals_success_tooltip_success_inviter, newClaimsCount, newClaimsCount));
            } else {
                textView.setText(R.string.ecosia_referrals_success_tooltip_success_invitee);
            }
        }
        popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        ViewTreeObserver observer = anchor.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int width = anchor.getWidth();
                int height = RelativeLayout.LayoutParams.WRAP_CONTENT;
                int marginInPixels = (int) mContext.getResources().getDimension(R.dimen.your_impact_tooltip_bottom_margin);
                mPopupWindow = new PopupWindow(popupView, width, height, true);
                int yOffset = -anchor.getHeight() - popupView.getMeasuredHeight() - marginInPixels;
                mPopupWindow.showAsDropDown(anchor, 0, yOffset, Gravity.CENTER_HORIZONTAL);

                ImageView closeButton = popupView.findViewById(R.id.tooltip_close_button);
                closeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPopupWindow.dismiss();
                    }
                });

                anchor.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    public void dismiss() {
        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
        }
    }
}
