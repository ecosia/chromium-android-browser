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

#include "components/adblock/content/browser/element_hider_impl.h"

#include "base/functional/callback_forward.h"
#include "base/strings/string_split.h"
#include "base/strings/string_util.h"
#include "base/test/bind.h"
#include "base/test/mock_callback.h"
#include "components/adblock/core/subscription/subscription_service.h"
#include "components/adblock/core/subscription/test/mock_subscription_collection.h"
#include "components/adblock/core/subscription/test/mock_subscription_service.h"
#include "components/grit/components_resources.h"
#include "content/public/test/test_renderer_host.h"
#include "testing/gmock/include/gmock/gmock.h"
#include "testing/gtest/include/gtest/gtest.h"
#include "ui/base/resource/mock_resource_bundle_delegate.h"
#include "ui/base/resource/resource_bundle.h"
#include "url/gurl.h"

namespace {
using NiceMockResourceBundleDelegate = NiceMock<ui::MockResourceBundleDelegate>;
}  // namespace

namespace adblock {

class AdblockElementHiderImplTest : public content::RenderViewHostTestHarness {
 protected:
  void SetUp() override {
    content::RenderViewHostTestHarness::SetUp();
    orig_instance_ = ui::ResourceBundle::SwapSharedInstanceForTesting(nullptr);
    ui::ResourceBundle::InitSharedInstanceWithLocale(
        "en-US", &mock_delegate_,
        ui::ResourceBundle::DO_NOT_LOAD_COMMON_RESOURCES);
    EXPECT_CALL(mock_delegate_, LoadDataResourceString(IDR_ADBLOCK_SNIPPETS_JS))
        .WillRepeatedly(testing::Return("snippets_lib"));
    EXPECT_CALL(mock_delegate_,
                LoadDataResourceString(IDR_ADBLOCK_ELEMHIDE_EMU_JS))
        .WillRepeatedly(testing::Return("{{elemHidingEmulatedPatternsDef}}"));
  }

  void TearDown() override {
    content::RenderViewHostTestHarness::TearDown();
    ui::ResourceBundle::CleanupSharedInstance();
    ui::ResourceBundle::SwapSharedInstanceForTesting(orig_instance_);
  }

  MockSubscriptionService sub_service_;
  NiceMockResourceBundleDelegate mock_delegate_;
  ui::ResourceBundle* orig_instance_;

