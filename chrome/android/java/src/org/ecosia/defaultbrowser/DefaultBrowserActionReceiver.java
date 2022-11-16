package org.ecosia.defaultbrowser;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import org.ecosia.tracking.TrackingManager;


 import static org.chromium.chrome.browser.ui.default_browser_promo.EcosiaDefaultBrowserPromoDialog.DEFAULT_BROWSER_ACCEPT;
 import static org.chromium.chrome.browser.ui.default_browser_promo.EcosiaDefaultBrowserPromoDialog.DEFAULT_BROWSER_DATA;
 import static org.chromium.chrome.browser.ui.default_browser_promo.EcosiaDefaultBrowserPromoDialog.DEFAULT_BROWSER_DISMISS;

public class DefaultBrowserActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
         String event = intent.getStringExtra(DEFAULT_BROWSER_DATA);
         if (event == null) return;
         switch (event) {
             case DEFAULT_BROWSER_ACCEPT:
                 TrackingManager.getInstance(context.getApplicationContext()).defaultBrowserChangeEvent();
                 break;
             case DEFAULT_BROWSER_DISMISS:
                 TrackingManager.getInstance(context.getApplicationContext()).defaultBrowserDismissEvent();
                 break;
         }
    }
}
