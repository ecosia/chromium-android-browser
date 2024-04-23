package org.ecosia.adblock;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;

public class AdblockSwitchTranslucentOverlay extends FrameLayout {

    private WeakReference<View> mTargetView = new WeakReference<>(null);

    public AdblockSwitchTranslucentOverlay(Context context) {
        super(context);
    }

    public AdblockSwitchTranslucentOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AdblockSwitchTranslucentOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AdblockSwitchTranslucentOverlay(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setTargetView(final View targetView) {
        mTargetView = new WeakReference<>(targetView);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final View targetView = mTargetView.get();
        if (targetView != null) {
            targetView.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        final View targetView = mTargetView.get();
        if (targetView != null) {
            targetView.performClick();
        }
        return super.performClick();
    }
}
