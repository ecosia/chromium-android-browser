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

#include <memory>

#include "chrome/browser/extensions/api/adblock_private/adblock_private_apitest_base.h"
#include "chrome/browser/profiles/profile.h"
#include "chrome/browser/ui/browser.h"
#include "components/adblock/content/browser/factories/subscription_service_factory.h"
#include "components/adblock/content/browser/test/adblock_browsertest_base.h"
#include "components/adblock/core/subscription/subscription_config.h"
#include "components/adblock/core/subscription/subscription_service.h"
#include "net/dns/mock_host_resolver.h"
#include "net/test/embedded_test_server/embedded_test_server.h"
#include "net/test/embedded_test_server/http_request.h"
#include "net/test/embedded_test_server/http_response.h"

namespace extensions {

class AdblockPrivateApiEnhancedFilterListDelivery
    : public AdblockPrivateApiTestBase,
      public testing::WithParamInterface<
          std::tuple<AdblockPrivateApiTestBase::EyeoExtensionApi,
                     AdblockPrivateApiTestBase::Mode>> {
 public:
  AdblockPrivateApiEnhancedFilterListDelivery()
      : https_server_(net::EmbeddedTestServer::TYPE_HTTPS) {
    const auto testing_interval = base::Seconds(1);
    adblock::SubscriptionServiceFactory::
        SetUpdateCheckAndDelayIntervalsForTesting(testing_interval,
                                                  testing_interval);
    https_server_.RegisterRequestHandler(base::BindRepeating(
        &AdblockPrivateApiEnhancedFilterListDelivery::RequestHandler,
        base::Unretained(this)));
    net::EmbeddedTestServer::ServerCertificateConfig cert_config;
    cert_config.dns_names = {kEyeoFilterListHost};
    https_server_.SetSSLConfig(cert_config);
    EXPECT_TRUE(https_server_.Start());
    adblock::SetFilterListServerPortForTesting(https_server_.port());
    geolocated_list_1_ =
        base::StringPrintf("https://%s:%d/easylistpolish.txt",
                           kEyeoFilterListHost, https_server_.port());
    geolocated_list_2_ =
        base::StringPrintf("https://%s:%d/easylistgermany.txt",
                           kEyeoFilterListHost, https_server_.port());
  }

  ~AdblockPrivateApiEnhancedFilterListDelivery() override = default;
  AdblockPrivateApiEnhancedFilterListDelivery(
      const AdblockPrivateApiEnhancedFilterListDelivery&) = delete;
  AdblockPrivateApiEnhancedFilterListDelivery& operator=(
      const AdblockPrivateApiEnhancedFilterListDelivery&) = delete;

  bool IsIncognito() override {
    return std::get<1>(GetParam()) ==
           AdblockPrivateApiTestBase::Mode::Incognito;
  }

  std::string GetApiEndpoint() override {
    return std::get<0>(GetParam()) ==
                   AdblockPrivateApiTestBase::EyeoExtensionApi::Old
               ? "adblockPrivate"
               : "eyeoFilteringPrivate";
  }

  std::unique_ptr<net::test_server::HttpResponse> RequestHandler(
      const net::test_server::HttpRequest& request) {
    if (base::StartsWith(request.relative_url, "/recommendations.json")) {
      std::unique_ptr<net::test_server::BasicHttpResponse> http_response(
          new net::test_server::BasicHttpResponse);
      http_response->set_code(net::HTTP_OK);
      std::string payload = base::StringPrintf(
          R"(
          [{"url": "%s"}, {"url": "%s"}]
        )",
          geolocated_list_1_.c_str(), geolocated_list_2_.c_str());
      http_response->set_content(payload);
      http_response->set_content_type("text/plain");
      return std::move(http_response);
    } else if (base::StartsWith(request.relative_url, "/easylistpolish.txt") ||
               base::StartsWith(request.relative_url, "/easylistgermany.txt")) {
      std::unique_ptr<net::test_server::BasicHttpResponse> http_response(
          new net::test_server::BasicHttpResponse);
      http_response->set_code(net::HTTP_OK);
      http_response->set_content("[Adblock Plus 2.0]");
      http_response->set_content_type("text/plain");
      return std::move(http_response);
    }

    // Unhandled requests result in the Embedded test server sending a 404.
    // This is fine for the purpose of this test.
    return nullptr;
  }

  void SetUpOnMainThread() override {
    AdblockPrivateApiTestBase::SetUpOnMainThread();
    host_resolver()->AddRule(kEyeoFilterListHost, "127.0.0.1");
  }

  void WaitForGeolocatedLists() {
    auto* subscription_service =
        adblock::SubscriptionServiceFactory::GetForBrowserContext(
            browser()->profile()->GetOriginalProfile());
    std::vector<GURL> subscriptions = {GURL(geolocated_list_1_),
                                       GURL(geolocated_list_2_)};
    auto waiter = adblock::SubscriptionInstalledWaiter(subscription_service);
    waiter.WaitUntilSubscriptionsInstalled(std::move(subscriptions));
  }

 protected:
  net::EmbeddedTestServer https_server_;
  std::string geolocated_list_1_;
  std::string geolocated_list_2_;

 private:
  static constexpr char kEyeoFilterListHost[] =
      "easylist-downloads.adblockplus.org";
};

IN_PROC_BROWSER_TEST_P(AdblockPrivateApiEnhancedFilterListDelivery,
                       GeolocationDiabledHidesFilterLists) {
  WaitForGeolocatedLists();
  EXPECT_TRUE(RunTestWithParams("disableGeolocation",
                                {{"geolocated_list_1", geolocated_list_1_},
                                 {"geolocated_list_2", geolocated_list_2_}}))
      << message_;
}

INSTANTIATE_TEST_SUITE_P(
    ,
    AdblockPrivateApiEnhancedFilterListDelivery,
    testing::Combine(
        testing::Values(AdblockPrivateApiTestBase::EyeoExtensionApi::Old,
                        AdblockPrivateApiTestBase::EyeoExtensionApi::New),
        testing::Values(AdblockPrivateApiTestBase::Mode::Normal,
                        AdblockPrivateApiTestBase::Mode::Incognito)));

}  // namespace extensions
