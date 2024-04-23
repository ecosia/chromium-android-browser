/*
 * Copyright 2013 The Chromium Authors
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

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Browser;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.google.protobuf.InvalidProtocolBufferException;

import org.chromium.base.ContentUriUtils;
import org.chromium.base.shared_preferences.PreferenceKeyRegistry;
import org.chromium.base.shared_preferences.SharedPreferencesManager;
import org.chromium.build.BuildConfig;
import org.chromium.chrome.browser.download.DownloadDialogBridge;
import org.chromium.chrome.browser.download.settings.DownloadLocationHelperImpl; 
import org.chromium.chrome.browser.preferences.LegacyChromePreferenceKeys;
import org.chromium.chrome.browser.profiles.ProfileManager;
import org.jni_zero.CalledByNative;
import org.jni_zero.JniType;
import org.jni_zero.NativeMethods;

import org.chromium.base.ContextUtils;
import org.chromium.base.ObserverList;
import org.chromium.base.ThreadUtils;
import org.chromium.base.metrics.RecordUserAction;
import org.chromium.chrome.R;

import org.chromium.base.task.AsyncTask;
import org.chromium.chrome.browser.IntentHandler;
import org.chromium.chrome.browser.document.ChromeLauncherActivity;
import org.chromium.chrome.browser.download.DirectoryOption;
import org.chromium.chrome.browser.download.DownloadLocationDialogType;
import org.chromium.chrome.browser.download.dialogs.DownloadLocationCustomView;
import org.chromium.chrome.browser.download.dialogs.DownloadLocationDialogController;
import org.chromium.chrome.browser.download.dialogs.DownloadLocationDialogCoordinator;
import org.chromium.chrome.browser.flags.ChromeFeatureList;
import org.chromium.chrome.browser.partnerbookmarks.PartnerBookmarksShim;
import org.chromium.chrome.browser.preferences.ChromePreferenceKeys;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.components.bookmarks.BookmarkId;
import org.chromium.components.bookmarks.BookmarkItem;
import org.chromium.components.bookmarks.BookmarkType;
import org.chromium.components.power_bookmarks.PowerBookmarkMeta;
import org.chromium.components.power_bookmarks.PowerBookmarkType;
import org.chromium.content_public.browser.WebContents;
import org.chromium.ui.base.PageTransition;
import org.chromium.ui.base.WindowAndroid;
import org.chromium.ui.modaldialog.DialogDismissalCause;
import org.chromium.ui.modaldialog.ModalDialogManager;
import org.chromium.ui.modaldialog.ModalDialogProperties;
import org.chromium.ui.modelutil.PropertyModel;
import org.chromium.ui.widget.Toast;
import org.chromium.url.GURL;
import org.chromium.chrome.browser.bookmarks.BookmarkBridgeJni;
import org.chromium.chrome.browser.preferences.ChromeSharedPreferences;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides the communication channel for Android to fetch and manipulate the bookmark model stored
 * in native.
 */
class BookmarkBridge {
    private final ObserverList<BookmarkModelObserver> mObservers = new ObserverList<>();

    private long mNativeBookmarkBridge;
    private boolean mIsDestroyed;
    private boolean mIsDoingExtensiveChanges;
    private boolean mIsNativeBookmarkModelLoaded;

    // Lazily set pseudo-constants. These should never change at runtime. Used to avoid crossing
    // JNI to fetch information.
    private @Nullable BookmarkId mRootFolderId;
    private @Nullable BookmarkId mMobileFolderId;
    private @Nullable BookmarkId mOtherFolderId;
    private @Nullable BookmarkId mDesktopFolderId;
    private @Nullable BookmarkId mLocalOrSyncableReadingListFolderId;

    // Ecosia: Bookmark Import / Export BEGIN
    private static final int IMPORT_ERROR_INVALID_FILE_FORMAT = 1;
    private static final int IMPORT_ERROR_INVALID_FILE_LENGTH_TOO_SMALL = 2;
    private static final int IMPORT_ERROR_INVALID_FILE_LENGTH_TOO_LARGE = 3;
    private static final int IMPORT_ERROR_INVALID_FILE_COULD_NOT_READ = 4;
    private static final int IMPORT_ERROR_INVALID_FILE_EMPTY_CONTENTS = 5;
    private static final int IMPORT_ERROR_INVALID_FILE_TYPE = 6;

    private DownloadLocationCustomView mCustomView;

    private PropertyModel mDialogModel;

    private ModalDialogManager mModalDialogManager;

    /**
     * Import bookmarks from a selected file.
     *
     * @param window The current window of the bookmarks activity or page.
     */
    public void importBookmarks(WindowAndroid window) {
        assert mIsNativeBookmarkModelLoaded;
        BookmarkBridgeJni.get().importBookmarks(mNativeBookmarkBridge, BookmarkBridge.this, window);
    }

    /**
     * Export bookmarks to a path selected by the user.
     *
     * @param window The current window of the bookmarks activity or page.
     */
    public void exportBookmarks(WindowAndroid window, ModalDialogManager modalDialogManager) {
        assert mIsNativeBookmarkModelLoaded;
        exportBookmarksImplUseSaf(window);
    }

