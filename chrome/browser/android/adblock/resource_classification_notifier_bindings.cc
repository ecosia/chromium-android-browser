/*
 * This file is part of eyeo Chromium SDK,
 * Copyright (C) 2006-present eyeo GmbH
 *
 * eyeo Chromium SDK is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * eyeo Chromium SDK is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with eyeo Chromium SDK.  If not, see <http://www.gnu.org/licenses/>.
 */

#include "chrome/browser/android/adblock/resource_classification_notifier_bindings.h"

#include <iterator>
#include <memory>

#include "base/android/jni_android.h"
#include "base/android/jni_array.h"
#include "base/android/jni_string.h"
#include "base/ranges/algorithm.h"
#include "chrome/browser/android/adblock/resource_classification_notifier_bindings_factory.h"
#include "chrome/browser/android/tab_android.h"
#include "chrome/browser/browser_process.h"
#include "chrome/browser/profiles/profile.h"
#include "chrome/browser/profiles/profile_manager.h"
#include "components/adblock/android/jni_headers/ResourceClassificationNotifier_jni.h"
#include "components/adblock/content/browser/resource_classification_runner.h"
#include "components/adblock/core/common/adblock_utils.h"

namespace adblock {
namespace {

constexpr int kNoId = -1;

int GetTabId(content::RenderFrameHost* render_frame_host) {
  auto* web_contents =
      content::WebContents::FromRenderFrameHost(render_frame_host);
  if (!web_contents)
    return kNoId;

  auto* tab = TabAndroid::FromWebContents(web_contents);
  if (!tab)
    return kNoId;

  return tab->GetAndroidId();
}

}  // namespace

using base::android::AttachCurrentThread;
using base::android::CheckException;
using base::android::ConvertJavaStringToUTF8;
using base::android::ConvertUTF8ToJavaString;
using base::android::GetClass;
using base::android::JavaParamRef;
using base::android::JavaRef;
using base::android::MethodID;
using base::android::ScopedJavaGlobalRef;
using base::android::ScopedJavaLocalRef;
using base::android::ToJavaArrayOfObjects;
using base::android::ToJavaArrayOfStrings;

ResourceClassificationNotifierBindings::ResourceClassificationNotifierBindings(
    ResourceClassificationRunner* classification_runner)
    : classification_runner_(classification_runner) {
  classification_runner_->AddObserver(this);
}

ResourceClassificationNotifierBindings::
    ~ResourceClassificationNotifierBindings() {
  classification_runner_->RemoveObserver(this);
}

void ResourceClassificationNotifierBindings::Bind(
    JavaObjectWeakGlobalRef resource_classifier_java) {
  bound_counterpart_ = resource_classifier_java;
}

void ResourceClassificationNotifierBindings::OnAdMatched(
    const GURL& url,
    FilterMatchResult result,
    const std::vector<GURL>& parent_frame_urls,
    ContentType content_type,
    content::RenderFrameHost* render_frame_host,
    const GURL& subscription) {
  DCHECK_CALLED_ON_VALID_SEQUENCE(sequence_checker_);
  DCHECK(render_frame_host);
  DCHECK(result == FilterMatchResult::kBlockRule ||
         result == FilterMatchResult::kAllowRule);
  const bool was_blocked = result == FilterMatchResult::kBlockRule;
  DVLOG(3) << "[eyeo] Ad matched " << url << "(type: " << content_type
           << (was_blocked ? ", blocked" : ", allowed") << ")";
  JNIEnv* env = AttachCurrentThread();

  ScopedJavaLocalRef<jobject> obj = bound_counterpart_.get(env);
  if (obj.is_null())
    return;

  ScopedJavaLocalRef<jstring> j_url = ConvertUTF8ToJavaString(env, url.spec());
  ScopedJavaLocalRef<jobjectArray> j_parents =
      ToJavaArrayOfStrings(env, adblock::utils::ConvertURLs(parent_frame_urls));
  ScopedJavaLocalRef<jstring> j_subscription =
      ConvertUTF8ToJavaString(env, subscription.spec());
  int tab_id = GetTabId(render_frame_host);
  Java_ResourceClassificationNotifier_adMatchedCallback(
      env, obj, j_url, was_blocked, j_parents, j_subscription,
      static_cast<int>(content_type), tab_id);
}

void ResourceClassificationNotifierBindings::OnPageAllowed(
    const GURL& url,
    content::RenderFrameHost* render_frame_host,
    const GURL& subscription) {
  DCHECK_CALLED_ON_VALID_SEQUENCE(sequence_checker_);
  DCHECK(render_frame_host);
  DVLOG(3) << "[eyeo] Page allowed " << url;
  JNIEnv* env = AttachCurrentThread();

  ScopedJavaLocalRef<jobject> obj = bound_counterpart_.get(env);
  if (obj.is_null())
    return;

  ScopedJavaLocalRef<jstring> j_url = ConvertUTF8ToJavaString(env, url.spec());
  ScopedJavaLocalRef<jstring> j_subscription =
      ConvertUTF8ToJavaString(env, subscription.spec());
  int tab_id = GetTabId(render_frame_host);
  Java_ResourceClassificationNotifier_pageAllowedCallback(
      env, obj, j_url, j_subscription, tab_id);
}

void ResourceClassificationNotifierBindings::OnPopupMatched(
    const GURL& url,
    FilterMatchResult result,
    const GURL& opener_url,
    content::RenderFrameHost* render_frame_host,
    const GURL& subscription) {
  DCHECK_CALLED_ON_VALID_SEQUENCE(sequence_checker_);
  DCHECK(render_frame_host);
  DCHECK(result == FilterMatchResult::kBlockRule ||
         result == FilterMatchResult::kAllowRule);
  const bool was_blocked = result == FilterMatchResult::kBlockRule;
  DVLOG(3) << "[eyeo] Popup matched " << url
           << (was_blocked ? ", blocked" : ", allowed");
  JNIEnv* env = AttachCurrentThread();

  ScopedJavaLocalRef<jobject> obj = bound_counterpart_.get(env);
  if (obj.is_null())
    return;

  ScopedJavaLocalRef<jstring> j_url = ConvertUTF8ToJavaString(env, url.spec());
  ScopedJavaLocalRef<jstring> j_opener =
      ConvertUTF8ToJavaString(env, opener_url.spec());
  ScopedJavaLocalRef<jstring> j_subscription =
      ConvertUTF8ToJavaString(env, subscription.spec());
  int tab_id = GetTabId(render_frame_host);
  Java_ResourceClassificationNotifier_popupMatchedCallback(
      env, obj, j_url, was_blocked, j_opener, j_subscription, tab_id);
}

}  // namespace adblock

static void JNI_ResourceClassificationNotifier_Bind(
    JNIEnv* env,
    const base::android::JavaParamRef<jobject>& caller) {
  auto* bindings = adblock::ResourceClassificationNotifierBindingsFactory::
      GetForBrowserContext(ProfileManager::GetLastUsedProfile());
  DCHECK(bindings);

  bindings->Bind(JavaObjectWeakGlobalRef(env, caller));
}
