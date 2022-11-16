package org.ecosia.ntp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import org.chromium.chrome.R;

public class TreeCounterProgressbar extends View {
    private static final int START_ANGLE = 150;
    private static final int SWEEP_ANGLE = 240;
    private static final int NO_OF_SEARCHES_FOR_A_TREE = 50;
    private static final int INITIAL_PROGRESS = 4;
    private static final int STROKE_WIDTH = 12;
    private int mProgress;
    private int mTreeCycles;
    private final RectF mRect;

    public TreeCounterProgressbar(Context context) {
        super(context);
        mRect = new RectF();
    }

    public TreeCounterProgressbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRect = new RectF();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int mCenterX = getMeasuredWidth() / 2;
        int mCenterY = getMeasuredHeight() / 2;
        int radius = Math.min(mCenterX, mCenterY) - STROKE_WIDTH;
        int startTop = mCenterY - radius;
        int startLeft = mCenterX - radius;
        int endBottom = mCenterY + radius;
        int endRight = mCenterX + radius;
        mRect.set(startLeft, startTop, endRight, endBottom);
        Paint progressPaint = getPaint();
        progressPaint.setColor(ContextCompat.getColor(getContext(), R.color.ecosia_brand_primary));
        Paint backgroundPaint = getPaint();
        backgroundPaint.setColor(ContextCompat.getColor(getContext(), R.color.ecosia_background_progressbar));
        canvas.drawArc(mRect, START_ANGLE, SWEEP_ANGLE, false, backgroundPaint);
        canvas.drawArc(mRect, START_ANGLE, mProgress, false, progressPaint);
    }

    private Paint getPaint() {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(STROKE_WIDTH);
        return paint;
    }

    protected void CalculateProgress(int val) {
        float degreesPerSearch = (float) SWEEP_ANGLE / NO_OF_SEARCHES_FOR_A_TREE;
        mProgress = (int) Math.ceil((val % NO_OF_SEARCHES_FOR_A_TREE) * degreesPerSearch);
        boolean isTablet = getResources().getBoolean(org.chromium.chrome.browser.omnibox.R.bool.tabletScreenSize);
        int defaultStart = isTablet ? 6 : INITIAL_PROGRESS;
        mProgress = mProgress > 0 ? mProgress : defaultStart;
        mTreeCycles = val / NO_OF_SEARCHES_FOR_A_TREE;
        invalidate();
    }

    protected int getNoOfTrees() {
        return mTreeCycles;
    }
}
