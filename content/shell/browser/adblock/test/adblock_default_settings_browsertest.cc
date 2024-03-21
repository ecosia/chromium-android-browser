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

#include "base/check.h"
#include "base/environment.h"
#include "base/functional/callback_forward.h"
#include "base/run_loop.h"
#include "components/adblock/content/browser/factories/subscription_service_factory.h"
#include "components/adblock/core/subscription/subscription_config.h"
#include "content/public/test/browser_test.h"
#include "content/public/test/browser_test_utils.h"
#include "content/public/test/content_browser_test.h"
#include "content/public/test/content_browser_test_utils.h"
#include "content/shell/app/shell_main_delegate.h"
#include "content/shell/browser/shell.h"
#include "net/dns/mock_host_resolver.h"
#include "net/test/embedded_test_server/embedded_test_server.h"

namespace adblock {

class AdblockDefaultSettingsTestBase : public content::ContentBrowserTest {
 public:
  AdblockDefaultSettingsTestBase()
      : https_server_(net::EmbeddedTestServer::TYPE_HTTPS) {}
  // We need to set server and request handler asap
  void SetUpInProcessBrowserTestFixture() override {
    ContentBrowserTest::SetUpInProcessBrowserTestFixture();
    host_resolver()->AddRule("easylist-downloads.adblockplus.org", "127.0.0.1");
    https_server_.RegisterRequestHandler(
        base::BindRepeating(&AdblockDefaultSettingsTestBase::RequestHandler,
                            base::Unretained(this)));
    net::EmbeddedTestServer::ServerCertificateConfig cert_config;
    cert_config.dns_names = {"easylist-downloads.adblockplus.org"};
    https_server_.SetSSLConfig(cert_config);
    ASSERT_TRUE(https_server_.Start());
    SetFilterListServerPortForTesting(https_server_.port());
  }

  virtual std::unique_ptr<net::test_server::HttpResponse> RequestHandler(
      const net::test_server::HttpRequest& request) {
    if (request.method == net::test_server::HttpMethod::METHOD_GET &&
        (base::StartsWith(request.relative_url, "/abp-filters-anti-cv.txt") ||
         base::StartsWith(request.relative_url, "/easylist.txt") ||
         base::StartsWith(request.relative_url, "/exceptionrules.txt"))) {
      default_lists_.insert(request.relative_url.substr(
          1, request.relative_url.find_first_of("?") - 1));
      if (CheckExpectedDownloads()) {
        NotifyTestFinished();
      }
    }

    // Unhandled requests result in the Embedded test server sending a 404. This
    // is fine for the purpose of this test.
    return nullptr;
  }

  bool CheckExpectedDownloads() {
    return (default_lists_.find("abp-filters-anti-cv.txt") !=
            default_lists_.end()) &&
           (default_lists_.find("easylist.txt") != default_lists_.end()) &&
           (default_lists_.find("exceptionrules.txt") != default_lists_.end());
  }

  void NotifyTestFinished() {
    finish_condition_met_ = true;
    // If the test is currently waiting for the finish condition to be met, we
    // need to quit the run loop.
    if (quit_closure_) {
      quit_closure_.Run();
    }
  }

  void RunUntilTestFinished() {
    // If the finish condition is already met, we don't need to run the run
    // loop.
    if (finish_condition_met_) {
      return;
    }
    // Wait until NotifyTestFinished() gets called.
    base::RunLoop run_loop;
    quit_closure_ = run_loop.QuitClosure();
    std::move(run_loop).Run();
  }

  bool RunsOnEyeoCI() {
    auto env = base::Environment::Create();
    std::string value;
    env->GetVar("CI_PROJECT_NAME", &value);
    return value == "chromium-sdk";
  }

 protected:
  net::EmbeddedTestServer https_server_;
  std::set<std::string> default_lists_;
  bool finish_condition_met_ = false;
  base::RepeatingClosure quit_closure_;
};

IN_PROC_BROWSER_TEST_F(AdblockDefaultSettingsTestBase, TestInitialDownloads) {
  RunUntilTestFinished();
}

}  // namespace adblock