  const GURL kUrl{"https://domain.com"};
  const std::vector<GURL> kFrameHierarchy{kUrl};
  const SiteKey kSitekey{""};
};

TEST_F(AdblockElementHiderImplTest, BatchesSelectors) {
  std::vector<base::StringPiece> selectors(1u << 11u, "selector");
  std::vector<base::StringPiece> emu_selectors;

  ElementHiderImpl element_hide(&sub_service_);
  EXPECT_CALL(sub_service_, GetCurrentSnapshot())
      .WillOnce([this, selectors, emu_selectors]() {
        auto collection = std::make_unique<MockSubscriptionCollection>();
        EXPECT_CALL(*collection,
                    FindBySpecialFilter(SpecialFilterType::Document, kUrl,
                                        kFrameHierarchy, kSitekey))
            .WillOnce(testing::Return(absl::nullopt));
        EXPECT_CALL(*collection,
                    FindBySpecialFilter(SpecialFilterType::Elemhide, kUrl,
                                        kFrameHierarchy, kSitekey))
            .WillOnce(testing::Return(absl::nullopt));
        EXPECT_CALL(*collection,
                    GetElementHideSelectors(kUrl, kFrameHierarchy, kSitekey))
            .WillOnce(testing::Return(selectors));
        EXPECT_CALL(*collection, GetElementHideEmulationSelectors(kUrl))
            .WillOnce(testing::Return(emu_selectors));
        SubscriptionService::Snapshot snapshot;
        snapshot.push_back(std::move(collection));
        return snapshot;
      });

  element_hide.ApplyElementHidingEmulationOnPage(
      kUrl, kFrameHierarchy, main_rfh(), kSitekey,
      base::BindLambdaForTesting(
          [&](const ElementHider::ElemhideInjectionData& data) {
            const auto lines =
                base::SplitString(data.stylesheet, "\n", base::KEEP_WHITESPACE,
                                  base::SPLIT_WANT_NONEMPTY);
            EXPECT_EQ(lines.size(), 2u);
            for (auto& line : lines) {
              EXPECT_EQ(base::SplitString(line, ",", base::KEEP_WHITESPACE,
                                          base::SPLIT_WANT_NONEMPTY)
                            .size(),
                        (1u << 10u));  // must not be bigger than 2 ^ 10 = 1024

              EXPECT_TRUE(base::EndsWith(line, "{display: none !important;}"));
            }
          }));
}

TEST_F(AdblockElementHiderImplTest, AppliesElementHidingOnSiteWithWeirdUrl) {
  std::vector<base::StringPiece> selectors{"a", "b"};
  std::vector<base::StringPiece> emu_selectors;

  // When loading web bundles, URLs of iframes may not look like ordinary
  // addresses:
  const GURL kNonStandardFrameUrl{
      "uuid-in-package:429fcc4e-0696-4bad-b099-ee9175f023ad"};

  ElementHiderImpl element_hide(&sub_service_);
  EXPECT_CALL(sub_service_, GetCurrentSnapshot())
      .WillOnce([this, selectors, emu_selectors, &kNonStandardFrameUrl]() {
        auto collection = std::make_unique<MockSubscriptionCollection>();
        EXPECT_CALL(*collection,
                    FindBySpecialFilter(SpecialFilterType::Document,
                                        kNonStandardFrameUrl, kFrameHierarchy,
                                        kSitekey))
            .WillOnce(testing::Return(absl::nullopt));
        EXPECT_CALL(*collection,
                    FindBySpecialFilter(SpecialFilterType::Elemhide,
                                        kNonStandardFrameUrl, kFrameHierarchy,
                                        kSitekey))
            .WillOnce(testing::Return(absl::nullopt));
        EXPECT_CALL(*collection,
                    GetElementHideSelectors(kNonStandardFrameUrl,
                                            kFrameHierarchy, kSitekey))
            .WillOnce(testing::Return(selectors));
        EXPECT_CALL(*collection,
                    GetElementHideEmulationSelectors(kNonStandardFrameUrl))
            .WillOnce(testing::Return(emu_selectors));
        SubscriptionService::Snapshot snapshot;
        snapshot.push_back(std::move(collection));
        return snapshot;
      });

  element_hide.ApplyElementHidingEmulationOnPage(
      kNonStandardFrameUrl, kFrameHierarchy, main_rfh(), kSitekey,
      base::BindLambdaForTesting(
          [&](const ElementHider::ElemhideInjectionData& data) {
            EXPECT_EQ(data.stylesheet, "a, b {display: none !important;}\n");
          }));
}

TEST_F(AdblockElementHiderImplTest, GeneratesSnippetsWhenEhAllowListed) {
  EXPECT_CALL(sub_service_, GetCurrentSnapshot()).WillOnce([this]() {
    auto collection = std::make_unique<MockSubscriptionCollection>();
    EXPECT_CALL(*collection,
                FindBySpecialFilter(SpecialFilterType::Document, kUrl,
                                    kFrameHierarchy, kSitekey))
        .WillOnce(testing::Return(absl::nullopt));
    EXPECT_CALL(*collection,
                FindBySpecialFilter(SpecialFilterType::Elemhide, kUrl,
                                    kFrameHierarchy, kSitekey))
        .WillOnce(testing::Return(GURL("about:blank")));
    base::Value::List snippets;
    snippets.Append(base::Value::List());
    EXPECT_CALL(*collection, GenerateSnippets(kUrl, kFrameHierarchy))
        .WillOnce(testing::Return(testing::ByMove(std::move(snippets))));
    SubscriptionService::Snapshot snapshot;
    snapshot.push_back(std::move(collection));
    return snapshot;
  });

  ElementHiderImpl element_hide(&sub_service_);
  element_hide.ApplyElementHidingEmulationOnPage(
      kUrl, kFrameHierarchy, main_rfh(), kSitekey,
      base::BindLambdaForTesting(
          [&](const ElementHider::ElemhideInjectionData& data) {
            EXPECT_EQ(data.stylesheet, "");
            EXPECT_EQ(data.elemhide_js, "");
            EXPECT_TRUE(!data.snippet_js.empty());
          }));
  task_environment()->RunUntilIdle();
}

TEST_F(AdblockElementHiderImplTest, GeneratesNothingWhenDocumentAllowListed) {
  EXPECT_CALL(sub_service_, GetCurrentSnapshot()).WillOnce([this]() {
    auto collection = std::make_unique<MockSubscriptionCollection>();
    EXPECT_CALL(*collection,
                FindBySpecialFilter(SpecialFilterType::Document, kUrl,
                                    kFrameHierarchy, kSitekey))
        .WillOnce(testing::Return(GURL("about:blank")));
    SubscriptionService::Snapshot snapshot;
    snapshot.push_back(std::move(collection));
    return snapshot;
  });

  ElementHiderImpl element_hide(&sub_service_);
  element_hide.ApplyElementHidingEmulationOnPage(
      kUrl, kFrameHierarchy, main_rfh(), kSitekey,
      base::BindLambdaForTesting(
          [&](const ElementHider::ElemhideInjectionData& data) {
            EXPECT_EQ(data.stylesheet, "");
            EXPECT_EQ(data.elemhide_js, "");
            EXPECT_EQ(data.snippet_js, "");
          }));
  task_environment()->RunUntilIdle();
}

TEST_F(AdblockElementHiderImplTest, UsesTwoConfigs) {
  std::vector<base::StringPiece> selectors_config_1{"a1", "b1"};
  std::vector<base::StringPiece> emu_selectors_config_1{"c1", "d1"};
  base::Value::List snippets_config_1;
  snippets_config_1.Append("snippets_config_1_code_1");
  snippets_config_1.Append("snippets_config_1_code_2");
  std::vector<base::StringPiece> selectors_config_2{"a2", "b2"};
  std::vector<base::StringPiece> emu_selectors_config_2{"c2", "d2"};
  base::Value::List snippets_config_2;
  snippets_config_2.Append("snippets_config_2_code_1");
  snippets_config_2.Append("snippets_config_2_code_2");

  ElementHiderImpl element_hide(&sub_service_);
  EXPECT_CALL(sub_service_, GetCurrentSnapshot()).WillOnce([&]() {
    auto collection1 = std::make_unique<MockSubscriptionCollection>();
    EXPECT_CALL(*collection1,
                FindBySpecialFilter(SpecialFilterType::Document, kUrl,
                                    kFrameHierarchy, kSitekey))
        .WillOnce(testing::Return(absl::nullopt));
    EXPECT_CALL(*collection1,
                FindBySpecialFilter(SpecialFilterType::Elemhide, kUrl,
                                    kFrameHierarchy, kSitekey))
        .WillOnce(testing::Return(absl::nullopt));
    EXPECT_CALL(*collection1,
                GetElementHideSelectors(kUrl, kFrameHierarchy, kSitekey))
        .WillOnce(testing::Return(selectors_config_1));
    EXPECT_CALL(*collection1, GetElementHideEmulationSelectors(kUrl))
        .WillOnce(testing::Return(emu_selectors_config_1));
    EXPECT_CALL(*collection1, GenerateSnippets(kUrl, kFrameHierarchy))
        .WillOnce(
            testing::Return(testing::ByMove(std::move(snippets_config_1))));
    auto collection2 = std::make_unique<MockSubscriptionCollection>();
    EXPECT_CALL(*collection2,
                FindBySpecialFilter(SpecialFilterType::Document, kUrl,
                                    kFrameHierarchy, kSitekey))
        .WillOnce(testing::Return(absl::nullopt));
    EXPECT_CALL(*collection2,
                FindBySpecialFilter(SpecialFilterType::Elemhide, kUrl,
                                    kFrameHierarchy, kSitekey))
        .WillOnce(testing::Return(absl::nullopt));
    EXPECT_CALL(*collection2,
                GetElementHideSelectors(kUrl, kFrameHierarchy, kSitekey))
        .WillOnce(testing::Return(selectors_config_2));
    EXPECT_CALL(*collection2, GetElementHideEmulationSelectors(kUrl))
        .WillOnce(testing::Return(emu_selectors_config_2));
    EXPECT_CALL(*collection2, GenerateSnippets(kUrl, kFrameHierarchy))
        .WillOnce(
            testing::Return(testing::ByMove(std::move(snippets_config_2))));
    SubscriptionService::Snapshot snapshot;
    snapshot.push_back(std::move(collection1));
    snapshot.push_back(std::move(collection2));
    return snapshot;
  });

  element_hide.ApplyElementHidingEmulationOnPage(
      kUrl, kFrameHierarchy, main_rfh(), kSitekey,
      base::BindLambdaForTesting(
          [&](const ElementHider::ElemhideInjectionData& data) {
            EXPECT_EQ(data.stylesheet,
                      "a1, b1, a2, b2 {display: none !important;}\n");
            EXPECT_EQ(data.elemhide_js,
                      "{selector:\"c1\", text:\"" + kUrl.host() +
                          "\"}, \n{selector:\"d1\", text:\"" + kUrl.host() +
                          "\"}, \n{selector:\"c2\", text:\"" + kUrl.host() +
                          "\"}, \n{selector:\"d2\", text:\"" + kUrl.host() +
                          "\"}, \n");
            EXPECT_EQ(
                data.snippet_js,
                "(snippets_lib)({}, "
                "...[\"snippets_config_1_code_1\",\"snippets_config_1_code_2\","
                "\"snippets_config_2_code_1\",\"snippets_config_2_code_2\"]);");
          }));
}

TEST_F(AdblockElementHiderImplTest, UsesSecondConfigWhenFirstAllowlisted) {
  std::vector<base::StringPiece> selectors_config_2{"a2", "b2"};
  std::vector<base::StringPiece> emu_selectors_config_2{"c2", "d2"};
  base::Value::List snippets_config_2;
  snippets_config_2.Append("snippets_config_2_code_1");
  snippets_config_2.Append("snippets_config_2_code_2");

  ElementHiderImpl element_hide(&sub_service_);
  EXPECT_CALL(sub_service_, GetCurrentSnapshot()).WillOnce([&]() {
    auto collection1 = std::make_unique<MockSubscriptionCollection>();
    EXPECT_CALL(*collection1,
                FindBySpecialFilter(SpecialFilterType::Document, kUrl,
                                    kFrameHierarchy, kSitekey))
        .WillOnce(testing::Return(GURL("about:blank")));
    EXPECT_CALL(*collection1,
                GetElementHideSelectors(kUrl, kFrameHierarchy, kSitekey))
        .Times(0);
    EXPECT_CALL(*collection1, GetElementHideEmulationSelectors(kUrl)).Times(0);
    EXPECT_CALL(*collection1, GenerateSnippets(kUrl, kFrameHierarchy)).Times(0);
    auto collection2 = std::make_unique<MockSubscriptionCollection>();
    EXPECT_CALL(*collection2,
                FindBySpecialFilter(SpecialFilterType::Document, kUrl,
                                    kFrameHierarchy, kSitekey))
        .WillOnce(testing::Return(absl::nullopt));
    EXPECT_CALL(*collection2,
                FindBySpecialFilter(SpecialFilterType::Elemhide, kUrl,
                                    kFrameHierarchy, kSitekey))
        .WillOnce(testing::Return(absl::nullopt));
    EXPECT_CALL(*collection2,
                GetElementHideSelectors(kUrl, kFrameHierarchy, kSitekey))
        .WillOnce(testing::Return(selectors_config_2));
    EXPECT_CALL(*collection2, GetElementHideEmulationSelectors(kUrl))
        .WillOnce(testing::Return(emu_selectors_config_2));
    EXPECT_CALL(*collection2, GenerateSnippets(kUrl, kFrameHierarchy))
        .WillOnce(
            testing::Return(testing::ByMove(std::move(snippets_config_2))));
    SubscriptionService::Snapshot snapshot;
    snapshot.push_back(std::move(collection1));
    snapshot.push_back(std::move(collection2));
    return snapshot;
  });

  element_hide.ApplyElementHidingEmulationOnPage(
      kUrl, kFrameHierarchy, main_rfh(), kSitekey,
      base::BindLambdaForTesting([&](const ElementHider::ElemhideInjectionData&
                                         data) {
        EXPECT_EQ(data.stylesheet, "a2, b2 {display: none !important;}\n");
        EXPECT_EQ(data.elemhide_js, "{selector:\"c2\", text:\"" + kUrl.host() +
                                        "\"}, \n{selector:\"d2\", text:\"" +
                                        kUrl.host() + "\"}, \n");
        EXPECT_EQ(
            data.snippet_js,
            "(snippets_lib)({}, "
            "...[\"snippets_config_2_code_1\",\"snippets_config_2_code_2\"]);");
      }));
}

}  // namespace adblock
