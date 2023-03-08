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
#include <list>
#include "base/ranges/algorithm.h"
#include "base/run_loop.h"
#include "chrome/browser/adblock/adblock_controller_factory.h"
#include "chrome/browser/adblock/subscription_service_factory.h"
#include "chrome/browser/profiles/profile.h"
#include "chrome/browser/ui/browser.h"
#include "chrome/test/base/in_process_browser_test.h"
#include "chrome/test/base/ui_test_utils.h"
#include "components/adblock/core/adblock_controller.h"
#include "components/adblock/core/common/adblock_constants.h"
#include "components/adblock/core/configuration/filtering_configuration.h"
#include "components/adblock/core/configuration/persistent_filtering_configuration.h"
#include "components/adblock/core/subscription/subscription_service.h"
#include "components/version_info/version_info.h"
#include "content/public/browser/browser_task_traits.h"
#include "content/public/test/browser_test.h"
#include "content/public/test/browser_test_utils.h"
#include "gmock/gmock.h"
#include "net/dns/mock_host_resolver.h"
#include "net/test/embedded_test_server/embedded_test_server.h"
#include "url/gurl.h"

namespace adblock {

class SubscriptionInstalledWaiter
    : public SubscriptionService::SubscriptionObserver {
 public:
  explicit SubscriptionInstalledWaiter(
      SubscriptionService* subscription_service)
      : subscription_service_(subscription_service) {
    subscription_service_->AddObserver(this);
  }

  ~SubscriptionInstalledWaiter() override {
    subscription_service_->RemoveObserver(this);
  }

  void WaitUntilSubscriptionsInstalled(std::vector<GURL> subscriptions) {
    awaited_subscriptions_ = std::move(subscriptions);
    run_loop_.Run();
  }

  void OnSubscriptionInstalled(const GURL& subscription_url) override {
    awaited_subscriptions_.erase(
        base::ranges::remove(awaited_subscriptions_, subscription_url),
        awaited_subscriptions_.end());
    if (awaited_subscriptions_.empty())
      run_loop_.Quit();
  }

 protected:
  SubscriptionService* subscription_service_;
  base::RunLoop run_loop_;
  std::vector<GURL> awaited_subscriptions_;
};

class AdblockFilteringConfigurationBrowserTest : public InProcessBrowserTest {
 public:
  void SetUpInProcessBrowserTestFixture() override {
    InProcessBrowserTest::SetUpInProcessBrowserTestFixture();
    host_resolver()->AddRule("*", "127.0.0.1");
    embedded_test_server()->ServeFilesFromSourceDirectory(
        "chrome/test/data/adblock");
    ASSERT_TRUE(embedded_test_server()->Start());
  }

  void SetUpOnMainThread() override {
    auto* controller =
        AdblockControllerFactory::GetForBrowserContext(browser()->profile());
    controller->RemoveCustomFilter(kAllowlistEverythingFilter);
  }

  GURL BlockingFilterListUrl() {
    return embedded_test_server()->GetURL(
        "/filterlist_that_blocks_resource.txt");
  }

  GURL ElementHidingFilterListUrl() {
    return embedded_test_server()->GetURL(
        "/filterlist_that_hides_resource.txt");
  }

  GURL AllowingFilterListUrl() {
    return embedded_test_server()->GetURL(
        "/filterlist_that_allows_resource.txt");
  }

  GURL GetPageUrl() {
    return embedded_test_server()->GetURL("test.org", "/innermost_frame.html");
  }

  void NavigateToPage() {
    ASSERT_TRUE(NavigateToURL(
        browser()->tab_strip_model()->GetActiveWebContents(), GetPageUrl()));
  }

  std::unique_ptr<PersistentFilteringConfiguration> MakeConfiguration(
      std::string name) {
    return std::make_unique<PersistentFilteringConfiguration>(
        browser()->profile()->GetPrefs(), name);
  }

  void InstallFilteringConfiguration(
      std::unique_ptr<FilteringConfiguration> configuration) {
    SubscriptionServiceFactory::GetForBrowserContext(browser()->profile())
        ->InstallFilteringConfiguration(std::move(configuration));
  }

