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

#include "components/adblock/core/adblock_controller_impl.h"

#include <algorithm>
#include <iterator>
#include <numeric>
#include <vector>

#include "absl/types/optional.h"
#include "base/functional/bind.h"
#include "base/command_line.h"
#include "base/logging.h"
#include "base/ranges/algorithm.h"
#include "base/strings/string_util.h"
#include "base/version.h"
#include "components/adblock/core/adblock_switches.h"
#include "components/adblock/core/common/adblock_constants.h"
#include "components/adblock/core/common/adblock_prefs.h"
#include "components/adblock/core/common/adblock_utils.h"
#include "components/adblock/core/subscription/installed_subscription.h"
#include "components/adblock/core/subscription/subscription.h"
#include "components/adblock/core/subscription/subscription_config.h"
#include "components/adblock/core/subscription/subscription_service.h"
#include "components/language/core/common/locale_util.h"
#include "components/prefs/pref_service.h"
#include "components/prefs/scoped_user_pref_update.h"
#include "components/version_info/version_info.h"
#include "url/gurl.h"

namespace adblock {

namespace {

template <typename T>
std::vector<T> MigrateItemsFromList(PrefService* pref_service,
                                    const std::string& pref_name) {
  std::vector<T> results;
  if (pref_service->FindPreference(pref_name)->HasUserSetting()) {
    const auto& list = pref_service->GetList(pref_name);
    for (const auto& item : list)
      if (item.is_string())
        results.emplace_back(item.GetString());
    pref_service->ClearPref(pref_name);
  }
  return results;
}

absl::optional<bool> MigrateBoolFromPrefs(PrefService* pref_service,
                                          const std::string& pref_name) {
  if (pref_service->FindPreference(pref_name)->HasUserSetting()) {
    bool value = pref_service->GetBoolean(pref_name);
    pref_service->ClearPref(pref_name);
    return value;
  }
  return absl::nullopt;
}

}  // namespace

AdblockControllerImpl::AdblockControllerImpl(
    FilteringConfiguration* adblock_filtering_configuration,
    SubscriptionService* subscription_service,
    const std::string& locale,
    std::vector<KnownSubscriptionInfo> known_subscriptions)
    : adblock_filtering_configuration_(adblock_filtering_configuration),
      subscription_service_(subscription_service),
      language_(language::ExtractBaseLanguage(locale)),
      known_subscriptions_(std::move(known_subscriptions)) {
  subscription_service_->AddObserver(this);
  // language::ExtractBaseLanguage is pretty basic, if it doesn't return
  // something that looks like a valid language, fallback to English and use the
  // default EasyList.
  if (language_.size() != 2u)
    language_ = "en";
}

AdblockControllerImpl::~AdblockControllerImpl() {
  subscription_service_->RemoveObserver(this);
}

void AdblockControllerImpl::AddObserver(AdblockController::Observer* observer) {
  DCHECK_CALLED_ON_VALID_SEQUENCE(sequence_checker_);
  observers_.AddObserver(observer);
}

void AdblockControllerImpl::RemoveObserver(
    AdblockController::Observer* observer) {
  DCHECK_CALLED_ON_VALID_SEQUENCE(sequence_checker_);
  observers_.RemoveObserver(observer);
}

void AdblockControllerImpl::SetAdblockEnabled(bool enabled) {
  adblock_filtering_configuration_->SetEnabled(enabled);
  for (auto& observer : observers_)
    observer.OnEnabledStateChanged();
}

bool AdblockControllerImpl::IsAdblockEnabled() const {
  return adblock_filtering_configuration_->IsEnabled();
}

void AdblockControllerImpl::SetAcceptableAdsEnabled(bool enabled) {
  if (enabled) {
    InstallSubscription(AcceptableAdsUrl());
  } else {
    UninstallSubscription(AcceptableAdsUrl());
  }
}

bool AdblockControllerImpl::IsAcceptableAdsEnabled() const {
  return base::ranges::any_of(
      adblock_filtering_configuration_->GetFilterLists(),
      [&](const auto& url) { return url == AcceptableAdsUrl(); });
}

void AdblockControllerImpl::InstallSubscription(const GURL& url) {
  adblock_filtering_configuration_->AddFilterList(url);
}

void AdblockControllerImpl::UninstallSubscription(const GURL& url) {
  adblock_filtering_configuration_->RemoveFilterList(url);
}

std::vector<scoped_refptr<Subscription>>
AdblockControllerImpl::GetInstalledSubscriptions() const {
  return GetSubscriptionsThatMatchConfiguration();
}

void AdblockControllerImpl::AddAllowedDomain(const std::string& domain) {
  adblock_filtering_configuration_->AddAllowedDomain(domain);
}

void AdblockControllerImpl::RemoveAllowedDomain(const std::string& domain) {
  adblock_filtering_configuration_->RemoveAllowedDomain(domain);
}

std::vector<std::string> AdblockControllerImpl::GetAllowedDomains() const {
  return adblock_filtering_configuration_->GetAllowedDomains();
}

void AdblockControllerImpl::AddCustomFilter(const std::string& filter) {
  adblock_filtering_configuration_->AddCustomFilter(filter);
}

void AdblockControllerImpl::RemoveCustomFilter(const std::string& filter) {
  adblock_filtering_configuration_->RemoveCustomFilter(filter);
}

std::vector<std::string> AdblockControllerImpl::GetCustomFilters() const {
  return adblock_filtering_configuration_->GetCustomFilters();
}

std::vector<KnownSubscriptionInfo>
AdblockControllerImpl::GetKnownSubscriptions() const {
  return known_subscriptions_;
}

void AdblockControllerImpl::OnSubscriptionInstalled(
    const GURL& subscription_url) {
  for (auto& observer : observers_)
    observer.OnSubscriptionUpdated(subscription_url);
}

void AdblockControllerImpl::RunFirstRunLogic(PrefService* pref_service) {
  // If the state of installed subscriptions in SubscriptionService is different
  // than the state in prefs, prefs take precedence.
  if (pref_service->GetBoolean(prefs::kInstallFirstStartSubscriptions)) {
    // On first run, install additional subscriptions.
    for (const auto& cur : known_subscriptions_) {
      if (cur.first_run == SubscriptionFirstRunBehavior::Subscribe) {
        if (cur.url == AcceptableAdsUrl() &&
            base::CommandLine::ForCurrentProcess()->HasSwitch(
                switches::kDisableAcceptableAds)) {
          // Do not install Acceptable Ads on first run because a command line
          // switch forbids it. Mostly used for testing.
          continue;
        }
        InstallSubscription(cur.url);
      }
    }

    InstallLanguageBasedRecommendedSubscriptions();
    pref_service->SetBoolean(prefs::kInstallFirstStartSubscriptions, false);
  }
}

void AdblockControllerImpl::MigrateLegacyPrefs(PrefService* pref_service) {
  if (auto aa_value = MigrateBoolFromPrefs(pref_service,
                                           prefs::kEnableAcceptableAdsLegacy)) {
    SetAcceptableAdsEnabled(*aa_value);
    VLOG(1) << "[eyeo] Migrated kEnableAcceptableAdsLegacy pref";
  }

  if (auto enable_value =
          MigrateBoolFromPrefs(pref_service, prefs::kEnableAdblockLegacy)) {
    SetAdblockEnabled(*enable_value);
    VLOG(1) << "[eyeo] Migrated kEnableAdblockLegacy pref";
  }

  for (const auto& url : MigrateItemsFromList<GURL>(
           pref_service, prefs::kAdblockCustomSubscriptionsLegacy)) {
    adblock_filtering_configuration_->AddFilterList(url);
    VLOG(1) << "[eyeo] Migrated " << url
            << " from kAdblockCustomSubscriptionsLegacy pref";
  }

  for (const auto& url : MigrateItemsFromList<GURL>(
           pref_service, prefs::kAdblockSubscriptionsLegacy)) {
    adblock_filtering_configuration_->AddFilterList(url);
    VLOG(1) << "[eyeo] Migrated " << url
            << " from kAdblockSubscriptionsLegacy pref";
  }

  for (const auto& domain : MigrateItemsFromList<std::string>(
           pref_service, prefs::kAdblockAllowedDomainsLegacy)) {
    adblock_filtering_configuration_->AddAllowedDomain(domain);
    VLOG(1) << "[eyeo] Migrated " << domain
            << " from kAdblockAllowedDomainsLegacy pref";
  }

  for (const auto& filter : MigrateItemsFromList<std::string>(
           pref_service, prefs::kAdblockCustomFiltersLegacy)) {
    adblock_filtering_configuration_->AddCustomFilter(filter);
    VLOG(1) << "[eyeo] Migrated " << filter
            << " from kAdblockCustomFiltersLegacy pref";
  }
}

void AdblockControllerImpl::InstallLanguageBasedRecommendedSubscriptions() {
  bool language_specific_subscription_installed = false;
  for (const auto& subscription : known_subscriptions_) {
    if (subscription.first_run ==
            SubscriptionFirstRunBehavior::SubscribeIfLocaleMatch &&
        std::find(subscription.languages.begin(), subscription.languages.end(),
                  language_) != subscription.languages.end()) {
      VLOG(1) << "[eyeo] Using recommended subscription for language \""
              << language_ << "\": " << subscription.title;
      language_specific_subscription_installed = true;
      InstallSubscription(subscription.url);
    }
  }
  if (language_specific_subscription_installed)
    return;

  // If there's no language-specific recommended subscription, see if we may
  // install the default subscription..
  if (base::ranges::any_of(
          known_subscriptions_, [&](const KnownSubscriptionInfo& subscription) {
            return subscription.url == DefaultSubscriptionUrl() &&
                   subscription.first_run !=
                       SubscriptionFirstRunBehavior::Ignore;
          })) {
    VLOG(1) << "[eyeo] Using the default subscription for language \""
            << language_ << "\"";
    InstallSubscription(DefaultSubscriptionUrl());
  }
  VLOG(1) << "[eyeo] No default subscription found, neither "
             "language-specific, nor generic.";
}

std::vector<scoped_refptr<Subscription>>
AdblockControllerImpl::GetSubscriptionsThatMatchConfiguration() const {
  return subscription_service_->GetCurrentSubscriptions(
      adblock_filtering_configuration_);
}

}  // namespace adblock
