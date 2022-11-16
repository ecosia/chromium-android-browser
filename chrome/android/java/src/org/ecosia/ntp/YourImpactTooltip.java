package org.ecosia.ntp;

import org.chromium.chrome.R;
import org.ecosia.utils.SharedPreferencesHelpers;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

public class YourImpactTooltip {

    public static final String NTP_IMPACT_TOOLTIP_DISMISSED = "NTP_IMPACT_TOOLTIP_DISMISSED";
    private static final int MAGIC_OFFSET_DP = -129;    // TODO check MOB-2010 - wrong tooltip position on physical devices

    private final Context mContext;
    private PopupWindow mPopupWindow;

    public YourImpactTooltip(final Context context) {
        mContext = context;
    }

    public void showTooltipWindow(final View view, final View anchor) {
        LayoutInflater inflater =
                (LayoutInflater) view.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup parent = ((ViewGroup) view.getParent());
        View popupView = inflater.inflate(R.layout.ecosia_ntp_impact_about_tooltip, parent, false);
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
                // TODO check MOB-2010 - wrong tooltip position on physical devices
                //int yOffset = -anchor.getHeight(); // - popupView.getMeasuredHeight() - marginInPixels;
                //mPopupWindow.showAsDropDown(anchor, 0, yOffset, Gravity.CENTER_HORIZONTAL);
                int magicOffsetPx = (int) convertDpToPixel(MAGIC_OFFSET_DP);
                mPopupWindow.showAsDropDown(anchor, 0, magicOffsetPx, Gravity.CENTER_HORIZONTAL);
                mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        SharedPreferencesHelpers.putBoolean(mContext, NTP_IMPACT_TOOLTIP_DISMISSED, true);
                    }
                });

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

    public float convertDpToPixel(float dp){
        return dp * ((float) mContext.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public void dismiss() {
        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
        }
    }
}
