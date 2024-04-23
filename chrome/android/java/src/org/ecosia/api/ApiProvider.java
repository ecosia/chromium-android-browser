package org.ecosia.api;

import android.content.res.Resources;

import org.chromium.base.version_info.VersionInfo;

public class ApiProvider {

    // Base URLs
    private static final String ECOSIA_PROD_BASE_URL = "https://api.ecosia.org";
    private static final String ECOSIA_STAGING_BASE_URL = "https://api.ecosia-staging.xyz";

    // API Paths
    private static final String UNLEASH_API_PATH = "/v2/toggles";
    private static final String REFERRALS_API_PATH = "/v1/referrals";

    public enum UseCase {
        UNLEASH, REFERRALS
    }

    public enum Environment {
        PROD("prod"), STAGING("staging");

        private String mName;
        private Environment(String name) {
            mName = name;
        }

        public String toString() {
            return mName;
        }
    }

    private static String getApiBaseUrl() {
        switch (getEnvironment()) {
            case PROD:
                return ECOSIA_PROD_BASE_URL;
            case STAGING:
                return ECOSIA_STAGING_BASE_URL;
        }
        throw new RuntimeException("Unsupported Environment");
    }

    private static String getApiPath(UseCase useCase) {
        switch (useCase) {
            case UNLEASH:
                return UNLEASH_API_PATH;
            case REFERRALS:
                return REFERRALS_API_PATH;
        }
        throw new RuntimeException("Invalid UseCase");
    }

    public static Environment getEnvironment() {
        return VersionInfo.isOfficialBuild() ? Environment.PROD : Environment.STAGING;
    }

    public static String getApiUrl(UseCase useCase) {
        return getApiBaseUrl() + getApiPath(useCase);
    }

}
