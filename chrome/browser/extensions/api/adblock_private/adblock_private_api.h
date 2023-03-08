// This file is part of eyeo Chromium SDK,
// Copyright (C) 2006-present eyeo GmbH
//
// eyeo Chromium SDK is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License version 3 as
// published by the Free Software Foundation.
//
// eyeo Chromium SDK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with eyeo Chromium SDK.  If not, see <http://www.gnu.org/licenses/>.

#ifndef CHROME_BROWSER_EXTENSIONS_API_ADBLOCK_PRIVATE_ADBLOCK_PRIVATE_API_H_
#define CHROME_BROWSER_EXTENSIONS_API_ADBLOCK_PRIVATE_ADBLOCK_PRIVATE_API_H_

#include "chrome/common/extensions/api/adblock_private.h"
#include "components/adblock/core/adblock_controller.h"
#include "extensions/browser/browser_context_keyed_api_factory.h"
#include "extensions/browser/event_router.h"
#include "extensions/browser/extension_function.h"

class Profile;

namespace extensions {

class AdblockPrivateAPI : public BrowserContextKeyedAPI,
                          public EventRouter::Observer {
 public:
  static BrowserContextKeyedAPIFactory<AdblockPrivateAPI>* GetFactoryInstance();

  static AdblockPrivateAPI* Get(content::BrowserContext* context);

  explicit AdblockPrivateAPI(content::BrowserContext* context);
  ~AdblockPrivateAPI() override;
  friend class BrowserContextKeyedAPIFactory<AdblockPrivateAPI>;

  // BrowserContextKeyedAPI implementation.
  static const char* service_name() { return "AdblockPrivateAPI"; }
  static const bool kServiceRedirectedInIncognito = true;
  static const bool kServiceIsCreatedWithBrowserContext = true;
  void Shutdown() override;

  // EventRouter::Observer:
  void OnListenerAdded(const extensions::EventListenerInfo& details) override;

 private:
  content::BrowserContext* context_;
  class AdblockAPIEventRouter;
  std::unique_ptr<AdblockAPIEventRouter> event_router_;
};

template <>
void BrowserContextKeyedAPIFactory<
    AdblockPrivateAPI>::DeclareFactoryDependencies();

namespace api {

class AdblockPrivateSetEnabledFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("adblockPrivate.setEnabled",
                             ADBLOCKPRIVATE_SETENABLED)
  AdblockPrivateSetEnabledFunction();

 private:
  ~AdblockPrivateSetEnabledFunction() override;

  ResponseAction Run() override;

  AdblockPrivateSetEnabledFunction(const AdblockPrivateSetEnabledFunction&) =
      delete;
  AdblockPrivateSetEnabledFunction& operator=(
      const AdblockPrivateSetEnabledFunction&) = delete;
};

class AdblockPrivateIsEnabledFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("adblockPrivate.isEnabled",
                             ADBLOCKPRIVATE_ISENABLED)
  AdblockPrivateIsEnabledFunction();

 private:
  ~AdblockPrivateIsEnabledFunction() override;

  ResponseAction Run() override;

  AdblockPrivateIsEnabledFunction(const AdblockPrivateIsEnabledFunction&) =
      delete;
  AdblockPrivateIsEnabledFunction& operator=(
      const AdblockPrivateIsEnabledFunction&) = delete;
};

class AdblockPrivateSetAcceptableAdsEnabledFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("adblockPrivate.setAcceptableAdsEnabled",
                             ADBLOCKPRIVATE_SETACCEPTABLEADSENABLED)
  AdblockPrivateSetAcceptableAdsEnabledFunction();

 private:
  ~AdblockPrivateSetAcceptableAdsEnabledFunction() override;

  ResponseAction Run() override;

  AdblockPrivateSetAcceptableAdsEnabledFunction(
      const AdblockPrivateSetAcceptableAdsEnabledFunction&) = delete;
  AdblockPrivateSetAcceptableAdsEnabledFunction& operator=(
      const AdblockPrivateSetAcceptableAdsEnabledFunction&) = delete;
};

