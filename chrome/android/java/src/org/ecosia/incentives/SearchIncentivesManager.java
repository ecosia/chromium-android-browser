package org.ecosia.incentives;

import android.content.Context;
import android.os.LocaleList;

import org.ecosia.unleash.Toggle;
import org.ecosia.unleash.Unleash;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SearchIncentivesManager {

    private static final String LANGUAGE_REGION_HEADER_KEY = "x-ecosia-app-language-region";
    private static final String LANGUAGE_REGION_DEFAULT = "en-us";

    private static volatile SearchIncentivesManager sInstance;
    private Map<String, String> mLanguageRegionHeader;
    private final Unleash mUnleash;

    private SearchIncentivesManager(final Context context) {
        initializeLanguageRegionHeader(context);
        mUnleash = Unleash.getInstance(context);
    }

    public static SearchIncentivesManager getInstance(final Context context) {
        if (sInstance == null) {
            synchronized (SearchIncentivesManager.class) {
                if (sInstance == null) {
                    sInstance = new SearchIncentivesManager(context);
                }
            }
        }
        return sInstance;
    }

    private void initializeLanguageRegionHeader(final Context context) {
        mLanguageRegionHeader = new HashMap<>();
        mLanguageRegionHeader.put(LANGUAGE_REGION_HEADER_KEY, getCurrentLanguageRegion(context));
    }

    private String getCurrentLanguageRegion(Context context) {
        LocaleList locales = context.getResources().getConfiguration().getLocales();
        if (!locales.isEmpty()) {
            Locale locale = locales.get(0);
            return formatLocale(locale);
        }
        return LANGUAGE_REGION_DEFAULT;
    }

    private String formatLocale(Locale locale) {
        return locale.getLanguage() + "-" + locale.getCountry().toLowerCase();
    }

    public Map<String, String> getLanguageRegionHeader() {
        return mLanguageRegionHeader;
    }

    public boolean isRestricted() {
        return mUnleash.isEnabled(Toggle.Name.INCENTIVE_RESTRICTED_SEARCH);
    }
}
