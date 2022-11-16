package org.ecosia.adblock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

import androidx.core.graphics.drawable.DrawableCompat;

import org.chromium.components.adblock.AdblockController;
import org.chromium.ui.util.ColorUtils;
import org.ecosia.tracking.TrackingManager;

public class AdblockPopupWindow {

    private final PopupWindow mPopupWindow;
    private final Context mContext;
    private final View mView;
    private final SwitchCompat mAdblockSwitch;

    @SuppressLint("InflateParams")  // Popup window doesn't have a parent, thus we ignore inflater warning
    public AdblockPopupWindow(Context context) {
        mContext = context;

        LayoutInflater inflater = (LayoutInflater) context.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = inflater.inflate(org.chromium.chrome.R.layout.ecosia_adblock_dialog, null);
        resolveDayNightMode();  // Fix for dark mode bug in drawables
        mPopupWindow = new PopupWindow();
        mPopupWindow.setWidth(RelativeLayout.LayoutParams.WRAP_CONTENT);
        mPopupWindow.setHeight(RelativeLayout.LayoutParams.WRAP_CONTENT);
        mPopupWindow.setContentView(mView);
        mPopupWindow.setFocusable(true);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mPopupWindow.setElevation(8);    // Magic number! Not working with other values :)
        }

        TextView adblockSettingsButton = mView.findViewById(org.chromium.chrome.R.id.adblock_dialog_settings_button);
        adblockSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupWindow.dismiss();
                Intent intent = new Intent(context, AdblockSettingsActivity.class);
                context.startActivity(intent);
                TrackingManager.getInstance(context).displayAdblockPopupWindowEvent();
            }
        });

        mAdblockSwitch = mView.findViewById(org.chromium.chrome.R.id.adblock_dialog_blocker_switch);
        mAdblockSwitch.setChecked(AdblockController.getInstance().isEnabled());
        resolveSwitchDayNightMode();
        mAdblockSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AdblockController.getInstance().setEnabled(isChecked);
                TrackingManager.getInstance(context).changeAdblockEvent(isChecked);
                resolveSwitchDayNightMode();
            }
        });

        final AdblockSwitchTranslucentOverlay adBlockSwitchOverlay = mView.findViewById(org.chromium.chrome.R.id.adblock_dialog_blocker_switch_overlay);
        adBlockSwitchOverlay.setTargetView(mAdblockSwitch);
    }

    public void showAsDropdown(AdblockButton anchorView) {
        mPopupWindow.showAsDropDown(anchorView, 0, -anchorView.getHeight()/2); // Workaround for vertical offset
        View view = mPopupWindow.getContentView();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)view.getLayoutParams();
        int rightMargin = (int) mContext.getResources().getDimension(org.chromium.chrome.R.dimen.adblock_popup_right_margin);
        params.setMargins(0, 0, rightMargin, 0);    // Workaround for adding margin on the right
        view.setLayoutParams(params);
        mAdblockSwitch.setChecked(AdblockController.getInstance().isEnabled());
        TrackingManager.getInstance(mContext).showAdblockPopupDialog();
    }

    // Dark mode for custom drawables doesn't work properly when forcing the change from within the app theme settings
    private void resolveDayNightMode() {
        if (ColorUtils.inNightMode(mContext)) {
            setDarkMode();
        } else {
            setLightMode();
        }
    }

    private void resolveSwitchDayNightMode() {
        int switchOffColor = mContext.getResources().getColor(org.chromium.chrome.R.color.ecosia_switchoff);
        int trackOffColor;
        int switchOnColor;
        int trackOnColor;
        if(ColorUtils.inNightMode(mContext)) {
           switchOnColor = mContext.getResources().getColor(org.chromium.chrome.R.color.ecosia_brand_primary_dark);
           trackOnColor = mContext.getResources().getColor(org.chromium.chrome.R.color.ecosia_button_secondary_default_dark);
            trackOffColor = mContext.getResources().getColor(org.chromium.chrome.R.color.ecosia_track_off_dark);
        } else {
            switchOnColor = mContext.getResources().getColor(org.chromium.chrome.R.color.ecosia_brand_primary_light);
            trackOnColor = mContext.getResources().getColor(org.chromium.chrome.R.color.ecosia_button_secondary_default_light);
            trackOffColor = mContext.getResources().getColor(org.chromium.chrome.R.color.ecosia_track_off_light);
        }
        mAdblockSwitch.getThumbDrawable().setColorFilter(mAdblockSwitch.isChecked() ? switchOnColor : switchOffColor,PorterDuff.Mode.MULTIPLY);
        mAdblockSwitch.getTrackDrawable().setColorFilter(mAdblockSwitch.isChecked() ? trackOnColor : trackOffColor, PorterDuff.Mode.MULTIPLY);
    }

    private void setDarkMode() {
        int backgroundColor = mContext.getResources().getColor(org.chromium.chrome.R.color.ecosia_abp_popup_dark);
        int textColor = mContext.getResources().getColor(org.chromium.chrome.R.color.ecosia_abp_popup_text_dark);
        applyColorMode(backgroundColor, textColor);
    }

    private void setLightMode() {
        int backgroundColor = mContext.getResources().getColor(org.chromium.chrome.R.color.ecosia_abp_popup);
        int textColor = mContext.getResources().getColor(org.chromium.chrome.R.color.ecosia_abp_popup_text);
        applyColorMode(backgroundColor, textColor);
    }

    private void applyColorMode(int backgroundColor, int textColor) {
        final TextView settingsButton = mView.findViewById(org.chromium.chrome.R.id.adblock_dialog_settings_button);
        settingsButton.setTextColor(textColor);

        final SwitchCompat adblockSwitch = mView.findViewById(org.chromium.chrome.R.id.adblock_dialog_blocker_switch);
        adblockSwitch.setTextColor(textColor);

        final LinearLayout adblockMenu = mView.findViewById(org.chromium.chrome.R.id.adblock_dialog);
        final Resources.Theme theme = mContext.getTheme();
        final Resources resources = mContext.getResources();
        final Drawable drawable = resources.getDrawable(org.chromium.chrome.R.drawable.adblock_popup_window, theme);
        adblockMenu.setBackground(drawable);
    }
}
