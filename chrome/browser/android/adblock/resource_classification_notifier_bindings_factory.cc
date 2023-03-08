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

#include "chrome/browser/android/adblock/resource_classification_notifier_bindings_factory.h"

#include "chrome/browser/adblock/resource_classification_runner_factory.h"
#include "chrome/browser/android/adblock/resource_classification_notifier_bindings.h"
#include "chrome/browser/profiles/incognito_helpers.h"
#include "components/keyed_service/content/browser_context_dependency_manager.h"

namespace adblock {

// static
ResourceClassificationNotifierBindings*
ResourceClassificationNotifierBindingsFactory::GetForBrowserContext(
    content::BrowserContext* context) {
  return static_cast<ResourceClassificationNotifierBindings*>(
      GetInstance()->GetServiceForBrowserContext(context, true));
}
// static
ResourceClassificationNotifierBindingsFactory*
ResourceClassificationNotifierBindingsFactory::GetInstance() {
  static base::NoDestructor<ResourceClassificationNotifierBindingsFactory>
      instance;
  return instance.get();
}

ResourceClassificationNotifierBindingsFactory::
    ResourceClassificationNotifierBindingsFactory()
    : BrowserContextKeyedServiceFactory(
          "ResourceClassificationNotifierBindings",
          BrowserContextDependencyManager::GetInstance()) {
  DependsOn(ResourceClassificationRunnerFactory::GetInstance());
}

ResourceClassificationNotifierBindingsFactory::
    ~ResourceClassificationNotifierBindingsFactory() = default;

KeyedService*
ResourceClassificationNotifierBindingsFactory::BuildServiceInstanceFor(
    content::BrowserContext* context) const {
  return new ResourceClassificationNotifierBindings(
      ResourceClassificationRunnerFactory::GetForBrowserContext(context));
}

content::BrowserContext*
ResourceClassificationNotifierBindingsFactory::GetBrowserContextToUse(
    content::BrowserContext* context) const {
  return chrome::GetBrowserContextRedirectedInIncognito(context);
}

bool ResourceClassificationNotifierBindingsFactory::
    ServiceIsCreatedWithBrowserContext() const {
  return true;
}

}  // namespace adblock
