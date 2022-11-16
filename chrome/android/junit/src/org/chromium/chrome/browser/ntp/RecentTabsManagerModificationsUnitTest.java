// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.ntp;

import org.chromium.base.test.BaseRobolectricTestRunner;
import org.chromium.chrome.browser.ntp.RecentTabsManager;
import org.chromium.chrome.browser.ui.signin.SyncPromoController.SyncPromoState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;



/**
 * Tests for Ecosia Modifications on the chromium RecentTabsManager class:
 *  org.chromium.chrome.browser.ntp.RecentTabsManager
 */
@RunWith(BaseRobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class RecentTabsManagerModificationsUnitTest {

    private RecentTabsManager mRecentTabsManager;

    @Before
    public void setUp() throws Exception {
        mRecentTabsManager = RecentTabsManager.createTestInstance();
    }

    @Test
    public void testSyncPromoIsDisabled() {
        final int promoState = mRecentTabsManager.getPromoState();
        Assert.assertEquals(SyncPromoState.NO_PROMO, promoState);
    }

}
