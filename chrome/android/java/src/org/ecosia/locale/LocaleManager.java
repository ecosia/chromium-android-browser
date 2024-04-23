package org.ecosia.locale;

import android.content.Context;
import android.os.LocaleList;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LocaleManager {

    private static final String LANGUAGE_REGION_HEADER_KEY = "x-ecosia-app-language-region";
    private static final String LANGUAGE_REGION_DEFAULT = "en-us";

    private static volatile LocaleManager sInstance;
    private Map<String, String> mLanguageRegionHeader;

    private LocaleManager(final Context context) {
        initializeLanguageRegionHeader(context);
    }

    public static LocaleManager getInstance(final Context context) {
        if (sInstance == null) {
            synchronized (LocaleManager.class) {
                if (sInstance == null) {
                    sInstance = new LocaleManager(context);
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
}
