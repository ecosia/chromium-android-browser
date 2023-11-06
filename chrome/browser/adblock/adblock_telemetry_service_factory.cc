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

#include "chrome/browser/adblock/adblock_telemetry_service_factory.h"

#include "base/no_destructor.h"
#include "chrome/browser/adblock/subscription_service_factory.h"
#include "chrome/browser/profiles/incognito_helpers.h"
#include "chrome/browser/profiles/profile.h"
#include "components/adblock/core/adblock_telemetry_service.h"

namespace adblock {

// static
AdblockTelemetryService* AdblockTelemetryServiceFactory::GetForProfile(
    Profile* profile) {
  return static_cast<AdblockTelemetryService*>(
      GetInstance()->GetServiceForBrowserContext(profile, true));
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
  return Profile::FromBrowserContext(context)->GetOriginalProfile()->GetPrefs();
}

content::BrowserContext* AdblockTelemetryServiceFactory::GetBrowserContextToUse(
    content::BrowserContext* context) const {
  return chrome::GetBrowserContextRedirectedInIncognito(context);
}

}  // namespace adblock
