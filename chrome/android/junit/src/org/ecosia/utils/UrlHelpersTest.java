package org.ecosia.utils;

import org.chromium.base.test.BaseRobolectricTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(BaseRobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class UrlHelpersTest {

    @Test
    public void isEcosiaSerp_prodSerpWithProtocol_isTrue() {
        Assert.assertTrue(UrlHelpers.isEcosiaSerp("https://www.ecosia.org/search?q=tequila+sunrise"));
    }

    @Test
    public void isEcosiaSerp_devSerpWithProtocol_isTrue() {
        Assert.assertTrue(UrlHelpers.isEcosiaSerp("https://www.ecosia-dev.xyz/search?q=tequila+sunrise"));
    }

    @Test
    public void isEcosiaSerp_stagingSerpWithProtocol_isTrue() {
        Assert.assertTrue(UrlHelpers.isEcosiaSerp("https://www.ecosia-staging.xyz/search?q=tequila+sunrise"));
    }

    @Test
    public void isEcosiaSerp_index_isFalse() {
        Assert.assertFalse(UrlHelpers.isEcosiaSerp("https://www.ecosia.org"));
    }

    @Test
    public void isEcosiaSerp_serpWithoutProtocol_isFalse() {
        Assert.assertFalse(UrlHelpers.isEcosiaSerp("www.ecosia.org/"));
    }

    @Test
    public void isEcosiaSerp_blog_isFalse() {
        Assert.assertFalse(UrlHelpers.isEcosiaSerp("https://blog.ecosia.org/"));
    }

    @Test
    public void isEcosiaSerp_otherDomain_isFalse() {
        Assert.assertFalse(UrlHelpers.isEcosiaSerp("https://www.google.com"));
    }

    @Test
    public void isEcosiaSerp_notUrl_isFalse() {
        Assert.assertFalse(UrlHelpers.isEcosiaSerp("some random text"));
    }

    @Test
    public void isEcosiaSerp_emptyString_isFalse() {
        Assert.assertFalse(UrlHelpers.isEcosiaSerp(""));
    }
}
