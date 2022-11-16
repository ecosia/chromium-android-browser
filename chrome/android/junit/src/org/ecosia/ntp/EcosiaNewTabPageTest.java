/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */


package org.ecosia.ntp;

import org.chromium.base.test.BaseRobolectricTestRunner;
import org.ecosia.ntp.EcosiaNewTabPage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.chromium.chrome.browser.tab.Tab;
import org.robolectric.annotation.Config;


/**
 * Tests for the native android New Tab Page.
 *
 * TODO(https://crbug.com/906151): Add new goldens and enable ExploreSites.
 */
@RunWith(BaseRobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class EcosiaNewTabPageTest {

    private Tab mTab;
    private EcosiaNewTabPage mNtp;

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testIfTabExists() {
        Assert.assertEquals(null, mTab);
    }

}
