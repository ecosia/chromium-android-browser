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

import android.Manifest.permission;
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

import com.google.common.primitives.UnsignedLongs;
import com.google.protobuf.InvalidProtocolBufferException;

import org.chromium.base.ContentUriUtils;
import org.chromium.base.ContextUtils;
import org.chromium.base.ObserverList;
import org.chromium.base.ThreadUtils;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.NativeMethods;
import org.chromium.base.metrics.RecordHistogram;
import org.chromium.base.metrics.RecordUserAction;
import org.chromium.base.task.AsyncTask;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.IntentHandler;
import org.chromium.chrome.browser.commerce.ShoppingFeatures;
import org.chromium.chrome.browser.commerce.ShoppingServiceFactory;
import org.chromium.chrome.browser.document.ChromeLauncherActivity;
import org.chromium.chrome.browser.download.DirectoryOption;
import org.chromium.chrome.browser.download.DownloadLocationDialogType;
import org.chromium.chrome.browser.download.dialogs.DownloadLocationCustomView;
import org.chromium.chrome.browser.download.dialogs.DownloadLocationDialogController;
import org.chromium.chrome.browser.download.dialogs.DownloadLocationDialogCoordinator;
import org.chromium.chrome.browser.flags.ChromeFeatureList;
import org.chromium.chrome.browser.partnerbookmarks.PartnerBookmarksShim;
import org.chromium.chrome.browser.preferences.ChromePreferenceKeys;
import org.chromium.chrome.browser.preferences.SharedPreferencesManager;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.read_later.ReadingListUtils;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.components.bookmarks.BookmarkId;
import org.chromium.components.bookmarks.BookmarkItem;
import org.chromium.components.bookmarks.BookmarkType;
import org.chromium.components.commerce.core.CommerceSubscription;
import org.chromium.components.commerce.core.IdentifierType;
import org.chromium.components.commerce.core.ManagementType;
import org.chromium.components.commerce.core.ShoppingService;
import org.chromium.components.commerce.core.SubscriptionsObserver;
import org.chromium.components.power_bookmarks.PowerBookmarkMeta;
import org.chromium.components.power_bookmarks.PowerBookmarkType;
import org.chromium.components.power_bookmarks.ShoppingSpecifics;
import org.chromium.content_public.browser.WebContents;
import org.chromium.ui.base.PageTransition;
import org.chromium.ui.base.WindowAndroid;
import org.chromium.ui.modaldialog.DialogDismissalCause;
import org.chromium.ui.modaldialog.ModalDialogManager;
import org.chromium.ui.modaldialog.ModalDialogProperties;
import org.chromium.ui.modelutil.PropertyModel;
import org.chromium.ui.widget.Toast;
import org.chromium.url.GURL;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Provides the communication channel for Android to fetch and manipulate the
 * bookmark model stored in native.
 */
class BookmarkBridge {
    private long mNativeBookmarkBridge;
    private boolean mIsDestroyed;
    private boolean mIsDoingExtensiveChanges;
    private boolean mIsNativeBookmarkModelLoaded;
    private final ObserverList<BookmarkModelObserver> mObservers =
            new ObserverList<BookmarkModelObserver>();
    private ShoppingService mShoppingService;
    private SubscriptionsObserver mSubscriptionsObserver;

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
        if (ChromeFeatureList.isEnabled(ChromeFeatureList.BOOKMARKS_EXPORT_USESAF) ||
                Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)
            exportBookmarksImplUseSaf(window);
        else
            exportBookmarksImplUseFile(window, modalDialogManager);
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
        SharedPreferencesManager sharedPrefs = SharedPreferencesManager.getInstance();
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

