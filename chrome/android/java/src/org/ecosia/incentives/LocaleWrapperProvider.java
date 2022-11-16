package org.ecosia.incentives;

import androidx.annotation.VisibleForTesting;

import java.util.Locale;

public class LocaleWrapperProvider {

    private static LocaleWrapper mockedLocaleWrapper;

    @VisibleForTesting
    public static void setMockedLocaleWrapper(LocaleWrapper localeWrapper) {
        mockedLocaleWrapper = localeWrapper;
    }

    public static LocaleWrapper provideLocaleWrapper() {
        if (mockedLocaleWrapper != null) {
            return mockedLocaleWrapper;
        } else {
            return new DefaultLocaleWrapper();
        }
    }

    private static class DefaultLocaleWrapper implements LocaleWrapper {
        @Override
        public String getCountry() {
            return Locale.getDefault().getCountry();
        }
    }
}
