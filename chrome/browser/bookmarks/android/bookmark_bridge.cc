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
 

#include "chrome/browser/bookmarks/android/bookmark_bridge.h"

#include <stddef.h>
#include <stdint.h>

#include <algorithm>
#include <memory>
#include <queue>
#include <string>
#include <utility>
#include <vector>

#include "base/android/jni_array.h"
#include "base/android/jni_string.h"
#include "base/containers/adapters.h"
#include "base/containers/stack.h"
#include "base/feature_list.h"
#include "base/functional/bind.h"
#include "base/guid.h"
#include "base/i18n/string_compare.h"
#include "base/memory/raw_ptr.h"
#include "base/strings/utf_string_conversions.h"
#include "chrome/android/chrome_jni_headers/BookmarkBridge_jni.h"
#include "chrome/browser/android/bookmarks/partner_bookmarks_reader.h"
#include "chrome/browser/android/reading_list/reading_list_manager_factory.h"
#include "chrome/browser/bookmarks/bookmark_model_factory.h"
#include "chrome/browser/bookmarks/managed_bookmark_service_factory.h"
#include "chrome/browser/commerce/shopping_service_factory.h"
#include "chrome/browser/profiles/incognito_helpers.h"
#include "chrome/browser/profiles/profile.h"
#include "chrome/browser/profiles/profile_android.h"
#include "chrome/browser/signin/identity_manager_factory.h"
#include "chrome/browser/undo/bookmark_undo_service_factory.h"
#include "components/bookmarks/browser/bookmark_model.h"
#include "components/bookmarks/browser/bookmark_utils.h"
#include "components/bookmarks/browser/scoped_group_bookmark_actions.h"
#include "components/bookmarks/browser/titled_url_match.h"
#include "components/bookmarks/common/android/bookmark_type.h"
#include "components/bookmarks/common/bookmark_metrics.h"
#include "components/bookmarks/common/bookmark_pref_names.h"
#include "components/bookmarks/managed/managed_bookmark_service.h"
#include "components/dom_distiller/core/url_utils.h"
#include "components/power_bookmarks/core/power_bookmark_utils.h"
#include "components/power_bookmarks/core/proto/power_bookmark_meta.pb.h"
#include "components/prefs/pref_service.h"
#include "components/query_parser/query_parser.h"
#include "components/signin/public/identity_manager/identity_manager.h"
#include "components/undo/bookmark_undo_service.h"
#include "components/undo/undo_manager.h"
#include "content/public/browser/browser_thread.h"
#include "content/public/browser/web_contents.h"
// Ecosia: Bookmark Import / Export BEGIN
#include "base/android/content_uri_utils.h"
#include "base/android/path_utils.h"
#include "base/strings/utf_string_conversions.h"
#include "chrome/utility/importer/bookmark_html_reader.h"
#include "chrome/browser/bookmarks/bookmark_html_writer.h"
#include "chrome/browser/importer/profile_writer.h"
#include "chrome/browser/platform_util.h"
#include "chrome/browser/ui/chrome_select_file_policy.h"
#include "chrome/common/importer/imported_bookmark_entry.h"
#include "chrome/common/importer/importer_data_types.h"
#include "chrome/common/url_constants.h"
#include "components/favicon_base/favicon_usage_data.h"
#include "components/search_engines/template_url.h"
#include "components/url_formatter/url_fixer.h"
#include "ui/android/window_android.h"
#include "base/task/task_traits.h"
#include "base/task/thread_pool.h"
#include "content/public/browser/browser_task_traits.h"
// Ecosia: Bookmark Import / Export END

using base::android::AttachCurrentThread;
using base::android::ConvertUTF16ToJavaString;
using base::android::ConvertUTF8ToJavaString;
using base::android::JavaParamRef;
using base::android::JavaRef;
using base::android::ScopedJavaGlobalRef;
using base::android::ScopedJavaLocalRef;
using base::android::ToJavaIntArray;
using bookmarks::BookmarkModel;
using bookmarks::BookmarkNode;
using bookmarks::BookmarkType;
using bookmarks::android::JavaBookmarkIdCreateBookmarkId;
using bookmarks::android::JavaBookmarkIdGetId;
using bookmarks::android::JavaBookmarkIdGetType;
using content::BrowserThread;
using power_bookmarks::PowerBookmarkMeta;

// Ecosia: Bookmark Import / Export BEGIN
namespace internal {

// Returns true if |url| has a valid scheme that we allow to import. We
// filter out the URL with a unsupported scheme.
bool CanImportURL(const GURL& url) {
 // The URL is not valid.
 if (!url.is_valid())
   return false;

 // Filter out the URLs with unsupported schemes.
 const char* const kInvalidSchemes[] = {"wyciwyg", "place"};
 for (size_t i = 0; i < std::size(kInvalidSchemes); ++i) {
   if (url.SchemeIs(kInvalidSchemes[i]))
     return false;
 }

 // Check if |url| is about:blank.
 if (url == url::kAboutBlankURL)
   return true;

 // If |url| starts with chrome:// or about:, check if it's one of the URLs
 // that we support.
 if (url.SchemeIs(content::kChromeUIScheme) ||
     url.SchemeIs(url::kAboutScheme)) {
   if (url.host_piece() == chrome::kChromeUIAboutHost)
     return true;

   GURL fixed_url(url_formatter::FixupURL(url.spec(), std::string()));
   for (size_t i = 0; i < chrome::kNumberOfChromeHostURLs; ++i) {
     if (fixed_url.DomainIs(chrome::kChromeHostURLs[i]))
       return true;
   }

   for (size_t i = 0; i < chrome::kNumberOfChromeDebugURLs; ++i) {
     if (fixed_url == chrome::kChromeDebugURLs[i])
       return true;
   }

   // If url has either chrome:// or about: schemes but wasn't found in the
   // above lists, it means we don't support it, so we don't allow the user
   // to import it.
   return false;
 }

 // Otherwise, we assume the url has a valid (importable) scheme.
 return true;
}

} // internal
// Ecosia: Bookmark Import / Export END

namespace {

// Ecosia: Bookmark Import / Export BEGIN
class FileBookmarksExportObserver: public BookmarksExportObserver {
 public:
 FileBookmarksExportObserver(
          const JavaParamRef<jobject>& obj,
          ui::WindowAndroid* window,
          const std::string& export_path) :
   obj_(ScopedJavaGlobalRef<jobject>(obj)),
   window_(window),
   export_path_(export_path) {}

 void OnExportFinished(Result result) override {
   if (result == Result::kSuccess) {
     LOG(INFO) << "Bookmarks exported successfully to " << export_path_;
   } else if (result == Result::kCouldNotCreateFile) {
     LOG(ERROR) << "Bookmarks export: could not create file " << export_path_;
   } else if (result == Result::kCouldNotWriteHeader) {
     LOG(ERROR) << "Bookmarks export: could not write header";
   } else if (result == Result::kCouldNotWriteNodes) {
     LOG(ERROR) << "Bookmarks export: could not write nodes";
   }

   JNIEnv* env = AttachCurrentThread();
   Java_BookmarkBridge_bookmarksExported(env, obj_, window_->GetJavaObject(),
                                         ConvertUTF8ToJavaString(env, export_path_),
                                         result == Result::kSuccess);
   delete this;
 }

 private:
 const ScopedJavaGlobalRef<jobject> obj_;
 ui::WindowAndroid* window_;
 const std::string export_path_;
};
// Ecosia: Bookmark Import / Export END

class BookmarkTitleComparer {
 public:
  explicit BookmarkTitleComparer(BookmarkBridge* bookmark_bridge,
                                 const icu::Collator* collator)
      : bookmark_bridge_(bookmark_bridge), collator_(collator) {}