    private void exportBookmarksImplUseFile(WindowAndroid window, ModalDialogManager modalDialogManager) {
        Context context = window.getContext().get();

        // standard name for bookmark file
        final String standardBookmarkName = "bookmarks.html";

        // use the download ui and standard file saving
        DownloadLocationDialogController controller = new DownloadLocationDialogController() {
            @Override
            public void onDownloadLocationDialogComplete(String returnedPath) {
            }

            @Override
            public void onDownloadLocationDialogCanceled() {
            }
        };

        DownloadLocationDialogCoordinator dialog = new DownloadLocationDialogCoordinator() {
            @Override
            protected void onDirectoryOptionsRetrieved(ArrayList<DirectoryOption> dirs) {
                if (mDialogModel != null) return;

                // Actually show the dialog.
                mCustomView = (DownloadLocationCustomView) LayoutInflater.from(context).inflate(
                        R.layout.download_location_dialog, null);
                mCustomView.initialize(DownloadLocationDialogType.DEFAULT, /*totalBytes*/ 0);
                mCustomView.setTitle(context.getString(R.string.bookmarks_export_alert_title));
                mCustomView.setFileName(standardBookmarkName);
                mCustomView.showDontShowAgainCheckbox(false);

                Resources resources = context.getResources();
                mDialogModel = new PropertyModel.Builder(ModalDialogProperties.ALL_KEYS)
                        .with(ModalDialogProperties.CONTROLLER, this)
                        .with(ModalDialogProperties.CUSTOM_VIEW, mCustomView)
                        .with(ModalDialogProperties.POSITIVE_BUTTON_TEXT, resources,
                                R.string.bookmarks_export_button)
                        .with(ModalDialogProperties.NEGATIVE_BUTTON_TEXT, resources,
                                R.string.cancel)
                        .build();

                mModalDialogManager.showDialog(mDialogModel, ModalDialogManager.ModalDialogType.APP);
            }

            @Override
            public void onDismiss(PropertyModel model, int dismissalCause) {
                switch (dismissalCause) {
                    case DialogDismissalCause.POSITIVE_BUTTON_CLICKED: {
                        String fileName = mCustomView.getFileName();
                        String directory = mCustomView.getDirectoryOption().location;
                        if (fileName != null && directory != null) {
                            File file = new File(directory, fileName);

                            if (window.hasPermission(permission.WRITE_EXTERNAL_STORAGE)) {
                                BookmarkBridgeJni.get().exportBookmarks(mNativeBookmarkBridge,
                                        BookmarkBridge.this, window, file.getPath());
                            } else {
                                String[] requestPermissions = new String[]{permission.WRITE_EXTERNAL_STORAGE};
                                window.requestPermissions(requestPermissions, (permissions, grantResults) -> {
                                    if (grantResults.length >= 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                                        BookmarkBridgeJni.get().exportBookmarks(mNativeBookmarkBridge,
                                                BookmarkBridge.this, window, file.getPath());
                                    }
                                });
                            }
                            ;
                        }
                    }
                    break;
                }
                mDialogModel = null;
                mCustomView = null;
            }
        };
        dialog.initialize(controller);
        dialog.showDialog(context, modalDialogManager, /*totalBytes*/ 0,
                DownloadLocationDialogType.DEFAULT, /*suggestedPath*/ "", /*isIncognito*/ false);
    }
    // Ecosia: Bookmark Import / Export END

    /**
     * Handler to fetch the bookmarks, titles, urls and folder hierarchy.
     * @param profile Profile instance corresponding to the active profile.
     */
    static BookmarkModel getForProfile(Profile profile) {
        ThreadUtils.assertOnUiThread();
        return BookmarkBridgeJni.get().getForProfile(profile);
    }

    @CalledByNative
    static BookmarkModel createBookmarkModel(long nativeBookmarkBridge) {
        return new BookmarkModel(nativeBookmarkBridge);
    }

    BookmarkBridge(long nativeBookmarkBridge) {
        mNativeBookmarkBridge = nativeBookmarkBridge;
        mIsDoingExtensiveChanges = BookmarkBridgeJni.get().isDoingExtensiveChanges(
                mNativeBookmarkBridge, BookmarkBridge.this);
        // TODO(crbug.com/1382191): Move this observer to native.
        mSubscriptionsObserver = new SubscriptionsObserver() {
            @Override
            public void onSubscribe(List<CommerceSubscription> subscriptions, boolean succeeded) {}

            @Override
            public void onUnsubscribe(List<CommerceSubscription> subscriptions, boolean succeeded) {
                if (!succeeded) return;
                removeExplicitShoppingSubscriptions(subscriptions);
            }
        };
        if (ShoppingFeatures.isShoppingListEnabled()) {
            mShoppingService =
                    ShoppingServiceFactory.getForProfile(Profile.getLastUsedRegularProfile());
            mShoppingService.addSubscriptionsObserver(mSubscriptionsObserver);
        }
    }

    @VisibleForTesting
    SubscriptionsObserver getSubscriptionObserver() {
        return mSubscriptionsObserver;
    }

    /**
     * Destroys this instance so no further calls can be executed.
     */
    void destroy() {
        mIsDestroyed = true;
        if (mNativeBookmarkBridge != 0) {
            BookmarkBridgeJni.get().destroy(mNativeBookmarkBridge, BookmarkBridge.this);
            mNativeBookmarkBridge = 0;
            mIsNativeBookmarkModelLoaded = false;
        }
        mObservers.clear();

        if (mShoppingService != null) {
            mShoppingService.removeSubscriptionsObserver(mSubscriptionsObserver);
        }
    }

    /** Returns whether the bridge has been destroyed. */
    private boolean isDestroyed() {
        return mIsDestroyed;
    }

