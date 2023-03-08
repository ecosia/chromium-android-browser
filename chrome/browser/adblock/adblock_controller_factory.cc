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

#include "chrome/browser/adblock/adblock_controller_factory.h"

#include <memory>

#include "base/command_line.h"
#include "chrome/browser/adblock/subscription_service_factory.h"
#include "chrome/browser/browser_process.h"
#include "chrome/browser/profiles/incognito_helpers.h"
#include "chrome/browser/profiles/profile.h"
#include "components/adblock/core/adblock_controller_impl.h"
#include "components/adblock/core/adblock_switches.h"
#include "components/adblock/core/common/adblock_prefs.h"
#include "components/adblock/core/configuration/persistent_filtering_configuration.h"
#include "components/adblock/core/subscription/subscription_config.h"
#include "components/keyed_service/content/browser_context_dependency_manager.h"
#include "components/pref_registry/pref_registry_syncable.h"
#include "components/version_info/version_info.h"
#include "content/public/browser/browser_context.h"

namespace adblock {

// static
AdblockController* AdblockControllerFactory::GetForBrowserContext(
    content::BrowserContext* context) {
  return static_cast<AdblockController*>(
      GetInstance()->GetServiceForBrowserContext(context, true));
}
// static
AdblockControllerFactory* AdblockControllerFactory::GetInstance() {
  static base::NoDestructor<AdblockControllerFactory> instance;
  return instance.get();
}

AdblockControllerFactory::AdblockControllerFactory()
    : BrowserContextKeyedServiceFactory(
          "AdblockController",
          BrowserContextDependencyManager::GetInstance()) {
  DependsOn(SubscriptionServiceFactory::GetInstance());
}

AdblockControllerFactory::~AdblockControllerFactory() = default;

KeyedService* AdblockControllerFactory::BuildServiceInstanceFor(
    content::BrowserContext* context) const {
  auto* prefs = Profile::FromBrowserContext(context)->GetPrefs();
  auto adblock_filtering_configuration =
      std::make_unique<PersistentFilteringConfiguration>(prefs, "adblock");

  if (base::CommandLine::ForCurrentProcess()->HasSwitch(
          adblock::switches::kDisableAcceptableAds)) {
    adblock_filtering_configuration->RemoveFilterList(AcceptableAdsUrl());
  }
  if (base::CommandLine::ForCurrentProcess()->HasSwitch(
          switches::kDisableAdblock)) {
    adblock_filtering_configuration->SetEnabled(false);
  }

  auto* subscription_service =
      SubscriptionServiceFactory::GetForBrowserContext(context);
  auto controller = std::make_unique<AdblockControllerImpl>(
      adblock_filtering_configuration.get(), subscription_service,
      g_browser_process->GetApplicationLocale(),
      config::GetKnownSubscriptions());
  controller->RunFirstRunLogic(prefs);
  controller->MigrateLegacyPrefs(prefs);

  subscription_service->InstallFilteringConfiguration(
      std::move(adblock_filtering_configuration));

  return controller.release();
}

content::BrowserContext* AdblockControllerFactory::GetBrowserContextToUse(
    content::BrowserContext* context) const {
  return chrome::GetBrowserContextRedirectedInIncognito(context);
}

void AdblockControllerFactory::RegisterProfilePrefs(
    user_prefs::PrefRegistrySyncable* registry) {
  adblock::prefs::RegisterProfilePrefs(registry);
}

bool AdblockControllerFactory::ServiceIsNULLWhileTesting() const {
  return true;
}

bool AdblockControllerFactory::ServiceIsCreatedWithBrowserContext() const {
  return true;
}

}  // namespace adblock