  bool operator()(const BookmarkNode* lhs, const BookmarkNode* rhs) {
    if (collator_) {
      return base::i18n::CompareString16WithCollator(
                 *collator_, bookmark_bridge_->GetTitle(lhs),
                 bookmark_bridge_->GetTitle(rhs)) == UCOL_LESS;
    } else {
      return lhs->GetTitle() < rhs->GetTitle();
    }
  }

 private:
  raw_ptr<BookmarkBridge> bookmark_bridge_;  // weak
  raw_ptr<const icu::Collator> collator_;
};

std::unique_ptr<icu::Collator> GetICUCollator() {
  UErrorCode error = U_ZERO_ERROR;
  std::unique_ptr<icu::Collator> collator_;
  collator_.reset(icu::Collator::createInstance(error));
  if (U_FAILURE(error))
    collator_.reset(nullptr);

  return collator_;
}

// The key used to connect the instance of the bookmark bridge to the bookmark
// model.
const char kBookmarkBridgeUserDataKey[] = "bookmark_bridge";
}  // namespace

ScopedJavaLocalRef<jobject> JNI_BookmarkBridge_GetForProfile(
    JNIEnv* env,
    const JavaParamRef<jobject>& j_profile) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);

  Profile* profile = ProfileAndroid::FromProfileAndroid(j_profile);
  if (!profile)
    return nullptr;

  BookmarkModel* model = BookmarkModelFactory::GetForBrowserContext(profile);
  if (!model)
    return nullptr;

  BookmarkBridge* bookmark_bridge = static_cast<BookmarkBridge*>(
      model->GetUserData(kBookmarkBridgeUserDataKey));

  if (!bookmark_bridge) {
    bookmark_bridge = new BookmarkBridge(
        profile, model, ManagedBookmarkServiceFactory::GetForProfile(profile),
        PartnerBookmarksShim::BuildForBrowserContext(
            chrome::GetBrowserContextRedirectedInIncognito(profile)),
        ReadingListManagerFactory::GetForBrowserContext(profile));
    model->SetUserData(kBookmarkBridgeUserDataKey,
                       base::WrapUnique(bookmark_bridge));
  }

  return ScopedJavaLocalRef(bookmark_bridge->GetJavaBookmarkModel());
}

BookmarkBridge::BookmarkBridge(
    Profile* profile,
    BookmarkModel* model,
    bookmarks::ManagedBookmarkService* managed_bookmark_service,
    PartnerBookmarksShim* partner_bookmarks_shim,
    ReadingListManager* reading_list_manager)
    : profile_(profile),
      bookmark_model_(model),
      managed_bookmark_service_(managed_bookmark_service),
      partner_bookmarks_shim_(partner_bookmarks_shim),
      reading_list_manager_(reading_list_manager),
      weak_ptr_factory_(this) {
  profile_observation_.Observe(profile_.get());
  bookmark_model_->AddObserver(this);
  partner_bookmarks_shim_->AddObserver(this);
  reading_list_manager_->AddObserver(this);

  pref_change_registrar_.Init(profile_->GetPrefs());
  pref_change_registrar_.Add(
      bookmarks::prefs::kEditBookmarksEnabled,
      base::BindRepeating(&BookmarkBridge::EditBookmarksEnabledChanged,
                          base::Unretained(this)));

  NotifyIfDoneLoading();

  // Since a sync or import could have started before this class is
  // initialized, we need to make sure that our initial state is
  // up to date.
  if (bookmark_model_->IsDoingExtensiveChanges())
    ExtensiveBookmarkChangesBeginning(bookmark_model_);

  java_bookmark_model_ = Java_BookmarkBridge_createBookmarkModel(
      base::android::AttachCurrentThread(), reinterpret_cast<intptr_t>(this));
}

BookmarkBridge::~BookmarkBridge() {
  if (profile_) {
    DCHECK(profile_observation_.IsObservingSource(profile_.get()));
    profile_observation_.Reset();
  }
  bookmark_model_->RemoveObserver(this);
  if (partner_bookmarks_shim_)
    partner_bookmarks_shim_->RemoveObserver(this);
  reading_list_manager_->RemoveObserver(this);
  // Ecosia: Bookmark Import / Export
  // There may be pending file dialogs, we need to tell them that we've gone
  // away so they don't try and call back to us.
  if (select_file_dialog_)
    select_file_dialog_->ListenerDestroyed();
}

void BookmarkBridge::Destroy(JNIEnv*, const JavaParamRef<jobject>&) {
  // This will call the destructor because the user data is a unique pointer.
  bookmark_model_->RemoveUserData(kBookmarkBridgeUserDataKey);
}

base::android::ScopedJavaLocalRef<jobject>
BookmarkBridge::GetBookmarkIdForWebContents(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj,
    const JavaParamRef<jobject>& jweb_contents,
    jboolean only_editable) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);

  auto* web_contents = content::WebContents::FromJavaWebContents(jweb_contents);
  if (!web_contents)
    return nullptr;

  GURL url = dom_distiller::url_utils::GetOriginalUrlFromDistillerUrl(
      web_contents->GetLastCommittedURL());

  // TODO(crbug.com/1150559): This is a hack to avoid a historical issue that
  // this function doesn't wait for any backend loaded.
  if (reading_list_manager_->IsLoaded()) {
    const auto* node = reading_list_manager_->Get(url);
    if (node)
      return JavaBookmarkIdCreateBookmarkId(env, node->id(),
                                            GetBookmarkType(node));
  }

  // Get all the nodes for |url| and sort them by date added.
  std::vector<const bookmarks::BookmarkNode*> nodes;
  bookmarks::ManagedBookmarkService* managed =
      ManagedBookmarkServiceFactory::GetForProfile(profile_);
  bookmarks::BookmarkModel* model =
      BookmarkModelFactory::GetForBrowserContext(profile_);

  model->GetNodesByURL(url, &nodes);
  std::sort(nodes.begin(), nodes.end(), &bookmarks::MoreRecentlyAdded);

  // Return the first node matching the search criteria.
  for (const auto* node : nodes) {
    if (only_editable && !managed->CanBeEditedByUser(node))
      continue;
    return JavaBookmarkIdCreateBookmarkId(env, node->id(),
                                          GetBookmarkType(node));
  }

  return nullptr;
}

jboolean BookmarkBridge::IsEditBookmarksEnabled(JNIEnv* env) {
  return IsEditBookmarksEnabled();
}

void BookmarkBridge::LoadEmptyPartnerBookmarkShimForTesting(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj) {
  if (partner_bookmarks_shim_->IsLoaded())
    return;
  partner_bookmarks_shim_->SetPartnerBookmarksRoot(
      PartnerBookmarksReader::CreatePartnerBookmarksRootForTesting());
  PartnerBookmarksShim::DisablePartnerBookmarksEditing();
  DCHECK(partner_bookmarks_shim_->IsLoaded());
}

// Loads a fake partner bookmarks shim for testing.
// This is used in BookmarkBridgeTest.java.
void BookmarkBridge::LoadFakePartnerBookmarkShimForTesting(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj) {
  if (partner_bookmarks_shim_->IsLoaded())
    return;
  std::unique_ptr<BookmarkNode> root_partner_node =
      PartnerBookmarksReader::CreatePartnerBookmarksRootForTesting();
  BookmarkNode* partner_bookmark_a =
      root_partner_node->Add(std::make_unique<BookmarkNode>(
          1, base::GUID::GenerateRandomV4(), GURL("http://www.a.com")));
  partner_bookmark_a->SetTitle(u"Partner Bookmark A");
  BookmarkNode* partner_bookmark_b =
      root_partner_node->Add(std::make_unique<BookmarkNode>(
          2, base::GUID::GenerateRandomV4(), GURL("http://www.b.com")));
  partner_bookmark_b->SetTitle(u"Partner Bookmark B");
  partner_bookmarks_shim_->SetPartnerBookmarksRoot(
      std::move(root_partner_node));
  PartnerBookmarksShim::DisablePartnerBookmarksEditing();
  DCHECK(partner_bookmarks_shim_->IsLoaded());
}

