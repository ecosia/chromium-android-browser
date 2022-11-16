/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

/* This activity is not and should not be used. It was created just to prove using our
 * own pacakge and testing packages, along with all the config changes needed.
 */

package org.ecosia.dummy;

import android.app.Activity;

public final class EcosiaDummyActivity extends Activity {
    public static boolean someBool;

    public void SetSomeBool(boolean newValue) {
      EcosiaDummyActivity.someBool = newValue;
    }
}
