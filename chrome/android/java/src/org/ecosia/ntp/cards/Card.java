package org.ecosia.ntp.cards;

import android.view.View;
import android.widget.RelativeLayout;

public class Card {

    private RelativeLayout mLayout;

    @SuppressWarnings("MissingCasesInEnumSwitch")
    public Card(RelativeLayout layout, CardPosition position) {
        mLayout = layout;

        switch (position) {
            case TOP:
                layout.setPadding(layout.getPaddingLeft(), layout.getPaddingTop() * 2, layout.getPaddingRight(), layout.getPaddingBottom());
                break;

            case BOTTOM:
                layout.setPadding(layout.getPaddingLeft(), layout.getPaddingTop(), layout.getPaddingRight(), layout.getPaddingBottom() * 2);
                break;
        }
    }

    public void initialize(View.OnClickListener clickListener) {
        mLayout.setOnClickListener(clickListener);
    }
}