    /**
     * @param tab Tab whose current URL is checked against.
     * @return {@code true} if the current Tab URL has a bookmark associated with it. If the
     *         bookmark backend is not loaded, return {@code false}.
     */
    public boolean hasBookmarkIdForTab(@Nullable Tab tab) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return false;
        return getUserBookmarkIdForTab(tab) != null;
    }

    /**
     * @param tab Tab whose current URL is checked against.
     * @return BookmarkId or {@link null} if bookmark backend is not loaded or the tab is frozen.
     */
    @Nullable
    public BookmarkId getUserBookmarkIdForTab(@Nullable Tab tab) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        if (tab == null || tab.isFrozen() || mNativeBookmarkBridge == 0) return null;
        return BookmarkBridgeJni.get().getBookmarkIdForWebContents(
                mNativeBookmarkBridge, this, tab.getWebContents(), true);
    }

    /**
     * Load an empty partner bookmark shim for testing. The root node for bookmark will be an
     * empty node.
     */
    @VisibleForTesting
    public void loadEmptyPartnerBookmarkShimForTesting() {
        BookmarkBridgeJni.get().loadEmptyPartnerBookmarkShimForTesting(
                mNativeBookmarkBridge, BookmarkBridge.this);
    }

    /**
     * Load a fake partner bookmark shim for testing. To see (or edit) the titles and URLs of the
     * partner bookmarks, go to bookmark_bridge.cc.
     */
    @VisibleForTesting
    public void loadFakePartnerBookmarkShimForTesting() {
        BookmarkBridgeJni.get().loadFakePartnerBookmarkShimForTesting(
                mNativeBookmarkBridge, BookmarkBridge.this);
    }

    /**
     * Add an observer to bookmark model changes.
     * @param observer The observer to be added.
     */
    public void addObserver(BookmarkModelObserver observer) {
        mObservers.addObserver(observer);
    }

    /**
     * Remove an observer of bookmark model changes.
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
     * Schedules a runnable to run after the bookmark model is loaded. If the
     * model is already loaded, executes the runnable immediately. If not, also
     * kick off partner bookmark reading.
     * @return Whether the given runnable is executed synchronously.
     */
    public boolean finishLoadingBookmarkModel(final Runnable runAfterModelLoaded) {
        if (isBookmarkModelLoaded()) {
            runAfterModelLoaded.run();
            return true;
        }

        long startTime = SystemClock.elapsedRealtime();
        addObserver(new BookmarkModelObserver() {
            @Override
            public void bookmarkModelLoaded() {
                removeObserver(this);
                RecordHistogram.recordTimesHistogram(
                        "PartnerBookmark.LoadingTime", SystemClock.elapsedRealtime() - startTime);
                runAfterModelLoaded.run();
            }
            @Override
            public void bookmarkModelChanged() {
            }

            // Ecosia: Bookmark Import / Export
            @Override
            public void bookmarkModelNeedsReloadAfterBookmarksImport() {
                // no action required here
            }
        });

        // Start reading as a fail-safe measure to avoid waiting forever if the caller forgets to
        // call kickOffReading().
        PartnerBookmarksShim.kickOffReading(ContextUtils.getApplicationContext());
        return false;
    }

    /**
     * Gets the {@link BookmarkItem} which is referenced by the given {@link BookmarkId}.
     * @param id The {@link BookmarkId} used to lookup the corresponding {@link BookmarkItem}.
     * @return A BookmarkItem instance for the given BookmarkId.
     *         <code>null</code> if it doesn't exist.
     */
    @Nullable
    public BookmarkItem getBookmarkById(@Nullable BookmarkId id) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        if (id == null) return null;

        if (BookmarkId.SHOPPING_FOLDER.equals(id)) {
            return new BookmarkItem(id, /*title=*/null, /*url=*/null,
                    /*isFolder=*/true, /*parentId=*/getRootFolderId(), /*isEditable=*/false,
                    /*isManaged=*/false, /*dateAdded=*/0L, /*read=*/false);
        }

        return BookmarkBridgeJni.get().getBookmarkByID(
                mNativeBookmarkBridge, BookmarkBridge.this, id.getId(), id.getType());
    }

    /**
     * @return The top level folder's parents.
     */
    public List<BookmarkId> getTopLevelFolderParentIDs() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return new ArrayList<BookmarkId>();
        assert mIsNativeBookmarkModelLoaded;
        List<BookmarkId> result = new ArrayList<BookmarkId>();
        BookmarkBridgeJni.get().getTopLevelFolderParentIDs(
                mNativeBookmarkBridge, BookmarkBridge.this, result);
        return result;
    }

    /**
     * @param getSpecial Whether special top folders should be returned.
     * @param getNormal  Whether normal top folders should be returned.
     * @return The top level folders. Note that special folders come first and normal top folders
     *         will be in the alphabetical order.
     */
    public List<BookmarkId> getTopLevelFolderIDs(boolean getSpecial, boolean getNormal) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return new ArrayList<BookmarkId>();
        assert mIsNativeBookmarkModelLoaded;
        List<BookmarkId> result = new ArrayList<BookmarkId>();
        BookmarkBridgeJni.get().getTopLevelFolderIDs(
                mNativeBookmarkBridge, BookmarkBridge.this, getSpecial, getNormal, result);
        return result;
    }

    /** Returns the synthetic reading list folder. */
    public BookmarkId getReadingListFolder() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get().getReadingListFolder(
                mNativeBookmarkBridge, BookmarkBridge.this);
    }

    /**
     * Populates folderList with BookmarkIds of folders users can move bookmarks
     * to and all folders have corresponding depth value in depthList. Folders
     * having depths of 0 will be shown as top-layered folders. These include
     * "Desktop Folder" itself as well as all children of "mobile" and "other".
     * Children of 0-depth folders have depth of 1, and so on.
     *
     * The result list will be sorted alphabetically by title. "mobile", "other",
     * root node, managed folder, partner folder are NOT included as results.
     */
    @VisibleForTesting
    public void getAllFoldersWithDepths(List<BookmarkId> folderList,
            List<Integer> depthList) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return;
        assert mIsNativeBookmarkModelLoaded;
        BookmarkBridgeJni.get().getAllFoldersWithDepths(
                mNativeBookmarkBridge, BookmarkBridge.this, folderList, depthList);
    }

    /**
     * Calls {@link #getAllFoldersWithDepths(List, List)} and remove all folders and children
     * in bookmarksToMove. This method is useful when finding a list of possible parent folers when
     * moving some folders (a folder cannot be moved to its own children).
     */
    public void getMoveDestinations(List<BookmarkId> folderList,
            List<Integer> depthList, List<BookmarkId> bookmarksToMove) {
        if (mNativeBookmarkBridge == 0) return;
        ThreadUtils.assertOnUiThread();
        assert mIsNativeBookmarkModelLoaded;
        BookmarkBridgeJni.get().getAllFoldersWithDepths(
                mNativeBookmarkBridge, BookmarkBridge.this, folderList, depthList);
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

    /**
     * @return The BookmarkId for root folder node
     */
    public BookmarkId getRootFolderId() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get().getRootFolderId(mNativeBookmarkBridge, BookmarkBridge.this);
    }

    /**
     * @return The BookmarkId for Mobile folder node
     */
    public BookmarkId getMobileFolderId() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get().getMobileFolderId(
                mNativeBookmarkBridge, BookmarkBridge.this);
    }

    /**
     * @return Id representing the special "other" folder from bookmark model.
     */
    public BookmarkId getOtherFolderId() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get().getOtherFolderId(mNativeBookmarkBridge, BookmarkBridge.this);
    }

    /**
     * @return BookmarkId representing special "desktop" folder, namely "bookmark bar".
     */
    public BookmarkId getDesktopFolderId() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get().getDesktopFolderId(
                mNativeBookmarkBridge, BookmarkBridge.this);
    }

    /**
     * Gets Bookmark GUID which is immutable and differs from the BookmarkId in that it is
     * consistent across different clients and stable throughout the lifetime of the bookmark, with
     * the exception of nodes added to the Managed Bookmarks folder, whose GUIDs are re-assigned at
     * start-up every time.
     *
     * @return Bookmark GUID of the given node.
     */
    @VisibleForTesting
    public String getBookmarkGuidByIdForTesting(BookmarkId id) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get().getBookmarkGuidByIdForTesting(
                mNativeBookmarkBridge, BookmarkBridge.this, id.getId(), id.getType());
    }

    /**
     * @return The number of children that the given node has.
     */
    public int getChildCount(BookmarkId id) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return 0;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get().getChildCount(
                mNativeBookmarkBridge, BookmarkBridge.this, id.getId(), id.getType());
    }

    /**
     * Reads sub-folder IDs, sub-bookmark IDs, or both of the given folder.
     *
     * @return Child IDs of the given folder, with the specified type.
     */
    public List<BookmarkId> getChildIDs(BookmarkId id) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return new ArrayList<BookmarkId>();
        assert mIsNativeBookmarkModelLoaded;
        if (BookmarkId.SHOPPING_FOLDER.equals(id)) {
            return searchBookmarks("", null, PowerBookmarkType.SHOPPING, -1);
        }
        List<BookmarkId> result = new ArrayList<BookmarkId>();
        BookmarkBridgeJni.get().getChildIDs(
                mNativeBookmarkBridge, BookmarkBridge.this, id.getId(), id.getType(), result);
        return result;
    }

    /**
     * Disable price tracking for the list of subscriptions. This flips the bit in ShoppingSpecifics
     * but does not actually unsubscribe from the subscription service -- this assumes that is
     * already done.
     * TODO(1284730): This should live somewhere other than BookmarkBridge.
     *
     * @param subscriptions The list of subscriptions to disable.
     */
    private void removeExplicitShoppingSubscriptions(List<CommerceSubscription> subscriptions) {
        if (mNativeBookmarkBridge == 0) return;
        if (subscriptions == null) return;

        List<BookmarkId> products = searchBookmarks("", null, PowerBookmarkType.SHOPPING, -1);
        if (products == null || products.size() == 0) return;

        // Put the offer and cluster IDs into a map so they can be quickly checked.
        HashSet<Long> offerIdMap = new HashSet<>();
        HashSet<Long> clusterIdMap = new HashSet<>();
        for (CommerceSubscription c : subscriptions) {
            // Ensure the subscription is explicit.
            if (c == null || c.managementType != ManagementType.USER_MANAGED) {
                continue;
            }

            if (c.idType == IdentifierType.OFFER_ID) {
                offerIdMap.add(UnsignedLongs.parseUnsignedLong(c.id));
            } else if (c.idType == IdentifierType.PRODUCT_CLUSTER_ID) {
                clusterIdMap.add(UnsignedLongs.parseUnsignedLong(c.id));
            }
        }

        // Look at all the products the user has saved to find any of the above product or cluster
        // IDs.
        for (BookmarkId product : products) {
            PowerBookmarkMeta meta = getPowerBookmarkMeta(product);
            if (!meta.hasShoppingSpecifics()) continue;

            ShoppingSpecifics specifics = meta.getShoppingSpecifics();
            if (offerIdMap.contains(specifics.getOfferId())
                    || clusterIdMap.contains(specifics.getProductClusterId())) {
                // Reset the meta using a copy of the existing one, but set the price tracking flag
                // to false.
                setPowerBookmarkMeta(product,
                        PowerBookmarkMeta.newBuilder(meta)
                                .setShoppingSpecifics(
                                        ShoppingSpecifics.newBuilder(meta.getShoppingSpecifics())
                                                .setIsPriceTracked(false)
                                                .build())
                                .build());
            }
        }
    }

    /**
     * Gets the child of a folder at the specific position.
     * @param folderId Id of the parent folder
     * @param index Position of child among all children in folder
     * @return BookmarkId of the child, which will be null if folderId does not point to a folder or
     *         index is invalid.
     */
    public BookmarkId getChildAt(BookmarkId folderId, int index) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get().getChildAt(mNativeBookmarkBridge, BookmarkBridge.this,
                folderId.getId(), folderId.getType(), index);
    }

    /**
     * Get the total number of bookmarks in the sub tree of the specified folder.
     * @param id The {@link BookmarkId} of the folder to be queried.
     * @return The total number of bookmarks in the folder.
     */
    public int getTotalBookmarkCount(BookmarkId id) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return 0;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get().getTotalBookmarkCount(
                mNativeBookmarkBridge, BookmarkBridge.this, id.getId(), id.getType());
    }

    /**
     * Synchronously gets a list of bookmarks that match the specified search query.
     * @param query Keyword used for searching bookmarks.
     * @param maxNumberOfResult Maximum number of result to fetch.
     * @return List of bookmark IDs that are related to the given query.
     */
    public List<BookmarkId> searchBookmarks(String query, int maxNumberOfResult) {
        return searchBookmarks(query, null, null, maxNumberOfResult);
    }

    /**
     * Synchronously gets a list of bookmarks that match the specified search query.
     * @param query Keyword used for searching bookmarks.
     * @param tags A list of tags the resulting bookmarks should have.
     * @param powerBookmarkType The type of power bookmark type to search for (or null for all).
     * @param maxNumberOfResult Maximum number of result to fetch.
     * @return List of bookmark IDs that are related to the given query.
     */
    public List<BookmarkId> searchBookmarks(String query, @Nullable String[] tags,
            @Nullable PowerBookmarkType powerBookmarkType, int maxNumberOfResult) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return new ArrayList<BookmarkId>();
        List<BookmarkId> bookmarkMatches = new ArrayList<>();
        int typeInt = powerBookmarkType == null ? -1 : powerBookmarkType.getNumber();
        BookmarkBridgeJni.get().searchBookmarks(mNativeBookmarkBridge, BookmarkBridge.this,
                bookmarkMatches, query, tags, typeInt, maxNumberOfResult);
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
            SharedPreferencesManager sharedPrefs = SharedPreferencesManager.getInstance();
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
     * @param powerBookmarkType The type of power bookmark type to search for (or null for all).
     * @return List of bookmark IDs that are related to the given query.
     */
    public List<BookmarkId> getBookmarksOfType(@NonNull PowerBookmarkType powerBookmarkType) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return new ArrayList<BookmarkId>();
        List<BookmarkId> bookmarkMatches = new ArrayList<>();
        int typeInt = powerBookmarkType.getNumber();
        BookmarkBridgeJni.get().getBookmarksOfType(
                mNativeBookmarkBridge, BookmarkBridge.this, bookmarkMatches, typeInt);
        return bookmarkMatches;
    }

    /**
     * Set title of the given bookmark.
     */
    public void setBookmarkTitle(BookmarkId id, String title) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return;
        assert mIsNativeBookmarkModelLoaded;
        BookmarkBridgeJni.get().setBookmarkTitle(
                mNativeBookmarkBridge, BookmarkBridge.this, id.getId(), id.getType(), title);
    }

    /**
     * Set URL of the given bookmark.
     */
    public void setBookmarkUrl(BookmarkId id, GURL url) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return;
        assert mIsNativeBookmarkModelLoaded;
        assert id.getType() == BookmarkType.NORMAL;
        BookmarkBridgeJni.get().setBookmarkUrl(
                mNativeBookmarkBridge, BookmarkBridge.this, id.getId(), id.getType(), url);
    }

    /**
     * Retrieve the PowerBookmarkMeta for a node if it exists.
     * @param id The {@link BookmarkId} of the bookmark to fetch the meta for.
     * @return The meta or null if none exists.
     */
    public @Nullable PowerBookmarkMeta getPowerBookmarkMeta(@Nullable BookmarkId id) {
        if (mNativeBookmarkBridge == 0) return null;
        if (id == null) return null;
        byte[] protoBytes = BookmarkBridgeJni.get().getPowerBookmarkMeta(
                mNativeBookmarkBridge, this, id.getId(), id.getType());

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
     * @param id The ID of the bookmark to set the meta on.
     * @param meta The meta to store.
     */
    public void setPowerBookmarkMeta(BookmarkId id, PowerBookmarkMeta meta) {
        if (mNativeBookmarkBridge == 0) return;
        if (meta == null) return;
        BookmarkBridgeJni.get().setPowerBookmarkMeta(mNativeBookmarkBridge, BookmarkBridge.this,
                id.getId(), id.getType(), meta.toByteArray());
    }

    /**
     * Delete the PowerBookmarkMeta from a node.
     * @param id The ID of the bookmark to remove the meta from.
     */
    public void deletePowerBookmarkMeta(BookmarkId id) {
        if (mNativeBookmarkBridge == 0) return;
        BookmarkBridgeJni.get().deletePowerBookmarkMeta(
                mNativeBookmarkBridge, BookmarkBridge.this, id.getId(), id.getType());
    }

    /**
     * @return Whether the given bookmark exist in the current bookmark model, e.g., not deleted.
     */
    public boolean doesBookmarkExist(BookmarkId id) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return false;
        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get().doesBookmarkExist(
                mNativeBookmarkBridge, BookmarkBridge.this, id.getId(), id.getType());
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
        if (mNativeBookmarkBridge == 0) return new ArrayList<BookmarkItem>();
        assert mIsNativeBookmarkModelLoaded;
        List<BookmarkItem> result = new ArrayList<BookmarkItem>();
        BookmarkBridgeJni.get().getBookmarksForFolder(
                mNativeBookmarkBridge, BookmarkBridge.this, folderId, result);
        return result;
    }

    /**
     * Check whether the given folder should be visible. This is for top permanent folders that we
     * want to hide when there is no child.
     * @return Whether the given folder should be visible.
     */
    public boolean isFolderVisible(BookmarkId id) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return false;
        assert mIsNativeBookmarkModelLoaded;
        if (ReadingListUtils.isSwappableReadingListItem(id)) {
            return true;
        }
        return BookmarkBridgeJni.get().isFolderVisible(
                mNativeBookmarkBridge, BookmarkBridge.this, id.getId(), id.getType());
    }

    /**
     * Deletes a specified bookmark node.
     * @param bookmarkId The ID of the bookmark to be deleted.
     */
    public void deleteBookmark(BookmarkId bookmarkId) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return;
        BookmarkBridgeJni.get().deleteBookmark(
                mNativeBookmarkBridge, BookmarkBridge.this, bookmarkId);
    }

    /**
     * Removes all the non-permanent bookmark nodes that are editable by the user. Observers are
     * only notified when all nodes have been removed. There is no notification for individual node
     * removals.
     */
    public void removeAllUserBookmarks() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return;
        BookmarkBridgeJni.get().removeAllUserBookmarks(mNativeBookmarkBridge, BookmarkBridge.this);
    }

    /**
     * Move the bookmark to the new index within same folder or to a different folder.
     * @param bookmarkId The id of the bookmark that is being moved.
     * @param newParentId The parent folder id.
     * @param index The new index for the bookmark.
     */
    public void moveBookmark(BookmarkId bookmarkId, BookmarkId newParentId, int index) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return;
        BookmarkBridgeJni.get().moveBookmark(
                mNativeBookmarkBridge, BookmarkBridge.this, bookmarkId, newParentId, index);
    }

    /**
     * Add a new folder to the given parent folder
     *
     * @param parent Folder where to add. Must be a normal editable folder, instead of a partner
     *               bookmark folder or a managed bookmark folder or root node of the entire
     *               bookmark model.
     * @param index The position to locate the new folder
     * @param title The title text of the new folder
     * @return Id of the added node. If adding failed (index is invalid, string is null, parent is
     *         not editable), returns null.
     */
    public BookmarkId addFolder(BookmarkId parent, int index, String title) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert parent.getType() == BookmarkType.NORMAL;
        assert index >= 0;
        assert title != null;

        return BookmarkBridgeJni.get().addFolder(
                mNativeBookmarkBridge, BookmarkBridge.this, parent, index, title);
    }

    /**
     * Add a new bookmark to a specific position below parent.
     *
     * @param parent Folder where to add. Must be a normal editable folder, instead of a partner
     *               bookmark folder or a managed bookmark folder or root node of the entire
     *               bookmark model.
     * @param index The position where the bookmark will be placed in parent folder
     * @param title Title of the new bookmark. If empty, the URL will be used as the title.
     * @param url Url of the new bookmark
     * @return Id of the added node. If adding failed (index is invalid, string is null, parent is
     *         not editable), returns null.
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
        return BookmarkBridgeJni.get().addBookmark(
                mNativeBookmarkBridge, this, parent, index, title, url);
    }

    /** Record the user action for adding a bookmark. */
    private void recordBookmarkAdded() {
        RecordUserAction.record("BookmarkAdded");
    }

    /**
     * Undo the last undoable action on the top of the bookmark undo stack
     */
    public void undo() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return;
        BookmarkBridgeJni.get().undo(mNativeBookmarkBridge, BookmarkBridge.this);
    }

    /**
     * Start grouping actions for a single undo operation
     * Note: This only works with BookmarkModel, not partner bookmarks.
     */
    public void startGroupingUndos() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return;
        BookmarkBridgeJni.get().startGroupingUndos(mNativeBookmarkBridge, BookmarkBridge.this);
    }

    /**
     * End grouping actions for a single undo operation
     * Note: This only works with BookmarkModel, not partner bookmarks.
     */
    public void endGroupingUndos() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return;
        BookmarkBridgeJni.get().endGroupingUndos(mNativeBookmarkBridge, BookmarkBridge.this);
    }

    public boolean isEditBookmarksEnabled() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return false;
        return BookmarkBridgeJni.get().isEditBookmarksEnabled(mNativeBookmarkBridge);
    }

    /**
     * Notifies the observer that bookmark model has been loaded.
     */
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
        BookmarkBridgeJni.get().reorderChildren(
                mNativeBookmarkBridge, BookmarkBridge.this, parent, newOrder);
    }

    /**
     * Adds an article to the reading list. If the article was already bookmarked, the existing
     * bookmark ID will be returned.
     * @param title The title to be used for the reading list item.
     * @param url The URL of the reading list item.
     * @return The bookmark ID created after saving the article to the reading list, or null on
     *         error.
     */
    public @Nullable BookmarkId addToReadingList(String title, GURL url) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert title != null;
        assert url != null;
        assert mIsNativeBookmarkModelLoaded;

        return BookmarkBridgeJni.get().addToReadingList(
                mNativeBookmarkBridge, BookmarkBridge.this, title, url);
    }

    /**
     * @param url The URL of the reading list item.
     * @return The reading list item with the URL, or null if no such reading list item.
     */
    public BookmarkItem getReadingListItem(GURL url) {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;
        assert url != null;
        assert mIsNativeBookmarkModelLoaded;

        return BookmarkBridgeJni.get().getReadingListItem(
                mNativeBookmarkBridge, BookmarkBridge.this, url);
    }

    /**
     * Helper method to mark an article as read.
     * @param url The URL of the reading list item.
     * @param read Whether the article should be marked as read.
     */
    public void setReadStatusForReadingList(GURL url, boolean read) {
        if (mNativeBookmarkBridge == 0) return;
        BookmarkBridgeJni.get().setReadStatus(
                mNativeBookmarkBridge, BookmarkBridge.this, url, read);
    }

    /**
     * Checks whether supplied URL has already been bookmarked.
     * @param url The URL to check.
     * @return Whether the URL has been bookmarked.
     */
    public boolean isBookmarked(GURL url) {
        if (mNativeBookmarkBridge == 0) return false;
        return BookmarkBridgeJni.get().isBookmarked(mNativeBookmarkBridge, url);
    }

    public BookmarkId getPartnerFolderId() {
        ThreadUtils.assertOnUiThread();
        if (mNativeBookmarkBridge == 0) return null;

        assert mIsNativeBookmarkModelLoaded;
        return BookmarkBridgeJni.get().getPartnerFolderId(
                mNativeBookmarkBridge, BookmarkBridge.this);
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
    private void bookmarkModelLoaded() {
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
            observer.bookmarkNodeRemoved(parent, oldIndex, node,
                    mIsDoingExtensiveChanges);
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
    private static BookmarkItem createBookmarkItem(long id, int type, String title, GURL url,
            boolean isFolder, long parentId, int parentIdType, boolean isEditable,
            boolean isManaged, long dateAdded, boolean read) {
        return new BookmarkItem(new BookmarkId(id, type), title, url, isFolder,
                new BookmarkId(parentId, parentIdType), isEditable, isManaged, dateAdded, read);
    }

    @CalledByNative
    private static void addToList(List<BookmarkItem> bookmarksList, BookmarkItem bookmark) {
        bookmarksList.add(bookmark);
    }

    @CalledByNative
    private static void addToBookmarkIdList(List<BookmarkId> bookmarkIdList, long id, int type) {
        bookmarkIdList.add(new BookmarkId(id, type));
    }

    @CalledByNative
    private static void addToBookmarkIdListWithDepth(List<BookmarkId> folderList, long id,
            int type, List<Integer> depthList, int depth) {
        folderList.add(new BookmarkId(id, type));
        depthList.add(depth);
    }

    private static List<Pair<Integer, Integer>> createPairsList(int[] left, int[] right) {
        List<Pair<Integer, Integer>> pairList = new ArrayList<Pair<Integer, Integer>>();
        for (int i = 0; i < left.length; i++) {
            pairList.add(new Pair<Integer, Integer>(left[i], right[i]));
        }
        return pairList;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    @NativeMethods
    public interface Natives {
        BookmarkModel getForProfile(Profile profile);
        BookmarkId getBookmarkIdForWebContents(long nativeBookmarkBridge, BookmarkBridge caller,
                WebContents webContents, boolean onlyEditable);
        BookmarkItem getBookmarkByID(
                long nativeBookmarkBridge, BookmarkBridge caller, long id, int type);
        void getTopLevelFolderParentIDs(
                long nativeBookmarkBridge, BookmarkBridge caller, List<BookmarkId> bookmarksList);
        void getTopLevelFolderIDs(long nativeBookmarkBridge, BookmarkBridge caller,
                boolean getSpecial, boolean getNormal, List<BookmarkId> bookmarksList);
        BookmarkId getReadingListFolder(long nativeBookmarkBridge, BookmarkBridge caller);
        void getAllFoldersWithDepths(long nativeBookmarkBridge, BookmarkBridge caller,
                List<BookmarkId> folderList, List<Integer> depthList);
        BookmarkId getRootFolderId(long nativeBookmarkBridge, BookmarkBridge caller);
        BookmarkId getMobileFolderId(long nativeBookmarkBridge, BookmarkBridge caller);
        BookmarkId getOtherFolderId(long nativeBookmarkBridge, BookmarkBridge caller);
        BookmarkId getDesktopFolderId(long nativeBookmarkBridge, BookmarkBridge caller);
        BookmarkId getPartnerFolderId(long nativeBookmarkBridge, BookmarkBridge caller);
        String getBookmarkGuidByIdForTesting(
                long nativeBookmarkBridge, BookmarkBridge caller, long id, int type);
        int getChildCount(long nativeBookmarkBridge, BookmarkBridge caller, long id, int type);
        void getChildIDs(long nativeBookmarkBridge, BookmarkBridge caller, long id, int type,
                List<BookmarkId> bookmarksList);
        void importBookmarks(long nativeBookmarkBridge, BookmarkBridge caller, WindowAndroid window); // Ecosia: Bookmark Import / Export
        void exportBookmarks(long nativeBookmarkBridge, BookmarkBridge caller, WindowAndroid window, // Ecosia: Bookmark Import / Export
                             String export_path);
        BookmarkId getChildAt(
                long nativeBookmarkBridge, BookmarkBridge caller, long id, int type, int index);
        int getTotalBookmarkCount(
                long nativeBookmarkBridge, BookmarkBridge caller, long id, int type);
        void setBookmarkTitle(
                long nativeBookmarkBridge, BookmarkBridge caller, long id, int type, String title);
        void setBookmarkUrl(
                long nativeBookmarkBridge, BookmarkBridge caller, long id, int type, GURL url);
        byte[] getPowerBookmarkMeta(
                long nativeBookmarkBridge, BookmarkBridge caller, long id, int type);
        void setPowerBookmarkMeta(
                long nativeBookmarkBridge, BookmarkBridge caller, long id, int type, byte[] meta);
        void deletePowerBookmarkMeta(
                long nativeBookmarkBridge, BookmarkBridge caller, long id, int type);
        boolean doesBookmarkExist(
                long nativeBookmarkBridge, BookmarkBridge caller, long id, int type);
        void getBookmarksForFolder(long nativeBookmarkBridge, BookmarkBridge caller,
                BookmarkId folderId, List<BookmarkItem> bookmarksList);
        boolean isFolderVisible(
                long nativeBookmarkBridge, BookmarkBridge caller, long id, int type);
        BookmarkId addFolder(long nativeBookmarkBridge, BookmarkBridge caller, BookmarkId parent,
                int index, String title);
        void deleteBookmark(
                long nativeBookmarkBridge, BookmarkBridge caller, BookmarkId bookmarkId);
        void removeAllUserBookmarks(long nativeBookmarkBridge, BookmarkBridge caller);
        void moveBookmark(long nativeBookmarkBridge, BookmarkBridge caller, BookmarkId bookmarkId,
                BookmarkId newParentId, int index);
        BookmarkId addBookmark(long nativeBookmarkBridge, BookmarkBridge caller, BookmarkId parent,
                int index, String title, GURL url);
        BookmarkId addToReadingList(
                long nativeBookmarkBridge, BookmarkBridge caller, String title, GURL url);
        BookmarkItem getReadingListItem(long nativeBookmarkBridge, BookmarkBridge caller, GURL url);
        void setReadStatus(
                long nativeBookmarkBridge, BookmarkBridge caller, GURL url, boolean read);
        void undo(long nativeBookmarkBridge, BookmarkBridge caller);
        void startGroupingUndos(long nativeBookmarkBridge, BookmarkBridge caller);
        void endGroupingUndos(long nativeBookmarkBridge, BookmarkBridge caller);
        void loadEmptyPartnerBookmarkShimForTesting(
                long nativeBookmarkBridge, BookmarkBridge caller);
        void loadFakePartnerBookmarkShimForTesting(
                long nativeBookmarkBridge, BookmarkBridge caller);
        void searchBookmarks(long nativeBookmarkBridge, BookmarkBridge caller,
                List<BookmarkId> bookmarkMatches, String query, String[] tags,
                int powerBookmarkType, int maxNumber);
        void getBookmarksOfType(long nativeBookmarkBridge, BookmarkBridge caller,
                List<BookmarkId> bookmarkMatches, int powerBookmarkType);
        boolean isDoingExtensiveChanges(long nativeBookmarkBridge, BookmarkBridge caller);
        void destroy(long nativeBookmarkBridge, BookmarkBridge caller);
        boolean isEditBookmarksEnabled(long nativeBookmarkBridge);
        void reorderChildren(long nativeBookmarkBridge, BookmarkBridge caller, BookmarkId parent,
                long[] orderedNodes);
        boolean isBookmarked(long nativeBookmarkBridge, GURL url);
    }
}
