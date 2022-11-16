/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */


package org.ecosia.dummy;

import android.app.Activity;
import android.content.Intent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import org.chromium.base.test.BaseRobolectricTestRunner;

@RunWith(BaseRobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public final class EcosiaDummyActivityUnitTest {
    @Before
    public void setUp() throws InterruptedException {
        EcosiaDummyActivity.someBool = false;
    }

    @Test
    public void testDummySomeBool() {
        Intent intent = new Intent();
        // TODO: checkout FirstRunIntegrationUnitTest.java for Intent & Activity initialization helper methods
        Activity a = Robolectric.buildActivity(EcosiaDummyActivity.class, intent).create().get();
        EcosiaDummyActivity da = (EcosiaDummyActivity)a;
        Assert.assertFalse(EcosiaDummyActivity.someBool);
        da.SetSomeBool(true);
        Assert.assertTrue(EcosiaDummyActivity.someBool);
    }
}
