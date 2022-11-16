/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.ecosia.cookies;

import org.chromium.base.ContextUtils;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.components.content_settings.CookieControlsBridge;
import org.chromium.components.content_settings.CookieControlsObserver;
import org.chromium.content_public.browser.WebContents;
import org.ecosia.utils.SharedPreferencesHelpers;

public class EcosiaCookieObserver implements CookieControlsObserver {
    private static final String ECOSIA_DOMAIN_END = "ecosia.org";
    private static final String ECOSIA_SEARCH_PATH = "/search";
    private static final String PREF_ECFG_VALUE =  "com.ecosia.PREF_ECFG_VALUE";

    private static volatile EcosiaCookieObserver sInstance;
    private CookieControlsBridge mBridge;
    private String mRawCookie;

    private EcosiaCookieObserver() {
        mRawCookie = SharedPreferencesHelpers.getString(ContextUtils.getApplicationContext(), PREF_ECFG_VALUE, null);
    }

    public static EcosiaCookieObserver getInstance() {
        if (sInstance == null) {
            synchronized (EcosiaCookieObserver.class) {
                if (sInstance == null) {
                    sInstance = new EcosiaCookieObserver();
                }
            }
        }
        return sInstance;
    }

    public void observeWebContents(WebContents webContents) {
        String host = webContents.getVisibleUrl().getHost();
        String path = webContents.getVisibleUrl().getPath();

        if (host != null && host.endsWith(ECOSIA_DOMAIN_END)) {
            if (path != null && path.startsWith(ECOSIA_SEARCH_PATH)) {

                // do not read cookie for Incognito Profile
                Profile profile = Profile.fromWebContents(webContents);
                if (profile.isOffTheRecord()) {
                    return;
                }

                // deinit old bridge
                if (mBridge != null) {
                    mBridge.onUiClosing();
                    mBridge.destroy();
                    mBridge = null;
                }

                // create new bridge to obtain updated value
                mBridge = new CookieControlsBridge(EcosiaCookieObserver.getInstance(), webContents, null);
            }
        }
    }

    public ECFGCookie getCookie() {
        return new ECFGCookie(mRawCookie);
    }

    @Override
    public void onCookieBlockingStatusChanged(int status, int enforcement) {
        // empty
    }

    @Override
    public void onCookiesCountChanged(int allowedCookies, int blockedCookies) {
        // empty
    }

    @Override
    public void onCookiesChanged(String cookie) {
        mRawCookie = cookie;
        SharedPreferencesHelpers.putString(ContextUtils.getApplicationContext(), PREF_ECFG_VALUE, cookie);
    }
}
