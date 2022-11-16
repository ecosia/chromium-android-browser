package org.ecosia.utils;

import android.content.Context;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;
import android.util.AttributeSet;
import android.widget.TextView;

public class LongTitleSwitchPreference extends SwitchPreferenceCompat {

    public LongTitleSwitchPreference(final Context context, final AttributeSet attrs,
                                     final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LongTitleSwitchPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        TextView title = (TextView) holder.findViewById(android.R.id.title);
        title.setSingleLine(false);
    }
}
