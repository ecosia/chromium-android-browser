package org.ecosia.utils;

import android.net.Uri;

public class UrlHelpers {

    private static final String[] ECOSIA_DOMAINS = {
            "ecosia.org",
            "ecosia-dev.xyz",
            "ecosia-staging.xyz"
    };
    private static final String ECOSIA_SEARCH_PATH = "/search";

    public static boolean isEcosiaSerp(final String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        Uri parsedUrl = Uri.parse(url);
        String authority = parsedUrl.getAuthority();

        if (authority != null && isEcosiaDomain(authority)) {
            String path = parsedUrl.getPath();
            return path != null && path.startsWith(ECOSIA_SEARCH_PATH);
        }

        return false;
    }

    private static boolean isEcosiaDomain(String authority) {
        for (String domain : ECOSIA_DOMAINS) {
            if (authority.endsWith(domain)) {
                return true;
            }
        }
        return false;
    }
}
