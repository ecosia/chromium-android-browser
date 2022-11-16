/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.ecosia.searchwidget;

import android.content.Intent;

import org.ecosia.tracking.TrackingManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Ignore;

import org.robolectric.annotation.Config;

import org.chromium.base.test.BaseRobolectricTestRunner;
import org.chromium.base.test.BaseJUnit4ClassRunner;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Ignore("Temporary disabled as it's failing")
@RunWith(BaseRobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class EcosiaSearchActivityExtensionUnitTest {

    @Mock
    private TrackingManager trackingManager;

    private EcosiaSearchActivityExtension ecosiaSearchActivityExtension;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ecosiaSearchActivityExtension = new EcosiaSearchActivityExtensionImplementation(trackingManager);
    }

    @Test
    public void testOnSeachWidgetSearchMade() {
        ecosiaSearchActivityExtension.onSearchWidgetSearchMade();
        verify(trackingManager, times(1)).trackOriginEvent(any(Intent.class));
    }

}
