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

#include "android_webview/browser/adblock/aw_adblock_jni_factory.h"
#include "android_webview/browser/adblock/aw_filtering_configuration_bindings_factory.h"
#include "android_webview/browser/adblock/aw_resource_classification_notifier_bindings_factory.h"
#include "android_webview/browser/adblock/aw_subscription_service_factory.h"
#include "android_webview/browser/aw_browser_context.h"
#include "components/adblock/android/filtering_configuration_bindings.h"
#include "components/adblock/android/resource_classification_notifier_bindings.h"
#include "content/public/browser/web_contents.h"

namespace adblock {
namespace {
constexpr int kNoTabId = -1;
}

SubscriptionService* GetSubscriptionService() {
  return SubscriptionServiceFactory::GetForBrowserContext(
      android_webview::AwBrowserContext::GetDefault());
}

AdblockJNI* GetJNI() {
  return AdblockJNIFactory::GetForBrowserContext(
      android_webview::AwBrowserContext::GetDefault());
}

adblock::FilteringConfigurationBindings& GetFilteringConfigurationBindings() {
  auto* bindings =
      adblock::FilteringConfigurationBindingsFactory::GetForBrowserContext(
          android_webview::AwBrowserContext::GetDefault());
  DCHECK(bindings) << "FilteringConfigurationBindings should be non-null even "
                      "in tests, to keep the code simple";
  return *bindings;
}

adblock::ResourceClassificationNotifierBindings&
GetResourceClassificationNotifierBindings() {
  auto* bindings = adblock::ResourceClassificationNotifierBindingsFactory::
      GetForBrowserContext(android_webview::AwBrowserContext::GetDefault());
  DCHECK(bindings)
      << "ResourceClassificationNotifierBindings should be non-null even "
         "in tests, to keep the code simple";
  return *bindings;
}

int GetTabId(content::RenderFrameHost* render_frame_host) {
  return kNoTabId;
}

}  // namespace adblock