ScopedJavaLocalRef<jobject> BookmarkBridge::GetBookmarkByID(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj,
    jlong id,
    jint type) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());
  const BookmarkNode* node = GetNodeByID(id, type);
  return node ? CreateJavaBookmark(node) : ScopedJavaLocalRef<jobject>();
}

bool BookmarkBridge::IsDoingExtensiveChanges(JNIEnv* env,
                                             const JavaParamRef<jobject>& obj) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  return bookmark_model_->IsDoingExtensiveChanges();
}

void BookmarkBridge::GetTopLevelFolderParentIDs(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj,
    const JavaParamRef<jobject>& j_result_obj) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  Java_BookmarkBridge_addToBookmarkIdList(
      env, j_result_obj, bookmark_model_->root_node()->id(),
      GetBookmarkType(bookmark_model_->root_node()));
}

void BookmarkBridge::GetTopLevelFolderIDs(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj,
    jboolean get_special,
    jboolean get_normal,
    const JavaParamRef<jobject>& j_result_obj) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());

  std::vector<const BookmarkNode*> top_level_folders;

  if (get_special) {
    if (managed_bookmark_service_->managed_node() &&
        !managed_bookmark_service_->managed_node()->children().empty()) {
      top_level_folders.push_back(managed_bookmark_service_->managed_node());
    }
    if (partner_bookmarks_shim_->HasPartnerBookmarks() &&
        IsReachable(partner_bookmarks_shim_->GetPartnerBookmarksRoot())) {
      top_level_folders.push_back(
          partner_bookmarks_shim_->GetPartnerBookmarksRoot());
    }
    if (reading_list_manager_->GetRoot()) {
      top_level_folders.push_back(reading_list_manager_->GetRoot());
    }
  }
  std::size_t special_count = top_level_folders.size();

  if (get_normal) {
    DCHECK_EQ(4u, bookmark_model_->root_node()->children().size());
    for (const auto& node : bookmark_model_->mobile_node()->children()) {
      if (node->is_folder())
        top_level_folders.push_back(node.get());
    }

    for (const auto& node : bookmark_model_->bookmark_bar_node()->children()) {
      if (node->is_folder())
        top_level_folders.push_back(node.get());
    }

    for (const auto& node : bookmark_model_->other_node()->children()) {
      if (node->is_folder())
        top_level_folders.push_back(node.get());
    }

    std::unique_ptr<icu::Collator> collator = GetICUCollator();
    std::stable_sort(top_level_folders.begin() + special_count,
                     top_level_folders.end(),
                     BookmarkTitleComparer(this, collator.get()));
  }

  for (std::vector<const BookmarkNode*>::const_iterator it =
           top_level_folders.begin();
       it != top_level_folders.end(); ++it) {
    Java_BookmarkBridge_addToBookmarkIdList(env, j_result_obj, (*it)->id(),
                                            GetBookmarkType(*it));
  }
}

base::android::ScopedJavaLocalRef<jobject> BookmarkBridge::GetReadingListFolder(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  const BookmarkNode* root_node = reading_list_manager_->GetRoot();
  ScopedJavaLocalRef<jobject> folder_id_obj = JavaBookmarkIdCreateBookmarkId(
      env, root_node->id(), GetBookmarkType(root_node));
  return folder_id_obj;
}

void BookmarkBridge::GetAllFoldersWithDepths(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj,
    const JavaParamRef<jobject>& j_folders_obj,
    const JavaParamRef<jobject>& j_depths_obj) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());

  std::unique_ptr<icu::Collator> collator = GetICUCollator();

  // Vector to temporarily contain all child bookmarks at same level for sorting
  std::vector<const BookmarkNode*> bookmarks = {
      bookmark_model_->mobile_node(),
      bookmark_model_->bookmark_bar_node(),
      bookmark_model_->other_node(),
  };

  // Push all sorted top folders in stack and give them depth of 0.
  // Note the order to push folders to stack should be opposite to the order in
  // output.
  base::stack<std::pair<const BookmarkNode*, int>> stk;
  for (const auto* bookmark : base::Reversed(bookmarks))
    stk.emplace(bookmark, 0);

  while (!stk.empty()) {
    const BookmarkNode* node = stk.top().first;
    int depth = stk.top().second;
    stk.pop();
    Java_BookmarkBridge_addToBookmarkIdListWithDepth(
        env, j_folders_obj, node->id(), GetBookmarkType(node), j_depths_obj,
        depth);
    bookmarks.clear();
    for (const auto& child : node->children()) {
      if (child->is_folder() &&
          managed_bookmark_service_->CanBeEditedByUser(child.get())) {
        bookmarks.push_back(child.get());
      }
    }
    std::stable_sort(bookmarks.begin(), bookmarks.end(),
                     BookmarkTitleComparer(this, collator.get()));
    for (const auto* bookmark : base::Reversed(bookmarks))
      stk.emplace(bookmark, depth + 1);
  }
}

ScopedJavaLocalRef<jobject> BookmarkBridge::GetRootFolderId(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  const BookmarkNode* root_node = bookmark_model_->root_node();
  ScopedJavaLocalRef<jobject> folder_id_obj = JavaBookmarkIdCreateBookmarkId(
      env, root_node->id(), GetBookmarkType(root_node));
  return folder_id_obj;
}

ScopedJavaLocalRef<jobject> BookmarkBridge::GetMobileFolderId(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  const BookmarkNode* mobile_node = bookmark_model_->mobile_node();
  ScopedJavaLocalRef<jobject> folder_id_obj = JavaBookmarkIdCreateBookmarkId(
      env, mobile_node->id(), GetBookmarkType(mobile_node));
  return folder_id_obj;
}

ScopedJavaLocalRef<jobject> BookmarkBridge::GetOtherFolderId(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  const BookmarkNode* other_node = bookmark_model_->other_node();
  ScopedJavaLocalRef<jobject> folder_id_obj = JavaBookmarkIdCreateBookmarkId(
      env, other_node->id(), GetBookmarkType(other_node));
  return folder_id_obj;
}

ScopedJavaLocalRef<jobject> BookmarkBridge::GetDesktopFolderId(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  const BookmarkNode* desktop_node = bookmark_model_->bookmark_bar_node();
  ScopedJavaLocalRef<jobject> folder_id_obj = JavaBookmarkIdCreateBookmarkId(
      env, desktop_node->id(), GetBookmarkType(desktop_node));
  return folder_id_obj;
}

ScopedJavaLocalRef<jobject> BookmarkBridge::GetPartnerFolderId(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(partner_bookmarks_shim_->IsLoaded());

  const BookmarkNode* partner_node =
      partner_bookmarks_shim_->GetPartnerBookmarksRoot();
  ScopedJavaLocalRef<jobject> folder_id_obj = JavaBookmarkIdCreateBookmarkId(
      env, partner_node->id(), GetBookmarkType(partner_node));
  return folder_id_obj;
}

base::android::ScopedJavaLocalRef<jstring>
BookmarkBridge::GetBookmarkGuidByIdForTesting(
    JNIEnv* env,
    const base::android::JavaParamRef<jobject>& obj,
    jlong id,
    jint type) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  const BookmarkNode* node = GetNodeByID(id, type);
  DCHECK(node) << "Bookmark with id " << id << " doesn't exist.";
  return base::android::ConvertUTF8ToJavaString(
      env, node->guid().AsLowercaseString());
}

jint BookmarkBridge::GetChildCount(JNIEnv* env,
                                   const JavaParamRef<jobject>& obj,
                                   jlong id,
                                   jint type) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());
  const BookmarkNode* node = GetNodeByID(id, type);
  return static_cast<jint>(node->children().size());
}

