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

#include <vector>

#include "base/ranges/algorithm.h"
#include "chrome/browser/adblock/adblock_controller_factory.h"
#include "chrome/browser/adblock/resource_classification_runner_factory.h"
#include "chrome/browser/profiles/profile.h"
#include "chrome/browser/ui/browser.h"
#include "chrome/test/base/in_process_browser_test.h"
#include "chrome/test/base/ui_test_utils.h"
#include "components/adblock/content/browser/adblock_filter_match.h"
#include "components/adblock/content/browser/frame_hierarchy_builder.h"
#include "components/adblock/content/browser/resource_classification_runner.h"
#include "components/adblock/core/adblock_controller.h"
#include "components/adblock/core/common/adblock_constants.h"
#include "components/blocked_content/popup_blocker_tab_helper.h"
#include "content/public/test/browser_test.h"
#include "content/public/test/browser_test_utils.h"
#include "net/dns/mock_host_resolver.h"
#include "net/test/embedded_test_server/embedded_test_server.h"
#include "testing/gtest/include/gtest/gtest.h"

namespace adblock {

class ResourceClassificationRunnerObserver
    : public ResourceClassificationRunner::Observer {
 public:
  ~ResourceClassificationRunnerObserver() override {
    VerifyNoUnexpectedNotifications();
  }
  // ResourceClassificationRunner::Observer:
  void OnAdMatched(const GURL& url,
                   FilterMatchResult match_result,
                   const std::vector<GURL>& parent_frame_urls,
                   ContentType content_type,
                   content::RenderFrameHost* render_frame_host,
                   const GURL& subscription) override {
    if (match_result == FilterMatchResult::kAllowRule) {
      allowed_ads_notifications.push_back(url);
    } else {
      blocked_ads_notifications.push_back(url);
    }
  }

  void OnPageAllowed(const GURL& url,
                     content::RenderFrameHost* render_frame_host,
                     const GURL& subscription) override {
    allowed_pages_notifications.push_back(url);
  }

  void OnPopupMatched(const GURL& url,
                      FilterMatchResult match_result,
                      const GURL& opener_url,
                      content::RenderFrameHost* render_frame_host,
                      const GURL& subscription) override {
    if (match_result == FilterMatchResult::kAllowRule) {
      allowed_popups_notifications.push_back(url);
    } else {
      blocked_popups_notifications.push_back(url);
    }
  }

  void VerifyNotificationSent(base::StringPiece path, std::vector<GURL>& list) {
    auto it = base::ranges::find(list, path, &GURL::ExtractFileName);
    ASSERT_FALSE(it == list.end()) << "Path " << path << " not on list";
    // Remove expected notifications so that we can verify there are no
    // unexpected notifications left by the end of each test.
    list.erase(it);
  }

  void VerifyNoUnexpectedNotifications() {
    EXPECT_TRUE(blocked_ads_notifications.empty());
    EXPECT_TRUE(allowed_ads_notifications.empty());
    EXPECT_TRUE(blocked_popups_notifications.empty());
    EXPECT_TRUE(allowed_popups_notifications.empty());
    EXPECT_TRUE(allowed_pages_notifications.empty());
  }

  std::vector<GURL> blocked_ads_notifications;
  std::vector<GURL> allowed_ads_notifications;
  std::vector<GURL> blocked_popups_notifications;
  std::vector<GURL> allowed_popups_notifications;
  std::vector<GURL> allowed_pages_notifications;
};

// Simulated setup:
// http://outer.com/outermost_frame.html
//   has an iframe: http://middle.com/middle_frame.html
//     has an iframe: http://inner.com/innermost_frame.html
//       has a subresource http://inner.com/resource.png
//
// All of these files are in chrome/test/data/adblock. Cross-domain distribution
// is simulated via SetupCrossSiteRedirector.
// innermost_frame.html reports whether resource.png is visible via
// window.top.postMessage to outermost_frame.html, which stores a global
// subresource_visible JS variable.
class AdblockFrameHierarchyBrowserTest : public InProcessBrowserTest {
 public:
  void SetUpOnMainThread() override {
    InProcessBrowserTest::SetUpOnMainThread();
    host_resolver()->AddRule("*", "127.0.0.1");
    embedded_test_server()->ServeFilesFromSourceDirectory(
        "chrome/test/data/adblock");
    content::SetupCrossSiteRedirector(embedded_test_server());
    ASSERT_TRUE(embedded_test_server()->Start());
    auto* classification_runner =
        ResourceClassificationRunnerFactory::GetForBrowserContext(
            browser()->profile());
    classification_runner->AddObserver(&observer);
  }

