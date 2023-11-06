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

public abstract class TestPagesHeaderFilterTestBase {
    public static final String HEADER_TESTPAGES_URL =
            TestPagesHelperBase.FILTER_TESTPAGES_TESTCASES_ROOT + "header";
    public static final String HEADER_EXCEPTIONS_TESTPAGES_URL =
            TestPagesHelperBase.EXCEPTION_TESTPAGES_TESTCASES_ROOT + "header";
    private TestPagesHelperBase mHelper;

    protected void setUp(TestPagesHelperBase helper) {
        mHelper = helper;
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHeaderFilterScript() throws Exception {
        mHelper.addCustomFilter(
                String.format("||%s/testfiles/header/$header=content-type=application/javascript",
                        TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(HEADER_TESTPAGES_URL);
        Assert.assertEquals(1, mHelper.numBlocked());
        Assert.assertEquals(1, mHelper.numBlockedByType(AdblockContentType.CONTENT_TYPE_SCRIPT));
        Assert.assertTrue(mHelper.isBlocked(String.format(
                "https://%s/testfiles/header/script.js", TestPagesHelperBase.TESTPAGES_DOMAIN)));
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 0, "div#functionproperty-target > div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHeaderFilterImage() throws Exception {
        mHelper.addCustomFilter(
                String.format("||%s/testfiles/header/image.png$header=content-type=image/png",
                        TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(HEADER_TESTPAGES_URL);
        Assert.assertEquals(1, mHelper.numBlocked());
        Assert.assertEquals(1, mHelper.numBlockedByType(AdblockContentType.CONTENT_TYPE_IMAGE));
        Assert.assertTrue(mHelper.isBlocked(String.format(
                "https://%s/testfiles/header/image.png", TestPagesHelperBase.TESTPAGES_DOMAIN)));
        TestVerificationUtils.verifyDisplayedCount(mHelper, 0, "img[id='image-fail-1']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHeaderFilterImageAndComma() throws Exception {
        mHelper.addCustomFilter(String.format("||%s/testfiles/header/image2.png$header=date=\\x2c",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(HEADER_TESTPAGES_URL);
        Assert.assertEquals(1, mHelper.numBlocked());
        Assert.assertEquals(1, mHelper.numBlockedByType(AdblockContentType.CONTENT_TYPE_IMAGE));
        Assert.assertTrue(mHelper.isBlocked(String.format(
                "https://%s/testfiles/header/image2.png", TestPagesHelperBase.TESTPAGES_DOMAIN)));
        TestVerificationUtils.verifyDisplayedCount(mHelper, 0, "img[id='comma-fail-1']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHeaderFilterStylesheet() throws Exception {
        mHelper.addCustomFilter(String.format("||%s/testfiles/header/$header=content-type=text/css",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(HEADER_TESTPAGES_URL);
        Assert.assertEquals(1, mHelper.numBlocked());
        Assert.assertEquals(
                1, mHelper.numBlockedByType(AdblockContentType.CONTENT_TYPE_STYLESHEET));
        Assert.assertTrue(
                mHelper.isBlocked(String.format("https://%s/testfiles/header/stylesheet.css",
                        TestPagesHelperBase.TESTPAGES_DOMAIN)));
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHeaderFilterException() throws Exception {
        // Add blocking filter, expect blocked image
        mHelper.addCustomFilter(
                String.format("||%s/testfiles/header_exception/$header=content-type=image/png",
                        TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(HEADER_EXCEPTIONS_TESTPAGES_URL);
        Assert.assertEquals(1, mHelper.numBlocked());
        Assert.assertEquals(1, mHelper.numBlockedByType(AdblockContentType.CONTENT_TYPE_IMAGE));
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 0, "img[id='image-header-exception-pass-1']");

        // Add exception filter, expect image allowed
        mHelper.addCustomFilter(String.format(
                "@@%s/testfiles/header_exception/$header", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(HEADER_EXCEPTIONS_TESTPAGES_URL);
        Assert.assertEquals(1, mHelper.numAllowed());
        Assert.assertEquals(1, mHelper.numAllowedByType(AdblockContentType.CONTENT_TYPE_IMAGE));
        Assert.assertTrue(
                mHelper.isAllowed(String.format("https://%s/testfiles/header_exception/image.png",
                        TestPagesHelperBase.TESTPAGES_DOMAIN)));
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 1, "img[id='image-header-exception-pass-1']");
    }
}
