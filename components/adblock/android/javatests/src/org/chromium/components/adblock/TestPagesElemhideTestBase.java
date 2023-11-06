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

import org.junit.Test;

import org.chromium.base.test.util.Feature;

public abstract class TestPagesElemhideTestBase {
    public static final String ELEMENT_HIDING_TESTPAGES_URL =
            TestPagesHelperBase.FILTER_TESTPAGES_TESTCASES_ROOT + "element-hiding";
    public static final String ELEMENT_HIDING_EXCEPTIONS_TESTPAGES_URL =
            TestPagesHelperBase.EXCEPTION_TESTPAGES_TESTCASES_ROOT + "elemhide";
    private TestPagesHelperBase mHelper;

    protected void setUp(TestPagesHelperBase helper) {
        mHelper = helper;
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideFiltersIdSelector() throws Exception {
        mHelper.addCustomFilter(String.format("%s###eh-id", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(mHelper, 1, "div[id='eh-id']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideFiltersIdSelectorDoubleCurlyBraces() throws Exception {
        mHelper.addCustomFilter(
                String.format("%s##div[id='{{eh-id}}']", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(mHelper, 1, "div[id='{{eh-id}}']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideFiltersClassSelector() throws Exception {
        mHelper.addCustomFilter(
                String.format("%s##.eh-class", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(mHelper, 1, "div[class='eh-class']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideFiltersDescendantSelector() throws Exception {
        mHelper.addCustomFilter(String.format(
                "%s##.testcase-area > .eh-descendant", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(mHelper, 1, "div[class='eh-descendant']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideFiltersSiblingSelector() throws Exception {
        mHelper.addCustomFilter(String.format("%s##.testcase-examplecontent + .eh-sibling",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(mHelper, 1, "div[class='eh-sibling']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideFiltersAttributeSelector1() throws Exception {
        mHelper.addCustomFilter(String.format(
                "%s##div[height=\"100\"][width=\"100\"]", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div[id='attribute-selector-1-fail-1']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideFiltersAttributeSelector2() throws Exception {
        mHelper.addCustomFilter(String.format("%s##div[href=\"http://testcase-attribute.com/\"]",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div[id='attribute-selector-2-fail-1']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideFiltersAttributeSelector3() throws Exception {
        mHelper.addCustomFilter(String.format(
                "%s##div[style=\"width: 200px;\"]", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div[id='attribute-selector-3-fail-1']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideFiltersStartsWithSelector1() throws Exception {
        mHelper.addCustomFilter(String.format("%s##div[href^=\"http://testcase-startswith.com/\"]",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div[id='starts-with-selector-1-fail-1']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideFiltersStartsWithSelector2() throws Exception {
        mHelper.addCustomFilter(String.format(
                "%s##div[style^=\"width: 201px;\"]", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div[id='starts-with-selector-2-fail-1']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideFiltersEndsWithSelector1() throws Exception {
        mHelper.addCustomFilter(String.format(
                "%s##div[style$=\"width: 202px;\"]", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div[id='ends-with-selector-1-fail-1']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideFiltersContains() throws Exception {
        mHelper.addCustomFilter(String.format(
                "%s##div[style*=\"width: 203px;\"]", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(mHelper, 1, "div[id='contains-fail-1']");
    }

    // Exceptions:
    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideFiltersBasicException() throws Exception {
        mHelper.addCustomFilter(
                String.format("%s##.ex-elemhide", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.addCustomFilter(String.format(
                "||%s/testfiles/elemhide/basic/*", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_EXCEPTIONS_TESTPAGES_URL);
        // No exceptions added yet, both objects should be blocked.
        TestVerificationUtils.verifyHiddenCount(mHelper, 1, "img[id='basic-usage-fail-1']");
        TestVerificationUtils.verifyHiddenCount(mHelper, 1, "div[id='basic-usage-pass-1']");
        // Add exception filter and reload.
        mHelper.addCustomFilter(String.format(
                "@@%s/en/exceptions/elemhide$elemhide", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_EXCEPTIONS_TESTPAGES_URL);
        // Image should remain blocked, div should be unblocked.
        TestVerificationUtils.verifyHiddenCount(mHelper, 1, "img[id='basic-usage-fail-1']");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 1, "div[id='basic-usage-area'] > div[id='basic-usage-pass-1']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideFiltersIframeException() throws Exception {
        mHelper.addCustomFilter(
                String.format("%s##.targ-elemhide", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.addCustomFilter(String.format(
                "||%s/testfiles/elemhide/iframe/*.png", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_EXCEPTIONS_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(mHelper, 1, "img[id='iframe-fail-1']");
        TestVerificationUtils.verifyHiddenCount(mHelper, 1, "div[id='iframe-pass-1']");

        // Add exception filter and reload.
        mHelper.addCustomFilter(String.format(
                "@@%s/en/exceptions/elemhide$elemhide", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_EXCEPTIONS_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(mHelper, 1, "img[id='iframe-fail-1']");
        TestVerificationUtils.verifyDisplayedCount(mHelper, 1, "div[id='iframe-pass-1']");
    }
}
