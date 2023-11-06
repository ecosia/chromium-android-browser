/*
 * This file is part of eyeo Chromium SDK,
 * Copyright (C) 2006-present eyeo GmbH
 *
 * eyeo Chromium SDK is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * eyeo Chromium SDK is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with eyeo Chromium SDK.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.chromium.components.adblock;

import androidx.test.filters.LargeTest;

import org.junit.Assert;
import org.junit.Test;

import org.chromium.base.test.util.Feature;
import org.chromium.components.adblock.AdblockContentType;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class TestPagesWebsocketTestBase {
    private TestPagesHelperBase mHelper;

    protected void setUp(TestPagesHelperBase helper) {
        mHelper = helper;
        mHelper.addCustomFilter(
                String.format("$websocket,domain=%s", TestPagesHelperBase.TESTPAGES_DOMAIN));
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testVerifyWebsocketFilter() throws Exception {
        final String wssUrl =
                String.format("wss://%s/websocket", TestPagesHelperBase.TESTPAGES_DOMAIN);
        final CountDownLatch countDownLatch =
                mHelper.setOnAdMatchedExpectations(new HashSet<>(List.of(wssUrl)), null);
        mHelper.loadUrl(TestPagesHelperBase.FILTER_TESTPAGES_TESTCASES_ROOT + "websocket");
        // Wait with 10 seconds max timeout
        countDownLatch.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(1, mHelper.numBlockedByType(AdblockContentType.CONTENT_TYPE_WEBSOCKET));
        Assert.assertTrue(mHelper.isBlocked(wssUrl));
    }
}
