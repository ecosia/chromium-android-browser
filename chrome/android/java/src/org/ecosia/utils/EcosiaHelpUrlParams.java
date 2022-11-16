/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.ecosia.utils;

import org.chromium.content_public.browser.LoadUrlParams;
import org.chromium.ui.base.PageTransition;

public class EcosiaHelpUrlParams extends LoadUrlParams {

    private static final String URL_ECOSIA_HELP = "https://ecosia.helpscoutdocs.com/";

    public EcosiaHelpUrlParams() {
        super(URL_ECOSIA_HELP, PageTransition.LINK);
    }
}
