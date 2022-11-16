package org.ecosia.ntp.cards;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import org.chromium.chrome.R;

public class ImpactCardContainer extends LinearLayout {
    public ImpactCardContainer(Context context) {
        super(context);
        init();
    }

    public ImpactCardContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
    }

    public void addImpactCard(CardView cardView) {
        if (getChildCount() > 0) {
            View separator = createSeparator();
            addView(separator);
        }

        addView(cardView);
    }

    private View createSeparator() {
        View separator = new View(getContext());
        separator.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                (int) getResources().getDimension(R.dimen.card_separator_height)
        ));
        separator.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.ecosia_disabled));
        return separator;
    }
}

