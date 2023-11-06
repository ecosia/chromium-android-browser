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

#include "components/adblock/android/adblock_jni.h"

#include <algorithm>
#include <iterator>
#include <vector>

#include "base/android/jni_android.h"
#include "base/android/jni_array.h"
#include "base/android/jni_string.h"
#include "base/android/jni_weak_ref.h"
#include "base/logging.h"
#include "components/adblock/android/java_bindings_getters.h"
#include "components/adblock/android/jni_headers/AdblockController_jni.h"
#include "components/adblock/core/common/adblock_constants.h"
#include "components/adblock/core/subscription/subscription_config.h"
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

ScopedJavaLocalRef<jobject> ToJava(JNIEnv* env,
                                   ScopedJavaLocalRef<jclass>& url_class,
                                   jmethodID& url_constructor,
                                   const std::string& url,
                                   const std::string& title,
                                   const std::string& version,
                                   const std::vector<std::string>& languages) {
  ScopedJavaLocalRef<jobject> url_param(
      env, env->NewObject(url_class.obj(), url_constructor,
                          ConvertUTF8ToJavaString(env, url).obj()));
  CheckException(env);
  return Java_Subscription_Constructor(env, url_param,
                                       ConvertUTF8ToJavaString(env, title),
                                       ConvertUTF8ToJavaString(env, version),
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
    jobjects.push_back(ToJava(
        env, url_class, url_constructor, sub->GetSourceUrl().spec(),
        sub->GetTitle(), sub->GetCurrentVersion(), std::vector<std::string>{}));
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
                                  sub.url.spec(), sub.title, "",
                                  sub.languages));
      }
    }
  }
  return jobjects;
}

}  // namespace

AdblockJNI::AdblockJNI(SubscriptionService* subscription_service)
    : subscription_service_(subscription_service) {
  DCHECK_CALLED_ON_VALID_SEQUENCE(sequence_checker_);
  if (subscription_service_) {
    subscription_service_->AddObserver(this);
  }
}

AdblockJNI::~AdblockJNI() {
  DCHECK_CALLED_ON_VALID_SEQUENCE(sequence_checker_);
  if (subscription_service_) {
    subscription_service_->RemoveObserver(this);
  }
}

void AdblockJNI::Bind(JavaObjectWeakGlobalRef weak_java_controller) {
  DCHECK_CALLED_ON_VALID_SEQUENCE(sequence_checker_);
  weak_java_controller_ = weak_java_controller;
}

void AdblockJNI::OnSubscriptionInstalled(const GURL& url) {
  DCHECK_CALLED_ON_VALID_SEQUENCE(sequence_checker_);
  JNIEnv* env = AttachCurrentThread();
  ScopedJavaLocalRef<jobject> obj = weak_java_controller_.get(env);
  if (obj.is_null()) {
    return;
  }

  ScopedJavaLocalRef<jstring> j_url = ConvertUTF8ToJavaString(env, url.spec());
  Java_AdblockController_subscriptionUpdatedCallback(env, obj, j_url);
}

}  // namespace adblock

static void JNI_AdblockController_Bind(
    JNIEnv* env,
    const base::android::JavaParamRef<jobject>& caller) {
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);
  if (!adblock::GetSubscriptionService()) {
    return;
  }
  JavaObjectWeakGlobalRef weak_controller_ref(env, caller);
  adblock::GetJNI()->Bind(weak_controller_ref);
}

static base::android::ScopedJavaLocalRef<jobjectArray>
JNI_AdblockController_GetInstalledSubscriptions(JNIEnv* env) {
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);
  auto* subscription_service = adblock::GetSubscriptionService();
  if (!subscription_service) {
    return ToJavaArrayOfObjects(env,
                                std::vector<ScopedJavaLocalRef<jobject>>{});
  }

  return ToJavaArrayOfObjects(
      env, adblock::CSubscriptionsToJObjects(
               env, subscription_service->GetCurrentSubscriptions(
                        subscription_service->GetFilteringConfiguration(
                            adblock::kAdblockFilteringConfigurationName))));
}

static base::android::ScopedJavaLocalRef<jobjectArray>
JNI_AdblockController_GetRecommendedSubscriptions(JNIEnv* env) {
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);

  auto list = adblock::config::GetKnownSubscriptions();
  return ToJavaArrayOfObjects(env,
                              adblock::CSubscriptionsToJObjects(env, list));
}
