// Copyright 2020 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.components.omnibox;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import org.chromium.components.security_state.ConnectionSecurityLevel;

/** Utility class to get security state info for the omnibox. */
public class SecurityStatusIcon {
    /**
     * @return the id of the resource identifying the icon corresponding to the securityLevel.
     */
    @DrawableRes
    public static int getSecurityIconResource(@ConnectionSecurityLevel int securityLevel,
            boolean isSmallDevice, boolean skipIconForNeutralState,
            boolean useUpdatedConnectionSecurityIndicators) {
        switch (securityLevel) {
            case ConnectionSecurityLevel.NONE:
                if (isSmallDevice && skipIconForNeutralState) return 0;
                // Ecosia: fix info button bug on tablets (MOB-1874)
                // return R.drawable.omnibox_info;
                return 0;
            case ConnectionSecurityLevel.WARNING:
            case ConnectionSecurityLevel.DANGEROUS:
                return R.drawable.omnibox_not_secure_warning;
            case ConnectionSecurityLevel.SECURE_WITH_POLICY_INSTALLED_CERT:
            case ConnectionSecurityLevel.SECURE:
                /* Ecosia https://ecosia.atlassian.net/browse/MOB-1571
                return useUpdatedConnectionSecurityIndicators ? R.drawable.omnibox_https_valid_arrow
                                                              : R.drawable.omnibox_https_valid;
                 */
                return R.drawable.ic_ecosia_secure_connection;
            default:
                assert false;
        }
        return 0;
    }

    /**
     * @return The resource ID of the content description for the security icon.
     */
    @StringRes
    public static int getSecurityIconContentDescriptionResourceId(
            @ConnectionSecurityLevel int securityLevel) {
        switch (securityLevel) {
            case ConnectionSecurityLevel.NONE:
            case ConnectionSecurityLevel.WARNING:
                return R.string.accessibility_security_btn_warn;
            case ConnectionSecurityLevel.DANGEROUS:
                return R.string.accessibility_security_btn_dangerous;
            case ConnectionSecurityLevel.SECURE_WITH_POLICY_INSTALLED_CERT:
            case ConnectionSecurityLevel.SECURE:
                return R.string.accessibility_security_btn_secure;
            default:
                assert false;
        }
        return 0;
    }
}
