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

#include "components/adblock/android/java_bindings_getters.h"

#include "chrome/browser/adblock/subscription_service_factory.h"
#include "chrome/browser/android/adblock/adblock_jni_factory.h"
#include "chrome/browser/android/adblock/filtering_configuration_bindings_factory.h"
#include "chrome/browser/android/adblock/resource_classification_notifier_bindings_factory.h"
#include "chrome/browser/android/tab_android.h"
#include "chrome/browser/browser_process.h"
#include "chrome/browser/profiles/profile.h"
#include "chrome/browser/profiles/profile_manager.h"
#include "components/adblock/android/filtering_configuration_bindings.h"
#include "components/adblock/android/resource_classification_notifier_bindings.h"
#include "content/public/browser/web_contents.h"

namespace adblock {
namespace {
constexpr int kNoTabId = -1;
}

SubscriptionService* GetSubscriptionService() {
  if (!g_browser_process || !g_browser_process->profile_manager()) {
    return nullptr;
  }
  return SubscriptionServiceFactory::GetForBrowserContext(
      g_browser_process->profile_manager()->GetLastUsedProfile());
}

AdblockJNI* GetJNI() {
  if (!g_browser_process || !g_browser_process->profile_manager()) {
    return nullptr;
  }
  return AdblockJNIFactory::GetForBrowserContext(
      g_browser_process->profile_manager()->GetLastUsedProfile());
}

adblock::FilteringConfigurationBindings& GetFilteringConfigurationBindings() {
  auto* bindings =
      adblock::FilteringConfigurationBindingsFactory::GetForBrowserContext(
          g_browser_process->profile_manager()->GetLastUsedProfile());
  DCHECK(bindings) << "FilteringConfigurationBindings should be non-null even "
                      "in tests, to keep the code simple";
  return *bindings;
}

adblock::ResourceClassificationNotifierBindings&
GetResourceClassificationNotifierBindings() {
  auto* bindings = adblock::ResourceClassificationNotifierBindingsFactory::
      GetForBrowserContext(
          g_browser_process->profile_manager()->GetLastUsedProfile());
  DCHECK(bindings)
      << "ResourceClassificationNotifierBindings should be non-null even "
         "in tests, to keep the code simple";
  return *bindings;
}

int GetTabId(content::RenderFrameHost* render_frame_host) {
  auto* web_contents =
      content::WebContents::FromRenderFrameHost(render_frame_host);
  if (!web_contents) {
    return kNoTabId;
  }

  auto* tab = TabAndroid::FromWebContents(web_contents);
  if (!tab) {
    return kNoTabId;
  }

  return tab->GetAndroidId();
}

}  // namespace adblock
