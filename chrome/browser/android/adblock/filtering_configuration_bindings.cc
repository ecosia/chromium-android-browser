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

#include "chrome/browser/android/adblock/filtering_configuration_bindings.h"

#include <iterator>
#include <memory>

#include "base/android/jni_array.h"
#include "base/android/jni_string.h"
#include "base/ranges/algorithm.h"
#include "chrome/browser/android/adblock/filtering_configuration_bindings_factory.h"
#include "chrome/browser/browser_process.h"
#include "chrome/browser/profiles/profile.h"
#include "chrome/browser/profiles/profile_manager.h"
#include "components/adblock/android/jni_headers/FilteringConfiguration_jni.h"
#include "components/adblock/core/configuration/filtering_configuration.h"
#include "components/adblock/core/configuration/persistent_filtering_configuration.h"
#include "components/adblock/core/subscription/subscription_service.h"

namespace adblock {

FilteringConfigurationBindings::FilteringConfigurationBindings(
    SubscriptionService* subscription_service,
    PrefService* pref_service)
    : subscription_service_(subscription_service),
      pref_service_(pref_service) {}

FilteringConfigurationBindings::~FilteringConfigurationBindings() {
  for (auto& pair : bound_counterparts_) {
    pair.first->RemoveObserver(this);
  }
}

void FilteringConfigurationBindings::Bind(
    const std::string& configuration_name,
    JavaObjectWeakGlobalRef filtering_configuration_java) {
  auto* existing_configuration =
      GetInstalledConfigurationWithName(configuration_name);
  if (existing_configuration) {
    auto existing_binding = bound_counterparts_.find(existing_configuration);
    if (existing_binding == bound_counterparts_.end()) {
      // This is the first Java-side counterpart bound to this
      // FilteringConfiguration.
      existing_configuration->AddObserver(this);
    }
    bound_counterparts_[existing_configuration].push_back(
        std::move(filtering_configuration_java));
  } else {
    // There is no already-installed FilteringConfiguration with this name.
    // Create one and bind to it.
    auto new_filtering_configuration =
        std::make_unique<PersistentFilteringConfiguration>(pref_service_,
                                                           configuration_name);
    new_filtering_configuration->AddObserver(this);
    bound_counterparts_[new_filtering_configuration.get()].push_back(
        std::move(filtering_configuration_java));
    subscription_service_->InstallFilteringConfiguration(
        std::move(new_filtering_configuration));
  }
}

FilteringConfiguration*
FilteringConfigurationBindings::GetInstalledConfigurationWithName(
    const std::string& configuration_name) {
  const auto installed_configurations =
      subscription_service_->GetInstalledFilteringConfigurations();
  auto existing_configuration_it =
      base::ranges::find(installed_configurations, configuration_name,
                         &FilteringConfiguration::GetName);
  return existing_configuration_it != installed_configurations.end()
             ? *existing_configuration_it
             : nullptr;
}

void FilteringConfigurationBindings::OnEnabledStateChanged(
    FilteringConfiguration* config) {
  Notify(config, Java_FilteringConfiguration_enabledStateChanged);
}

void FilteringConfigurationBindings::OnFilterListsChanged(
    FilteringConfiguration* config) {
  Notify(config, Java_FilteringConfiguration_filterListsChanged);
}

void FilteringConfigurationBindings::OnAllowedDomainsChanged(
    FilteringConfiguration* config) {
  Notify(config, Java_FilteringConfiguration_allowedDomainsChanged);
}

void FilteringConfigurationBindings::OnCustomFiltersChanged(
    FilteringConfiguration* config) {
  Notify(config, Java_FilteringConfiguration_customFiltersChanged);
}

void FilteringConfigurationBindings::Notify(
    FilteringConfiguration* config,
    FilteringConfigurationBindings::JavaEventListener event_listener_function) {
  auto bound_weak_refs = bound_counterparts_.find(config);
  DCHECK(bound_weak_refs != bound_counterparts_.end())
      << "This should never receive notifications from unobserved "
         "FilteringConfigurations";
  JNIEnv* env = base::android::AttachCurrentThread();
  for (auto& weak_ref : bound_weak_refs->second) {
    auto java_counterpart = weak_ref.get(env);
    if (java_counterpart.is_null())
      continue;  // Bound counterpart may have been destroyed on Java side
    event_listener_function(env, java_counterpart);
  }
}

}  // namespace adblock

adblock::FilteringConfigurationBindings& GetBindingsFromLastUsedProfile() {
  auto* bindings =
      adblock::FilteringConfigurationBindingsFactory::GetForBrowserContext(
          g_browser_process->profile_manager()->GetLastUsedProfile());
  DCHECK(bindings) << "FilteringConfigurationBindings should be non-null even "
                      "in tests, to keep the code simple";
  return *bindings;
}

