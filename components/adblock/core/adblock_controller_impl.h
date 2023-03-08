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

#ifndef COMPONENTS_ADBLOCK_CORE_ADBLOCK_CONTROLLER_IMPL_H_
#define COMPONENTS_ADBLOCK_CORE_ADBLOCK_CONTROLLER_IMPL_H_

#include <string>
#include <vector>

#include "base/memory/weak_ptr.h"
#include "base/sequence_checker.h"
#include "components/adblock/core/adblock_controller.h"
#include "components/adblock/core/configuration/filtering_configuration.h"
#include "components/adblock/core/subscription/subscription_config.h"
#include "components/adblock/core/subscription/subscription_service.h"
#include "components/prefs/pref_member.h"

class GURL;
class PrefService;

namespace adblock {

/**
 * @brief Implementation of the AdblockController interface. Uses a
 * FilteringConfiguration as the backend for all the state set via the
 * interface.
 */
class AdblockControllerImpl : public AdblockController,
                              public SubscriptionService::SubscriptionObserver {
 public:
  AdblockControllerImpl(FilteringConfiguration* adblock_filtering_configuration,
                        SubscriptionService* subscription_service,
                        const std::string& locale,
                        std::vector<KnownSubscriptionInfo> known_subscriptions);
  ~AdblockControllerImpl() override;

  AdblockControllerImpl(const AdblockControllerImpl&) = delete;
  AdblockControllerImpl& operator=(const AdblockControllerImpl&) = delete;
  AdblockControllerImpl(AdblockControllerImpl&&) = delete;
  AdblockControllerImpl& operator=(AdblockControllerImpl&&) = delete;

  void AddObserver(AdblockController::Observer* observer) override;
  void RemoveObserver(AdblockController::Observer* observer) override;

  void SetAdblockEnabled(bool enabled) override;
  bool IsAdblockEnabled() const override;
  void SetAcceptableAdsEnabled(bool enabled) override;
  bool IsAcceptableAdsEnabled() const override;

  void InstallSubscription(const GURL& url) override;
  void UninstallSubscription(const GURL& url) override;
  std::vector<scoped_refptr<Subscription>> GetInstalledSubscriptions()
      const override;

  void AddAllowedDomain(const std::string& domain) override;
  void RemoveAllowedDomain(const std::string& domain) override;
  std::vector<std::string> GetAllowedDomains() const override;

  void AddCustomFilter(const std::string& filter) override;
  void RemoveCustomFilter(const std::string& filter) override;
  std::vector<std::string> GetCustomFilters() const override;

  std::vector<KnownSubscriptionInfo> GetKnownSubscriptions() const override;

  // SubscriptionObserver:
  void OnSubscriptionInstalled(const GURL& subscription_url) override;

  void RunFirstRunLogic(PrefService* pref_service);
  void MigrateLegacyPrefs(PrefService* pref_service);

 protected:
  SEQUENCE_CHECKER(sequence_checker_);
  void NotifySubscriptionChanged(const GURL& subscription_url);
  void InstallLanguageBasedRecommendedSubscriptions();
  std::vector<scoped_refptr<Subscription>>
  GetSubscriptionsThatMatchConfiguration() const;

  FilteringConfiguration* adblock_filtering_configuration_;
  SubscriptionService* subscription_service_;
  std::string language_;
  std::vector<KnownSubscriptionInfo> known_subscriptions_;
  base::ObserverList<AdblockController::Observer> observers_;
  base::WeakPtrFactory<AdblockControllerImpl> weak_ptr_factory_{this};
};

}  // namespace adblock

#endif  // COMPONENTS_ADBLOCK_ADBLOCK_CONTROLLER_IMPL_H_