void BookmarkBridge::GetChildIDs(JNIEnv* env,
                                 const JavaParamRef<jobject>& obj,
                                 jlong id,
                                 jint type,
                                 const JavaParamRef<jobject>& j_result_obj) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());

  const BookmarkNode* parent = GetNodeByID(id, type);
  if (!parent->is_folder() || !IsReachable(parent))
    return;

  // Get the folder contents
  for (const auto& child : parent->children()) {
    if (IsFolderAvailable(child.get()) && IsReachable(child.get())) {
      Java_BookmarkBridge_addToBookmarkIdList(env, j_result_obj, child->id(),
                                              GetBookmarkType(child.get()));
    }
  }

  // Partner bookmark root node is under mobile node.
  if (parent == bookmark_model_->mobile_node() &&
      partner_bookmarks_shim_->HasPartnerBookmarks() &&
      IsReachable(partner_bookmarks_shim_->GetPartnerBookmarksRoot())) {
    Java_BookmarkBridge_addToBookmarkIdList(
        env, j_result_obj,
        partner_bookmarks_shim_->GetPartnerBookmarksRoot()->id(),
        BookmarkType::BOOKMARK_TYPE_PARTNER);
  }
}

ScopedJavaLocalRef<jobject> BookmarkBridge::GetChildAt(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj,
    jlong id,
    jint type,
    jint index) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());

  const BookmarkNode* parent = GetNodeByID(id, type);
  DCHECK(parent);
  const BookmarkNode* child =
      parent->children()[static_cast<size_t>(index)].get();
  return JavaBookmarkIdCreateBookmarkId(env, child->id(),
                                        GetBookmarkType(child));
}

jint BookmarkBridge::GetTotalBookmarkCount(
    JNIEnv* env,
    const base::android::JavaParamRef<jobject>& obj,
    jlong id,
    jint type) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());

  std::queue<const BookmarkNode*> nodes;
  const BookmarkNode* parent = GetNodeByID(id, type);
  DCHECK(parent->is_folder());

  int count = 0;
  nodes.push(parent);
  while (!nodes.empty()) {
    const BookmarkNode* node = nodes.front();
    nodes.pop();

    for (const auto& child : node->children()) {
      // Do not count deleted partner bookmarks or folders, which will have
      // empty titles. See PartnerBookmarkShim::RemoveBookmark().
      if (partner_bookmarks_shim_->IsPartnerBookmark(child.get()) &&
          partner_bookmarks_shim_->GetTitle(child.get()).empty())
        continue;
      if (child->is_folder())
        nodes.push(child.get());
      else
        ++count;
    }
    // If we are looking at the mobile bookmarks folder,
    // and we have partner bookmarks
    if (node == bookmark_model_->mobile_node() &&
        partner_bookmarks_shim_->HasPartnerBookmarks() &&
        IsReachable(partner_bookmarks_shim_->GetPartnerBookmarksRoot())) {
      nodes.push(partner_bookmarks_shim_->GetPartnerBookmarksRoot());
    }
  }

  return count;
}

// Ecosia: Bookmark Import / Export BEGIN
void BookmarkBridge::ImportBookmarks(JNIEnv* env,
                                       const JavaParamRef<jobject>& obj,
                                       const JavaParamRef<jobject>& java_window) {
  DCHECK(IsLoaded());
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);

  window_ = ui::WindowAndroid::FromJavaWindowAndroid(java_window);
  CHECK(window_);

  select_file_dialog_ = ui::SelectFileDialog::Create(
    this, std::make_unique<ChromeSelectFilePolicy>(nullptr));

  //NOTE: extension and description are not used on Android, thus not set
  ui::SelectFileDialog::FileTypeInfo file_type_info;

  const std::vector<std::u16string> v_accept_types = { u"text/html" };

  // Android needs the original MIME types and an additional capture value.
  std::pair<std::vector<std::u16string>, bool> accept_types =
      std::make_pair(v_accept_types, /* use_media_capture */ false);

  select_file_dialog_->SelectFile(
        ui::SelectFileDialog::SELECT_OPEN_FILE,
        std::u16string(),
        export_path_,
        &file_type_info,
        0,
        base::FilePath::StringType(),
        window_,
        &accept_types
  );
}

void BookmarkBridge::ExportBookmarks(JNIEnv* env,
                                     const JavaParamRef<jobject>& obj,
                                     const JavaParamRef<jobject>& java_window,
                                     const JavaParamRef<jstring>& j_export_path) {
  DCHECK(IsLoaded());
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);

  window_ = ui::WindowAndroid::FromJavaWindowAndroid(java_window);
  CHECK(window_);

  std::u16string export_path =
    base::android::ConvertJavaStringToUTF16(env, j_export_path);

  export_path_ = base::FilePath::FromUTF16Unsafe(export_path);

  if (export_path_.empty()) {
    if (!base::android::GetDownloadsDirectory(&export_path_)) {
      LOG(ERROR) << "Could not retrieve downloads directory for bookmarks export";
      return;
    }
    export_path_ = export_path_.Append(FILE_PATH_LITERAL("bookmarks.html"));
  }

  observer_ = new FileBookmarksExportObserver(obj, window_, export_path_.MaybeAsASCII());
  bookmark_html_writer::WriteBookmarks(profile_, export_path_, observer_);
}

// Attempts to create a TemplateURL from the provided data. |title| is optional.
// If TemplateURL creation fails, returns null.
std::unique_ptr<TemplateURL> CreateTemplateURL(const std::u16string& url,
                                               const std::u16string& keyword,
                                               const std::u16string& title) {
  if (url.empty() || keyword.empty())
    return nullptr;
  TemplateURLData data;
  data.SetKeyword(keyword);
  // We set short name by using the title if it exists.
  // Otherwise, we use the shortcut.
  data.SetShortName(title.empty() ? keyword : title);
  data.SetURL(TemplateURLRef::DisplayURLToURLRef(url));
  return std::make_unique<TemplateURL>(data);
}

void BookmarkBridge::FileSelected(const base::FilePath& path, int index,
                            void* params) {
  base::ThreadPool::PostTaskAndReplyWithResult(
      FROM_HERE, {base::TaskPriority::BEST_EFFORT, base::MayBlock()},
      base::BindOnce(&BookmarkBridge::FileSelectedImpl,
        base::Unretained(this),
        path),
      base::BindOnce(&BookmarkBridge::FileSelectedImplOnUIThread,
        base::Unretained(this),
        path));
}

const std::string BookmarkBridge::FileSelectedImpl(const base::FilePath& path) {
  JNIEnv* env = AttachCurrentThread();
  ScopedJavaLocalRef<jobject> window = window_->GetJavaObject();

  // propagate import start
  Java_BookmarkBridge_onBookmarkImportStarted(env, ScopedJavaLocalRef<jobject>(java_bookmark_model_));

  std::string mime_type = base::GetContentUriMimeType(path);
  if (std::strcmp(mime_type.c_str(), "text/html") != 0) {
    // File has incorrect mime type
    Java_BookmarkBridge_onBookmarkImportError(
        env, ScopedJavaLocalRef<jobject>(java_bookmark_model_), 6, window);
    return "";
  }

  base::File file;
  if (path.IsContentUri()) {
    file = base::OpenContentUriForRead(path);
  } else {
    file.Initialize(path, base::File::FLAG_OPEN | base::File::FLAG_READ);
  }
  if (!file.IsValid()) {
    // Cannot open bookmarks file for import
    Java_BookmarkBridge_onBookmarkImportError(
        env, ScopedJavaLocalRef<jobject>(java_bookmark_model_), 1, window);
    return "";
  }

  auto fileLength = file.GetLength();
  if (-1 == fileLength) {
    // Cannot read bookmarks file length
    Java_BookmarkBridge_onBookmarkImportError(
        env, ScopedJavaLocalRef<jobject>(java_bookmark_model_), 2, window);
    return "";
  }

  if (fileLength > 10 * 1024 * 1024) {
    // Bookmark file is bigger than 10MB
    Java_BookmarkBridge_onBookmarkImportError(
        env, ScopedJavaLocalRef<jobject>(java_bookmark_model_), 3, window);
    return "";
  }

  std::vector<char> buffer(fileLength);
  if (-1 == file.ReadAtCurrentPos(buffer.data(), fileLength)) {
    // Could not read bookmarks file
    Java_BookmarkBridge_onBookmarkImportError(
        env, ScopedJavaLocalRef<jobject>(java_bookmark_model_), 4, window);
    return "";
  }

  if (buffer.empty()) {
    // Empty bookmarks file
    Java_BookmarkBridge_onBookmarkImportError(
        env, ScopedJavaLocalRef<jobject>(java_bookmark_model_), 5, window);
    return "";
  }

  std::string contents(buffer.begin(), buffer.end());
  return contents;
}

