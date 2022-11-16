package org.ecosia.defaultbrowser;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View;

import org.chromium.chrome.browser.app.ChromeActivity;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.components.browser_ui.widget.PromoDialog;
import org.chromium.components.embedder_support.util.UrlUtilities;
import org.chromium.url.GURL;
import org.ecosia.utils.SharedPreferencesHelpers;

public class WhatsNewPopup extends PromoDialog {

    private final static String PREF_WHATS_NEW_POPUP = "PREF_WHATS_NEW_POPUP";

    public WhatsNewPopup(Activity activity) {
        super(activity);
        setDialogHeightToWrapContent();
    }

    @Override
    protected DialogParams getDialogParams() {
        PromoDialog.DialogParams params = new PromoDialog.DialogParams();
        params.drawableResource = org.chromium.chrome.R.drawable.whats_new;
        params.headerStringResource = org.chromium.chrome.R.string.whats_new_title;
        params.primaryButtonStringResource = org.chromium.chrome.R.string.discover_btn;
        params.subheaderTitle = getString(org.chromium.chrome.R.string.collective_action);
        params.subheaderCharSequence = getString(org.chromium.chrome.R.string.climate_impact) ;
        params.isPrimaryCheckShown = true;
        params.primaryDescIcon = org.chromium.chrome.browser.ui.default_browser_promo.R.drawable.tree;
        /* Customizable home page is disabled until its implementation on NTP
        params.subheader2Title = getString(org.chromium.chrome.R.string.custom_homepage);
        params.subheaderCharSequence2 = getString(org.chromium.chrome.R.string.custom_homepage_desc);
        params.isSecondaryCheckShown = true;
        params.secondaryDescIcon = org.chromium.chrome.browser.ui.default_browser_promo.R.drawable.custom_homepage;*/
        params.themePrimaryBtn = org.chromium.chrome.R.style.EcosiaButtonStyle;
        return params;
    }

    private String getString (int res) {
        Activity activity = getOwnerActivity();
        assert activity != null;
        return activity.getString(res);
    }

    @Override
    public void onClick(View view) {
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        //No dismiss button required
    }

    private static boolean shouldLaunchWhatsNewPopup(Activity parentActivity) {
        if (!(parentActivity instanceof ChromeActivity)) {
            return false;
        }

        final ChromeActivity activity = (ChromeActivity) parentActivity;
        final Tab activityTab = activity.getActivityTab();
        final GURL gUrl = activityTab.getUrl();
        if (!UrlUtilities.isNTPUrl(gUrl)) {
            return false;
        }

        final boolean wasAlreadyShown = SharedPreferencesHelpers.getBoolean(
                parentActivity.getApplicationContext(),
                PREF_WHATS_NEW_POPUP,
                false);

        return !wasAlreadyShown;
    }

    public static boolean launchWhatsNewPopup(Activity parentActivity) {
        if (!shouldLaunchWhatsNewPopup(parentActivity))
            return false;
        WhatsNewPopup popup = new WhatsNewPopup(parentActivity);
        popup.setOnDismissListener(popup);
        popup.show();

        setPrefWhatsNewPopup(parentActivity);
        return true;
    }

    public static void setPrefWhatsNewPopup(Activity parentActivity) {
        SharedPreferencesHelpers.putBoolean(
                parentActivity.getApplicationContext(),
                PREF_WHATS_NEW_POPUP,
                true);
    }
}