  void TearDownOnMainThread() override {
    auto* classification_runner =
        ResourceClassificationRunnerFactory::GetForBrowserContext(
            browser()->profile());
    classification_runner->RemoveObserver(&observer);
    InProcessBrowserTest::TearDownOnMainThread();
  }

  void SetFilters(std::vector<std::string> filters) {
    auto* controller =
        AdblockControllerFactory::GetForBrowserContext(browser()->profile());
    controller->RemoveCustomFilter(kAllowlistEverythingFilter);
    for (auto& filter : filters) {
      controller->AddCustomFilter(filter);
    }
  }

  void NavigateToOutermostFrame() {
    ASSERT_TRUE(ui_test_utils::NavigateToURL(
        browser(), embedded_test_server()->GetURL(
                       "/cross-site/outer.com/outermost_frame.html")));
  }

  void NavigateToPopupParentFrame() {
    ASSERT_TRUE(ui_test_utils::NavigateToURL(
        browser(), embedded_test_server()->GetURL(
                       "/cross-site/outer.com/popup_parent.html")));
  }

  void VerifyTargetResourceShown(bool expected_shown) {
    EXPECT_EQ(
        expected_shown,
        content::EvalJs(browser()->tab_strip_model()->GetActiveWebContents(),
                        "subresource_visible === true"));
  }

  int NumberOfOpenTabs() { return browser()->tab_strip_model()->GetTabCount(); }