void BookmarkBridge::FileSelectedImplOnUIThread(const base::FilePath& path,
                                        const std::string& contents) {
  if (contents.empty())
    return;

  // the following import logic comes from BookmarksFileImporter class
  std::vector<ImportedBookmarkEntry> bookmarks;
  std::vector<importer::SearchEngineInfo> search_engines;
  favicon_base::FaviconUsageDataList favicons;

  bookmark_html_reader::ImportBookmarksFile(
      base::RepeatingCallback<bool(void)>(),
      base::BindRepeating(internal::CanImportURL),
      contents,
      &bookmarks,
      &search_engines,
      &favicons);

  auto *writer = new ProfileWriter(profile_);

  if (!bookmarks.empty()) {
    // adding bookmarks will begin extensive changes to the model
    writer->AddBookmarksWithModel(bookmark_model_, bookmarks, u"Imported");
  }
  if (!search_engines.empty()) {
    TemplateURLService::OwnedTemplateURLVector owned_template_urls;
    for (const auto& search_engine : search_engines) {
      std::unique_ptr<TemplateURL> owned_template_url = CreateTemplateURL(
          search_engine.url, search_engine.keyword, search_engine.display_name);
      if (owned_template_url)
        owned_template_urls.push_back(std::move(owned_template_url));
    }
    writer->AddKeywords(std::move(owned_template_urls), false);
  }

  std::stringstream message;
  message << "Imported " << bookmarks.size() << " bookmarks and " <<
        search_engines.size() << " search engines from " << path.MaybeAsASCII();
  auto result = message.str();

  JNIEnv* env = AttachCurrentThread();
  Java_BookmarkBridge_onBookmarkImportSuccess(
    env, ScopedJavaLocalRef<jobject>(java_bookmark_model_), bookmarks.size(), search_engines.size(), base::android::ConvertUTF8ToJavaString(env, path.MaybeAsASCII()));

  LOG(INFO) << result;
}

void BookmarkBridge::FileSelectionCanceled(void* params) {
}
// Ecosia: Bookmark Import / Export END

void BookmarkBridge::SetBookmarkTitle(JNIEnv* env,
                                      const JavaParamRef<jobject>& obj,
                                      jlong id,
                                      jint type,
                                      const JavaParamRef<jstring>& j_title) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());

  const BookmarkNode* bookmark = GetNodeByID(id, type);
  const std::u16string title =
      base::android::ConvertJavaStringToUTF16(env, j_title);

  if (partner_bookmarks_shim_->IsPartnerBookmark(bookmark)) {
    partner_bookmarks_shim_->RenameBookmark(bookmark, title);
  } else if (reading_list_manager_->IsReadingListBookmark(bookmark)) {
    reading_list_manager_->SetTitle(bookmark->url(), title);
  } else {
    bookmark_model_->SetTitle(bookmark, title,
                              bookmarks::metrics::BookmarkEditSource::kUser);
  }
}

void BookmarkBridge::SetBookmarkUrl(JNIEnv* env,
                                    const JavaParamRef<jobject>& obj,
                                    jlong id,
                                    jint type,
                                    const JavaParamRef<jobject>& url) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());
  bookmark_model_->SetURL(GetNodeByID(id, type),
                          *url::GURLAndroid::ToNativeGURL(env, url),
                          bookmarks::metrics::BookmarkEditSource::kUser);
}

void BookmarkBridge::SetPowerBookmarkMeta(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj,
    jlong id,
    jint type,
    const JavaParamRef<jbyteArray>& bytes) {
  const BookmarkNode* node = GetNodeByID(id, type);
  if (!node || bytes.is_null())
    return;

  std::unique_ptr<power_bookmarks::PowerBookmarkMeta> meta =
      std::make_unique<power_bookmarks::PowerBookmarkMeta>();
  std::vector<uint8_t> byte_vec;
  base::android::JavaByteArrayToByteVector(env, bytes, &byte_vec);
  if (meta->ParseFromArray(byte_vec.data(), byte_vec.size())) {
    power_bookmarks::SetNodePowerBookmarkMeta(bookmark_model_, node,
                                              std::move(meta));
  } else {
    DCHECK(false) << "Failed to parse bytes from java into PowerBookmarkMeta!";
  }
}

ScopedJavaLocalRef<jbyteArray> BookmarkBridge::GetPowerBookmarkMeta(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj,
    jlong id,
    jint type) {
  const BookmarkNode* node = GetNodeByID(id, type);
  std::unique_ptr<power_bookmarks::PowerBookmarkMeta> meta =
      power_bookmarks::GetNodePowerBookmarkMeta(bookmark_model_, node);

  if (!meta)
    return ScopedJavaLocalRef<jbyteArray>(nullptr);

  int size = meta->ByteSize();
  std::string proto_bytes;
  meta->SerializeToString(&proto_bytes);
  std::vector<uint8_t> data;
  data.resize(size);
  meta->SerializeToArray(data.data(), size);

  return base::android::ToJavaByteArray(env, data.data(), size);
}

void BookmarkBridge::DeletePowerBookmarkMeta(
    JNIEnv* env,
    const base::android::JavaParamRef<jobject>& obj,
    jlong id,
    jint type) {
  const BookmarkNode* node = GetNodeByID(id, type);

  if (!node)
    return;

  power_bookmarks::DeleteNodePowerBookmarkMeta(bookmark_model_, node);
}

bool BookmarkBridge::DoesBookmarkExist(JNIEnv* env,
                                       const JavaParamRef<jobject>& obj,
                                       jlong id,
                                       jint type) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());

  const BookmarkNode* node = GetNodeByID(id, type);

  if (!node)
    return false;

  if (type == BookmarkType::BOOKMARK_TYPE_NORMAL ||
      type == BookmarkType::BOOKMARK_TYPE_READING_LIST) {
    return true;
  } else {
    DCHECK(type == BookmarkType::BOOKMARK_TYPE_PARTNER);
    return partner_bookmarks_shim_->IsReachable(node);
  }
}

void BookmarkBridge::GetBookmarksForFolder(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj,
    const JavaParamRef<jobject>& j_folder_id_obj,
    const JavaParamRef<jobject>& j_result_obj) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());

  long folder_id = JavaBookmarkIdGetId(env, j_folder_id_obj);
  int type = JavaBookmarkIdGetType(env, j_folder_id_obj);
  const BookmarkNode* folder = GetFolderWithFallback(folder_id, type);

  if (!folder->is_folder() || !IsReachable(folder))
    return;

  // Recreate the java bookmarkId object due to fallback.
  ScopedJavaLocalRef<jobject> folder_id_obj = JavaBookmarkIdCreateBookmarkId(
      env, folder->id(), GetBookmarkType(folder));

  // Get the folder contents.
  for (const auto& node : folder->children()) {
    if (IsFolderAvailable(node.get()))
      ExtractBookmarkNodeInformation(node.get(), j_result_obj);
  }

  if (folder == bookmark_model_->mobile_node() &&
      partner_bookmarks_shim_->HasPartnerBookmarks()) {
    ExtractBookmarkNodeInformation(
        partner_bookmarks_shim_->GetPartnerBookmarksRoot(), j_result_obj);
  }
}

