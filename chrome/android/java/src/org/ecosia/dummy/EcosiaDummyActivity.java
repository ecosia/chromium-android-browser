/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */


package org.ecosia.dummy;

import android.app.Activity;

public final class EcosiaDummyActivity extends Activity {
    public static boolean someBool;

    public void SetSomeBool(boolean newValue) {
      EcosiaDummyActivity.someBool = newValue;
    }
}
