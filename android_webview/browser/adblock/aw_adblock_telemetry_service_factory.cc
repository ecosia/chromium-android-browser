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

#include "android_webview/browser/adblock/aw_adblock_telemetry_service_factory.h"

#include "android_webview/browser/adblock/aw_subscription_service_factory.h"
#include "android_webview/browser/aw_browser_context.h"
#include "components/adblock/core/adblock_telemetry_service.h"
#include "content/public/browser/storage_partition.h"

namespace adblock {

// static
AdblockTelemetryService* AdblockTelemetryServiceFactory::GetForBrowserContext(
    content::BrowserContext* context) {
  return static_cast<AdblockTelemetryService*>(
      GetInstance()->GetServiceForBrowserContext(context, true));
}
// static
AdblockTelemetryServiceFactory* AdblockTelemetryServiceFactory::GetInstance() {
  static base::NoDestructor<AdblockTelemetryServiceFactory> instance;
  return instance.get();
}

AdblockTelemetryServiceFactory::AdblockTelemetryServiceFactory()
    : AdblockTelemetryServiceFactoryBase() {
  DependsOn(SubscriptionServiceFactory::GetInstance());
}

AdblockTelemetryServiceFactory::~AdblockTelemetryServiceFactory() = default;

SubscriptionService* AdblockTelemetryServiceFactory::GetSubscriptionService(
    content::BrowserContext* context) const {
  return SubscriptionServiceFactory::GetForBrowserContext(context);
}

PrefService* AdblockTelemetryServiceFactory::GetPrefs(
    content::BrowserContext* context) const {
  return static_cast<android_webview::AwBrowserContext*>(context)
      ->GetPrefService();
}

}  // namespace adblock
