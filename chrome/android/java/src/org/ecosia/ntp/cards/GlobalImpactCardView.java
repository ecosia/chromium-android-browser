package org.ecosia.ntp.cards;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import org.chromium.chrome.R;

public class GlobalImpactCardView extends CardView {
    private ImageView imageView;
    private TextView topTextView;
    private TextView bottomTextView;

    public GlobalImpactCardView(@NonNull Context context) {
        super(context);
        init();
    }

    public GlobalImpactCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GlobalImpactCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.ecosia_ntp_global_impact_card, this);

        imageView = findViewById(R.id.image_view);
        topTextView = findViewById(R.id.global_impact_top_text_view);
        bottomTextView = findViewById(R.id.global_impact_bottom_text_view);
    }

    public void setImage(int resId) {
        imageView.setImageResource(resId);
    }

    public void setTopText(String text) {
        topTextView.setText(text);
    }

    public void setBottomText(String text) {
        bottomTextView.setText(text);
    }
}
