package org.ecosia.utils;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import org.chromium.chrome.R;

public class ViewResizer {
    private Context mContext;

    public ViewResizer(Context context) {
        mContext = context;
    }

    public void resizeContentViews(final View containerView, final View viewToBeResized) {
        final Resources resources = mContext.getResources();
        final boolean tabletLikeScreenSize = resources.getBoolean(R.bool.device_screen_size_is_tablet_like);
        if (tabletLikeScreenSize) {
            final int maxWidth = resolveMaxWidth(resources, containerView);
            final ViewGroup.LayoutParams layoutParams = viewToBeResized.getLayoutParams();
            if (layoutParams.width > maxWidth || layoutParams.width <= 0) {
                layoutParams.width = maxWidth;
                viewToBeResized.setLayoutParams(layoutParams);
            }
        }
    }

    private int resolveMaxWidth(final Resources resources, final View containerView) {
        final int maxWidthFromResources = resources.getDimensionPixelSize(R.dimen.ecosia_ntp_tablet_max_width);
        final int containerViewWidth = containerView.getWidth();
        return Math.min(containerViewWidth, maxWidthFromResources);
    }
}
