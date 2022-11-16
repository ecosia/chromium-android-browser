/*
 * Copyright 2015 The Chromium Authors
 * Copyright (C) 2023 Ecosia Android App source (for GPL 3.0)
 *
 * Licensed under the GNU General Public License, Version 3.0 and BSD-style license (found in LICENSE file);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * License: GPL-3.0-only - https://spdx.org/licenses/GPL-3.0-only.html
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.chromium.chrome.browser.bookmarks;

import android.app.Activity;
import android.content.ComponentName;

import androidx.annotation.VisibleForTesting;

import org.chromium.chrome.R;
import org.chromium.chrome.browser.app.ChromeActivity;
import org.chromium.chrome.browser.ui.messages.snackbar.SnackbarManager;
import org.chromium.chrome.browser.ui.native_page.BasicNativePage;
import org.chromium.chrome.browser.ui.native_page.NativePageHost;
import org.chromium.components.browser_ui.modaldialog.AppModalPresenter;
import org.chromium.components.embedder_support.util.UrlConstants;
import org.chromium.ui.modaldialog.ModalDialogManager;

/**
 * A native page holding a {@link BookmarkManager} on _tablet_.
 */
public class BookmarkPage extends BasicNativePage {
    private BookmarkManager mManager;
    private String mTitle;

    /**
     * Create a new instance of the bookmarks page.
     * @param componentName The current activity component, used to open bookmarks.
     * @param snackbarManager Allows control over the app snackbar.
     * @param isIncognito Whether the bookmark UI is loaded in incognito mode.
     * @param host A NativePageHost to load urls.
     */
    public BookmarkPage(ComponentName componentName, SnackbarManager snackbarManager,
            boolean isIncognito, NativePageHost host, Activity activity) {
        super(host);

        mManager = new BookmarkManager(
                host.getContext(), componentName, false, isIncognito, snackbarManager);
        mManager.setBasicNativePage(this);
        // Ecosia: Bookmark Import / Export
        mManager.setWindow(((ChromeActivity) activity).getWindowAndroid(),
                new ModalDialogManager(
                        new AppModalPresenter(activity), ModalDialogManager.ModalDialogType.APP));
        mTitle = host.getContext().getResources().getString(R.string.bookmarks);

        initWithView(mManager.getView());
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    @Override
    public String getHost() {
        return UrlConstants.BOOKMARKS_HOST;
    }

    @Override
    public void updateForUrl(String url) {
        super.updateForUrl(url);
        mManager.updateForUrl(url);
    }

    @Override
    public void destroy() {
        mManager.onDestroyed();
        mManager = null;
        super.destroy();
    }

    @VisibleForTesting
    public BookmarkManager getManagerForTesting() {
        return mManager;
    }
}