  void WaitUntilSubscriptionsInstalled(std::vector<GURL> subscriptions) {
    SubscriptionInstalledWaiter waiter(
        SubscriptionServiceFactory::GetForBrowserContext(browser()->profile()));
    waiter.WaitUntilSubscriptionsInstalled(std::move(subscriptions));
  }

  std::string GetResourcesComputedStyle() {
    const std::string javascript =
        "window.getComputedStyle(document.getElementById('subresource'))."
        "display";
    return content::EvalJs(browser()
                               ->tab_strip_model()
                               ->GetActiveWebContents()
                               ->GetPrimaryMainFrame(),
                           javascript)
        .ExtractString();
  }

  void ExpectResourceBlocked() {
    EXPECT_EQ("none", GetResourcesComputedStyle());
  }

  void ExpectResourceShown() {
    EXPECT_EQ("inline", GetResourcesComputedStyle());
  }
};

IN_PROC_BROWSER_TEST_F(AdblockFilteringConfigurationBrowserTest,
                       NoBlockingByDefault) {
  auto configuration = MakeConfiguration("config");
  InstallFilteringConfiguration(std::move(configuration));

  NavigateToPage();
  ExpectResourceShown();
}

IN_PROC_BROWSER_TEST_F(AdblockFilteringConfigurationBrowserTest,
                       ResourceBlockedByFilteringConfigurationsList) {
  auto configuration = MakeConfiguration("config");
  configuration->AddFilterList(BlockingFilterListUrl());

  InstallFilteringConfiguration(std::move(configuration));

  WaitUntilSubscriptionsInstalled({BlockingFilterListUrl()});

  NavigateToPage();
  ExpectResourceBlocked();
}

IN_PROC_BROWSER_TEST_F(AdblockFilteringConfigurationBrowserTest,
                       ResourceHiddenByFilteringConfigurationsList) {
  auto configuration = MakeConfiguration("config");
  configuration->AddFilterList(ElementHidingFilterListUrl());

  InstallFilteringConfiguration(std::move(configuration));

  WaitUntilSubscriptionsInstalled({ElementHidingFilterListUrl()});

  NavigateToPage();
  ExpectResourceBlocked();
}

IN_PROC_BROWSER_TEST_F(AdblockFilteringConfigurationBrowserTest,
                       ResourceAllowedByFilteringConfigurationsList) {
  auto configuration = MakeConfiguration("config");
  configuration->AddFilterList(BlockingFilterListUrl());
  configuration->AddFilterList(ElementHidingFilterListUrl());
  configuration->AddFilterList(AllowingFilterListUrl());

  InstallFilteringConfiguration(std::move(configuration));

  WaitUntilSubscriptionsInstalled({BlockingFilterListUrl(),
                                   AllowingFilterListUrl(),
                                   ElementHidingFilterListUrl()});

  NavigateToPage();
  ExpectResourceShown();
}

IN_PROC_BROWSER_TEST_F(AdblockFilteringConfigurationBrowserTest,
                       BlockingTakesPrecedenceBetweenConfigurations) {
  auto blocking_configuration = MakeConfiguration("blocking");
  blocking_configuration->AddFilterList(BlockingFilterListUrl());

  auto allowing_configuration = MakeConfiguration("allowing");
  allowing_configuration->AddFilterList(AllowingFilterListUrl());

  InstallFilteringConfiguration(std::move(blocking_configuration));
  InstallFilteringConfiguration(std::move(allowing_configuration));

  WaitUntilSubscriptionsInstalled(
      {BlockingFilterListUrl(), AllowingFilterListUrl()});

  NavigateToPage();
  ExpectResourceBlocked();
}

IN_PROC_BROWSER_TEST_F(AdblockFilteringConfigurationBrowserTest,
                       ElementBlockedByCustomFilter) {
  auto configuration = MakeConfiguration("config");
  configuration->AddCustomFilter("*resource.png");

  InstallFilteringConfiguration(std::move(configuration));

  NavigateToPage();
  ExpectResourceBlocked();
}

IN_PROC_BROWSER_TEST_F(AdblockFilteringConfigurationBrowserTest,
                       ElementAllowedByCustomFilter) {
  auto configuration = MakeConfiguration("config");
  configuration->AddCustomFilter("*resource.png");
  configuration->AddCustomFilter("@@*resource.png");

  InstallFilteringConfiguration(std::move(configuration));

  NavigateToPage();
  ExpectResourceShown();
}

IN_PROC_BROWSER_TEST_F(AdblockFilteringConfigurationBrowserTest,
                       ElementAllowedByAllowedDomain) {
  auto configuration = MakeConfiguration("config");
  configuration->AddCustomFilter("*resource.png");
  configuration->AddAllowedDomain(GetPageUrl().host());

  InstallFilteringConfiguration(std::move(configuration));

  NavigateToPage();
  ExpectResourceShown();
}

IN_PROC_BROWSER_TEST_F(AdblockFilteringConfigurationBrowserTest,
                       PRE_CustomFiltersPersist) {
  auto configuration = MakeConfiguration("persistent");
  // This custom filter will survive browser restart.
  configuration->AddCustomFilter("*resource.png");
}

IN_PROC_BROWSER_TEST_F(AdblockFilteringConfigurationBrowserTest,
                       CustomFiltersPersist) {
  auto configuration = MakeConfiguration("persistent");
  EXPECT_THAT(configuration->GetCustomFilters(),
              testing::UnorderedElementsAre("*resource.png"));

  InstallFilteringConfiguration(std::move(configuration));

  NavigateToPage();
  ExpectResourceBlocked();
}

IN_PROC_BROWSER_TEST_F(AdblockFilteringConfigurationBrowserTest,
                       DisabledConfigurationDoesNotBlock) {
  auto configuration = MakeConfiguration("config");
  configuration->AddCustomFilter("*resource.png");
  configuration->SetEnabled(false);

  InstallFilteringConfiguration(std::move(configuration));

  NavigateToPage();
  ExpectResourceShown();
}

IN_PROC_BROWSER_TEST_F(AdblockFilteringConfigurationBrowserTest,
                       ConfigurationCanBeUsedAfterInstalling) {
  auto configuration = MakeConfiguration("config");
  auto* configuration_ptr = configuration.get();

  InstallFilteringConfiguration(std::move(configuration));

  configuration_ptr->AddCustomFilter("*resource.png");

  NavigateToPage();
  ExpectResourceBlocked();
}

IN_PROC_BROWSER_TEST_F(AdblockFilteringConfigurationBrowserTest,
                       ConfigurationCanBeDisabledAfterInstalling) {
  auto configuration = MakeConfiguration("config");
  auto* configuration_ptr = configuration.get();

  InstallFilteringConfiguration(std::move(configuration));

  configuration_ptr->AddCustomFilter("*resource.png");
  configuration_ptr->SetEnabled(false);

  NavigateToPage();
  ExpectResourceShown();
}

IN_PROC_BROWSER_TEST_F(AdblockFilteringConfigurationBrowserTest,
                       SubscriptionsDownloadedAfterConfigurationEnabled) {
  auto configuration = MakeConfiguration("config");
  configuration->SetEnabled(false);
  configuration->AddFilterList(BlockingFilterListUrl());
  configuration->AddFilterList(ElementHidingFilterListUrl());
  configuration->AddFilterList(AllowingFilterListUrl());
  auto* configuration_ptr = configuration.get();

  InstallFilteringConfiguration(std::move(configuration));

  configuration_ptr->SetEnabled(true);

  WaitUntilSubscriptionsInstalled({BlockingFilterListUrl(),
                                   AllowingFilterListUrl(),
                                   ElementHidingFilterListUrl()});
}

IN_PROC_BROWSER_TEST_F(AdblockFilteringConfigurationBrowserTest,
                       PRE_DownloadedSubscriptionsPersistOnDisk) {
  auto configuration = MakeConfiguration("config");
  // This filter list setting will survive browser restart.
  configuration->AddFilterList(BlockingFilterListUrl());

  InstallFilteringConfiguration(std::move(configuration));

  // This downloaded subscription won't need to be re-downloaded after restart.
  WaitUntilSubscriptionsInstalled({BlockingFilterListUrl()});
}

IN_PROC_BROWSER_TEST_F(AdblockFilteringConfigurationBrowserTest,
                       DownloadedSubscriptionsPersistOnDisk) {
  auto configuration = MakeConfiguration("config");
  InstallFilteringConfiguration(std::move(configuration));

  NavigateToPage();
  ExpectResourceBlocked();
}

}  // namespace adblock
