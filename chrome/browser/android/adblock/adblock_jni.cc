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
#include "chrome/browser/android/adblock/adblock_jni.h"

#include <algorithm>
#include <iterator>
#include <vector>

#include "base/android/jni_android.h"
#include "base/android/jni_array.h"
#include "base/android/jni_string.h"
#include "base/android/jni_weak_ref.h"
#include "base/logging.h"
#include "chrome/browser/adblock/adblock_controller_factory.h"
#include "chrome/browser/android/adblock/adblock_jni_factory.h"
#include "chrome/browser/browser_process.h"
#include "chrome/browser/profiles/profile_manager.h"
#include "components/adblock/android/jni_headers/AdblockComposeFilterSuggestionsCallback_jni.h"
#include "components/adblock/android/jni_headers/AdblockController_jni.h"
#include "components/adblock/android/jni_headers/AdblockElement_jni.h"
#include "content/public/browser/browser_thread.h"

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

namespace adblock {

namespace {

AdblockController* GetController() {
  if (!g_browser_process || !g_browser_process->profile_manager())
    return nullptr;
  return AdblockControllerFactory::GetForBrowserContext(
      g_browser_process->profile_manager()->GetLastUsedProfile());
}

AdblockJNI* GetJNI() {
  if (!g_browser_process || !g_browser_process->profile_manager())
    return nullptr;
  return AdblockJNIFactory::GetForBrowserContext(
      g_browser_process->profile_manager()->GetLastUsedProfile());
}

// TODO(mpawlowski): this is currently useless, see DPD-699
class JavaElement {
 public:
  JavaElement(JNIEnv* environment, const ScopedJavaGlobalRef<jobject>& object)
      : obj_(object) {}

  ~JavaElement() {}

  std::string GetLocalName() const {
    JNIEnv* env = AttachCurrentThread();
    auto name = Java_AdblockElement_getLocalName(env, obj_);
    return name.is_null() ? "" : ConvertJavaStringToUTF8(name);
  }

  std::string GetDocumentLocation() const {
    JNIEnv* env = AttachCurrentThread();
    auto location = Java_AdblockElement_getDocumentLocation(env, obj_);
    return location.is_null() ? "" : ConvertJavaStringToUTF8(location);
  }

  std::string GetAttribute(const std::string& name) const {
    JNIEnv* env = AttachCurrentThread();
    auto attr = Java_AdblockElement_getAttribute(
        env, obj_, ConvertUTF8ToJavaString(env, name));

    return attr.is_null() ? "" : ConvertJavaStringToUTF8(attr);
  }

  std::vector<const JavaElement*> GetChildren() const {
    std::vector<const JavaElement*> res;

    if (children_.empty()) {
      JNIEnv* env = AttachCurrentThread();
      auto array = Java_AdblockElement_getChildren(env, obj_);

      if (!array.is_null()) {
        jsize array_length = env->GetArrayLength(array.obj());
        children_.reserve(array_length);

        for (jsize n = 0; n != array_length; ++n) {
          children_.push_back(JavaElement(
              env, ScopedJavaGlobalRef<jobject>(
                       env, env->GetObjectArrayElement(array.obj(), n))));
        }
      }
    }

    res.reserve(children_.size());
    std::transform(children_.begin(), children_.end(), std::back_inserter(res),
                   [](const auto& cur) { return &cur; });

    return res;
  }

 private:
  ScopedJavaGlobalRef<jobject> obj_;
  mutable std::vector<JavaElement> children_;
};

void ComposeFilterSuggestionsResult(
    const base::android::ScopedJavaGlobalRef<jobject>& element,
    const base::android::ScopedJavaGlobalRef<jobject>& callback,
    const std::vector<std::string>& filters) {
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);
  JNIEnv* env = AttachCurrentThread();
  auto j_filters = ToJavaArrayOfStrings(env, filters);
  Java_AdblockComposeFilterSuggestionsCallback_onDone(env, callback, element,
                                                      j_filters);
}

ScopedJavaLocalRef<jobject> ToJava(JNIEnv* env,
                                   ScopedJavaLocalRef<jclass>& url_class,
                                   jmethodID& url_constructor,
                                   const std::string& url,
                                   const std::string& title,
                                   const std::vector<std::string>& languages) {
  ScopedJavaLocalRef<jobject> url_param(
      env, env->NewObject(url_class.obj(), url_constructor,
                          ConvertUTF8ToJavaString(env, url).obj()));
  CheckException(env);
  return Java_Subscription_Constructor(env, url_param,
                                       ConvertUTF8ToJavaString(env, title),
                                       ToJavaArrayOfStrings(env, languages));
}

std::vector<ScopedJavaLocalRef<jobject>> CSubscriptionsToJObjects(
    JNIEnv* env,
    const std::vector<scoped_refptr<Subscription>>& subscriptions) {
  ScopedJavaLocalRef<jclass> url_class = GetClass(env, "java/net/URL");
  jmethodID url_constructor = MethodID::Get<MethodID::TYPE_INSTANCE>(
      env, url_class.obj(), "<init>", "(Ljava/lang/String;)V");
  std::vector<ScopedJavaLocalRef<jobject>> jobjects;
  jobjects.reserve(subscriptions.size());
  for (auto& sub : subscriptions) {
    jobjects.push_back(ToJava(env, url_class, url_constructor,
                              sub->GetSourceUrl().spec(), sub->GetTitle(),
                              std::vector<std::string>{}));
  }
  return jobjects;
}

std::vector<ScopedJavaLocalRef<jobject>> CSubscriptionsToJObjects(
    JNIEnv* env,
    std::vector<KnownSubscriptionInfo>& subscriptions) {
  ScopedJavaLocalRef<jclass> url_class = GetClass(env, "java/net/URL");
  jmethodID url_constructor = MethodID::Get<MethodID::TYPE_INSTANCE>(
      env, url_class.obj(), "<init>", "(Ljava/lang/String;)V");
  std::vector<ScopedJavaLocalRef<jobject>> jobjects;
  jobjects.reserve(subscriptions.size());
  for (auto& sub : subscriptions) {
    if (sub.ui_visibility == SubscriptionUiVisibility::Visible) {
      // The checks here are when one makes f.e. adblock:custom visible
      DCHECK(sub.url.is_valid());
      if (sub.url.is_valid()) {
        jobjects.push_back(ToJava(env, url_class, url_constructor,
                                  sub.url.spec(), sub.title, sub.languages));
      }
    }
  }
  return jobjects;
}

}  // namespace

