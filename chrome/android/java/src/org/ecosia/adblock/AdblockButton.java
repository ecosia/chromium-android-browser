package org.ecosia.adblock;

import android.content.Context;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;

import org.chromium.ui.widget.ChromeImageButton;

public class AdblockButton extends ChromeImageButton {

    public AdblockButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        final int adblockButtonIcon = org.chromium.chrome.R.drawable.adblocker_button;
        setImageDrawable(ContextCompat.getDrawable(context, adblockButtonIcon));
    }
}
