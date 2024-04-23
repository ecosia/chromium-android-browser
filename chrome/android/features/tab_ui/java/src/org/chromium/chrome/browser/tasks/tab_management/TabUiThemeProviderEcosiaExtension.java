package org.chromium.chrome.browser.tasks.tab_management;

import android.content.res.ColorStateList;

public interface TabUiThemeProviderEcosiaExtension {

    public int getCardViewBackgroundColor(final boolean isSelected, final boolean isIncognito);

    public int getTitleTextColor(final boolean isSelected, final boolean isIncognito);

    /**
     * This is used for
     *  a) The X for closing a tab
     *  b) The outline for an unselected tab
     *  c) The marker of a selected tab
     */
    public ColorStateList getActionButtonTintList(final boolean isIncognito);

    /**
     * This is the checkmark of a selected tab
     */
    public ColorStateList getToggleActionButtonCheckedDrawableTintList(final boolean isIncognito);

    public int getTabGroupNumberTextColor(final boolean isSelected, final boolean isIncognito);

}