  ResourceClassificationRunnerObserver observer;
};

IN_PROC_BROWSER_TEST_F(AdblockFrameHierarchyBrowserTest,
                       SubresourceShownWithNoFilters) {
  SetFilters({});
  NavigateToOutermostFrame();
  VerifyTargetResourceShown(true);
}

IN_PROC_BROWSER_TEST_F(AdblockFrameHierarchyBrowserTest, SubresourceBlocked) {
  SetFilters({"/resource.png"});
  NavigateToOutermostFrame();
  VerifyTargetResourceShown(false);
  observer.VerifyNotificationSent("resource.png",
                                  observer.blocked_ads_notifications);
}

IN_PROC_BROWSER_TEST_F(AdblockFrameHierarchyBrowserTest,
                       SubresourceAllowedViaInnerFrame) {
  SetFilters({"/resource.png", "@@||inner.com^$document"});
  NavigateToOutermostFrame();
  VerifyTargetResourceShown(true);
  observer.VerifyNotificationSent("resource.png",
                                  observer.allowed_ads_notifications);
}

IN_PROC_BROWSER_TEST_F(AdblockFrameHierarchyBrowserTest,
                       SubresourceAllowedViaMiddleFrame) {
  SetFilters({"/resource.png", "@@||middle.com^$document"});
  NavigateToOutermostFrame();
  VerifyTargetResourceShown(true);
  observer.VerifyNotificationSent("resource.png",
                                  observer.allowed_ads_notifications);
}

IN_PROC_BROWSER_TEST_F(AdblockFrameHierarchyBrowserTest,
                       SubresourceAllowedViaOutermostFrame) {
  SetFilters({"/resource.png", "@@||outer.com^$document"});
  NavigateToOutermostFrame();
  VerifyTargetResourceShown(true);
  observer.VerifyNotificationSent("resource.png",
                                  observer.allowed_ads_notifications);
  observer.VerifyNotificationSent("outermost_frame.html",
                                  observer.allowed_pages_notifications);
}

IN_PROC_BROWSER_TEST_F(AdblockFrameHierarchyBrowserTest,
                       SubresourceBlockedWhenInvalidAllowRule) {
  SetFilters({"/resource.png", "@@||bogus.com^$document"});
  NavigateToOutermostFrame();
  VerifyTargetResourceShown(false);
  observer.VerifyNotificationSent("resource.png",
                                  observer.blocked_ads_notifications);
}

IN_PROC_BROWSER_TEST_F(AdblockFrameHierarchyBrowserTest,
                       PopupHandledByChromiumWithoutFilters) {
  // Without any popup-specific filters, blocking popups is handed over to
  // Chromium, which has it's own heuristics that are not based on filters.
  SetFilters({});
  NavigateToPopupParentFrame();
  // The popup was not opened:
  EXPECT_EQ(1, NumberOfOpenTabs());
  // Because Chromium's built-in popup blocker stopped it:
  EXPECT_EQ(1u, blocked_content::PopupBlockerTabHelper::FromWebContents(
                    browser()->tab_strip_model()->GetActiveWebContents())
                    ->GetBlockedPopupsCount());
}

IN_PROC_BROWSER_TEST_F(AdblockFrameHierarchyBrowserTest, PopupBlockedByFilter) {
  SetFilters({"popup.html$popup"});
  NavigateToPopupParentFrame();
  EXPECT_EQ(1, NumberOfOpenTabs());
  // Observer was notified about blocked popup:
  observer.VerifyNotificationSent("popup.html",
                                  observer.blocked_popups_notifications);
}

IN_PROC_BROWSER_TEST_F(AdblockFrameHierarchyBrowserTest, PopupAllowedByFilter) {
  SetFilters({"popup.html$popup", "@@popup.html$popup"});
  NavigateToPopupParentFrame();
  // Popup was allowed to open in a new tab
  EXPECT_EQ(2, NumberOfOpenTabs());
  observer.VerifyNotificationSent("popup.html",
                                  observer.allowed_popups_notifications);
}

IN_PROC_BROWSER_TEST_F(AdblockFrameHierarchyBrowserTest,
                       PopupAllowedByDomainSpecificFilter) {
  // The frame that wants to open the popup is hosted on middle.com.
  // The $popup allow rule applies to that frame.
  SetFilters({"popup.html$popup", "@@popup.html$popup,domain=middle.com"});
  NavigateToPopupParentFrame();
  // Popup was allowed to open in a new tab
  EXPECT_EQ(2, NumberOfOpenTabs());
  observer.VerifyNotificationSent("popup.html",
                                  observer.allowed_popups_notifications);
}

IN_PROC_BROWSER_TEST_F(AdblockFrameHierarchyBrowserTest,
                       PopupNotAllowedByDomainSpecificFilter) {
  // The frame that wants to open the popup is hosted on middle.com.
  // The $popup allow rule does not apply because it is specific to outer.com.
  // outer.com is not the frame that is opening the popup.
  SetFilters({"popup.html$popup", "@@popup.html$popup,domain=outer.com"});
  NavigateToPopupParentFrame();
  EXPECT_EQ(1, NumberOfOpenTabs());
  // Observer was notified about blocked popup:
  observer.VerifyNotificationSent("popup.html",
                                  observer.blocked_popups_notifications);
}

IN_PROC_BROWSER_TEST_F(AdblockFrameHierarchyBrowserTest,
                       PopupAllowedByParentDocument) {
  // The outermost frame has a blanket allowing rule of $document type.
  SetFilters({"popup.html$popup", "@@||outer.com^$document,domain=outer.com"});
  NavigateToPopupParentFrame();
  // Popup was allowed to open in a new tab
  EXPECT_EQ(2, NumberOfOpenTabs());
  observer.VerifyNotificationSent("popup.html",
                                  observer.allowed_popups_notifications);
  observer.VerifyNotificationSent("popup_parent.html",
                                  observer.allowed_pages_notifications);
}

IN_PROC_BROWSER_TEST_F(AdblockFrameHierarchyBrowserTest,
                       PopupAllowedByIntermediateParentDocument) {
  // The middle frame has a blanket allowing rule of $document type.
  SetFilters(
      {"popup.html$popup", "@@||middle.com^$document,domain=middle.com"});
  NavigateToPopupParentFrame();
  // Popup was allowed to open in a new tab
  EXPECT_EQ(2, NumberOfOpenTabs());
  observer.VerifyNotificationSent("popup.html",
                                  observer.allowed_popups_notifications);
  observer.VerifyNotificationSent("popup.html",
                                  observer.allowed_pages_notifications);
}

// More tests can be added / parametrized, e.g.:
// - elemhide blocking filters (in conjunction with $elemhide allow rules)
// - $subdocument-based allow rules

}  // namespace adblock
