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

#include "chrome/browser/android/adblock/filtering_configuration_bindings_factory.h"

#include "chrome/browser/adblock/subscription_service_factory.h"
#include "chrome/browser/android/adblock/filtering_configuration_bindings.h"
#include "chrome/browser/profiles/incognito_helpers.h"
#include "chrome/browser/profiles/profile.h"
#include "components/keyed_service/content/browser_context_dependency_manager.h"

namespace adblock {

// static
FilteringConfigurationBindings*
FilteringConfigurationBindingsFactory::GetForBrowserContext(
    content::BrowserContext* context) {
  return static_cast<FilteringConfigurationBindings*>(
      GetInstance()->GetServiceForBrowserContext(context, true));
}
// static
FilteringConfigurationBindingsFactory*
FilteringConfigurationBindingsFactory::GetInstance() {
  static base::NoDestructor<FilteringConfigurationBindingsFactory> instance;
  return instance.get();
}

FilteringConfigurationBindingsFactory::FilteringConfigurationBindingsFactory()
    : BrowserContextKeyedServiceFactory(
          "FilteringConfigurationBindings",
          BrowserContextDependencyManager::GetInstance()) {
  DependsOn(SubscriptionServiceFactory::GetInstance());
}

FilteringConfigurationBindingsFactory::
    ~FilteringConfigurationBindingsFactory() = default;

KeyedService* FilteringConfigurationBindingsFactory::BuildServiceInstanceFor(
    content::BrowserContext* context) const {
  return new FilteringConfigurationBindings(
      SubscriptionServiceFactory::GetForBrowserContext(context),
      Profile::FromBrowserContext(context)->GetPrefs());
}

content::BrowserContext*
FilteringConfigurationBindingsFactory::GetBrowserContextToUse(
    content::BrowserContext* context) const {
  return chrome::GetBrowserContextRedirectedInIncognito(context);
}

bool FilteringConfigurationBindingsFactory::ServiceIsCreatedWithBrowserContext()
    const {
  return true;
}

}  // namespace adblock