jboolean BookmarkBridge::IsFolderVisible(JNIEnv* env,
                                         const JavaParamRef<jobject>& obj,
                                         jlong id,
                                         jint type) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  if (type == BookmarkType::BOOKMARK_TYPE_NORMAL) {
    const BookmarkNode* node = bookmarks::GetBookmarkNodeByID(
        bookmark_model_, static_cast<int64_t>(id));
    return node->IsVisible();
  }
  DCHECK_EQ(BookmarkType::BOOKMARK_TYPE_PARTNER, type);
  const BookmarkNode* node =
      partner_bookmarks_shim_->GetNodeByID(static_cast<long>(id));
  return partner_bookmarks_shim_->IsReachable(node);
}

void BookmarkBridge::SearchBookmarks(JNIEnv* env,
                                     const JavaParamRef<jobject>& obj,
                                     const JavaParamRef<jobject>& j_list,
                                     const JavaParamRef<jstring>& j_query,
                                     const JavaParamRef<jobjectArray>& j_tags,
                                     jint type,
                                     jint max_results) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(bookmark_model_->loaded());

  std::vector<const BookmarkNode*> results;

  power_bookmarks::PowerBookmarkQueryFields query;
  query.word_phrase_query = std::make_unique<std::u16string>(
      base::android::ConvertJavaStringToUTF16(env, j_query));
  if (query.word_phrase_query->empty()) {
    query.word_phrase_query.reset();
  }

  if (!j_tags.is_null()) {
    base::android::AppendJavaStringArrayToStringVector(env, j_tags,
                                                       &query.tags);
  }

  if (type >= 0) {
    query.type = static_cast<power_bookmarks::PowerBookmarkType>(type);
  }

  power_bookmarks::GetBookmarksMatchingProperties(bookmark_model_, query,
                                                  max_results, &results);

  reading_list_manager_->GetMatchingNodes(query, max_results, &results);
  if (partner_bookmarks_shim_->HasPartnerBookmarks() &&
      IsReachable(partner_bookmarks_shim_->GetPartnerBookmarksRoot())) {
    partner_bookmarks_shim_->GetPartnerBookmarksMatchingProperties(
        query, max_results, &results);
  }
  DCHECK((int)results.size() <= max_results || max_results == -1);
  for (const bookmarks::BookmarkNode* match : results) {
    if (!IsReachable(match))
      continue;
    Java_BookmarkBridge_addToBookmarkIdList(env, j_list, match->id(),
                                            GetBookmarkType(match));
  }
}

void BookmarkBridge::GetBookmarksOfType(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj,
    const base::android::JavaParamRef<jobject>& j_list,
    jint type) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  std::vector<const BookmarkNode*> results;
  power_bookmarks::PowerBookmarkQueryFields query;
  query.type = static_cast<power_bookmarks::PowerBookmarkType>(type);
  power_bookmarks::GetBookmarksMatchingProperties(bookmark_model_, query, -1,
                                                  &results);

  for (const bookmarks::BookmarkNode* match : results) {
    Java_BookmarkBridge_addToBookmarkIdList(env, j_list, match->id(),
                                            GetBookmarkType(match));
  }
}

ScopedJavaLocalRef<jobject> BookmarkBridge::AddFolder(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj,
    const JavaParamRef<jobject>& j_parent_id_obj,
    jint index,
    const JavaParamRef<jstring>& j_title) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());

  long bookmark_id = JavaBookmarkIdGetId(env, j_parent_id_obj);
  int type = JavaBookmarkIdGetType(env, j_parent_id_obj);
  const BookmarkNode* parent = GetNodeByID(bookmark_id, type);

  const BookmarkNode* new_node = bookmark_model_->AddFolder(
      parent, static_cast<size_t>(index),
      base::android::ConvertJavaStringToUTF16(env, j_title));
  DCHECK(new_node);
  ScopedJavaLocalRef<jobject> new_java_obj = JavaBookmarkIdCreateBookmarkId(
      env, new_node->id(), GetBookmarkType(new_node));
  return new_java_obj;
}

void BookmarkBridge::DeleteBookmark(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj,
    const JavaParamRef<jobject>& j_bookmark_id_obj) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());

  long bookmark_id = JavaBookmarkIdGetId(env, j_bookmark_id_obj);
  int type = JavaBookmarkIdGetType(env, j_bookmark_id_obj);
  const BookmarkNode* node = GetNodeByID(bookmark_id, type);

  // TODO(twellington): Switch back to a DCHECK after debugging
  // why this is called with an uneditable node.
  // See https://crbug.com/981172.
  if (!IsEditable(node)) {
    LOG(ERROR) << "Deleting non editable bookmark, type:" << type;
    NOTREACHED();
    return;
  }

  if (partner_bookmarks_shim_->IsPartnerBookmark(node)) {
    partner_bookmarks_shim_->RemoveBookmark(node);
  } else if (type == BookmarkType::BOOKMARK_TYPE_READING_LIST) {
    // Inside the Delete method, node will be destroyed and node->url will be
    // also destroyed. This causes heap-use-after-free at
    // ReadingListModelImpl::RemoveEntryByURLImpl. To avoid the
    // heap-use-after-free, make a copy of node->url() and use it.
    GURL url(node->url());
    reading_list_manager_->Delete(url);
  } else {
    bookmark_model_->Remove(node);
  }
}

void BookmarkBridge::RemoveAllUserBookmarks(JNIEnv* env,
                                            const JavaParamRef<jobject>& obj) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());
  bookmark_model_->RemoveAllUserBookmarks();
}

void BookmarkBridge::MoveBookmark(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj,
    const JavaParamRef<jobject>& j_bookmark_id_obj,
    const JavaParamRef<jobject>& j_parent_id_obj,
    jint index) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());

  long bookmark_id = JavaBookmarkIdGetId(env, j_bookmark_id_obj);
  int type = JavaBookmarkIdGetType(env, j_bookmark_id_obj);
  const BookmarkNode* node = GetNodeByID(bookmark_id, type);
  DCHECK(IsEditable(node));
  bookmark_id = JavaBookmarkIdGetId(env, j_parent_id_obj);
  type = JavaBookmarkIdGetType(env, j_parent_id_obj);
  const BookmarkNode* new_parent_node = GetNodeByID(bookmark_id, type);
  // Bookmark should not be moved to its own parent folder
  if (node->parent() != new_parent_node) {
    bookmark_model_->Move(node, new_parent_node, static_cast<size_t>(index));
  }
}

ScopedJavaLocalRef<jobject> BookmarkBridge::AddBookmark(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj,
    const JavaParamRef<jobject>& j_parent_id_obj,
    jint index,
    const JavaParamRef<jstring>& j_title,
    const JavaParamRef<jobject>& j_url) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());
  long bookmark_id = JavaBookmarkIdGetId(env, j_parent_id_obj);
  int type = JavaBookmarkIdGetType(env, j_parent_id_obj);
  const BookmarkNode* parent = GetNodeByID(bookmark_id, type);

  std::unique_ptr<GURL> url = url::GURLAndroid::ToNativeGURL(env, j_url);

  const BookmarkNode* new_node = bookmark_model_->AddNewURL(
      parent, static_cast<size_t>(index),
      base::android::ConvertJavaStringToUTF16(env, j_title), *url);
  DCHECK(new_node);
  ScopedJavaLocalRef<jobject> new_java_obj = JavaBookmarkIdCreateBookmarkId(
      env, new_node->id(), GetBookmarkType(new_node));
  return new_java_obj;
}

