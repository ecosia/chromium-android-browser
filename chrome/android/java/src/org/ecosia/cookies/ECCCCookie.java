package org.ecosia.cookies;

import javax.annotation.Nullable;

public class ECCCCookie {
    public static final String NAME = "ECCC";
    private static final String ANALYTICS_CHAR = "a";
    @Nullable
    private final String mRawCookieValue;

    public ECCCCookie(@Nullable String rawValue) {
        mRawCookieValue = rawValue;
    }

    public boolean isAnalyticsAllowed() {
        return mRawCookieValue != null && mRawCookieValue.contains(ANALYTICS_CHAR);
    }
}
