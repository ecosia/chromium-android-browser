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

#include "chrome/browser/adblock/subscription_service_factory.h"

#include <memory>

#include "chrome/browser/adblock/subscription_persistent_metadata_factory.h"
#include "chrome/browser/browser_process.h"
#include "chrome/browser/profiles/incognito_helpers.h"
#include "chrome/browser/profiles/profile.h"
#include "components/adblock/core/subscription/subscription_service.h"
#include "components/keyed_service/content/browser_context_dependency_manager.h"
#include "content/public/browser/browser_context.h"

namespace adblock {

// static
SubscriptionService* SubscriptionServiceFactory::GetForBrowserContext(
    content::BrowserContext* context) {
  return static_cast<SubscriptionService*>(
      GetInstance()->GetServiceForBrowserContext(context, true));
}
// static
SubscriptionServiceFactory* SubscriptionServiceFactory::GetInstance() {
  static base::NoDestructor<SubscriptionServiceFactory> instance;
  return instance.get();
}

SubscriptionServiceFactory::SubscriptionServiceFactory()
    : SubscriptionServiceFactoryBase() {
  DependsOn(SubscriptionPersistentMetadataFactory::GetInstance());
}

SubscriptionServiceFactory::~SubscriptionServiceFactory() = default;

SubscriptionPersistentMetadata*
SubscriptionServiceFactory::GetSubscriptionPersistentMetadata(
    content::BrowserContext* context) const {
  return SubscriptionPersistentMetadataFactory::GetForBrowserContext(context);
}

PrefService* SubscriptionServiceFactory::GetPrefs(
    content::BrowserContext* context) const {
  return Profile::FromBrowserContext(context)->GetOriginalProfile()->GetPrefs();
}

const std::string& SubscriptionServiceFactory::GetLocale() const {
  return g_browser_process->GetApplicationLocale();
}

content::BrowserContext* SubscriptionServiceFactory::GetBrowserContextToUse(
    content::BrowserContext* context) const {
  return chrome::GetBrowserContextRedirectedInIncognito(context);
}

}  // namespace adblock
