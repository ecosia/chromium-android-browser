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

package org.chromium.chrome.browser.app.bookmarks;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import org.chromium.base.IntentUtils;
import org.chromium.chrome.browser.IntentHandler;
import org.chromium.chrome.browser.SnackbarActivity;
import org.chromium.chrome.browser.back_press.BackPressHelper;
import org.chromium.chrome.browser.back_press.SecondaryActivityBackPressUma.SecondaryActivity;
import org.chromium.chrome.browser.bookmarks.BookmarkManagerCoordinator;
import org.chromium.chrome.browser.bookmarks.BookmarkPage;
import org.chromium.chrome.browser.bookmarks.BookmarkUiPrefs;
import org.chromium.chrome.browser.preferences.ChromeSharedPreferences;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.profiles.ProfileProvider;
import org.chromium.components.bookmarks.BookmarkId;
import org.chromium.components.browser_ui.modaldialog.AppModalPresenter;
import org.chromium.components.embedder_support.util.UrlConstants;
import org.chromium.ui.base.ActivityWindowAndroid;
import org.chromium.ui.base.IntentRequestTracker;
import org.chromium.ui.modaldialog.ModalDialogManager;

/**
 * The activity that displays the bookmark UI on the phone. It keeps a {@link
 * BookmarkManagerCoordinator} inside of it and creates a snackbar manager. This activity should
 * only be shown on phones; on tablet the bookmark UI is shown inside of a tab (see {@link
 * BookmarkPage}).
 */
public class BookmarkActivity extends SnackbarActivity {
    private BookmarkManagerCoordinator mBookmarkManagerCoordinator;
    public static final int EDIT_BOOKMARK_REQUEST_CODE = 14;
    public static final String INTENT_VISIT_BOOKMARK_ID = "BookmarkEditActivity.VisitBookmarkId";

    // Ecosia: Bookmark Import / Export
    private ActivityWindowAndroid mWindowAndroid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean isIncognito =
                IntentUtils.safeGetBooleanExtra(
                        getIntent(), IntentHandler.EXTRA_INCOGNITO_MODE, false);
        Profile profile = ProfileProvider.getOrCreateProfile(getProfileProvider(), isIncognito);
        mBookmarkManagerCoordinator =
                new BookmarkManagerCoordinator(
                        this,
                        IntentUtils.safeGetParcelableExtra(
                                getIntent(), IntentHandler.EXTRA_PARENT_COMPONENT),
                        true,
                        getSnackbarManager(),
                        profile,
                        new BookmarkUiPrefs(ChromeSharedPreferences.getInstance()));
        String url = getIntent().getDataString();
        if (TextUtils.isEmpty(url)) url = UrlConstants.BOOKMARKS_URL;
        mBookmarkManagerCoordinator.updateForUrl(url);
        setContentView(mBookmarkManagerCoordinator.getView());
        BackPressHelper.create(
                this,
                getOnBackPressedDispatcher(),
                mBookmarkManagerCoordinator,
                SecondaryActivity.BOOKMARK);

        // Ecosia: Bookmark Import / Export
        IntentRequestTracker intentRequestTracker = IntentRequestTracker.createFromActivity(this);
        mWindowAndroid = new ActivityWindowAndroid(this, true, intentRequestTracker);
        mWindowAndroid.getIntentRequestTracker().restoreInstanceState(savedInstanceState);
        mBookmarkManagerCoordinator.setWindow(mWindowAndroid);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBookmarkManagerCoordinator.onDestroyed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mWindowAndroid.getIntentRequestTracker().onActivityResult(requestCode, resultCode, data); // Ecosia: Bookmark Import / Export
        if (requestCode == EDIT_BOOKMARK_REQUEST_CODE && resultCode == RESULT_OK) {
            BookmarkId bookmarkId =
                    BookmarkId.getBookmarkIdFromString(
                            data.getStringExtra(INTENT_VISIT_BOOKMARK_ID));
            mBookmarkManagerCoordinator.openBookmark(bookmarkId);
        }
    }

    // Ecosia: Bookmark Import / Export
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (mWindowAndroid.handlePermissionResult(requestCode, permissions, grantResults))
            return;
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * @return The {@link BookmarkManagerCoordinator} for testing purposes.
     */
    public BookmarkManagerCoordinator getManagerForTesting() {
        return mBookmarkManagerCoordinator;
    }
}