AdblockJNI::AdblockJNI(AdblockController* controller)
    : controller_(controller) {
  DCHECK_CALLED_ON_VALID_SEQUENCE(sequence_checker_);
  if (controller_)
    controller_->AddObserver(this);
}

AdblockJNI::~AdblockJNI() {
  DCHECK_CALLED_ON_VALID_SEQUENCE(sequence_checker_);
  if (controller_)
    controller_->RemoveObserver(this);
}

void AdblockJNI::Bind(JavaObjectWeakGlobalRef weak_java_controller) {
  DCHECK_CALLED_ON_VALID_SEQUENCE(sequence_checker_);
  weak_java_controller_ = weak_java_controller;
}

void AdblockJNI::OnSubscriptionUpdated(const GURL& url) {
  DCHECK_CALLED_ON_VALID_SEQUENCE(sequence_checker_);
  JNIEnv* env = AttachCurrentThread();
  ScopedJavaLocalRef<jobject> obj = weak_java_controller_.get(env);
  if (obj.is_null())
    return;

  ScopedJavaLocalRef<jstring> j_url = ConvertUTF8ToJavaString(env, url.spec());
  Java_AdblockController_subscriptionUpdatedCallback(env, obj, j_url);
}

}  // namespace adblock

static void JNI_AdblockController_Bind(
    JNIEnv* env,
    const base::android::JavaParamRef<jobject>& caller) {
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);
  if (!adblock::GetController())
    return;
  JavaObjectWeakGlobalRef weak_controller_ref(env, caller);
  adblock::GetJNI()->Bind(weak_controller_ref);
}

static base::android::ScopedJavaLocalRef<jobjectArray>
JNI_AdblockController_GetInstalledSubscriptions(JNIEnv* env) {
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);
  if (!adblock::GetController())
    return ToJavaArrayOfObjects(env,
                                std::vector<ScopedJavaLocalRef<jobject>>{});

  return ToJavaArrayOfObjects(
      env, adblock::CSubscriptionsToJObjects(
               env, adblock::GetController()->GetInstalledSubscriptions()));
}

static base::android::ScopedJavaLocalRef<jobjectArray>
JNI_AdblockController_GetRecommendedSubscriptions(JNIEnv* env) {
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);
  if (!adblock::GetController())
    return ToJavaArrayOfObjects(env,
                                std::vector<ScopedJavaLocalRef<jobject>>{});

  auto list = adblock::GetController()->GetKnownSubscriptions();
  return ToJavaArrayOfObjects(env,
                              adblock::CSubscriptionsToJObjects(env, list));
}

static base::android::ScopedJavaLocalRef<jstring>
JNI_AdblockController_GetSelectedSubscriptionVersion(
    JNIEnv* env,
    const base::android::JavaParamRef<jstring>& subscription) {
  const GURL url(ConvertJavaStringToUTF8(subscription));
  const auto installed_subscriptions =
      adblock::GetController()->GetInstalledSubscriptions();
  auto it = base::ranges::find(installed_subscriptions, url,
                               &adblock::Subscription::GetSourceUrl);
  if (it == installed_subscriptions.end()) {
    return ConvertUTF8ToJavaString(env, {});
  }
  ScopedJavaLocalRef<jstring> j_version =
      ConvertUTF8ToJavaString(env, (*it)->GetCurrentVersion());
  return j_version;
}

static void JNI_AdblockController_ComposeFilterSuggestions(
    JNIEnv* env,
    const base::android::JavaParamRef<jobject>& element,
    const base::android::JavaParamRef<jobject>& callback) {
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);
  // TODO implement ComposeFilterSuggestions in flatbuffer core: DPD-699
  // This is currently not doing the right thing.
  base::android::ScopedJavaGlobalRef<jobject> element_ref(element);
  base::android::ScopedJavaGlobalRef<jobject> callback_ref(callback);
  adblock::ComposeFilterSuggestionsResult(element_ref, callback_ref, {});
}
