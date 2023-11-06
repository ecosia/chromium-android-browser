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

#ifndef COMPONENTS_ADBLOCK_CONTENT_BROWSER_ADBLOCK_TELEMETRY_SERVICE_FACTORY_BASE_H_
#define COMPONENTS_ADBLOCK_CONTENT_BROWSER_ADBLOCK_TELEMETRY_SERVICE_FACTORY_BASE_H_

#include "base/time/time.h"
#include "components/keyed_service/content/browser_context_dependency_manager.h"
#include "components/keyed_service/content/browser_context_keyed_service_factory.h"
#include "components/prefs/pref_service.h"

namespace adblock {

class SubscriptionService;
class AdblockTelemetryServiceFactoryBase
    : public BrowserContextKeyedServiceFactory {
 public:
  // Sets the initial delay and interval checks required for browser tests.
  // Must be called before BuildServiceInstanceFor().
  void SetCheckAndDelayIntervalsForTesting(base::TimeDelta check_interval,
                                           base::TimeDelta initial_delay);

 protected:
  virtual PrefService* GetPrefs(content::BrowserContext* context) const = 0;
  virtual SubscriptionService* GetSubscriptionService(
      content::BrowserContext* context) const = 0;
  AdblockTelemetryServiceFactoryBase();
  ~AdblockTelemetryServiceFactoryBase() override;

 private:
  // BrowserContextKeyedServiceFactory:
  KeyedService* BuildServiceInstanceFor(
      content::BrowserContext* context) const override;
  bool ServiceIsNULLWhileTesting() const override;
  bool ServiceIsCreatedWithBrowserContext() const override;
};

}  // namespace adblock

#endif  // COMPONENTS_ADBLOCK_CONTENT_BROWSER_ADBLOCK_TELEMETRY_SERVICE_FACTORY_BASE_H_