class AdblockPrivateIsAcceptableAdsEnabledFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("adblockPrivate.isAcceptableAdsEnabled",
                             ADBLOCKPRIVATE_ISACCEPTABLEADSENABLED)
  AdblockPrivateIsAcceptableAdsEnabledFunction();

 private:
  ~AdblockPrivateIsAcceptableAdsEnabledFunction() override;

  ResponseAction Run() override;

  AdblockPrivateIsAcceptableAdsEnabledFunction(
      const AdblockPrivateIsAcceptableAdsEnabledFunction&) = delete;
  AdblockPrivateIsAcceptableAdsEnabledFunction& operator=(
      const AdblockPrivateIsAcceptableAdsEnabledFunction&) = delete;
};

class AdblockPrivateGetBuiltInSubscriptionsFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("adblockPrivate.getBuiltInSubscriptions",
                             ADBLOCKPRIVATE_GETBUILTINSUBSCRIPTIONS)
  AdblockPrivateGetBuiltInSubscriptionsFunction();

 private:
  ~AdblockPrivateGetBuiltInSubscriptionsFunction() override;

  // ExtensionFunction:
  ResponseAction Run() override;

  AdblockPrivateGetBuiltInSubscriptionsFunction(
      const AdblockPrivateGetBuiltInSubscriptionsFunction&) = delete;
  AdblockPrivateGetBuiltInSubscriptionsFunction& operator=(
      const AdblockPrivateGetBuiltInSubscriptionsFunction&) = delete;
};

class AdblockPrivateInstallSubscriptionFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("adblockPrivate.installSubscription",
                             ADBLOCKPRIVATE_INSTALLSUBSCRIPTION)
  AdblockPrivateInstallSubscriptionFunction();

 private:
  ~AdblockPrivateInstallSubscriptionFunction() override;

  ResponseAction Run() override;

  AdblockPrivateInstallSubscriptionFunction(
      const AdblockPrivateInstallSubscriptionFunction&) = delete;
  AdblockPrivateInstallSubscriptionFunction& operator=(
      const AdblockPrivateInstallSubscriptionFunction&) = delete;
};

class AdblockPrivateUninstallSubscriptionFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("adblockPrivate.uninstallSubscription",
                             ADBLOCKPRIVATE_UNINSTALLSUBSCRIPTION)
  AdblockPrivateUninstallSubscriptionFunction();

 private:
  ~AdblockPrivateUninstallSubscriptionFunction() override;

  ResponseAction Run() override;

  AdblockPrivateUninstallSubscriptionFunction(
      const AdblockPrivateUninstallSubscriptionFunction&) = delete;
  AdblockPrivateUninstallSubscriptionFunction& operator=(
      const AdblockPrivateUninstallSubscriptionFunction&) = delete;
};

class AdblockPrivateGetInstalledSubscriptionsFunction
    : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("adblockPrivate.getInstalledSubscriptions",
                             ADBLOCKPRIVATE_GETINSTALLEDSUBSCRIPTIONS)
  AdblockPrivateGetInstalledSubscriptionsFunction();

 private:
  ~AdblockPrivateGetInstalledSubscriptionsFunction() override;

  ResponseAction Run() override;

  AdblockPrivateGetInstalledSubscriptionsFunction(
      const AdblockPrivateGetInstalledSubscriptionsFunction&) = delete;
  AdblockPrivateGetInstalledSubscriptionsFunction& operator=(
      const AdblockPrivateGetInstalledSubscriptionsFunction&) = delete;
};

class AdblockPrivateAddAllowedDomainFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("adblockPrivate.addAllowedDomain",
                             ADBLOCKPRIVATE_ADDALLOWEDDOMAIN)
  AdblockPrivateAddAllowedDomainFunction();

 private:
  ~AdblockPrivateAddAllowedDomainFunction() override;

  ResponseAction Run() override;

  AdblockPrivateAddAllowedDomainFunction(
      const AdblockPrivateAddAllowedDomainFunction&) = delete;
  AdblockPrivateAddAllowedDomainFunction& operator=(
      const AdblockPrivateAddAllowedDomainFunction&) = delete;
};

class AdblockPrivateRemoveAllowedDomainFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("adblockPrivate.removeAllowedDomain",
                             ADBLOCKPRIVATE_REMOVEALLOWEDDOMAIN)
  AdblockPrivateRemoveAllowedDomainFunction();

 private:
  ~AdblockPrivateRemoveAllowedDomainFunction() override;

  ResponseAction Run() override;

  AdblockPrivateRemoveAllowedDomainFunction(
      const AdblockPrivateRemoveAllowedDomainFunction&) = delete;
  AdblockPrivateRemoveAllowedDomainFunction& operator=(
      const AdblockPrivateRemoveAllowedDomainFunction&) = delete;
};

class AdblockPrivateGetAllowedDomainsFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("adblockPrivate.getAllowedDomains",
                             ADBLOCKPRIVATE_GETALLOWEDDOMAINS)
  AdblockPrivateGetAllowedDomainsFunction();

 private:
  ~AdblockPrivateGetAllowedDomainsFunction() override;

  ResponseAction Run() override;

  AdblockPrivateGetAllowedDomainsFunction(
      const AdblockPrivateGetAllowedDomainsFunction&) = delete;
  AdblockPrivateGetAllowedDomainsFunction& operator=(
      const AdblockPrivateGetAllowedDomainsFunction&) = delete;
};

class AdblockPrivateAddCustomFilterFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("adblockPrivate.addCustomFilter",
                             ADBLOCKPRIVATE_ADDCUSTOMFILTER)
  AdblockPrivateAddCustomFilterFunction();

 private:
  ~AdblockPrivateAddCustomFilterFunction() override;

  ResponseAction Run() override;

  AdblockPrivateAddCustomFilterFunction(
      const AdblockPrivateAddCustomFilterFunction&) = delete;
  AdblockPrivateAddCustomFilterFunction& operator=(
      const AdblockPrivateAddCustomFilterFunction&) = delete;
};

class AdblockPrivateRemoveCustomFilterFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("adblockPrivate.removeCustomFilter",
                             ADBLOCKPRIVATE_REMOVECUSTOMFILTER)
  AdblockPrivateRemoveCustomFilterFunction();

 private:
  ~AdblockPrivateRemoveCustomFilterFunction() override;

  ResponseAction Run() override;

  AdblockPrivateRemoveCustomFilterFunction(
      const AdblockPrivateRemoveCustomFilterFunction&) = delete;
  AdblockPrivateRemoveCustomFilterFunction& operator=(
      const AdblockPrivateRemoveCustomFilterFunction&) = delete;
};

class AdblockPrivateGetCustomFiltersFunction : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("adblockPrivate.getCustomFilters",
                             ADBLOCKPRIVATE_GETCUSTOMFILTERS)
  AdblockPrivateGetCustomFiltersFunction();

 private:
  ~AdblockPrivateGetCustomFiltersFunction() override;

  ResponseAction Run() override;

  AdblockPrivateGetCustomFiltersFunction(
      const AdblockPrivateGetCustomFiltersFunction&) = delete;
  AdblockPrivateGetCustomFiltersFunction& operator=(
      const AdblockPrivateGetCustomFiltersFunction&) = delete;
};

class AdblockPrivateGetSessionAllowedAdsCountFunction
    : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("adblockPrivate.getSessionAllowedAdsCount",
                             ADBLOCKPRIVATE_GETSESSIONALLOWEDADSCOUNT)
  AdblockPrivateGetSessionAllowedAdsCountFunction();

 private:
  ~AdblockPrivateGetSessionAllowedAdsCountFunction() override;

  ResponseAction Run() override;

  AdblockPrivateGetSessionAllowedAdsCountFunction(
      const AdblockPrivateGetSessionAllowedAdsCountFunction&) = delete;
  AdblockPrivateGetSessionAllowedAdsCountFunction& operator=(
      const AdblockPrivateGetSessionAllowedAdsCountFunction&) = delete;
};

class AdblockPrivateGetSessionBlockedAdsCountFunction
    : public ExtensionFunction {
 public:
  DECLARE_EXTENSION_FUNCTION("adblockPrivate.getSessionBlockedAdsCount",
                             ADBLOCKPRIVATE_GETSESSIONBLOCKEDADSCOUNT)
  AdblockPrivateGetSessionBlockedAdsCountFunction();

 private:
  ~AdblockPrivateGetSessionBlockedAdsCountFunction() override;

  ResponseAction Run() override;

  AdblockPrivateGetSessionBlockedAdsCountFunction(
      const AdblockPrivateGetSessionBlockedAdsCountFunction&) = delete;
  AdblockPrivateGetSessionBlockedAdsCountFunction& operator=(
      const AdblockPrivateGetSessionBlockedAdsCountFunction&) = delete;
};

}  // namespace api

}  // namespace extensions

#endif  // CHROME_BROWSER_EXTENSIONS_API_ADBLOCK_PRIVATE_ADBLOCK_PRIVATE_API_H_
