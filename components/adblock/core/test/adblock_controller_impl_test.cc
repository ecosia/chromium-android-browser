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

#include <iterator>
#include <memory>
#include <string>
#include <vector>

#include "base/functional/callback_forward.h"
#include "base/memory/scoped_refptr.h"
#include "base/test/gmock_callback_support.h"
#include "base/test/task_environment.h"
#include "base/value_iterators.h"
#include "components/adblock/core/adblock_controller.h"
#include "components/adblock/core/common/adblock_constants.h"
#include "components/adblock/core/common/adblock_prefs.h"
#include "components/adblock/core/configuration/filtering_configuration.h"
#include "components/adblock/core/subscription/subscription_config.h"
#include "components/adblock/core/subscription/test/mock_subscription.h"
#include "components/adblock/core/subscription/test/mock_subscription_service.h"
#include "components/prefs/pref_member.h"
#include "components/prefs/testing_pref_service.h"
#include "gmock/gmock.h"
#include "testing/gmock/include/gmock/gmock.h"
#include "testing/gtest/include/gtest/gtest.h"

namespace adblock {

using InstallationState = Subscription::InstallationState;
using testing::_;
using testing::Return;

namespace {

class MockObserver : public AdblockController::Observer {
 public:
  MOCK_METHOD(void, OnSubscriptionUpdated, (const GURL& url), (override));
  MOCK_METHOD(void, OnEnabledStateChanged, (), (override));
};

class MockFilteringConfiguration : public FilteringConfiguration {
 public:
  MOCK_METHOD(void, AddObserver, (Observer * observer), (override));
  MOCK_METHOD(void, RemoveObserver, (Observer * observer), (override));

  MOCK_METHOD(const std::string&, GetName, (), (const, override));

  MOCK_METHOD(void, SetEnabled, (bool enabled), (override));
  MOCK_METHOD(bool, IsEnabled, (), (const, override));

  MOCK_METHOD(void, AddFilterList, (GURL url), (override));
  MOCK_METHOD(void, RemoveFilterList, (GURL url), (override));
  MOCK_METHOD(std::vector<GURL>, GetFilterLists, (), (const, override));

  MOCK_METHOD(void, AddAllowedDomain, (std::string domain), (override));
  MOCK_METHOD(void, RemoveAllowedDomain, (std::string domain), (override));
  MOCK_METHOD(std::vector<std::string>,
              GetAllowedDomains,
              (),
              (const, override));

  MOCK_METHOD(void, AddCustomFilter, (std::string filter), (override));
  MOCK_METHOD(void, RemoveCustomFilter, (std::string filter), (override));
  MOCK_METHOD(std::vector<std::string>,
              GetCustomFilters,
              (),
              (const, override));
};

}  // namespace

bool operator==(const KnownSubscriptionInfo& lhs,
                const KnownSubscriptionInfo& rhs) {
  return lhs.url == rhs.url && lhs.languages == rhs.languages &&
         lhs.title == rhs.title;
}

class AdblockControllerImplTest : public testing::Test {
 public:
  AdblockControllerImplTest() {
    prefs::RegisterProfilePrefs(pref_service_.registry());
    RecreateController();
    custom_subscriptions_pref_.Init(prefs::kAdblockCustomSubscriptionsLegacy,
                                    &pref_service_);
    subscriptions_pref_.Init(prefs::kAdblockSubscriptionsLegacy,
                             &pref_service_);
  }

  void RecreateController(
      std::string locale = "pl-PL",
      std::vector<KnownSubscriptionInfo> known_subscriptions =
          config::GetKnownSubscriptions()) {
    if (testee_) {
      testee_->RemoveObserver(&observer_);
      testee_.reset();
    }
    testee_ = std::make_unique<AdblockControllerImpl>(
        &filtering_configuration_, &subscription_service_, locale,
        std::move(known_subscriptions));
    EXPECT_EQ(subscription_service_.observer_, testee_.get());
    testee_->AddObserver(&observer_);
  }

  void ExpectInstallationTriggered(const GURL& subscription_url) {
    EXPECT_CALL(filtering_configuration_, AddFilterList(subscription_url));
  }

