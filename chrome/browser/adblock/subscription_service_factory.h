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

#ifndef CHROME_BROWSER_ADBLOCK_SUBSCRIPTION_SERVICE_FACTORY_H_
#define CHROME_BROWSER_ADBLOCK_SUBSCRIPTION_SERVICE_FACTORY_H_

#include "base/no_destructor.h"
#include "components/adblock/content/browser/subscription_service_factory_base.h"
#include "content/public/browser/browser_context.h"

namespace adblock {

class SubscriptionService;
class SubscriptionServiceFactory : public SubscriptionServiceFactoryBase {
 public:
  static SubscriptionService* GetForBrowserContext(
      content::BrowserContext* context);
  static SubscriptionServiceFactory* GetInstance();

 protected:
  PrefService* GetPrefs(content::BrowserContext* context) const override;
  const std::string& GetLocale() const override;
  SubscriptionPersistentMetadata* GetSubscriptionPersistentMetadata(
      content::BrowserContext* context) const override;

 private:
  friend class base::NoDestructor<SubscriptionServiceFactory>;
  SubscriptionServiceFactory();
  ~SubscriptionServiceFactory() override;

  content::BrowserContext* GetBrowserContextToUse(
      content::BrowserContext* context) const override;
};

}  // namespace adblock

#endif  // CHROME_BROWSER_ADBLOCK_SUBSCRIPTION_SERVICE_FACTORY_H_