    private void exportBookmarksImplUseSaf(WindowAndroid window) {
        Context context = window.getContext().get();

        // standard name for boorkmark file
        final String standardBoorkmarkName = "bookmarks.html";

        // use the fileSelector and saf asking user for the file
        Intent fileSelector = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        fileSelector.addCategory(Intent.CATEGORY_OPENABLE);
        fileSelector.setType("text/html");
        fileSelector.putExtra(Intent.EXTRA_TITLE, standardBoorkmarkName);
        fileSelector.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        // get last exported uri path, if any
        SharedPreferencesManager sharedPrefs = SharedPreferencesManager.getInstanceForRegistry(ChromeSharedPreferences.REGISTRY);
        String bookmarksPath = sharedPrefs.readString(ChromePreferenceKeys.BOOKMARKS_LAST_EXPORT_URI, standardBoorkmarkName);
        Uri lastSelectedUri = Uri.parse(bookmarksPath);

        // prepare delegate for file selector
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int button) {
                if (button == AlertDialog.BUTTON_NEGATIVE) {
                    window.showIntent(fileSelector,
                            new WindowAndroid.IntentCallback() {
                                @Override
                                public void onIntentCompleted(int resultCode, Intent data) {
                                    if (data == null) return;
                                    Uri filePath = data.getData();
                                    doExportBookmarksImpl(window, filePath);
                                }
                            },
                            null);
                } else {
                    if (dialog != null) dialog.dismiss();
                    doExportBookmarksImpl(window, lastSelectedUri);
                }
            }
        };

        // as a workaround for https://issuetracker.google.com/issues/37136466
        // ask to overwrite if is a valid uri and the file is present
        if (DocumentsContract.isDocumentUri(context, lastSelectedUri)) {
            AsyncTask<Void> checkUriTask = new AsyncTask<Void>() {
                boolean uriExists = false;
                String actualFilePath = null;

                @Override
                protected Void doInBackground() {
                    uriExists = ContentUriUtils.contentUriExists(lastSelectedUri.toString());
                    if (uriExists) {
                        actualFilePath = ContentUriUtils.getFilePathFromContentUri(lastSelectedUri);
                        // get real actual file name on disk
                        if (actualFilePath == null) actualFilePath = lastSelectedUri.toString();
                        // set file name to last exported file name
                        fileSelector.putExtra(Intent.EXTRA_TITLE,
                                ContentUriUtils.getDisplayName(lastSelectedUri, context,
                                        DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    // check for permissions
                    if (uriExists) {
                        AlertDialog.Builder alert =
                                new AlertDialog.Builder(context, R.style.ThemeOverlay_BrowserUI_AlertDialog);
                        AlertDialog alertDialog =
                                alert.setTitle(R.string.bookmarks_export_alert_title)
                                        .setMessage(context.getString(R.string.bookmarks_export_alert_message, actualFilePath))
                                        .setPositiveButton(
                                                R.string.bookmarks_export_alert_message_yes, onClickListener)
                                        .setNegativeButton(R.string.bookmarks_export_alert_message_no, onClickListener)
                                        .create();
                        alertDialog.getDelegate().setHandleNativeActionModesEnabled(false);

                        // show dialog asking for overwrite
                        alertDialog.show();
                        return;
                    } else {
                        onClickListener.onClick(null, AlertDialog.BUTTON_NEGATIVE);
                    }
                }
            };
            checkUriTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            return;
        }

        // actually open the file selector
        onClickListener.onClick(null, AlertDialog.BUTTON_NEGATIVE);
    }

    private void doExportBookmarksImpl(WindowAndroid window, Uri filePath) {
        ContentResolver resolver = ContextUtils.getApplicationContext().getContentResolver();
        // since we want to persist the uri in settings, ask for persistable permissions
        resolver.takePersistableUriPermission(filePath, Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_READ_URI_PERMISSION);

        BookmarkBridgeJni.get().exportBookmarks(mNativeBookmarkBridge, BookmarkBridge.this,
                window, filePath.toString());
    }

    // Ecosia: Bookmark Import / Export END

    /** Returns whether account bookmark folders are currently active. */
    public boolean areAccountBookmarkFoldersActive() {
        ThreadUtils.assertOnUiThread();
        return BookmarkBridgeJni.get().areAccountBookmarkFoldersActive(mNativeBookmarkBridge);
    }

    /**
     * Handler to fetch the bookmarks, titles, urls and folder hierarchy.
     *
     * @param profile Profile instance corresponding to the active profile.
     */
    static BookmarkModel getForProfile(Profile profile) {
        ThreadUtils.assertOnUiThread();
        return BookmarkBridgeJni.get().nativeGetForProfile(profile);
    }

    @CalledByNative
    static BookmarkModel createBookmarkModel(long nativeBookmarkBridge) {
        return new BookmarkModel(nativeBookmarkBridge);
    }

    BookmarkBridge(long nativeBookmarkBridge) {
        mNativeBookmarkBridge = nativeBookmarkBridge;
        mIsDoingExtensiveChanges =
                BookmarkBridgeJni.get().isDoingExtensiveChanges(mNativeBookmarkBridge);
    }

    /** Destroys this instance so no further calls can be executed. */
    void destroy() {
        mIsDestroyed = true;
        if (mNativeBookmarkBridge != 0) {
            BookmarkBridgeJni.get().destroy(mNativeBookmarkBridge);
            mNativeBookmarkBridge = 0;
            mIsNativeBookmarkModelLoaded = false;
        }
        mObservers.clear();
    }

    /** Returns whether the bridge has been destroyed. */
    private boolean isDestroyed() {
        return mIsDestroyed;
    }

    /** Returns the most recently added BookmarkId */
    public @Nullable BookmarkId getMostRecentlyAddedUserBookmarkIdForUrl(@NonNull GURL url) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get()
                .getMostRecentlyAddedUserBookmarkIdForUrl(mNativeBookmarkBridge, url);
    }

    /**
     * Load an empty partner bookmark shim for testing. The root node for bookmark will be an empty
     * node.
     */
    public void loadEmptyPartnerBookmarkShimForTesting() {
        BookmarkBridgeJni.get()
                .loadEmptyPartnerBookmarkShimForTesting(mNativeBookmarkBridge); // IN-TEST
    }

    /**
     * Load a fake partner bookmark shim for testing. To see (or edit) the titles and URLs of the
     * partner bookmarks, go to bookmark_bridge.cc.
     */
    public void loadFakePartnerBookmarkShimForTesting() {
        BookmarkBridgeJni.get()
                .loadFakePartnerBookmarkShimForTesting(mNativeBookmarkBridge); // IN-TEST
    }

    /**
     * Add an observer to bookmark model changes.
     *
     * @param observer The observer to be added.
     */
    public void addObserver(BookmarkModelObserver observer) {
        mObservers.addObserver(observer);
    }

    /**
     * Remove an observer of bookmark model changes.
     *
     * @param observer The observer to be removed.
     */
    public void removeObserver(BookmarkModelObserver observer) {
        mObservers.removeObserver(observer);
    }

    /**
     * @return Whether or not the underlying bookmark model is loaded.
     */
    public boolean isBookmarkModelLoaded() {
        return mIsNativeBookmarkModelLoaded;
    }

    /**
     * Schedules a runnable to run after the bookmark model is loaded. If the model is already
     * loaded, executes the runnable immediately. If not, also kick off partner bookmark reading.
     *
     * @return Whether the given runnable is executed synchronously.
     */
    public boolean finishLoadingBookmarkModel(final Runnable runAfterModelLoaded) {
        if (isBookmarkModelLoaded()) {
            runAfterModelLoaded.run();
            return true;
        }

        addObserver(
                new BookmarkModelObserver() {
                    @Override
                    public void bookmarkModelLoaded() {
                        removeObserver(this);
                        runAfterModelLoaded.run();
                    }

                    @Override
                    public void bookmarkModelChanged() {}
            		
					// Ecosia: Bookmark Import / Export
            		@Override
            		public void bookmarkModelNeedsReloadAfterBookmarksImport() {
                		// only required after successful bookmark import
            		}
                });

        // Start reading as a fail-safe measure to avoid waiting forever if the caller forgets to
        // call kickOffReading().
        PartnerBookmarksShim.kickOffReading(ContextUtils.getApplicationContext());
        return false;
    }

    /**
     * Gets the {@link BookmarkItem} which is referenced by the given {@link BookmarkId}.
     *
     * @param id The {@link BookmarkId} used to lookup the corresponding {@link BookmarkItem}.
     * @return A BookmarkItem instance for the given BookmarkId. <code>null</code> if it doesn't
     *     exist.
     */
    public @Nullable BookmarkItem getBookmarkById(@Nullable BookmarkId id) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        if (id == null) return null;

        return BookmarkBridgeJni.get()
                .getBookmarkById(mNativeBookmarkBridge, id.getId(), id.getType());
    }

    /**
     * @return The top level folders, including special folders (managed bookmarks, reading list,
     *     partner bookmarks). Will show empty folder according to the logic in BookmarkClient.
     */
    public List<BookmarkId> getTopLevelFolderIds() {
        return getTopLevelFolderIds(/* ignoreVisibility= */ false);
    }

    /**
     * @param ignoreVisibility Whether the visible while empty logic, found in BookmarkClient, is
     *     used when gathering nodes. When true, all folders are shown regardless of client defined
     *     visibility. When false, the client defined visibility rules are used. See
     *     components/bookmarks/browser/bookmark_client.h for more information. When account
     *     bookmarks are active, only a subset of the local folders are included when this is true.
     *     This is to avoid overloading the user with a lof of unnecessary local folders (folders
     *     included are the local Mobile and Reading List folders).
     * @return The top level folders, including special folders (managed bookmarks, reading list,
     *     partner bookmarks).
     */
    public List<BookmarkId> getTopLevelFolderIds(boolean ignoreVisibility) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return new ArrayList<>();
        assert mIsNativeBookmarkModelLoaded;
        List<BookmarkId> result = new ArrayList<>();
        BookmarkBridgeJni.get()
                .getTopLevelFolderIds(mNativeBookmarkBridge, ignoreVisibility, result);
        return result;
    }

    /** Returns the local/syncable synthetic reading list folder. */
    public BookmarkId getLocalOrSyncableReadingListFolder() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        if (mLocalOrSyncableReadingListFolderId == null) {
            mLocalOrSyncableReadingListFolderId =
                    BookmarkBridgeJni.get()
                            .getLocalOrSyncableReadingListFolder(mNativeBookmarkBridge);
        }
        return mLocalOrSyncableReadingListFolderId;
    }

    /**
     * Returns the account synthetic reading list folder. Function will return null if the required
     * conditions to use account-bound data aren't satisfied: - The user is signed-in and not
     * syncing. - The user has the kReadingList sync data type enabled.
     */
    public BookmarkId getAccountReadingListFolder() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;

        // Note: The account reading list folder isn't cached because the availability can change
        // during runtime.
        return BookmarkBridgeJni.get().getAccountReadingListFolder(mNativeBookmarkBridge);
    }

    /** Returns the default reading list location. */
    public BookmarkId getDefaultReadingListFolder() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get().getDefaultReadingListFolder(mNativeBookmarkBridge);
    }

    /** Returns the default bookmark location. */
    public BookmarkId getDefaultBookmarkFolder() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get().getDefaultBookmarkFolder(mNativeBookmarkBridge);
    }

    /**
     * Populates folderList with BookmarkIds of folders users can move bookmarks to and all folders
     * have corresponding depth value in depthList. Folders having depths of 0 will be shown as
     * top-layered folders. These include "Desktop Folder" itself as well as all children of
     * "mobile" and "other". Children of 0-depth folders have depth of 1, and so on.
     *
     * <p>The result list will be sorted alphabetically by title. "mobile", "other", root node,
     * managed folder, partner folder are NOT included as results.
     */
    @VisibleForTesting
    public void getAllFoldersWithDepths(List<BookmarkId> folderList, List<Integer> depthList) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return;
        assert mIsNativeBookmarkModelLoaded;
        BookmarkBridgeJni.get()
                .getAllFoldersWithDepths(mNativeBookmarkBridge, folderList, depthList);
    }

    /**
     * Calls {@link #getAllFoldersWithDepths(List, List)} and remove all folders and children in
     * bookmarksToMove. This method is useful when finding a list of possible parent folers when
     * moving some folders (a folder cannot be moved to its own children).
     */
    public void getMoveDestinations(
            List<BookmarkId> folderList,
            List<Integer> depthList,
            List<BookmarkId> bookmarksToMove) {
        if (mNativeBookmarkBridge == 0) return;
        ThreadUtils.assertOnUiThread();
        assert mIsNativeBookmarkModelLoaded;
        BookmarkBridgeJni.get()
                .getAllFoldersWithDepths(mNativeBookmarkBridge, folderList, depthList);
        if (bookmarksToMove == null || bookmarksToMove.size() == 0) return;

        boolean shouldTrim = false;
        int trimThreshold = -1;
        for (int i = 0; i < folderList.size(); i++) {
            int depth = depthList.get(i);
            if (shouldTrim) {
                if (depth <= trimThreshold) {
                    shouldTrim = false;
                    trimThreshold = -1;
                } else {
                    folderList.remove(i);
                    depthList.remove(i);
                    i--;
                }
            }
            // Do not use else here because shouldTrim could be set true after if (shouldTrim)
            // statement.
            if (!shouldTrim) {
                BookmarkId folder = folderList.get(i);
                if (bookmarksToMove.contains(folder)) {
                    shouldTrim = true;
                    trimThreshold = depth;
                    folderList.remove(i);
                    depthList.remove(i);
                    i--;
                }
            }
        }
    }

    /** Returns the BookmarkId for root folder node. */
    public BookmarkId getRootFolderId() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        if (mRootFolderId == null) {
            mRootFolderId = BookmarkBridgeJni.get().getRootFolderId(mNativeBookmarkBridge);
        }
        return mRootFolderId;
    }

    /** Returns the BookmarkId for Mobile folder node. */
    public BookmarkId getMobileFolderId() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        if (mMobileFolderId == null) {
            mMobileFolderId = BookmarkBridgeJni.get().getMobileFolderId(mNativeBookmarkBridge);
        }
        return mMobileFolderId;
    }

    /** Returns Id representing the special "other" folder from bookmark model. */
    public BookmarkId getOtherFolderId() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        if (mOtherFolderId == null) {
            mOtherFolderId = BookmarkBridgeJni.get().getOtherFolderId(mNativeBookmarkBridge);
        }
        return mOtherFolderId;
    }

    /** Returns the BookmarkId representing special "desktop" folder, namely "bookmark bar". */
    public BookmarkId getDesktopFolderId() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        if (mDesktopFolderId == null) {
            mDesktopFolderId = BookmarkBridgeJni.get().getDesktopFolderId(mNativeBookmarkBridge);
        }
        return mDesktopFolderId;
    }

    /** Returns the id representing the special account "mobile" folder from bookmark model. */
    public BookmarkId getAccountMobileFolderId() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get().getAccountMobileFolderId(mNativeBookmarkBridge);
    }

    /** Returns the id representing the special account "other" folder from bookmark model. */
    public BookmarkId getAccountOtherFolderId() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get().getAccountOtherFolderId(mNativeBookmarkBridge);
    }

    /**
     * @return BookmarkId representing special account "desktop" folder, namely "bookmark bar".
     */
    public BookmarkId getAccountDesktopFolderId() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get().getAccountDesktopFolderId(mNativeBookmarkBridge);
    }

    /**
     * Gets Bookmark GUID which is immutable and differs from the BookmarkId in that it is
     * consistent across different clients and stable throughout the lifetime of the bookmark, with
     * the exception of nodes added to the Managed Bookmarks folder, whose GUIDs are re-assigned at
     * start-up every time.
     *
     * @return Bookmark GUID of the given node.
     */
    public String getBookmarkGuidByIdForTesting(BookmarkId id) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get()
                .getBookmarkGuidByIdForTesting(mNativeBookmarkBridge, id.getId(), id.getType());
    }

    /**
     * @return The number of children that the given node has.
     */
    public int getChildCount(BookmarkId id) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return 0;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get()
                .getChildCount(mNativeBookmarkBridge, id.getId(), id.getType());
    }

    /**
     * Reads sub-folder IDs, sub-bookmark IDs, or both of the given folder.
     *
     * @return Child IDs of the given folder, with the specified type.
     */
    public List<BookmarkId> getChildIds(BookmarkId id) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return new ArrayList<>();
        assert mIsNativeBookmarkModelLoaded;

        List<BookmarkId> result = new ArrayList<>();
        BookmarkBridgeJni.get()
                .getChildIds(mNativeBookmarkBridge, id.getId(), id.getType(), result);
        return result;
    }

    /**
     * Gets the child of a folder at the specific position.
     *
     * @param folderId Id of the parent folder
     * @param index Position of child among all children in folder
     * @return BookmarkId of the child, which will be null if folderId does not point to a folder or
     *     index is invalid.
     */
    public BookmarkId getChildAt(BookmarkId folderId, int index) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get()
                .getChildAt(mNativeBookmarkBridge, folderId.getId(), folderId.getType(), index);
    }

    /**
     * Get the total number of bookmarks in the sub tree of the specified folder.
     *
     * @param id The {@link BookmarkId} of the folder to be queried.
     * @return The total number of bookmarks in the folder.
     */
    public int getTotalBookmarkCount(BookmarkId id) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return 0;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get()
                .getTotalBookmarkCount(mNativeBookmarkBridge, id.getId(), id.getType());
    }

    /**
     * Synchronously gets a list of bookmarks that match the specified search query.
     *
     * @param query Keyword used for searching bookmarks.
     * @param maxNumberOfResult Maximum number of result to fetch.
     * @return List of bookmark IDs that are related to the given query.
     */
    public List<BookmarkId> searchBookmarks(String query, int maxNumberOfResult) {
        return searchBookmarks(query, null, null, maxNumberOfResult);
    }

    /**
     * Synchronously gets a list of bookmarks that match the specified search query.
     *
     * @param query Keyword used for searching bookmarks.
     * @param tags A list of tags the resulting bookmarks should have.
     * @param powerBookmarkType The type of power bookmark type to search for (or null for all).
     * @param maxNumberOfResult Maximum number of result to fetch.
     * @return List of bookmark IDs that are related to the given query.
     */
    public List<BookmarkId> searchBookmarks(
            String query,
            @Nullable String[] tags,
            @Nullable PowerBookmarkType powerBookmarkType,
            int maxNumberOfResult) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return new ArrayList<>();
        List<BookmarkId> bookmarkMatches = new ArrayList<>();
        int typeInt = powerBookmarkType == null ? -1 : powerBookmarkType.getNumber();
        BookmarkBridgeJni.get()
                .searchBookmarks(
                        mNativeBookmarkBridge,
                        bookmarkMatches,
                        query,
                        tags,
                        typeInt,
                        maxNumberOfResult);
        return bookmarkMatches;
    }

    // Ecosia: Bookmark Import / Export
    @CalledByNative
    public void bookmarksExported(WindowAndroid window, String bookmarksPath, boolean success) {
        if (!success) {
            ((Activity) window.getContext().get()).runOnUiThread(() -> {
                Context context = window.getContext().get();
                AlertDialog dialog = new AlertDialog.Builder(context, R.style.ThemeOverlay_BrowserUI_AlertDialog)
                        .setTitle(R.string.bookmarks_export_error_alert_title)
                        .setMessage(R.string.bookmarks_export_error_alert_message)
                        .setNeutralButton(R.string.bookmarks_export_error_alert_more_info_button, (DialogInterface.OnClickListener) (dialog12, which) -> {
                            startWebsiteIntentForUri(Uri.parse(context.getString(R.string.bookmarks_export_error_alert_more_info_url)));
                        })
                        .setPositiveButton(R.string.ok, (dialog1, which) -> {})
                        .create();
                dialog.show();
            });


        } else {
            SharedPreferencesManager sharedPrefs = SharedPreferencesManager.getInstanceForRegistry(ChromeSharedPreferences.REGISTRY);
            sharedPrefs.writeString(ChromePreferenceKeys.BOOKMARKS_LAST_EXPORT_URI, bookmarksPath);
            startWebsiteIntentForUri(
                    ContentUriUtils.isContentUri(bookmarksPath) ? Uri.parse(bookmarksPath) : Uri.parse("file://" + bookmarksPath)
            );
        }
    }

    private void startWebsiteIntentForUri(Uri uri) {
        Context context = ContextUtils.getApplicationContext();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.putExtra(Browser.EXTRA_APPLICATION_ID,
                context.getPackageName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(IntentHandler.EXTRA_PAGE_TRANSITION_TYPE, PageTransition.AUTO_BOOKMARK);

        // If the bookmark manager is shown in a tab on a phone (rather than in a separate
        // activity) the component name may be null. Send the intent through
        // ChromeLauncherActivity instead to avoid crashing. See crbug.com/615012.
        intent.setClass(context, ChromeLauncherActivity.class);

        IntentHandler.startActivityForTrustedIntent(intent);
    }

    /**
     * Synchronously gets a list of bookmarks of the given type
     *
     * @param powerBookmarkType The type of power bookmark type to search for (or null for all).
     * @return List of bookmark IDs that are related to the given query.
     */
    public List<BookmarkId> getBookmarksOfType(@NonNull PowerBookmarkType powerBookmarkType) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return new ArrayList<>();
        List<BookmarkId> bookmarkMatches = new ArrayList<>();
        int typeInt = powerBookmarkType.getNumber();
        BookmarkBridgeJni.get().getBookmarksOfType(mNativeBookmarkBridge, bookmarkMatches, typeInt);
        return bookmarkMatches;
    }

    /** Set title of the given bookmark. */
    public void setBookmarkTitle(BookmarkId id, String title) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return;
        assert mIsNativeBookmarkModelLoaded;
        BookmarkBridgeJni.get()
                .setBookmarkTitle(mNativeBookmarkBridge, id.getId(), id.getType(), title);
    }

    /** Set URL of the given bookmark. */
    public void setBookmarkUrl(BookmarkId id, GURL url) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return;
        assert mIsNativeBookmarkModelLoaded;
        assert id.getType() == BookmarkType.NORMAL;
        BookmarkBridgeJni.get()
                .setBookmarkUrl(mNativeBookmarkBridge, id.getId(), id.getType(), url);
    }

    /**
     * Retrieve the PowerBookmarkMeta for a node if it exists.
     *
     * @param id The {@link BookmarkId} of the bookmark to fetch the meta for.
     * @return The meta or null if none exists.
     */
    public @Nullable PowerBookmarkMeta getPowerBookmarkMeta(@Nullable BookmarkId id) {
        if (mNativeBookmarkBridge == 0) return null;
        if (id == null) return null;
        byte[] protoBytes =
                BookmarkBridgeJni.get()
                        .getPowerBookmarkMeta(mNativeBookmarkBridge, id.getId(), id.getType());

        if (protoBytes == null) return null;

        try {
            return PowerBookmarkMeta.parseFrom(protoBytes);
        } catch (InvalidProtocolBufferException ex) {
            deletePowerBookmarkMeta(id);
            return null;
        }
    }

    /**
     * Set the PowerBookmarkMeta for a node. This MUST be called in order to persist any changes
     * made to the proto in the java layer.
     *
     * @param id The ID of the bookmark to set the meta on.
     * @param meta The meta to store.
     */
    public void setPowerBookmarkMeta(BookmarkId id, PowerBookmarkMeta meta) {
        if (mNativeBookmarkBridge == 0) return;
        if (meta == null) return;
        BookmarkBridgeJni.get()
                .setPowerBookmarkMeta(
                        mNativeBookmarkBridge, id.getId(), id.getType(), meta.toByteArray());
    }

    /**
     * Delete the PowerBookmarkMeta from a node.
     *
     * @param id The ID of the bookmark to remove the meta from.
     */
    public void deletePowerBookmarkMeta(BookmarkId id) {
        if (mNativeBookmarkBridge == 0) return;
        BookmarkBridgeJni.get()
                .deletePowerBookmarkMeta(mNativeBookmarkBridge, id.getId(), id.getType());
    }

    /** Returns whether all of the given {@link BookmarkId}s exist in the current bookmark model. */
    public boolean doAllBookmarksExist(List<BookmarkId> bookmarkIds) {
        ThreadUtils.assertOnUiThread();
        for (BookmarkId bookmarkId : bookmarkIds) {
            if (!doesBookmarkExist(bookmarkId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return Whether the given bookmark exist in the current bookmark model, e.g., not deleted.
     */
    public boolean doesBookmarkExist(BookmarkId id) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return false;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get()
                .doesBookmarkExist(mNativeBookmarkBridge, id.getId(), id.getType());
    }

    /**
     * Fetches the bookmarks of the given folder. This is an always-synchronous version of another
     * getBookmarksForFolder function.
     *
     * @param folderId The parent folder id.
     * @return Bookmarks of the given folder.
     */
    public List<BookmarkItem> getBookmarksForFolder(BookmarkId folderId) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return new ArrayList<>();
        assert mIsNativeBookmarkModelLoaded;
        List<BookmarkItem> result = new ArrayList<>();
        BookmarkBridgeJni.get().getBookmarksForFolder(mNativeBookmarkBridge, folderId, result);
        return result;
    }

    /**
     * Check whether the given folder should be visible. This is for top permanent folders that we
     * want to hide when there is no child.
     *
     * @return Whether the given folder should be visible.
     */
    public boolean isFolderVisible(BookmarkId id) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return false;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get()
                .isFolderVisible(mNativeBookmarkBridge, id.getId(), id.getType());
    }

    /**
     * Deletes a specified bookmark node.
     *
     * @param bookmarkId The ID of the bookmark to be deleted.
     */
    public void deleteBookmark(BookmarkId bookmarkId) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return;
        BookmarkBridgeJni.get().deleteBookmark(mNativeBookmarkBridge, bookmarkId);
    }

    /**
     * Removes all the non-permanent bookmark nodes that are editable by the user. Observers are
     * only notified when all nodes have been removed. There is no notification for individual node
     * removals.
     */
    public void removeAllUserBookmarks() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return;
        assert mIsNativeBookmarkModelLoaded;
        BookmarkBridgeJni.get().removeAllUserBookmarks(mNativeBookmarkBridge);
    }

    /**
     * Move the bookmark to the new index within same folder or to a different folder.
     *
     * @param bookmarkId The id of the bookmark that is being moved.
     * @param newParentId The parent folder id.
     * @param index The new index for the bookmark, this argument is ignored if the types of
     *     bookmarkId and newParentId differ.
     */
    public void moveBookmark(BookmarkId bookmarkId, BookmarkId newParentId, int index) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return;
        assert mIsNativeBookmarkModelLoaded;
        BookmarkBridgeJni.get().moveBookmark(mNativeBookmarkBridge, bookmarkId, newParentId, index);
    }

    /**
     * Add a new folder to the given parent folder
     *
     * @param parent Folder where to add. Must be a normal editable folder, instead of a partner
     *     bookmark folder or a managed bookmark folder or root node of the entire bookmark model.
     * @param index The position to locate the new folder
     * @param title The title text of the new folder
     * @return Id of the added node. If adding failed (index is invalid, string is null, parent is
     *     not editable), returns null.
     */
    public BookmarkId addFolder(BookmarkId parent, int index, String title) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert parent.getType() == BookmarkType.NORMAL;
        assert index >= 0;
        assert title != null;

        return BookmarkBridgeJni.get().addFolder(mNativeBookmarkBridge, parent, index, title);
    }

    /**
     * Add a new bookmark to a specific position below parent.
     *
     * @param parent Folder where to add. Must be a normal editable folder, instead of a partner
     *     bookmark folder or a managed bookmark folder or root node of the entire bookmark model.
     * @param index The position where the bookmark will be placed in parent folder
     * @param title Title of the new bookmark. If empty, the URL will be used as the title.
     * @param url Url of the new bookmark
     * @return Id of the added node. If adding failed (index is invalid, string is null, parent is
     *     not editable), returns null.
     */
    public BookmarkId addBookmark(BookmarkId parent, int index, String title, GURL url) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert parent.getType() == BookmarkType.NORMAL;
        assert index >= 0;
        assert title != null;
        assert url != null;

        recordBookmarkAdded();

        if (TextUtils.isEmpty(title)) title = url.getSpec();
        return BookmarkBridgeJni.get()
                .addBookmark(mNativeBookmarkBridge, parent, index, title, url);
    }

    /** Record the user action for adding a bookmark. */
    private void recordBookmarkAdded() {
        RecordUserAction.record("BookmarkAdded");
    }

    /** Undo the last undoable action on the top of the bookmark undo stack */
    public void undo() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return;
        BookmarkBridgeJni.get().undo(mNativeBookmarkBridge);
    }

    /**
     * Start grouping actions for a single undo operation Note: This only works with BookmarkModel,
     * not partner bookmarks.
     */
    public void startGroupingUndos() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return;
        BookmarkBridgeJni.get().startGroupingUndos(mNativeBookmarkBridge);
    }

    /**
     * End grouping actions for a single undo operation Note: This only works with BookmarkModel,
     * not partner bookmarks.
     */
    public void endGroupingUndos() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return;
        BookmarkBridgeJni.get().endGroupingUndos(mNativeBookmarkBridge);
    }

    public boolean isEditBookmarksEnabled() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return false;
        return BookmarkBridgeJni.get().isEditBookmarksEnabled(mNativeBookmarkBridge);
    }

    /** Notifies the observer that bookmark model has been loaded. */
    @VisibleForTesting
    public void notifyBookmarkModelLoaded() {
        // Call isBookmarkModelLoaded() to do the check since it could be overridden by the child
        // class to add the addition logic.
        if (isBookmarkModelLoaded()) {
            for (BookmarkModelObserver observer : mObservers) {
                observer.bookmarkModelLoaded();
            }
        }
    }

    /**
     * Reorders the bookmarks of the folder "parent" to be as specified by newOrder.
     *
     * @param parent The parent folder for the reordered bookmarks.
     * @param newOrder A list of bookmark IDs that represents the new order for these bookmarks.
     */
    public void reorderBookmarks(BookmarkId parent, long[] newOrder) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return;
        BookmarkBridgeJni.get().reorderChildren(mNativeBookmarkBridge, parent, newOrder);
    }

    /**
     * Adds an item to the default reading list if it doesn't already exist.
     *
     * @param title The title to be used for the reading list item.
     * @param url The URL of the reading list item.
     * @return The bookmark ID created after saving the article to the reading list, or null on
     *     error.
     */
    public @Nullable BookmarkId addToDefaultReadingList(@NonNull String title, @NonNull GURL url) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert title != null;
        assert url != null;
        assert mIsNativeBookmarkModelLoaded;

        return addToReadingList(getLocalOrSyncableReadingListFolder(), title, url);
    }

    /**
     * Adds an item to the given reading list if it doesn't already exist.
     *
     * @param parentId The parent reading list to add to.
     * @param title The title to be used for the reading list item.
     * @param url The URL of the reading list item.
     * @return The bookmark ID created after saving the article to the reading list, or null on
     *     error.
     */
    public @Nullable BookmarkId addToReadingList(
            @NonNull BookmarkId parentId, @NonNull String title, @NonNull GURL url) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert parentId != null;
        assert title != null;
        assert url != null;
        assert mIsNativeBookmarkModelLoaded;

        return BookmarkBridgeJni.get()
                .addToReadingList(mNativeBookmarkBridge, parentId, title, url);
    }

    /**
     * Helper method to mark an item as read.
     *
     * @param id The {@link BookmarkId} to set the status for.
     * @param read Whether the item should be marked as read.
     */
    public void setReadStatusForReadingList(@NonNull BookmarkId id, boolean read) {
        if (mNativeBookmarkBridge == 0) return;
        assert id != null;
        BookmarkBridgeJni.get().setReadStatus(mNativeBookmarkBridge, id, read);
    }

    /**
     * Returns the total number of unread reading list items for the given {@link BookmarkId}.
     *
     * @param readingListParentId 1 of the 2 reading list parent ids.
     */
    public int getUnreadCount(@NonNull BookmarkId readingListParentId) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return 0;
        assert readingListParentId != null;
        return BookmarkBridgeJni.get().getUnreadCount(mNativeBookmarkBridge, readingListParentId);
    }

    /** Returns whether the given {@link BookmarkId} belongs to the account. */
    public boolean isAccountBookmark(BookmarkId id) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return false;
        return BookmarkBridgeJni.get().isAccountBookmark(mNativeBookmarkBridge, id);
    }

    /**
     * Checks whether supplied URL has already been bookmarked.
     *
     * @param url The URL to check.
     * @return Whether the URL has been bookmarked.
     */
    public boolean isBookmarked(GURL url) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return false;
        return BookmarkBridgeJni.get().isBookmarked(mNativeBookmarkBridge, url);
    }

    public BookmarkId getPartnerFolderId() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;

        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get().getPartnerFolderId(mNativeBookmarkBridge);
    }

    // Ecosia: Bookmark Import / Export
    @CalledByNative
    private void onBookmarkImportStarted() {
        new Handler(Looper.getMainLooper()).post(() -> {
            Context context = ContextUtils.getApplicationContext();
            Toast.makeText(context, context.getString(R.string.bookmarks_import_started), Toast.LENGTH_LONG).show();
        });
    }

    // Ecosia: Bookmark Import / Export
    @CalledByNative
    private void onBookmarkImportSuccess(int bookmarks, int searchEngine, String path) {
        Context context = ContextUtils.getApplicationContext();
        Toast.makeText(context, context.getString(R.string.bookmarks_import_success, bookmarks), Toast.LENGTH_LONG).show();

        for (BookmarkModelObserver observer : mObservers) {
            observer.bookmarkModelNeedsReloadAfterBookmarksImport();
        }
    }

    // Ecosia: Bookmark Import / Export
    @CalledByNative
    private void onBookmarkImportError(int id, WindowAndroid window) {
        Activity context = (Activity) window.getContext().get();

        context.runOnUiThread(() -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(window.getContext().get(), R.style.ThemeOverlay_BrowserUI_AlertDialog)
                    .setTitle(R.string.bookmarks_import_error_title)
                    .setPositiveButton(R.string.ok, (dialog1, which) -> {});

            switch (id) {
                case IMPORT_ERROR_INVALID_FILE_FORMAT:
                    dialogBuilder.setMessage(R.string.bookmarks_import_error_1);
                case IMPORT_ERROR_INVALID_FILE_LENGTH_TOO_SMALL:
                    dialogBuilder.setMessage(R.string.bookmarks_import_error_2);
                case IMPORT_ERROR_INVALID_FILE_LENGTH_TOO_LARGE:
                    dialogBuilder.setMessage(R.string.bookmarks_import_error_3);
                case IMPORT_ERROR_INVALID_FILE_COULD_NOT_READ:
                    dialogBuilder.setMessage(R.string.bookmarks_import_error_4);
                case IMPORT_ERROR_INVALID_FILE_EMPTY_CONTENTS:
                    dialogBuilder.setMessage(R.string.bookmarks_import_error_5);
                case IMPORT_ERROR_INVALID_FILE_TYPE:
                    dialogBuilder.setMessage(R.string.bookmarks_import_error_6);

            }
            dialogBuilder.create().show();
        });
    }

    @CalledByNative
    @VisibleForTesting
    void bookmarkModelLoaded() {
        mIsNativeBookmarkModelLoaded = true;
        notifyBookmarkModelLoaded();
    }

    @CalledByNative
    private void destroyFromNative() {
        destroy();
    }

    @CalledByNative
    private void bookmarkNodeMoved(
            BookmarkItem oldParent, int oldIndex, BookmarkItem newParent, int newIndex) {
        if (mIsDoingExtensiveChanges) return;

        for (BookmarkModelObserver observer : mObservers) {
            observer.bookmarkNodeMoved(oldParent, oldIndex, newParent, newIndex);
        }
    }

    @CalledByNative
    private void bookmarkNodeAdded(BookmarkItem parent, int index) {
        if (mIsDoingExtensiveChanges) return;

        for (BookmarkModelObserver observer : mObservers) {
            observer.bookmarkNodeAdded(parent, index);
        }
    }

    @CalledByNative
    private void bookmarkNodeRemoved(BookmarkItem parent, int oldIndex, BookmarkItem node) {
        for (BookmarkModelObserver observer : mObservers) {
            observer.bookmarkNodeRemoved(parent, oldIndex, node, mIsDoingExtensiveChanges);
        }
    }

    @CalledByNative
    private void bookmarkAllUserNodesRemoved() {
        for (BookmarkModelObserver observer : mObservers) {
            observer.bookmarkAllUserNodesRemoved();
        }
    }

    @CalledByNative
    private void bookmarkNodeChanged(BookmarkItem node) {
        if (mIsDoingExtensiveChanges) return;

        for (BookmarkModelObserver observer : mObservers) {
            observer.bookmarkNodeChanged(node);
        }
    }

    @CalledByNative
    private void bookmarkNodeChildrenReordered(BookmarkItem node) {
        if (mIsDoingExtensiveChanges) return;

        for (BookmarkModelObserver observer : mObservers) {
            observer.bookmarkNodeChildrenReordered(node);
        }
    }

    @CalledByNative
    private void extensiveBookmarkChangesBeginning() {
        mIsDoingExtensiveChanges = true;
    }

    @CalledByNative
    private void extensiveBookmarkChangesEnded() {
        mIsDoingExtensiveChanges = false;
        bookmarkModelChanged();
    }

    @CalledByNative
    private void bookmarkModelChanged() {
        if (mIsDoingExtensiveChanges) return;

        for (BookmarkModelObserver observer : mObservers) {
            observer.bookmarkModelChanged();
        }
    }

    @CalledByNative
    private void editBookmarksEnabledChanged() {
        for (BookmarkModelObserver observer : mObservers) {
            observer.editBookmarksEnabledChanged();
        }
    }

    @CalledByNative
    private static BookmarkItem createBookmarkItem(
            long id,
            int type,
            @JniType("std::u16string") String title,
            @JniType("GURL") GURL url,
            boolean isFolder,
            long parentId,
            int parentIdType,
            boolean isEditable,
            boolean isManaged,
            long dateAdded,
            boolean read,
            long dateLastOpened,
            boolean isAccountBookmark) {
        return new BookmarkItem(
                new BookmarkId(id, type),
                title,
                url,
                isFolder,
                new BookmarkId(parentId, parentIdType),
                isEditable,
                isManaged,
                dateAdded,
                read,
                dateLastOpened,
                isAccountBookmark);
    }

    @CalledByNative
    private static void addToList(List<BookmarkItem> bookmarksList, BookmarkItem bookmark) {
        bookmarksList.add(bookmark);
    }

    @CalledByNative
    private static void addToBookmarkIdList(
            List<BookmarkId> bookmarkIdList, long id, @BookmarkType int type) {
        bookmarkIdList.add(new BookmarkId(id, type));
    }

    @CalledByNative
    private static void addToBookmarkIdListWithDepth(
            List<BookmarkId> folderList,
            long id,
            @BookmarkType int type,
            List<Integer> depthList,
            int depth) {
        folderList.add(new BookmarkId(id, type));
        depthList.add(depth);
    }

    @CalledByNative
    private static void clearLastUsedParent() {
        BookmarkUtils.clearLastUsedPrefs();
    }

    private static List<Pair<Integer, Integer>> createPairsList(int[] left, int[] right) {
        List<Pair<Integer, Integer>> pairList = new ArrayList<>();
        for (int i = 0; i < left.length; i++) {
            pairList.add(new Pair<>(left[i], right[i]));
        }
        return pairList;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    @NativeMethods
    public interface Natives {
        BookmarkModel nativeGetForProfile(@JniType("Profile*") Profile profile);

        boolean areAccountBookmarkFoldersActive(long nativeBookmarkBridge);

        BookmarkId getMostRecentlyAddedUserBookmarkIdForUrl(
                long nativeBookmarkBridge, @JniType("GURL") GURL url);

        BookmarkItem getBookmarkById(long nativeBookmarkBridge, long id, int type);

        void getTopLevelFolderIds(
                long nativeBookmarkBridge,
                boolean ignoreVisibility,
                List<BookmarkId> bookmarksList);

        BookmarkId getLocalOrSyncableReadingListFolder(long nativeBookmarkBridge);

        BookmarkId getAccountReadingListFolder(long nativeBookmarkBridge);

        BookmarkId getDefaultReadingListFolder(long nativeBookmarkBridge);

        BookmarkId getDefaultBookmarkFolder(long nativeBookmarkBridge);

        // TODO(crbug.com/41487884): Remove this method.
        void getAllFoldersWithDepths(
                long nativeBookmarkBridge, List<BookmarkId> folderList, List<Integer> depthList);

        BookmarkId getRootFolderId(long nativeBookmarkBridge);

        BookmarkId getMobileFolderId(long nativeBookmarkBridge);

        BookmarkId getOtherFolderId(long nativeBookmarkBridge);

        BookmarkId getDesktopFolderId(long nativeBookmarkBridge);

        BookmarkId getAccountMobileFolderId(long nativeBookmarkBridge);

        BookmarkId getAccountOtherFolderId(long nativeBookmarkBridge);

        BookmarkId getAccountDesktopFolderId(long nativeBookmarkBridge);

        BookmarkId getPartnerFolderId(long nativeBookmarkBridge);

        @JniType("std::string")
        String getBookmarkGuidByIdForTesting( // IN-TEST
                long nativeBookmarkBridge, long id, int type);

        int getChildCount(long nativeBookmarkBridge, long id, int type);

        void getChildIds(
                long nativeBookmarkBridge, long id, int type, List<BookmarkId> bookmarksList);
        void importBookmarks(long nativeBookmarkBridge, BookmarkBridge caller, WindowAndroid window); // Ecosia: Bookmark Import / Export
        void exportBookmarks(long nativeBookmarkBridge, BookmarkBridge caller, WindowAndroid window, // Ecosia: Bookmark Import / Export
                             String export_path);
        BookmarkId getChildAt(long nativeBookmarkBridge, long id, int type, int index);

        int getTotalBookmarkCount(long nativeBookmarkBridge, long id, int type);

        void setBookmarkTitle(
                long nativeBookmarkBridge,
                long id,
                int type,
                @JniType("std::u16string") String title);

        void setBookmarkUrl(
                long nativeBookmarkBridge, long id, int type, @JniType("GURL") GURL url);

        byte[] getPowerBookmarkMeta(long nativeBookmarkBridge, long id, int type);

        void setPowerBookmarkMeta(long nativeBookmarkBridge, long id, int type, byte[] meta);

        void deletePowerBookmarkMeta(long nativeBookmarkBridge, long id, int type);

        boolean doesBookmarkExist(long nativeBookmarkBridge, long id, int type);

        // TODO(crbug.com/41487884): Remove this method.
        void getBookmarksForFolder(
                long nativeBookmarkBridge, BookmarkId folderId, List<BookmarkItem> bookmarksList);

        boolean isFolderVisible(long nativeBookmarkBridge, long id, int type);

        BookmarkId addFolder(
                long nativeBookmarkBridge,
                BookmarkId parent,
                int index,
                @JniType("std::u16string") String title);

        void deleteBookmark(long nativeBookmarkBridge, BookmarkId bookmarkId);

        void removeAllUserBookmarks(long nativeBookmarkBridge);

        void moveBookmark(
                long nativeBookmarkBridge,
                BookmarkId bookmarkId,
                BookmarkId newParentId,
                int index);

        BookmarkId addBookmark(
                long nativeBookmarkBridge,
                BookmarkId parentId,
                int index,
                @JniType("std::u16string") String title,
                @JniType("GURL") GURL url);

        BookmarkId addToReadingList(
                long nativeBookmarkBridge,
                BookmarkId parentId,
                @JniType("std::string") String title,
                @JniType("GURL") GURL url);

        void setReadStatus(long nativeBookmarkBridge, BookmarkId id, boolean read);

        int getUnreadCount(long nativeBookmarkBridge, BookmarkId id);

        boolean isAccountBookmark(long nativeBookmarkBridge, BookmarkId id);

        void undo(long nativeBookmarkBridge);

        void startGroupingUndos(long nativeBookmarkBridge);

        void endGroupingUndos(long nativeBookmarkBridge);

        void loadEmptyPartnerBookmarkShimForTesting(long nativeBookmarkBridge); // IN-TEST

        void loadFakePartnerBookmarkShimForTesting(long nativeBookmarkBridge); // IN-TEST

        void searchBookmarks(
                long nativeBookmarkBridge,
                List<BookmarkId> bookmarkMatches,
                @JniType("std::u16string") String query,
                String[] tags,
                int powerBookmarkType,
                int maxNumber);

        void getBookmarksOfType(
                long nativeBookmarkBridge, List<BookmarkId> bookmarkMatches, int powerBookmarkType);

        boolean isDoingExtensiveChanges(long nativeBookmarkBridge);

        void destroy(long nativeBookmarkBridge);

        boolean isEditBookmarksEnabled(long nativeBookmarkBridge);

        void reorderChildren(long nativeBookmarkBridge, BookmarkId parent, long[] orderedNodes);

        boolean isBookmarked(long nativeBookmarkBridge, @JniType("GURL") GURL url);
    }
}
