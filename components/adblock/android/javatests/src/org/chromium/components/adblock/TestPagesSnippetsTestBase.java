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

public abstract class TestPagesSnippetsTestBase {
    private TestPagesHelperBase mHelper;

    protected void setUp(TestPagesHelperBase helper) {
        mHelper = helper;
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testAbortCurrentInlineScriptBasic() throws Exception {
        mHelper.addCustomFilter(String.format("%s#$#abort-current-inline-script console.group",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT
                + "abort-current-inline-script");
        // All "Abort" snippets cancel creation of the target div, so it won't be hidden - it will
        // not exist in DOM. Therefore we verify it's not displayed instead of verifying it's
        // hidden.
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 0, "div#basic-target > div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testAbortCurrentInlineScriptSearch() throws Exception {
        mHelper.addCustomFilter(
                String.format("%s#$#abort-current-inline-script console.info acis-search",
                        TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT
                + "abort-current-inline-script");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 0, "div#search-target > div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testAbortCurrentInlineScriptRegex() throws Exception {
        mHelper.addCustomFilter(
                String.format("%s#$#abort-current-inline-script console.warn '/acis-regex[1-2]/'",
                        TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT
                + "abort-current-inline-script");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 0, "div#regex-target > div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testAbortOnPropertyReadBasic() throws Exception {
        mHelper.addCustomFilter(String.format(
                "%s#$#abort-on-property-read aoprb", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "abort-on-property-read");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 0, "div#basic-target > div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testAbortOnPropertyReadSubProperty() throws Exception {
        mHelper.addCustomFilter(String.format(
                "%s#$#abort-on-property-read aopr.sp", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "abort-on-property-read");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 0, "div#subproperty-target > div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testAbortOnPropertyReadFunctionProperty() throws Exception {
        mHelper.addCustomFilter(String.format(
                "%s#$#abort-on-property-read aoprf.fp", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "abort-on-property-read");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 0, "div#functionproperty-target > div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testAbortOnPropertyWriteBasic() throws Exception {
        mHelper.addCustomFilter(String.format(
                "%s#$#abort-on-property-write window.aopwb", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "abort-on-property-write");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 0, "div#basic-target > div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testAbortOnPropertyWriteSubProperty() throws Exception {
        mHelper.addCustomFilter(String.format("%s#$#abort-on-property-write window.aopwsp",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "abort-on-property-write");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 0, "div#subproperty-target > div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testAbortOnPropertyWriteFunctionProperty() throws Exception {
        mHelper.addCustomFilter(String.format(
                "%s#$#abort-on-property-write aopwf.fp", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "abort-on-property-write");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 0, "div#functionproperty-target > div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testAbortOnIframePropertyReadBasic() throws Exception {
        mHelper.addCustomFilter(String.format(
                "%s#$#abort-on-iframe-property-read aoiprb", TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT
                + "abort-on-iframe-property-read");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 0, "div#basic-target > div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testAbortOnIframePropertyReadSubProperty() throws Exception {
        mHelper.addCustomFilter(String.format("%s#$#abort-on-iframe-property-read aoipr.sp",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT
                + "abort-on-iframe-property-read");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 0, "div#subproperty-target > div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testAbortOnIframePropertyReadMultipleProperties() throws Exception {
        mHelper.addCustomFilter(String.format("%s#$#abort-on-iframe-property-read aoipr1 aoipr2",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT
                + "abort-on-iframe-property-read");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 0, "div#multipleproperties-target > div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testAbortOnIframePropertyWriteBasic() throws Exception {
        mHelper.addCustomFilter(String.format("%s#$#abort-on-iframe-property-write aoipwb",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT
                + "abort-on-iframe-property-write");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 0, "div#basic-target > div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testAbortOnIframePropertyWriteSubProperty() throws Exception {
        mHelper.addCustomFilter(String.format("%s#$#abort-on-iframe-property-write aoipw.sp",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT
                + "abort-on-iframe-property-write");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 0, "div#subproperty-target > div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testAbortOnIframePropertyWriteMultipleProperties() throws Exception {
        mHelper.addCustomFilter(String.format("%s#$#abort-on-iframe-property-write aoipw1 aoipw2",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT
                + "abort-on-iframe-property-write");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 0, "div#multipleproperties-target > div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHideIfContainsStatic() throws Exception {
        mHelper.addCustomFilter(String.format("%s#$#hide-if-contains 'hic-basic-static' p[id]",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "hide-if-contains");
        TestVerificationUtils.verifyHiddenCount(mHelper, 1, "p#hic-static-id");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHideIfContainsDynamic() throws Exception {
        mHelper.addCustomFilter(String.format("%s#$#hide-if-contains 'hic-basic-dynamic' p[id]",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "hide-if-contains");
        TestVerificationUtils.verifyHiddenCount(mHelper, 1, "p#hic-dynamic-id");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHideIfContainsSearch() throws Exception {
        mHelper.addCustomFilter(String.format("%s#$#hide-if-contains 'hic-search' p[id] .target",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "hide-if-contains");
        TestVerificationUtils.verifyHiddenCount(mHelper, 1, "div#search2-target > p.target");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 1, "div#search1-target > p[data-expectedresult='pass']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHideIfContainsRegex() throws Exception {
        mHelper.addCustomFilter(String.format("%s#$#hide-if-contains /hic-regex-[2-3]/ p[id]",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "hide-if-contains");
        // "hic-regex-1" does not match regex, should remain displayed.
        TestVerificationUtils.verifyDisplayedCount(mHelper, 1, "p#hic-regex-1");

        // "hic-regex-2" and "hic-regex-2" do match regex, should be hidden.
        TestVerificationUtils.verifyHiddenCount(mHelper, 1, "p#hic-regex-2");
        TestVerificationUtils.verifyHiddenCount(mHelper, 1, "p#hic-regex-3");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHideIfContainsFrame() throws Exception {
        mHelper.addCustomFilter(String.format("%s#$#hide-if-contains hidden span#frame-target",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "hide-if-contains");
        TestVerificationUtils.verifyHiddenCount(mHelper, 1, "span#frame-target");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHideIfContainsAndMatchesStyleStatic() throws Exception {
        mHelper.addCustomFilter(String.format(
                "%s#$#hide-if-contains-and-matches-style hicamss div[id] span.label /./ 'display:"
                        + " inline;'",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT
                + "hide-if-contains-and-matches-style");
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div#static-usage-area > div[data-expectedresult='fail']");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 1, "div#static-usage-area > div[data-expectedresult='pass']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHideIfContainsAndMatchesStyleDynamic() throws Exception {
        mHelper.addCustomFilter(String.format(
                "%s#$#hide-if-contains-and-matches-style hicamsd div[id] span.label /./ 'display:"
                        + " inline;'",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT
                + "hide-if-contains-and-matches-style");
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div#dynamic-target > div[data-expectedresult='fail']");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 1, "div#dynamic-target > div[data-expectedresult='pass']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHideIfContainsImage() throws Exception {
        mHelper.addCustomFilter(String.format("%s#$#hide-if-contains-image "
                        + "/^89504e470d0a1a0a0000000d4948445200000064000000640802/ "
                        + "div[shouldhide] div",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "hide-if-contains-image");
        TestVerificationUtils.verifyHiddenCount(mHelper, 2, "div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHideIfContainsVisibleTextBasicUsage() throws Exception {
        mHelper.addCustomFilter(String.format(
                "%s#$#hide-if-contains-visible-text Sponsored-hicvt-basic '#parent-basic > "
                        + ".article' '#parent-basic > .article .label'",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT
                + "hide-if-contains-visible-text");
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div#parent-basic > div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHideIfContainsVisibleTextContentUsage() throws Exception {
        mHelper.addCustomFilter(String.format(
                "%s#$#hide-if-contains-visible-text Sponsored-hicvt-content '#parent-content > "
                        + ".article' '#parent-content > .article .label'",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT
                + "hide-if-contains-visible-text");
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div#parent-content > div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHideIfHasAndMatchesStyleBasicUsage() throws Exception {
        mHelper.addCustomFilter(String.format(
                "%s#$#hide-if-has-and-matches-style a[href=\"#basic-target-ad\"] div[id] span"
                        + ".label /./ 'display: inline;'",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT
                + "hide-if-has-and-matches-style");
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div#basic-target > div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHideIfHasAndMatchesStyleLegitElements() throws Exception {
        mHelper.addCustomFilter(String.format(
                "%s#$#hide-if-has-and-matches-style a[href=\"#comments-target-ad\"] div[id] span"
                        + ".label ';' /\\\\bdisplay:\\ inline\\;/",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT
                + "hide-if-has-and-matches-style");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 1, "div#comments-target > div[data-expectedresult='pass']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHideIfLabeledBy() throws Exception {
        mHelper.addCustomFilter(String.format(
                "%s#$#hide-if-labelled-by 'Label' '#hilb-target [aria-labelledby]' '#hilb-target'",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "hide-if-labelled-by");
        TestVerificationUtils.verifyHiddenCount(mHelper, 1, "div[data-expectedresult='fail']");
        TestVerificationUtils.verifyDisplayedCount(mHelper, 1, "div[data-expectedresult='pass']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHideIfMatchesXPathBasicStaticUsage() throws Exception {
        mHelper.addCustomFilter(String.format("%s#$#hide-if-matches-xpath //*[@id=\"isnfnv\"]",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "hide-if-matches-xpath");
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div#basic-static-usage-area > div[data-expectedresult='fail']");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 1, "div#basic-static-usage-area > div[data-expectedresult='pass']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHideIfMatchesXPath3BasicStaticUsage() throws Exception {
        mHelper.addCustomFilter(String.format("%s#$#hide-if-matches-xpath3 //*[@id=\"isnfnv\"]",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "hide-if-matches-xpath");
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div#basic-static-usage-area > div[data-expectedresult='fail']");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 1, "div#basic-static-usage-area > div[data-expectedresult='pass']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHideIfMatchesXPathClassUsage() throws Exception {
        mHelper.addCustomFilter(
                String.format("%s#$#hide-if-matches-xpath //*[@class=\"to-be-hidden\"]",
                        TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "hide-if-matches-xpath");
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div#class-usage-area > div[data-expectedresult='fail']");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 1, "div#class-usage-area > div[data-expectedresult='pass']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHideIfMatchesXPath3ClassUsage() throws Exception {
        mHelper.addCustomFilter(
                String.format("%s#$#hide-if-matches-xpath3 //*[@class=\"to-be-hidden\"]",
                        TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "hide-if-matches-xpath");
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div#class-usage-area > div[data-expectedresult='fail']");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 1, "div#class-usage-area > div[data-expectedresult='pass']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHideIfMatchesXPathIdStartsWith() throws Exception {
        mHelper.addCustomFilter(
                String.format("%s#$#hide-if-matches-xpath //div[starts-with(@id,\"fail\")]",
                        TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "hide-if-matches-xpath");
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div#hide-if-id-starts-with-area > div[data-expectedresult='fail']");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 1, "div#hide-if-id-starts-with-area > div[data-expectedresult='pass']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHideIfMatchesXPath3IdStartsWith() throws Exception {
        mHelper.addCustomFilter(
                String.format("%s#$#hide-if-matches-xpath3 //div[starts-with(@id,\"fail\")]",
                        TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "hide-if-matches-xpath");
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div#hide-if-id-starts-with-area > div[data-expectedresult='fail']");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 1, "div#hide-if-id-starts-with-area > div[data-expectedresult='pass']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHideIfShadowContainsBasicUsage() throws Exception {
        mHelper.addCustomFilter(String.format("%s#$#hide-if-shadow-contains 'hisc-basic' p",
                TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "hide-if-shadow-contains");
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div#basic-target > p[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testHideIfShadowContainsRegexUsage() throws Exception {
        mHelper.addCustomFilter(
                String.format("%s#$#hide-if-shadow-contains '/hisc-regex[1-2]/' div",
                        TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "hide-if-shadow-contains");
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 2, "div#regex-target > div[data-expectedresult='fail']");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 1, "div#regex-target > div[data-expectedresult='pass']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testJsonPrune() throws Exception {
        mHelper.addCustomFilter(
                String.format("%s#$#json-prune 'data-expectedresult jsonprune aria-label'",
                        TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "json-prune?delay=100");
        // The object does not get hidden, it no longer exists in the DOM.
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 0, "div#testcase-area > div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testOverridePropertyRead() throws Exception {
        mHelper.addCustomFilter(
                String.format("%s#$#override-property-read overridePropertyRead.fp false",
                        TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(
                TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT + "override-property-read");
        TestVerificationUtils.verifyHiddenCount(
                mHelper, 1, "div#basic-target > div[data-expectedresult='fail']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testStripFetchQueryParameterBasicUsage() throws Exception {
        mHelper.addCustomFilter(String.format("%s#$#strip-fetch-query-parameter basicBlocked %s",
                TestPagesHelperBase.TESTPAGES_DOMAIN, TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT
                + "strip-fetch-query-parameter");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 0, "div#basic-target > div[data-expectedresult='fail']");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 1, "div#basic-target > div[data-expectedresult='pass']");
    }

    @Test
    @LargeTest
    @Feature({"adblock"})
    public void testStripFetchQueryParameterOtherUsage() throws Exception {
        mHelper.addCustomFilter(
                String.format("%s#$#strip-fetch-query-parameter otherAllowed2 other-domain",
                        TestPagesHelperBase.TESTPAGES_DOMAIN));
        mHelper.loadUrlWaitForContent(TestPagesHelperBase.SNIPPETS_TESTPAGES_TESTCASES_ROOT
                + "strip-fetch-query-parameter");
        TestVerificationUtils.verifyDisplayedCount(
                mHelper, 2, "div#other-target > div[data-expectedresult='pass']");
    }
}
