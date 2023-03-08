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

#include <string>

#include "chrome/browser/ui/browser.h"
#include "chrome/test/base/in_process_browser_test.h"
#include "chrome/test/base/ui_test_utils.h"
#include "components/adblock/content/browser/adblock_url_loader_factory_for_test.h"
#include "content/public/test/browser_test.h"
#include "testing/gmock/include/gmock/gmock.h"
#include "testing/gtest/include/gtest/gtest.h"

using testing::_;
using testing::Mock;
using testing::Return;

namespace {

class AdblockDebugUrlTest : public InProcessBrowserTest {
 public:
  AdblockDebugUrlTest() {}
  ~AdblockDebugUrlTest() override = default;
  AdblockDebugUrlTest(const AdblockDebugUrlTest&) = delete;
  AdblockDebugUrlTest& operator=(const AdblockDebugUrlTest&) = delete;

 protected:
  std::string ExecuteScriptAndExtractString(const std::string& js_code) const {
    content::WebContents* web_contents =
        browser()->tab_strip_model()->GetActiveWebContents();
    return content::EvalJs(web_contents->GetPrimaryMainFrame(), js_code)
        .ExtractString();
  }

  static constexpr char kReadPageBodyScript[] = R"(
    document.getElementsByTagName('body')[0].firstChild.innerHTML
  )";

  const std::string kAdblockDebugUrl =
      "http://" +
      adblock::AdblockURLLoaderFactoryForTest::kAdblockDebugDataHostName;
};

IN_PROC_BROWSER_TEST_F(AdblockDebugUrlTest, TestInvalidUrls) {
  GURL invalid_command_url(kAdblockDebugUrl + "/some_invalid_command");
  ASSERT_TRUE(ui_test_utils::NavigateToURL(browser(), invalid_command_url));
  ASSERT_EQ("INVALID_COMMAND",
            ExecuteScriptAndExtractString(kReadPageBodyScript));

  GURL invalid_payload_url1(kAdblockDebugUrl + "/add?wrong_param=value");
  ASSERT_TRUE(ui_test_utils::NavigateToURL(browser(), invalid_payload_url1));
  ASSERT_EQ("INVALID_PAYLOAD",
            ExecuteScriptAndExtractString(kReadPageBodyScript));

  GURL invalid_payload_url2(kAdblockDebugUrl + "/add?wrongpayload=value");
  ASSERT_TRUE(ui_test_utils::NavigateToURL(browser(), invalid_payload_url2));
  ASSERT_EQ("INVALID_PAYLOAD",
            ExecuteScriptAndExtractString(kReadPageBodyScript));

  GURL invalid_payload_url3(kAdblockDebugUrl + "/add?payload=");
  ASSERT_TRUE(ui_test_utils::NavigateToURL(browser(), invalid_payload_url3));
  ASSERT_EQ("INVALID_PAYLOAD",
            ExecuteScriptAndExtractString(kReadPageBodyScript));
}

IN_PROC_BROWSER_TEST_F(AdblockDebugUrlTest, TestValidUrls) {
  GURL clear_filters_url(kAdblockDebugUrl + "/clear");
  ASSERT_TRUE(ui_test_utils::NavigateToURL(browser(), clear_filters_url));
  ASSERT_EQ("OK", ExecuteScriptAndExtractString(kReadPageBodyScript));

  GURL list_filters_url(kAdblockDebugUrl + "/list");
  ASSERT_TRUE(ui_test_utils::NavigateToURL(browser(), list_filters_url));
  std::string expected_no_filters = "OK";
  ASSERT_EQ(expected_no_filters,
            ExecuteScriptAndExtractString(kReadPageBodyScript));

  GURL add_filters_url(kAdblockDebugUrl +
                       "/add?payload=%2FadsPlugin%2F%2A%0A%2Fadsponsor.");
  ASSERT_TRUE(ui_test_utils::NavigateToURL(browser(), add_filters_url));
  ASSERT_EQ("OK", ExecuteScriptAndExtractString(kReadPageBodyScript));

  ASSERT_TRUE(ui_test_utils::NavigateToURL(browser(), list_filters_url));
  std::string expected_two_filters = "OK\n\n/adsPlugin/*\n/adsponsor.\n";
  ASSERT_EQ(expected_two_filters,
            ExecuteScriptAndExtractString(kReadPageBodyScript));

  GURL remove_filter_url(kAdblockDebugUrl + "/remove?payload=%2Fadsponsor.");
  ASSERT_TRUE(ui_test_utils::NavigateToURL(browser(), remove_filter_url));
  ASSERT_EQ("OK", ExecuteScriptAndExtractString(kReadPageBodyScript));

  ASSERT_TRUE(ui_test_utils::NavigateToURL(browser(), list_filters_url));
  std::string expected_one_filter = "OK\n\n/adsPlugin/*\n";
  ASSERT_EQ(expected_one_filter,
            ExecuteScriptAndExtractString(kReadPageBodyScript));

  ASSERT_TRUE(ui_test_utils::NavigateToURL(browser(), clear_filters_url));
  ASSERT_EQ("OK", ExecuteScriptAndExtractString(kReadPageBodyScript));

  ASSERT_TRUE(ui_test_utils::NavigateToURL(browser(), list_filters_url));
  ASSERT_EQ(expected_no_filters,
            ExecuteScriptAndExtractString(kReadPageBodyScript));
}

}  // namespace