ScopedJavaLocalRef<jobject> BookmarkBridge::AddToReadingList(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj,
    const JavaParamRef<jstring>& j_title,
    const JavaParamRef<jobject>& j_url) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());

  const BookmarkNode* node = reading_list_manager_->Add(
      *url::GURLAndroid::ToNativeGURL(env, j_url),
      base::android::ConvertJavaStringToUTF8(env, j_title));
  return node ? JavaBookmarkIdCreateBookmarkId(env, node->id(),
                                               GetBookmarkType(node))
              : ScopedJavaLocalRef<jobject>();
}

ScopedJavaLocalRef<jobject> BookmarkBridge::GetReadingListItem(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj,
    const JavaParamRef<jobject>& j_url) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());

  const BookmarkNode* node =
      reading_list_manager_->Get(*url::GURLAndroid::ToNativeGURL(env, j_url));
  return node ? CreateJavaBookmark(node) : ScopedJavaLocalRef<jobject>();
}

void BookmarkBridge::SetReadStatus(JNIEnv* env,
                                   const JavaParamRef<jobject>& obj,
                                   const JavaParamRef<jobject>& j_url,
                                   jboolean j_read) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());

  reading_list_manager_->SetReadStatus(
      *url::GURLAndroid::ToNativeGURL(env, j_url), j_read);
}

void BookmarkBridge::Undo(JNIEnv* env, const JavaParamRef<jobject>& obj) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());
  BookmarkUndoService* undo_service =
      BookmarkUndoServiceFactory::GetForProfile(profile_);
  UndoManager* undo_manager = undo_service->undo_manager();
  undo_manager->Undo();
}

void BookmarkBridge::StartGroupingUndos(JNIEnv* env,
                                        const JavaParamRef<jobject>& obj) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());
  DCHECK(!grouped_bookmark_actions_.get());  // shouldn't have started already
  grouped_bookmark_actions_ =
      std::make_unique<bookmarks::ScopedGroupBookmarkActions>(bookmark_model_);
}

void BookmarkBridge::EndGroupingUndos(JNIEnv* env,
                                      const JavaParamRef<jobject>& obj) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  DCHECK(IsLoaded());
  DCHECK(grouped_bookmark_actions_.get());  // should only call after start
  grouped_bookmark_actions_.reset();
}

bool BookmarkBridge::IsBookmarked(JNIEnv* env,
                                  const JavaParamRef<jobject>& gurl) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  return bookmark_model_->IsBookmarked(
      *url::GURLAndroid::ToNativeGURL(env, gurl));
}

std::u16string BookmarkBridge::GetTitle(const BookmarkNode* node) const {
  if (partner_bookmarks_shim_->IsPartnerBookmark(node))
    return partner_bookmarks_shim_->GetTitle(node);

  return node->GetTitle();
}

ScopedJavaLocalRef<jobject> BookmarkBridge::CreateJavaBookmark(
    const BookmarkNode* node) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);

  JNIEnv* env = AttachCurrentThread();

  const BookmarkNode* parent = GetParentNode(node);
  int64_t parent_id = parent ? parent->id() : -1;

  GURL url;
  if (node->is_url())
    url = node->url();

  int type = GetBookmarkType(node);
  bool read = false;
  if (reading_list_manager_->IsReadingListBookmark(node)) {
    read = reading_list_manager_->GetReadStatus(node);
  }

  return Java_BookmarkBridge_createBookmarkItem(
      env, node->id(), type, ConvertUTF16ToJavaString(env, GetTitle(node)),
      url::GURLAndroid::FromNativeGURL(env, url), node->is_folder(), parent_id,
      GetBookmarkType(parent), IsEditable(node), IsManaged(node),
      node->date_added().ToJavaTime(), read);
}

void BookmarkBridge::ExtractBookmarkNodeInformation(
    const BookmarkNode* node,
    const JavaRef<jobject>& j_result_obj) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  JNIEnv* env = AttachCurrentThread();
  if (!IsReachable(node))
    return;
  Java_BookmarkBridge_addToList(env, j_result_obj, CreateJavaBookmark(node));
}

const BookmarkNode* BookmarkBridge::GetNodeByID(long node_id, int type) {
  const BookmarkNode* node;
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  if (type == BookmarkType::BOOKMARK_TYPE_PARTNER) {
    node = partner_bookmarks_shim_->GetNodeByID(static_cast<int64_t>(node_id));
  } else if (type == BookmarkType::BOOKMARK_TYPE_READING_LIST) {
    node = reading_list_manager_->GetNodeByID(static_cast<int64_t>(node_id));
  } else {
    node = bookmarks::GetBookmarkNodeByID(bookmark_model_,
                                          static_cast<int64_t>(node_id));
  }
  return node;
}

const BookmarkNode* BookmarkBridge::GetFolderWithFallback(long folder_id,
                                                          int type) {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  const BookmarkNode* folder = GetNodeByID(folder_id, type);
  if (!folder || folder->type() == BookmarkNode::URL ||
      !IsFolderAvailable(folder)) {
    if (!managed_bookmark_service_->managed_node()->children().empty())
      folder = managed_bookmark_service_->managed_node();
    else
      folder = bookmark_model_->mobile_node();
  }
  return folder;
}

bool BookmarkBridge::IsEditBookmarksEnabled() const {
  return profile_->GetPrefs()->GetBoolean(
      bookmarks::prefs::kEditBookmarksEnabled);
}

void BookmarkBridge::EditBookmarksEnabledChanged() {
  DCHECK_CURRENTLY_ON(BrowserThread::UI);
  if (!java_bookmark_model_)
    return;

  Java_BookmarkBridge_editBookmarksEnabledChanged(
      AttachCurrentThread(), ScopedJavaLocalRef<jobject>(java_bookmark_model_));
}

bool BookmarkBridge::IsEditable(const BookmarkNode* node) const {
  if (!node || (node->type() != BookmarkNode::FOLDER &&
                node->type() != BookmarkNode::URL)) {
    return false;
  }
  if (!IsEditBookmarksEnabled() || bookmark_model_->is_permanent_node(node))
    return false;
  if (partner_bookmarks_shim_->IsPartnerBookmark(node))
    return partner_bookmarks_shim_->IsEditable(node);
  if (reading_list_manager_->IsReadingListBookmark(node))
    return reading_list_manager_->GetRoot() != node;

  return managed_bookmark_service_->CanBeEditedByUser(node);
}

bool BookmarkBridge::IsManaged(const BookmarkNode* node) const {
  return bookmarks::IsDescendantOf(node,
                                   managed_bookmark_service_->managed_node());
}

const BookmarkNode* BookmarkBridge::GetParentNode(const BookmarkNode* node) {
  DCHECK(IsLoaded());
  if (node == partner_bookmarks_shim_->GetPartnerBookmarksRoot())
    return bookmark_model_->mobile_node();

  if (node == reading_list_manager_->GetRoot())
    return bookmark_model_->root_node();

  return node->parent();
}

int BookmarkBridge::GetBookmarkType(const BookmarkNode* node) {
  // TODO(crbug.com/1150559) return the wrong type when the backend is not
  // loaded?
  if (partner_bookmarks_shim_->IsLoaded() &&
      partner_bookmarks_shim_->IsPartnerBookmark(node))
    return BookmarkType::BOOKMARK_TYPE_PARTNER;

  if (reading_list_manager_->IsLoaded() &&
      reading_list_manager_->IsReadingListBookmark(node))
    return BookmarkType::BOOKMARK_TYPE_READING_LIST;

  return BookmarkType::BOOKMARK_TYPE_NORMAL;
}

bool BookmarkBridge::IsReachable(const BookmarkNode* node) const {
  if (!partner_bookmarks_shim_->IsPartnerBookmark(node))
    return true;
  return partner_bookmarks_shim_->IsReachable(node);
}

