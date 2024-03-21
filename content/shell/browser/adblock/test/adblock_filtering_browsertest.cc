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

#include "base/base64.h"
#include "base/run_loop.h"
#include "components/adblock/content/browser/factories/resource_classification_runner_factory.h"
#include "components/adblock/content/browser/factories/subscription_service_factory.h"
#include "components/adblock/content/browser/resource_classification_runner.h"
#include "components/adblock/core/common/adblock_constants.h"
#include "components/adblock/core/subscription/subscription_service.h"
#include "content/public/test/browser_test.h"
#include "content/public/test/browser_test_utils.h"
#include "content/public/test/content_browser_test.h"
#include "content/public/test/content_browser_test_utils.h"
#include "content/shell/app/shell_main_delegate.h"
#include "content/shell/browser/shell.h"
#include "content/shell/browser/shell_content_browser_client.h"
#include "crypto/rsa_private_key.h"
#include "crypto/signature_creator.h"
#include "net/dns/mock_host_resolver.h"
#include "net/test/embedded_test_server/embedded_test_server.h"

namespace adblock {

class AdblockFilteringTest : public content::ContentBrowserTest,
                             public ResourceClassificationRunner::Observer {
 public:
  void SetUpOnMainThread() override {
    host_resolver()->AddRule(kTestDomain, "127.0.0.1");
    embedded_test_server()->RegisterRequestHandler(base::BindRepeating(
        &AdblockFilteringTest::RequestHandler, base::Unretained(this)));
    ASSERT_TRUE(embedded_test_server()->Start());
    ResourceClassificationRunnerFactory::GetForBrowserContext(
        shell()->web_contents()->GetBrowserContext())
        ->AddObserver(this);
  }

  void TearDownOnMainThread() override {
    ResourceClassificationRunnerFactory::GetForBrowserContext(
        shell()->web_contents()->GetBrowserContext())
        ->RemoveObserver(this);
    EXPECT_TRUE(AllExpectationsMet());
  }

  virtual std::unique_ptr<net::test_server::HttpResponse> RequestHandler(
      const net::test_server::HttpRequest& request) {
    if (base::StartsWith(request.relative_url, "/test_page.html")) {
      static constexpr char kMainFrame[] =
          R"(
        <!DOCTYPE html>
        <html>
          <body>
            <img src="resource.png" />
            <a href="popup.html" target="_blank" id="popup_trigger">Trigger link based popup</a>
            <iframe src="sitekey_iframe.html?query"></iframe>
          </body>
        </html>)";
      std::unique_ptr<net::test_server::BasicHttpResponse> http_response(
          new net::test_server::BasicHttpResponse);
      http_response->set_code(net::HTTP_OK);
      http_response->set_content(kMainFrame);
      http_response->set_content_type("text/html");
      return std::move(http_response);
    } else if (base::StartsWith(request.relative_url, "/sitekey_iframe.html")) {
      static constexpr char kIframe[] =
          R"(
        <!DOCTYPE html>
        <html>
          <body>
            <img src="/iframe_image.png" />
          </body>
        </html>)";
      std::unique_ptr<net::test_server::BasicHttpResponse> http_response(
          new net::test_server::BasicHttpResponse);
      http_response->AddCustomHeader(
          kSiteKeyHeaderKey, sitekey_publickey_ + "_" + sitekey_signature_);
      http_response->set_code(net::HTTP_OK);
      http_response->set_content(kIframe);
      http_response->set_content_type("text/html");
      return std::move(http_response);
    }
    // Unhandled requests result in the Embedded test server sending a 404. This
    // is fine for the purpose of this test.
    return nullptr;
  }

  // Without this override there is no ShellAdblockContentBrowserClient
  // but default ShellContentBrowserClient.
  content::ContentMainDelegate* GetOptionalContentMainDelegateOverride()
      override {
    return new content::ShellMainDelegate(true);
  }

  GURL GetPageUrl(const std::string& page_url = "/test_page.html") {
    return embedded_test_server()->GetURL(kTestDomain, page_url);
  }

  void NavigateToPage() {
    ASSERT_TRUE(content::NavigateToURL(shell(), GetPageUrl()));
  }

  void SetFilters(std::vector<std::string> filters) {
    auto* adblock_configuration =
        SubscriptionServiceFactory::GetForBrowserContext(
            shell()->web_contents()->GetBrowserContext())
            ->GetFilteringConfiguration(kAdblockFilteringConfigurationName);
    adblock_configuration->RemoveCustomFilter(kAllowlistEverythingFilter);
    for (auto& filter : filters) {
      adblock_configuration->AddCustomFilter(filter);
    }
  }

  void SetupNotificationsWaiter(base::RunLoop* run_loop) {
    run_loop_ = run_loop;
  }

  void TriggerPopup() {
    EXPECT_TRUE(content::ExecJs(
        shell(), "document.getElementById('popup_trigger').click();"));
  }

  // ResourceClassificationRunner::Observer:
  void OnRequestMatched(const GURL& url,
                        FilterMatchResult match_result,
                        const std::vector<GURL>& parent_frame_urls,
                        ContentType content_type,
                        content::RenderFrameHost* render_frame_host,
                        const GURL& subscription,
                        const std::string& configuration_name) override {
    auto& list = (match_result == FilterMatchResult::kBlockRule
                      ? blocked_request_notifications_expectations_
                      : allowed_request_notifications_expectations_);
    auto it = std::find(list.begin(), list.end(), url.ExtractFileName());
    ASSERT_FALSE(it == list.end())
        << "Path " << url.ExtractFileName() << " not on list";
    list.erase(it);
    if (run_loop_ && AllExpectationsMet()) {
      run_loop_->Quit();
    }
  }

  void OnPageAllowed(const GURL& url,
                     content::RenderFrameHost* render_frame_host,
                     const GURL& subscription,
                     const std::string& configuration_name) override {
    auto& list = allowed_page_notifications_expectations_;
    auto it = std::find(list.begin(), list.end(), url.ExtractFileName());
    ASSERT_FALSE(it == list.end())
        << "Path " << url.ExtractFileName() << " not on list";
    list.erase(it);
    if (run_loop_ && AllExpectationsMet()) {
      run_loop_->Quit();
    }
  }

  void OnPopupMatched(const GURL& url,
                      FilterMatchResult match_result,
                      const GURL& opener_url,
                      content::RenderFrameHost* render_frame_host,
                      const GURL& subscription,
                      const std::string& configuration_name) override {
    auto& list = (match_result == FilterMatchResult::kBlockRule
                      ? blocked_popup_notifications_expectations_
                      : allowed_popup_notifications_expectations_);
    auto it = std::find(list.begin(), list.end(), url.ExtractFileName());
    ASSERT_FALSE(it == list.end())
        << "Path " << url.ExtractFileName() << " not on list";
    list.erase(it);
    if (run_loop_ && AllExpectationsMet()) {
      run_loop_->Quit();
    }
  }

 protected:
  std::vector<std::string> allowed_popup_notifications_expectations_;
  std::vector<std::string> blocked_popup_notifications_expectations_;
  std::vector<std::string> allowed_request_notifications_expectations_;
  std::vector<std::string> blocked_request_notifications_expectations_;
  std::vector<std::string> allowed_page_notifications_expectations_;
  std::string sitekey_signature_;
  std::string sitekey_publickey_;

  void CreateSitekey(const std::string& sitekey_uri) {
    std::string sitekey_ua =
        content::ShellContentBrowserClient::Get()->GetUserAgent();
    std::string sitekey_encryption_input =
        sitekey_uri + '\0' + kTestDomain + '\0' + sitekey_ua;
    std::unique_ptr<crypto::RSAPrivateKey> key_original(
        crypto::RSAPrivateKey::Create(1024));
    std::vector<uint8_t> priv_key;
    EXPECT_TRUE(key_original->ExportPrivateKey(&priv_key));
    std::vector<uint8_t> pub_key;
    EXPECT_TRUE(key_original->ExportPublicKey(&pub_key));
    std::unique_ptr<crypto::RSAPrivateKey> key(
        crypto::RSAPrivateKey::CreateFromPrivateKeyInfo(priv_key));
    std::unique_ptr<crypto::SignatureCreator> signer(
        crypto::SignatureCreator::Create(key.get(),
                                         crypto::SignatureCreator::SHA1));
    EXPECT_TRUE(signer.get());
    EXPECT_TRUE(signer->Update(
        reinterpret_cast<const uint8_t*>(sitekey_encryption_input.c_str()),
        sitekey_encryption_input.size()));
    std::vector<uint8_t> signature;
    EXPECT_TRUE(signer->Final(&signature));
    sitekey_signature_ = base::Base64Encode(signature);
    sitekey_publickey_ = base::Base64Encode(pub_key);
  }

 private:
  bool AllExpectationsMet() {
    return allowed_request_notifications_expectations_.empty() &&
           blocked_request_notifications_expectations_.empty() &&
           allowed_page_notifications_expectations_.empty() &&
           allowed_popup_notifications_expectations_.empty() &&
           blocked_popup_notifications_expectations_.empty();
  }
  raw_ptr<base::RunLoop> run_loop_ = nullptr;
  static constexpr char kTestDomain[] = "test.org";
};

