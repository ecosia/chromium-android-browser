/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.chromium.components.browser_ui.widget;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.AttributeSet;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import org.chromium.components.browser_ui.widget.R;

/**
 * This drawable requires API level 21 at the very least,
 * because we utilize the .applyTheme(...) method
 */
public class EcosiaSurfaceColorDrawable extends SurfaceColorDrawable {

    private int mColorOverride = Color.TRANSPARENT;

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        super.inflate(resources, parser, attrs, theme);
        mColorOverride = resources.getColor(R.color.ecosia_browser_ui_app_menu_item_background, theme);
    }

    @TargetApi(value = 21)
    @Override
    public void applyTheme(Resources.Theme theme) {
        super.applyTheme(theme);
        setColor(mColorOverride);
    }
}