  void ExpectNoInstallation(const GURL& subscription_url) {
    EXPECT_CALL(filtering_configuration_, AddFilterList(subscription_url))
        .Times(0);
  }

  void ExpectRemoval(const GURL& subscription_url) {
    EXPECT_CALL(filtering_configuration_, RemoveFilterList(subscription_url));
  }

  void ExpectNoRemoval(const GURL& subscription_url) {
    EXPECT_CALL(filtering_configuration_, RemoveFilterList(subscription_url))
        .Times(0);
  }

  base::test::TaskEnvironment task_environment_;
  MockObserver observer_;
  TestingPrefServiceSimple pref_service_;
  MockFilteringConfiguration filtering_configuration_;
  MockSubscriptionService subscription_service_;
  StringListPrefMember custom_subscriptions_pref_;
  StringListPrefMember subscriptions_pref_;
  std::unique_ptr<AdblockControllerImpl> testee_;
  const GURL kRecommendedSubscription{
      "https://easylist-downloads.adblockplus.org/easylistpolish+easylist.txt"};
};

TEST_F(AdblockControllerImplTest, EnableAndDisableAdBlocking) {
  EXPECT_CALL(filtering_configuration_, IsEnabled()).WillOnce(Return(true));
  EXPECT_TRUE(testee_->IsAdblockEnabled());
  EXPECT_CALL(filtering_configuration_, IsEnabled()).WillOnce(Return(false));
  EXPECT_FALSE(testee_->IsAdblockEnabled());

  // Switching state notifies observers and stores into FilteringConfiguration.
  EXPECT_CALL(observer_, OnEnabledStateChanged());
  EXPECT_CALL(filtering_configuration_, SetEnabled(true));
  testee_->SetAdblockEnabled(true);
}

TEST_F(AdblockControllerImplTest, GetKnownSubscriptions) {
  EXPECT_EQ(config::GetKnownSubscriptions(), testee_->GetKnownSubscriptions());
}

TEST_F(AdblockControllerImplTest, InstallingSubscription) {
  const GURL subscription_url("https://subscription.com/1.txt");
  ExpectInstallationTriggered(subscription_url);

  testee_->InstallSubscription(subscription_url);

  // SubscriptionService notifies observers about installation progress, this is
  // relayed to AdblockController's observer.
  EXPECT_CALL(observer_, OnSubscriptionUpdated(subscription_url));
  subscription_service_.observer_->OnSubscriptionInstalled(subscription_url);
}

TEST_F(AdblockControllerImplTest, UninstallingSubscription) {
  const GURL subscription_url("https://subscription.com/1.txt");

  // Uninstalling subscription just removes from FilteringConfiguration.
  ExpectRemoval(subscription_url);
  testee_->UninstallSubscription(subscription_url);
}

TEST_F(AdblockControllerImplTest, SubscriptionsPrefsMigrated) {
  const GURL subscription_url1("https://subscription.com/1.txt");
  const GURL subscription_url2("https://subscription.com/2.txt");
  subscriptions_pref_.SetValue(
      {subscription_url1.spec(), subscription_url2.spec()});

  ExpectInstallationTriggered(subscription_url1);
  ExpectInstallationTriggered(subscription_url2);

  RecreateController();
  testee_->MigrateLegacyPrefs(&pref_service_);

  // When recreating again, no installations triggered because migration is
  // only done once.
  ExpectNoInstallation(subscription_url1);
  ExpectNoInstallation(subscription_url2);
  RecreateController();
  testee_->MigrateLegacyPrefs(&pref_service_);
}

TEST_F(AdblockControllerImplTest, CustomSubscriptionsMigratedFromPrefs) {
  const GURL subscription_url1("https://subscription.com/1.txt");
  const GURL subscription_url2("https://subscription.com/2.txt");
  custom_subscriptions_pref_.SetValue(
      {subscription_url1.spec(), subscription_url2.spec()});
  ExpectInstallationTriggered(subscription_url1);
  ExpectInstallationTriggered(subscription_url2);

  RecreateController();
  testee_->MigrateLegacyPrefs(&pref_service_);

  // When recreating again, no installations triggered because migration is
  // only done once.
  ExpectNoInstallation(subscription_url1);
  ExpectNoInstallation(subscription_url2);
  RecreateController();
  testee_->MigrateLegacyPrefs(&pref_service_);
}

TEST_F(AdblockControllerImplTest, EnableAcceptableAdsfPrefMigrated) {
  pref_service_.SetBoolean(prefs::kEnableAcceptableAdsLegacy, false);
  // Acceptable Ads is not installed (and even removed, if it was installed
  // before) as old profile had it disabled.
  ExpectRemoval(AcceptableAdsUrl());
  RecreateController();
  testee_->MigrateLegacyPrefs(&pref_service_);
  // Migration only happens once.
  ExpectNoRemoval(AcceptableAdsUrl());
  RecreateController();
  testee_->MigrateLegacyPrefs(&pref_service_);

  pref_service_.SetBoolean(prefs::kEnableAcceptableAdsLegacy, true);
  // Acceptable Ads is installed but only once.
  ExpectInstallationTriggered(AcceptableAdsUrl());
  RecreateController();
  testee_->MigrateLegacyPrefs(&pref_service_);

  ExpectNoInstallation(AcceptableAdsUrl());
  RecreateController();
  testee_->MigrateLegacyPrefs(&pref_service_);
}

TEST_F(AdblockControllerImplTest, EnabledStatePrefMigrated) {
  // Pref is migrated once.
  pref_service_.SetBoolean(prefs::kEnableAdblockLegacy, false);
  EXPECT_CALL(filtering_configuration_, SetEnabled(false));
  RecreateController();
  testee_->MigrateLegacyPrefs(&pref_service_);
  EXPECT_CALL(filtering_configuration_, SetEnabled(_)).Times(0);
  RecreateController();
  testee_->MigrateLegacyPrefs(&pref_service_);

  // Works for true value as well.
  pref_service_.SetBoolean(prefs::kEnableAdblockLegacy, true);
  EXPECT_CALL(filtering_configuration_, SetEnabled(true));
  RecreateController();
  testee_->MigrateLegacyPrefs(&pref_service_);
  EXPECT_CALL(filtering_configuration_, SetEnabled(_)).Times(0);
  RecreateController();
  testee_->MigrateLegacyPrefs(&pref_service_);
}

TEST_F(AdblockControllerImplTest, EnablingAcceptableAdsInstallsSubscription) {
  // Initially, AA is not installed.
  EXPECT_CALL(filtering_configuration_, GetFilterLists())
      .WillRepeatedly(Return(std::vector<GURL>{DefaultSubscriptionUrl()}));
  EXPECT_FALSE(testee_->IsAcceptableAdsEnabled());

  // Enabling AA adds new filter list.
  ExpectInstallationTriggered(AcceptableAdsUrl());
  testee_->SetAcceptableAdsEnabled(true);

  // FilteringConfiguration now reports AA is installed.
  EXPECT_CALL(filtering_configuration_, GetFilterLists())
      .WillRepeatedly(Return(
          std::vector<GURL>{DefaultSubscriptionUrl(), AcceptableAdsUrl()}));
  EXPECT_TRUE(testee_->IsAcceptableAdsEnabled());
}

TEST_F(AdblockControllerImplTest, DisablingAcceptableAdsRemovesSubscription) {
  // Initially, AA is installed.
  EXPECT_CALL(filtering_configuration_, GetFilterLists())
      .WillRepeatedly(Return(
          std::vector<GURL>{DefaultSubscriptionUrl(), AcceptableAdsUrl()}));
  EXPECT_TRUE(testee_->IsAcceptableAdsEnabled());

  // Disabling AA removes the filter list.
  ExpectRemoval(AcceptableAdsUrl());
  testee_->SetAcceptableAdsEnabled(false);

  // FilteringConfiguration now reports AA is not installed.
  EXPECT_CALL(filtering_configuration_, GetFilterLists())
      .WillRepeatedly(Return(std::vector<GURL>{DefaultSubscriptionUrl()}));
  EXPECT_FALSE(testee_->IsAcceptableAdsEnabled());
}

TEST_F(AdblockControllerImplTest, CustomFilters) {
  const auto installed_filters = std::vector<std::string>{"abc", "def"};
  EXPECT_CALL(filtering_configuration_, GetCustomFilters())
      .WillOnce(Return(installed_filters));
  EXPECT_EQ(testee_->GetCustomFilters(), installed_filters);

  EXPECT_CALL(filtering_configuration_, AddCustomFilter("ggg"));
  testee_->AddCustomFilter("ggg");

  EXPECT_CALL(filtering_configuration_, RemoveCustomFilter("ggg"));
  testee_->RemoveCustomFilter("ggg");
}

TEST_F(AdblockControllerImplTest, AllowedDomains) {
  const auto installed_allowed_domains =
      std::vector<std::string>{"abc.com", "def.com"};
  EXPECT_CALL(filtering_configuration_, GetAllowedDomains())
      .WillOnce(Return(installed_allowed_domains));
  EXPECT_EQ(testee_->GetAllowedDomains(), installed_allowed_domains);

  EXPECT_CALL(filtering_configuration_, AddAllowedDomain("ggg.com"));
  testee_->AddAllowedDomain("ggg.com");

  EXPECT_CALL(filtering_configuration_, RemoveAllowedDomain("ggg.com"));
  testee_->RemoveAllowedDomain("ggg.com");
}

TEST_F(AdblockControllerImplTest,
       InstallLanguageBasedRecommendationAndAntiCvOnFirstRun) {
  // Default prefs state indicates a first run situation.
  pref_service_.SetBoolean(prefs::kInstallFirstStartSubscriptions, true);
  // Since there aren't any pre-existing subscriptions migrated, we will install
  // a language-based recommendation.
  ExpectInstallationTriggered(kRecommendedSubscription);
  // By default, Acceptable Ads are enabled, so they will be installed as well.
  ExpectInstallationTriggered(AcceptableAdsUrl());
  // Anti-CV filter list is installed as well, to enable snippets.
  ExpectInstallationTriggered(AntiCVUrl());

  RecreateController();
  testee_->RunFirstRunLogic(&pref_service_);
}

TEST_F(AdblockControllerImplTest,
       InstallDefaultRecommendationForNonMatchingLanguage) {
  pref_service_.SetBoolean(prefs::kInstallFirstStartSubscriptions, true);
  // Since the language does not have a language-specific recommendation, we
  // install a default list.
  ExpectInstallationTriggered(DefaultSubscriptionUrl());
  // By default, Acceptable Ads are enabled, so they will be installed as well.
  ExpectInstallationTriggered(AcceptableAdsUrl());
  // Anti-CV filter list is installed as well, to enable snippets.
  ExpectInstallationTriggered(AntiCVUrl());

  RecreateController("th-TH");
  testee_->RunFirstRunLogic(&pref_service_);
}

TEST_F(AdblockControllerImplTest, InstallAntiCvOnFirstRunIfMigrating) {
  // Previous version of eyeo Chromium SDK had a specific subscription
  // installed. Note that this is a different subscription from the recommended
  // one for the user's locale. It will be installed on first run in the current
  // version.
  const GURL kMigratedSubscription{
      "https://easylist-downloads.adblockplus.org/"
      "easylistgermany+easylist.txt"};
  subscriptions_pref_.SetValue({kMigratedSubscription.spec()});

  pref_service_.SetBoolean(prefs::kInstallFirstStartSubscriptions, true);
  ExpectInstallationTriggered(kMigratedSubscription);
  ExpectInstallationTriggered(kRecommendedSubscription);
  ExpectInstallationTriggered(AntiCVUrl());
  ExpectInstallationTriggered(AcceptableAdsUrl());

  RecreateController();
  testee_->RunFirstRunLogic(&pref_service_);
  testee_->MigrateLegacyPrefs(&pref_service_);
}

TEST_F(AdblockControllerImplTest, AADefaultOffFirstRun) {
  pref_service_.SetBoolean(prefs::kInstallFirstStartSubscriptions, true);
  const std::vector<KnownSubscriptionInfo> recommendations = {
      {AcceptableAdsUrl(),
       "AA",
       {},
       SubscriptionUiVisibility::Invisible,
       SubscriptionFirstRunBehavior::Ignore,
       SubscriptionPrivilegedFilterStatus::Forbidden}};
  ExpectNoInstallation(AcceptableAdsUrl());
  RecreateController("pl-PL", recommendations);
  testee_->RunFirstRunLogic(&pref_service_);
}

TEST_F(AdblockControllerImplTest, AADefaultOnFirstRun) {
  pref_service_.SetBoolean(prefs::kInstallFirstStartSubscriptions, true);
  const std::vector<KnownSubscriptionInfo> recommendations = {
      {AcceptableAdsUrl(),
       "AA",
       {},
       SubscriptionUiVisibility::Invisible,
       SubscriptionFirstRunBehavior::Subscribe,
       SubscriptionPrivilegedFilterStatus::Forbidden}};
  ExpectInstallationTriggered(AcceptableAdsUrl());
  RecreateController("pl-PL", recommendations);
  testee_->RunFirstRunLogic(&pref_service_);
}

TEST_F(AdblockControllerImplTest,
       NoInstallationWhenDefaultSubscriptionIgnored) {
  pref_service_.SetBoolean(prefs::kInstallFirstStartSubscriptions, true);
  const std::vector<KnownSubscriptionInfo> recommendations = {
      {DefaultSubscriptionUrl(),
       "EasyList",
       {},
       SubscriptionUiVisibility::Invisible,
       SubscriptionFirstRunBehavior::Ignore,
       SubscriptionPrivilegedFilterStatus::Forbidden}};
  ExpectNoInstallation(DefaultSubscriptionUrl());
  RecreateController("pl-PL", recommendations);
  testee_->RunFirstRunLogic(&pref_service_);
}

TEST_F(AdblockControllerImplTest, SeveralLanguageSpecificSubscriptions) {
  pref_service_.SetBoolean(prefs::kInstallFirstStartSubscriptions, true);
  const auto kEnglishUrl1 = GURL("https://english.com/filter1.txt");
  const auto kEnglishUrl2 = GURL("https://english.com/filter2.txt");
  const auto kGermanUrl1 = GURL("https://german.com/filter1.txt");
  const std::vector<KnownSubscriptionInfo> recommendations = {
      {DefaultSubscriptionUrl(),
       "EasyList",
       {},
       SubscriptionUiVisibility::Invisible,
       SubscriptionFirstRunBehavior::Subscribe,
       SubscriptionPrivilegedFilterStatus::Forbidden},
      {kEnglishUrl1,
       "English list 1",
       {"en"},
       SubscriptionUiVisibility::Invisible,
       SubscriptionFirstRunBehavior::SubscribeIfLocaleMatch,
       SubscriptionPrivilegedFilterStatus::Forbidden},
      {kEnglishUrl2,
       "English list 2",
       {"en"},
       SubscriptionUiVisibility::Invisible,
       SubscriptionFirstRunBehavior::SubscribeIfLocaleMatch,
       SubscriptionPrivilegedFilterStatus::Forbidden},
      {kGermanUrl1,
       "German list 1",
       {"de"},
       SubscriptionUiVisibility::Invisible,
       SubscriptionFirstRunBehavior::SubscribeIfLocaleMatch,
       SubscriptionPrivilegedFilterStatus::Forbidden}};
  // Installed because SubscriptionFirstRunBehavior::Subscribe, not specific to
  // any language.
  ExpectInstallationTriggered(DefaultSubscriptionUrl());
  // No installation because locale doesn't match.
  ExpectNoInstallation(kGermanUrl1);
  // All matching language-specific lists installed.
  ExpectInstallationTriggered(kEnglishUrl1);
  ExpectInstallationTriggered(kEnglishUrl2);
  RecreateController("en-US", recommendations);
  testee_->RunFirstRunLogic(&pref_service_);
}

}  // namespace adblock
