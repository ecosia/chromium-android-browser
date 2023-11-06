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

public abstract class TestPagesElemhideEmuInvTestBase {
    public static final String ELEMENT_HIDING_EMULATION_TESTPAGES_URL =
            TestPagesHelperBase.FILTER_TESTPAGES_TESTCASES_ROOT
            + "element-hiding-emulation-inversion";
    private TestPagesHelperBase mHelper;

    protected void setUp(TestPagesHelperBase helper) {
        mHelper = helper;
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideEmuNotAbpProperties() throws Exception {
        mHelper.addCustomFilter(
                String.format("%s#?#.ehei-properties:not(:-abp-properties(width: 238px))",
                        TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_EMULATION_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div[id='basic-not-abp-properties-usage-fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideEmuNotAbpHas() throws Exception {
        mHelper.addCustomFilter(
                String.format("%s#?#.ehei-has:not(:-abp-has(span.ehei-has-not-hide))",
                        TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_EMULATION_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div[id='basic-not-abp-has-usage-fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideEmuNotAbpContains() throws Exception {
        mHelper.addCustomFilter(
                String.format("%s#?#.ehei-contains:not(span:-abp-contains(example-content))",
                        TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_EMULATION_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "span[id='basic-not-abp-contains-usage-fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideEmuNotChained() throws Exception {
        mHelper.addCustomFilter(String.format(
                "%s#?#.ehei-chained-parent:not(:-abp-has(> div:-abp-properties(width: 198px)))",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_EMULATION_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div[id='chained-extended-selectors-with-not-selector-fail-1']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideEmuNotCaseIsensitive() throws Exception {
        mHelper.addCustomFilter(String.format("%s#?#.ehei-case:not(:-abp-properties(WiDtH: 209px))",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_EMULATION_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div[id='case-insensitive-extended-selectors-with-not-selector-fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideEmuNotWildcard() throws Exception {
        mHelper.addCustomFilter(String.format("%s#?#.ehei-wildcard:not(:-abp-properties(cursor:*))",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_EMULATION_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(mHelper, 1,
                "div[id='wildcard-in-extended-selector-combined-with-not-selector-fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideEmuNotRegexAbpProperties() throws Exception {
        mHelper.addCustomFilter(
                String.format("%s#?#.ehei-regex:not(:-abp-properties(/width: 11[1-5]px;/))",
                        TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_EMULATION_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div[id='regular-expression-in-not-abp-properties-fail-1']");
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div[id='regular-expression-in-not-abp-properties-fail-2']");
        TestVerificationUtils.verifyDisplayedCount(mHelper, 1, ".ehei-regex3");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testElemHideEmuNotRegexAbpContains() throws Exception {
        mHelper.addCustomFilter(String.format(
                "%s#?#.ehei-contains-regex:not(span:-abp-contains(/example-contentregex\\d/))",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrl(ELEMENT_HIDING_EMULATION_TESTPAGES_URL);
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "span[id='regular-expression-in-not-abp-contains-fail-1']");
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "span[id='regular-expression-in-not-abp-contains-fail-2']");
        TestVerificationUtils.verifyDisplayedCount(mHelper, 2, ".ehei-contains-regex");
    }
}