adblock::FilteringConfiguration& GetConfigurationWithName(
    const base::android::JavaParamRef<jstring>& configuration_name) {
  auto& bindings = GetBindingsFromLastUsedProfile();
  auto* configuration = bindings.GetInstalledConfigurationWithName(
      base::android::ConvertJavaStringToUTF8(configuration_name));
  DCHECK(configuration)
      << "FilteringConfigurationBindings::Bind should have ensured a "
         "configuration with this name is installed";
  return *configuration;
}

static void JNI_FilteringConfiguration_Bind(
    JNIEnv* env,
    const base::android::JavaParamRef<jstring>& configuration_name,
    const base::android::JavaParamRef<jobject>& caller) {
  auto& bindings = GetBindingsFromLastUsedProfile();
  JavaObjectWeakGlobalRef weak_configuration_ref(env, caller);
  auto cpp_name = base::android::ConvertJavaStringToUTF8(configuration_name);
  bindings.Bind(cpp_name, weak_configuration_ref);
}

static jboolean JNI_FilteringConfiguration_IsEnabled(
    JNIEnv* env,
    const base::android::JavaParamRef<jstring>& configuration_name) {
  return GetConfigurationWithName(configuration_name).IsEnabled() ? JNI_TRUE
                                                                  : JNI_FALSE;
}

static void JNI_FilteringConfiguration_SetEnabled(
    JNIEnv* env,
    const base::android::JavaParamRef<jstring>& configuration_name,
    jboolean j_enabled) {
  return GetConfigurationWithName(configuration_name)
      .SetEnabled(j_enabled == JNI_TRUE);
}

static void JNI_FilteringConfiguration_AddFilterList(
    JNIEnv* env,
    const base::android::JavaParamRef<jstring>& configuration_name,
    const base::android::JavaParamRef<jstring>& url) {
  GetConfigurationWithName(configuration_name)
      .AddFilterList(GURL{base::android::ConvertJavaStringToUTF8(url)});
}

static void JNI_FilteringConfiguration_RemoveFilterList(
    JNIEnv* env,
    const base::android::JavaParamRef<jstring>& configuration_name,
    const base::android::JavaParamRef<jstring>& url) {
  GetConfigurationWithName(configuration_name)
      .RemoveFilterList(GURL{base::android::ConvertJavaStringToUTF8(url)});
}

static base::android::ScopedJavaLocalRef<jobjectArray>
JNI_FilteringConfiguration_GetFilterLists(
    JNIEnv* env,
    const base::android::JavaParamRef<jstring>& configuration_name) {
  // For simplicity, convert GURL to std::string, pass to Java, and convert from
  // String to URL. Strings are easier to pass through JNI.
  std::vector<std::string> urls;
  base::ranges::transform(
      GetConfigurationWithName(configuration_name).GetFilterLists(),
      std::back_inserter(urls), &GURL::spec);
  return base::android::ToJavaArrayOfStrings(env, urls);
}

static void JNI_FilteringConfiguration_AddAllowedDomain(
    JNIEnv* env,
    const base::android::JavaParamRef<jstring>& configuration_name,
    const base::android::JavaParamRef<jstring>& allowed_domain) {
  GetConfigurationWithName(configuration_name)
      .AddAllowedDomain(base::android::ConvertJavaStringToUTF8(allowed_domain));
}

static void JNI_FilteringConfiguration_RemoveAllowedDomain(
    JNIEnv* env,
    const base::android::JavaParamRef<jstring>& configuration_name,
    const base::android::JavaParamRef<jstring>& allowed_domain) {
  GetConfigurationWithName(configuration_name)
      .RemoveAllowedDomain(ConvertJavaStringToUTF8(allowed_domain));
}

static base::android::ScopedJavaLocalRef<jobjectArray>
JNI_FilteringConfiguration_GetAllowedDomains(
    JNIEnv* env,
    const base::android::JavaParamRef<jstring>& configuration_name) {
  return base::android::ToJavaArrayOfStrings(
      env, GetConfigurationWithName(configuration_name).GetAllowedDomains());
}

static void JNI_FilteringConfiguration_AddCustomFilter(
    JNIEnv* env,
    const base::android::JavaParamRef<jstring>& configuration_name,
    const base::android::JavaParamRef<jstring>& custom_filter) {
  GetConfigurationWithName(configuration_name)
      .AddCustomFilter(ConvertJavaStringToUTF8(custom_filter));
}

static void JNI_FilteringConfiguration_RemoveCustomFilter(
    JNIEnv* env,
    const base::android::JavaParamRef<jstring>& configuration_name,
    const base::android::JavaParamRef<jstring>& custom_filter) {
  GetConfigurationWithName(configuration_name)
      .RemoveCustomFilter(
          base::android::ConvertJavaStringToUTF8(custom_filter));
}

static base::android::ScopedJavaLocalRef<jobjectArray>
JNI_FilteringConfiguration_GetCustomFilters(
    JNIEnv* env,
    const base::android::JavaParamRef<jstring>& configuration_name) {
  return base::android::ToJavaArrayOfStrings(
      env, GetConfigurationWithName(configuration_name).GetCustomFilters());
}