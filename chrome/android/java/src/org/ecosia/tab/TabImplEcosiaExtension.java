package org.ecosia.tab;

import android.content.Context;

import org.chromium.base.ContextUtils;
import org.chromium.url.GURL;
import org.ecosia.cookies.ECFGCookie;
import org.ecosia.cookies.EcosiaCookieObserver;
import org.ecosia.mmp.Singular;

public class TabImplEcosiaExtension {

    private static final String ECOSIA_DOMAIN = "ecosia.org";
    private static final String ECOSIA_SEARCH_PATH = "search";

    public static void invokeTrackingOnEcosiaDomain(final GURL url) {
        final String host = url.getHost();
        final String path = url.getPath();

        if (!host.contains(ECOSIA_DOMAIN) || !path.contains(ECOSIA_SEARCH_PATH)) {
            return;
        }

        final ECFGCookie cookie = EcosiaCookieObserver.getInstance().getEcfgCookie();
        final int searchCount = cookie.getPersonalCounter();

        if (searchCount > 10) {
            return;
        }

        final Context applicationContext = ContextUtils.getApplicationContext();
        final Singular singular = Singular.getInstance(applicationContext);

        switch (searchCount) {
            case 1: {
                singular.trackEvent(Singular.Events.SEARCH_1);
                break;
            }
            case 5: {
                singular.trackEvent(Singular.Events.SEARCH_5);
                break;
            }
            case 10: {
                singular.trackEvent(Singular.Events.SEARCH_10);
                break;
            }
        }
    }

}