bool BookmarkBridge::IsLoaded() const {
  return (bookmark_model_->loaded() && partner_bookmarks_shim_->IsLoaded() &&
          reading_list_manager_->IsLoaded());
}

bool BookmarkBridge::IsFolderAvailable(const BookmarkNode* folder) const {
  // The managed bookmarks folder is not shown if there are no bookmarks
  // configured via policy.
  if (folder == managed_bookmark_service_->managed_node() &&
      folder->children().empty())
    return false;

  auto* identity_manager =
      IdentityManagerFactory::GetForProfile(profile_->GetOriginalProfile());
  return (folder->type() != BookmarkNode::BOOKMARK_BAR &&
          folder->type() != BookmarkNode::OTHER_NODE) ||
         (identity_manager &&
          identity_manager->HasPrimaryAccount(signin::ConsentLevel::kSync));
}

void BookmarkBridge::NotifyIfDoneLoading() {
  if (!IsLoaded() || !java_bookmark_model_)
    return;

  Java_BookmarkBridge_bookmarkModelLoaded(
      AttachCurrentThread(), ScopedJavaLocalRef<jobject>(java_bookmark_model_));
}

// ------------- Observer-related methods ------------- //

// Called when there are changes to the bookmark model. It is most
// likely changes to the partner bookmarks.
void BookmarkBridge::BookmarkModelChanged() {
  if (!IsLoaded() || !java_bookmark_model_)
    return;

  Java_BookmarkBridge_bookmarkModelChanged(
      AttachCurrentThread(), ScopedJavaLocalRef<jobject>(java_bookmark_model_));
}

void BookmarkBridge::BookmarkModelLoaded(BookmarkModel* model,
                                         bool ids_reassigned) {
  NotifyIfDoneLoading();
}

void BookmarkBridge::BookmarkModelBeingDeleted(BookmarkModel* model) {
  if (!IsLoaded())
    return;

  DestroyJavaObject();
}

void BookmarkBridge::BookmarkNodeMoved(BookmarkModel* model,
                                       const BookmarkNode* old_parent,
                                       size_t old_index,
                                       const BookmarkNode* new_parent,
                                       size_t new_index) {
  if (!IsLoaded() || !java_bookmark_model_)
    return;

  Java_BookmarkBridge_bookmarkNodeMoved(
      AttachCurrentThread(), ScopedJavaLocalRef<jobject>(java_bookmark_model_),
      CreateJavaBookmark(old_parent), static_cast<int>(old_index),
      CreateJavaBookmark(new_parent), static_cast<int>(new_index));
}

void BookmarkBridge::BookmarkNodeAdded(BookmarkModel* model,
                                       const BookmarkNode* parent,
                                       size_t index,
                                       bool added_by_user) {
  if (!IsLoaded() || !java_bookmark_model_)
    return;

  Java_BookmarkBridge_bookmarkNodeAdded(
      AttachCurrentThread(), ScopedJavaLocalRef<jobject>(java_bookmark_model_),
      CreateJavaBookmark(parent), static_cast<int>(index));
}

void BookmarkBridge::BookmarkNodeRemoved(BookmarkModel* model,
                                         const BookmarkNode* parent,
                                         size_t old_index,
                                         const BookmarkNode* node,
                                         const std::set<GURL>& removed_urls) {
  if (!IsLoaded() || !java_bookmark_model_)
    return;

  Java_BookmarkBridge_bookmarkNodeRemoved(
      AttachCurrentThread(), ScopedJavaLocalRef<jobject>(java_bookmark_model_),
      CreateJavaBookmark(parent), static_cast<int>(old_index),
      CreateJavaBookmark(node));
}

void BookmarkBridge::BookmarkAllUserNodesRemoved(
    BookmarkModel* model,
    const std::set<GURL>& removed_urls) {
  if (!IsLoaded() || !java_bookmark_model_)
    return;

  Java_BookmarkBridge_bookmarkAllUserNodesRemoved(
      AttachCurrentThread(), ScopedJavaLocalRef<jobject>(java_bookmark_model_));
}

void BookmarkBridge::BookmarkNodeChanged(BookmarkModel* model,
                                         const BookmarkNode* node) {
  if (!IsLoaded() || !java_bookmark_model_)
    return;

  Java_BookmarkBridge_bookmarkNodeChanged(
      AttachCurrentThread(), ScopedJavaLocalRef<jobject>(java_bookmark_model_),
      CreateJavaBookmark(node));
}

void BookmarkBridge::BookmarkNodeChildrenReordered(BookmarkModel* model,
                                                   const BookmarkNode* node) {
  if (!IsLoaded() || !java_bookmark_model_)
    return;

  Java_BookmarkBridge_bookmarkNodeChildrenReordered(
      AttachCurrentThread(), ScopedJavaLocalRef<jobject>(java_bookmark_model_),
      CreateJavaBookmark(node));
}

void BookmarkBridge::ExtensiveBookmarkChangesBeginning(BookmarkModel* model) {
  if (!IsLoaded() || !java_bookmark_model_)
    return;

  Java_BookmarkBridge_extensiveBookmarkChangesBeginning(
      AttachCurrentThread(), ScopedJavaLocalRef<jobject>(java_bookmark_model_));
}

void BookmarkBridge::ExtensiveBookmarkChangesEnded(BookmarkModel* model) {
  if (!IsLoaded() || !java_bookmark_model_)
    return;

  Java_BookmarkBridge_extensiveBookmarkChangesEnded(
      AttachCurrentThread(), ScopedJavaLocalRef<jobject>(java_bookmark_model_));
}

void BookmarkBridge::PartnerShimChanged(PartnerBookmarksShim* shim) {
  BookmarkModelChanged();
}

void BookmarkBridge::PartnerShimLoaded(PartnerBookmarksShim* shim) {
  NotifyIfDoneLoading();
}

void BookmarkBridge::ShimBeingDeleted(PartnerBookmarksShim* shim) {
  partner_bookmarks_shim_ = nullptr;
}

void BookmarkBridge::ReadingListLoaded() {
  NotifyIfDoneLoading();
}

void BookmarkBridge::ReadingListChanged() {
  BookmarkModelChanged();
}

void BookmarkBridge::ReorderChildren(
    JNIEnv* env,
    const JavaParamRef<jobject>& obj,
    const JavaParamRef<jobject>& j_bookmark_id_obj,
    jlongArray arr) {
  DCHECK(IsLoaded());
  // get the BookmarkNode* for the "parent" bookmark parameter
  const long bookmark_id = JavaBookmarkIdGetId(env, j_bookmark_id_obj);
  const int bookmark_type = JavaBookmarkIdGetType(env, j_bookmark_id_obj);

  const BookmarkNode* bookmark_node = GetNodeByID(bookmark_id, bookmark_type);

  // populate a vector
  std::vector<const BookmarkNode*> ordered_nodes;
  jsize arraySize = env->GetArrayLength(arr);
  jlong* elements = env->GetLongArrayElements(arr, 0);

  // iterate through array, adding the BookmarkNode*s of the objects
  for (int i = 0; i < arraySize; ++i) {
    ordered_nodes.push_back(GetNodeByID(elements[i], 0));
  }

  bookmark_model_->ReorderChildren(bookmark_node, ordered_nodes);
}

// Should destroy the bookmark bridge, if OTR profile is destroyed not to delete
// related resources twice.
void BookmarkBridge::OnProfileWillBeDestroyed(Profile* profile) {
  weak_ptr_factory_.InvalidateWeakPtrs();
  DestroyJavaObject();
}

ScopedJavaGlobalRef<jobject> BookmarkBridge::GetJavaBookmarkModel() {
  return java_bookmark_model_;
}

void BookmarkBridge::DestroyJavaObject() {
  if (!java_bookmark_model_)
    return;

  Java_BookmarkBridge_destroyFromNative(
      AttachCurrentThread(), ScopedJavaLocalRef<jobject>(java_bookmark_model_));
}
