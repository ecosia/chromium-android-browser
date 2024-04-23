package org.ecosia.utils;

import android.content.Context;
import android.net.Uri;

import org.chromium.chrome.browser.app.ChromeActivity;
import org.chromium.content_public.browser.LoadUrlParams;

public class UrlHelpers {

    private static final String[] ECOSIA_DOMAINS = {
            "ecosia.org",
            "ecosia-dev.xyz",
            "ecosia-staging.xyz"
    };

    public static boolean isEcosiaSerp(final String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        Uri parsedUrl = Uri.parse(url);
        String authority = parsedUrl.getAuthority();

        return authority != null && isEcosiaDomain(authority) ;
    }

    private static boolean isEcosiaDomain(String authority) {
        for (String domain : ECOSIA_DOMAINS) {
            if (authority.endsWith(domain)) {
                return true;
            }
        }
        return false;
    }

    public static void openUrl(Context context, String url) {
        if (context instanceof ChromeActivity) {
            LoadUrlParams params = new LoadUrlParams(url);
            ((ChromeActivity<?>) context).getActivityTab().loadUrl(params);
        }
    }
}