IN_PROC_BROWSER_TEST_F(AdblockFilteringTest, VerifyBlocking) {
  SetFilters({"resource.png"});
  blocked_request_notifications_expectations_.emplace_back("resource.png");
  base::RunLoop run_loop;
  SetupNotificationsWaiter(&run_loop);
  NavigateToPage();
  run_loop.Run();
}

IN_PROC_BROWSER_TEST_F(AdblockFilteringTest, VerifyAllowing) {
  SetFilters({"resource.png", "@@resource.png"});
  allowed_request_notifications_expectations_.emplace_back("resource.png");
  base::RunLoop run_loop;
  SetupNotificationsWaiter(&run_loop);
  NavigateToPage();
  run_loop.Run();
}

IN_PROC_BROWSER_TEST_F(AdblockFilteringTest, VerifyPageAllowed) {
  SetFilters({"@@test.org$document"});
  allowed_page_notifications_expectations_.emplace_back("test_page.html");
  base::RunLoop run_loop;
  SetupNotificationsWaiter(&run_loop);
  NavigateToPage();
  run_loop.Run();
}

IN_PROC_BROWSER_TEST_F(AdblockFilteringTest, VerifyPopupBlocking) {
  SetFilters({"popup.html^$popup"});
  blocked_popup_notifications_expectations_.emplace_back("popup.html");
  base::RunLoop run_loop;
  SetupNotificationsWaiter(&run_loop);
  NavigateToPage();
  TriggerPopup();
  run_loop.Run();
}

