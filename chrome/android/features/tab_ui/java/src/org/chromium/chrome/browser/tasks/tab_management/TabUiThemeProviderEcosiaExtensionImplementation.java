package org.chromium.chrome.browser.tasks.tab_management;

import android.content.Context;
import android.content.res.ColorStateList;

import org.chromium.chrome.tab_ui.R;

public class TabUiThemeProviderEcosiaExtensionImplementation implements TabUiThemeProviderEcosiaExtension {

    private static TabUiThemeProviderEcosiaExtension sInstance;

    private final Context mContext;

    public static TabUiThemeProviderEcosiaExtension getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new TabUiThemeProviderEcosiaExtensionImplementation(context);
        }
        return sInstance;
    }

    private TabUiThemeProviderEcosiaExtensionImplementation(final Context context) {
        mContext = context;
    }

    @Override
    public int getTitleTextColor(final boolean isSelected, final boolean isIncognito) {
        if (isIncognito) {
            return isSelected ? mContext.getColor(R.color.ecosia_tabs_content_selected_dark) : mContext.getColor(R.color.ecosia_tabs_content_unselected_dark);
        }
        return isSelected ? mContext.getColor(R.color.ecosia_tabs_content_selected) : mContext.getColor(R.color.ecosia_tabs_content_unselected);
    }

    @Override
    public int getCardViewBackgroundColor(boolean isSelected, final boolean isIncognito) {
        if (isIncognito) {
            return isSelected ? mContext.getColor(R.color.ecosia_tabs_selected_dark) : mContext.getColor(R.color.ecosia_tabs_unselected_dark);
        }
        return isSelected ? mContext.getColor(R.color.ecosia_tabs_selected) : mContext.getColor(R.color.ecosia_tabs_unselected);
    }

    @Override
    public int getTabGroupNumberTextColor(boolean isSelected, final boolean isIncognito) {
        if (isIncognito) {
            return isSelected ? mContext.getColor(R.color.ecosia_tabs_group_selected_dark) : mContext.getColor(R.color.ecosia_tabs_group_unselected_dark);
        }
        return isSelected ? mContext.getColor(R.color.ecosia_tabs_group_selected) : mContext.getColor(R.color.ecosia_tabs_group_unselected);
    }

    @Override
    public ColorStateList getActionButtonTintList(final boolean isIncognito) {
        return createDefaultColorStateList(isIncognito);
    }

    private ColorStateList createDefaultColorStateList(final boolean isIncognito) {
        final int selected = isIncognito ? mContext.getColor(R.color.ecosia_tabs_content_selected_dark) : mContext.getColor(R.color.ecosia_tabs_content_selected);
        final int unselected = isIncognito ? mContext.getColor(R.color.ecosia_tabs_content_unselected_dark) : mContext.getColor(R.color.ecosia_tabs_content_unselected);
        return createColorStateList(selected, unselected);
    }

    private ColorStateList createColorStateList(final int selected, final int unselected) {
        final int[][] states = new int[][] {
                new int[] { android.R.attr.state_enabled}, // enabled
                new int[] {-android.R.attr.state_enabled}, // disabled
                new int[] {-android.R.attr.state_checked}, // unchecked
                new int[] { android.R.attr.state_pressed}  // pressed
        };

        final int[] colors = new int[] {
                selected,
                unselected,
                unselected,
                selected
        };

        return new ColorStateList(states, colors);
    }

    @Override
    public ColorStateList getToggleActionButtonCheckedDrawableTintList(final boolean isIncognito) {
        final int checkmarkColor = isIncognito ? mContext.getColor(R.color.ecosia_tabs_group_selected_checkmark_dark) : mContext.getColor(R.color.ecosia_tabs_group_selected_checkmark);
        return ColorStateList.valueOf(checkmarkColor);
    }
}
