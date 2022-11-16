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

import org.chromium.components.bookmarks.BookmarkId;
import org.chromium.components.browser_ui.widget.dragreorder.DragStateDelegate;
import org.chromium.components.browser_ui.widget.selectable_list.SelectableListLayout;
import org.chromium.components.browser_ui.widget.selectable_list.SelectionDelegate;
import org.chromium.components.favicon.LargeIconBridge;

import java.util.List;

/**
 * Interface used by UI components in the main bookmarks UI to broadcast UI change notifications
 * and get bookmark data model.
 */
public interface BookmarkDelegate {
    /**
     * Delegate used to open urls for main fragment on tablet.
     */
    interface BookmarkStateChangeListener {
        /**
         * Let the tab containing bookmark manager load the url and later handle UI updates.
         * @param url The url to open in tab.
         */
        public void onBookmarkUIStateChange(String url);
    }

    /**
     * Returns whether the bookmarks UI will be shown in a dialog, instead of a NativePage. This is
     * typically true on phones and false on tablets, but not always, e.g. in multi-window mode or
     * after upgrading to the new bookmarks.
     */
    boolean isDialogUi();

    /**
     * Shows bookmarks contained in the specified folder.
     * @param folder Parent folder that contains bookmarks to show as its children.
     */
    void openFolder(BookmarkId folder);

    /**
     * @return The {@link SelectionDelegate} responsible for tracking selected bookmarks.
     */
    SelectionDelegate<BookmarkId> getSelectionDelegate();

    /**
     * @return The {@link SelectableListLayout} displaying the list of bookmarks.
     */
    SelectableListLayout<BookmarkId> getSelectableListLayout();

    /**
     * Notifies the current mode set event to the given observer. For example, if the current mode
     * is MODE_ALL_BOOKMARKS, it calls onAllBookmarksModeSet.
     */
    void notifyStateChange(BookmarkUIObserver observer);

    /**
     * Closes the Bookmark UI (if on phone) and opens the given bookmark.
     * @param bookmark Bookmark to open.
     */
    void openBookmark(BookmarkId bookmark);

    /**
     * Closes the Bookmark UI (if on phone) and opens the given list of bookmarks in new tabs.
     * @param bookmarks Bookmarks to open.
     * @param incognito Whether the bookmarks should be opened in an incognito tab.
     */
    void openBookmarksInNewTabs(List<BookmarkId> bookmark, boolean incognito);

    /**
     * Shows the search UI.
     */
    void openSearchUI();

    /**
     * Imports bookmarks from user-selected file.
     */
    void importBookmarks(); // Ecosia: Bookmark Import / Export

    /**
     * Exports bookmarks to downloads directory.
     */
    void exportBookmarks(); // Ecosia: Bookmark Import / Export

    /**
     * Dismisses the search UI.
     */
    void closeSearchUI();

    /**
     * Add an observer to bookmark UI changes.
     */
    void addUIObserver(BookmarkUIObserver observer);

    /**
     * Remove an observer of bookmark UI changes.
     */
    void removeUIObserver(BookmarkUIObserver observer);

    /**
     * @return Bookmark data model associated with this UI.
     */
    BookmarkModel getModel();

    /**
     * @return Current UIState of bookmark main UI. If no mode is stored,
     *         {@link BookmarkUIState#STATE_LOADING} is returned.
     */
    int getCurrentState();

    /**
     * @return LargeIconBridge instance. By sharing the instance, we can also share the cache.
     */
    LargeIconBridge getLargeIconBridge();

    /**
     * @return The drag state delegate that is associated with this list of bookmarks.
     */
    DragStateDelegate getDragStateDelegate();

    /**
     * Move a bookmark one position down within its folder.
     *
     * @param bookmarkId The bookmark to move.
     */
    void moveDownOne(BookmarkId bookmarkId);

    /**
     * Move a bookmark one position up within its folder.
     *
     * @param bookmarkId The bookmark to move.
     */
    void moveUpOne(BookmarkId bookmarkId);

    /**
     * Notified when the menu is opened for a bookmark row displayed in the UI.
     */
    void onBookmarkItemMenuOpened();

    /**
     * Scroll the bookmarks list such that bookmarkId is shown in the view, and highlight it.
     *
     * @param bookmarkId The BookmarkId of the bookmark of interest.
     */
    void highlightBookmark(BookmarkId bookmarkId);

    /**
     * Ecosia: Notify implementers about back buttion press to use native back navigation implementation
     */
    void goBack();
}