IN_PROC_BROWSER_TEST_F(AdblockFilteringTest, VerifyPopupAllowing) {
  SetFilters({"popup.html^$popup", "@@popup.html^$popup"});
  allowed_popup_notifications_expectations_.emplace_back("popup.html");
  base::RunLoop run_loop;
  SetupNotificationsWaiter(&run_loop);
  NavigateToPage();
  TriggerPopup();
  run_loop.Run();
}

IN_PROC_BROWSER_TEST_F(AdblockFilteringTest, VerifyPageAllowedPopup) {
  SetFilters({"@@test.org$document"});
  allowed_page_notifications_expectations_.emplace_back("test_page.html");
  base::RunLoop run_loop;
  SetupNotificationsWaiter(&run_loop);
  NavigateToPage();
  run_loop.Run();
}

IN_PROC_BROWSER_TEST_F(AdblockFilteringTest, VerifySitekey) {
  CreateSitekey("/sitekey_iframe.html?query");
  std::string sitekey_filter =
      "@@iframe_image.png$sitekey=" + sitekey_publickey_;
  SetFilters({"iframe_image.png", sitekey_filter});
  allowed_request_notifications_expectations_.emplace_back("iframe_image.png");
  base::RunLoop run_loop;
  SetupNotificationsWaiter(&run_loop);
  NavigateToPage();
  run_loop.Run();
}

}  // namespace adblock
