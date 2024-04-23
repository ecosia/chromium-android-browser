/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.ecosia.cookies;

import static org.ecosia.utils.SharedPreferencesHelpers.PREF_ECCC_VALUE;

import org.chromium.base.ContextUtils;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.components.content_settings.CookieControlsBridge;
import org.chromium.components.content_settings.CookieControlsObserver;
import org.chromium.content_public.browser.WebContents;
import org.ecosia.utils.SharedPreferencesHelpers;
import org.chromium.base.Log;

public class EcosiaCookieObserver implements CookieControlsObserver {
    private static final String ECOSIA_DOMAIN_END = "ecosia.org";
    private static final String PREF_ECFG_VALUE =  "com.ecosia.PREF_ECFG_VALUE";
    private static final String TAG = EcosiaCookieObserver.class.getSimpleName();

    private static volatile EcosiaCookieObserver sInstance;
    private CookieControlsBridge mBridge;
    private String mRawEcfgCookie;
    private String mRawEcccCookie;

    private EcosiaCookieObserver() {
        mRawEcfgCookie = SharedPreferencesHelpers.getString(ContextUtils.getApplicationContext(), PREF_ECFG_VALUE, null);
        mRawEcccCookie = SharedPreferencesHelpers.getString(ContextUtils.getApplicationContext(), PREF_ECCC_VALUE, null);
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

            // do not read cookie for Incognito Profile
            Profile profile = Profile.fromWebContents(webContents);
            if (profile.isOffTheRecord()) {
                Log.d(TAG, "Incognito Tab: Don't fetch cookies");
                return;
            }

            // only initialise cookie bridge once and rely on observer pattern
            if (mBridge == null) {
                mBridge = new CookieControlsBridge(EcosiaCookieObserver.getInstance(), webContents, null);
                mBridge.startObservingEcosiaCookies();
            }
        }
    }

    public ECFGCookie getEcfgCookie() {
        return new ECFGCookie(mRawEcfgCookie);
    }

    public ECCCCookie getEcccCookie() {
        return new ECCCCookie(mRawEcccCookie);
    }

    @Override
    public void onEcosiaCookiesChanged(String name, String value) {
        Log.d(TAG, "Cookie name: " + name);
        Log.d(TAG, "Cookie value: " + value);

        if (name.equals(ECFGCookie.NAME)) {
            mRawEcfgCookie = value;
            SharedPreferencesHelpers.putString(ContextUtils.getApplicationContext(), PREF_ECFG_VALUE, value);
        } else if (name.equals(ECCCCookie.NAME)) {
            mRawEcccCookie = value;
            SharedPreferencesHelpers.putString(ContextUtils.getApplicationContext(), PREF_ECCC_VALUE, value);
        }
    }
}
