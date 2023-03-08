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

#include "components/adblock/core/converter/parser/content_filter.h"

#include "testing/gmock/include/gmock/gmock.h"
#include "testing/gtest/include/gtest/gtest.h"

namespace adblock {

using ::testing::ElementsAre;

TEST(AdblockContentFilterTest, ParseEmptyContentFilter) {
  EXPECT_FALSE(
      ContentFilter::FromString("", FilterType::ElemHide, "").has_value());
}

TEST(AdblockContentFilterTest, ParseElemHideFilter) {
  auto content_filter = ContentFilter::FromString(
      "example.org", FilterType::ElemHide,
      ".testcase-container > .testcase-eh-descendant");
  ASSERT_TRUE(content_filter.has_value());
  EXPECT_EQ(content_filter->type, FilterType::ElemHide);
  EXPECT_EQ(content_filter->selector,
            ".testcase-container > .testcase-eh-descendant");
  EXPECT_THAT(content_filter->domains.GetIncludeDomains(),
              ElementsAre("example.org"));
  EXPECT_TRUE(content_filter->domains.GetExcludeDomains().empty());
}

TEST(AdblockContentFilterTest, ParseElemHideFilterWithNonAsciiCharacters) {
  // Non-ASCII characters are allowed in selectors. They should be preserved.
  auto content_filter =
      ContentFilter::FromString("test.com", FilterType::ElemHide, ".ad_bÖx");
  ASSERT_TRUE(content_filter.has_value());
  EXPECT_EQ(content_filter->type, FilterType::ElemHide);
  EXPECT_EQ(content_filter->selector, ".ad_bÖx");
  EXPECT_THAT(content_filter->domains.GetIncludeDomains(),
              ElementsAre("test.com"));
  EXPECT_TRUE(content_filter->domains.GetExcludeDomains().empty());
}

TEST(AdblockContentFilterTest, ParseElemHideFilterMultipleDomains) {
  auto content_filter =
      ContentFilter::FromString("example.org,~foo.example.org,bar.example.org",
                                FilterType::ElemHide, "test-selector");
  ASSERT_TRUE(content_filter.has_value());
  EXPECT_EQ(content_filter->type, FilterType::ElemHide);
  EXPECT_EQ(content_filter->selector, "test-selector");
  EXPECT_THAT(content_filter->domains.GetIncludeDomains(),
              ElementsAre("example.org", "bar.example.org"));
  EXPECT_THAT(content_filter->domains.GetExcludeDomains(),
              ElementsAre("foo.example.org"));
}

TEST(AdblockContentFilterTest, ParseElemHideFilterWithIdSelector) {
  auto content_filter = ContentFilter::FromString(
      "example.org", FilterType::ElemHide, "#this_is_an_id");
  ASSERT_TRUE(content_filter.has_value());
  EXPECT_EQ(content_filter->type, FilterType::ElemHide);
  EXPECT_EQ(content_filter->selector, "#this_is_an_id");
  EXPECT_THAT(content_filter->domains.GetIncludeDomains(),
              ElementsAre("example.org"));
  EXPECT_TRUE(content_filter->domains.GetExcludeDomains().empty());
}

TEST(AdblockContentFilterTest, ParseElemHideFilterWithNoDomains) {
  auto content_filter =
      ContentFilter::FromString("", FilterType::ElemHide, "selector");
  ASSERT_TRUE(content_filter.has_value());
  EXPECT_EQ(content_filter->type, FilterType::ElemHide);
  EXPECT_EQ(content_filter->selector, "selector");
  EXPECT_TRUE(content_filter->domains.GetIncludeDomains().empty());
  EXPECT_TRUE(content_filter->domains.GetExcludeDomains().empty());
}

TEST(AdblockContentFilterTest, ParseElemHideFilterWithNoSelector) {
  ASSERT_FALSE(
      ContentFilter::FromString("example.org", FilterType::ElemHide, "")
          .has_value());
}

TEST(AdblockContentFilterTest, ParseElemHideExceptionFilter) {
  auto content_filter = ContentFilter::FromString(
      "example.org", FilterType::ElemHideException, "selector");
  ASSERT_TRUE(content_filter.has_value());
  EXPECT_EQ(content_filter->type, FilterType::ElemHideException);
  EXPECT_EQ(content_filter->selector, "selector");
  EXPECT_THAT(content_filter->domains.GetIncludeDomains(),
              ElementsAre("example.org"));
  EXPECT_TRUE(content_filter->domains.GetExcludeDomains().empty());
}

TEST(AdblockContentFilterTest, ParseElemHideExceptionFilterWithIdSelector) {
  auto content_filter = ContentFilter::FromString(
      "example.org", FilterType::ElemHideException, "#this_is_an_id");
  ASSERT_TRUE(content_filter.has_value());
  EXPECT_EQ(content_filter->type, FilterType::ElemHideException);
  EXPECT_EQ(content_filter->selector, "#this_is_an_id");
  EXPECT_THAT(content_filter->domains.GetIncludeDomains(),
              ElementsAre("example.org"));
  EXPECT_TRUE(content_filter->domains.GetExcludeDomains().empty());
}

TEST(AdblockContentFilterTest, ParseElemHideEmulationFilter) {
  auto content_filter = ContentFilter::FromString(
      "foo.example.org", FilterType::ElemHideEmulation, "foo");
  ASSERT_TRUE(content_filter.has_value());
  EXPECT_EQ(content_filter->type, FilterType::ElemHideEmulation);
  EXPECT_EQ(content_filter->selector, "foo");
  EXPECT_THAT(content_filter->domains.GetIncludeDomains(),
              ElementsAre("foo.example.org"));
  EXPECT_TRUE(content_filter->domains.GetExcludeDomains().empty());
}

TEST(AdblockContentFilterTest,
     ParseElemHideEmulationFilterWithNonAsciiCharacters) {
  // Non-ASCII characters are allowed in selectors. They should be preserved.
  auto content_filter = ContentFilter::FromString(
      "test.com", FilterType::ElemHideEmulation, ".ad_bÖx");
  ASSERT_TRUE(content_filter.has_value());
  EXPECT_EQ(content_filter->type, FilterType::ElemHideEmulation);
  EXPECT_EQ(content_filter->selector, ".ad_bÖx");
  EXPECT_THAT(content_filter->domains.GetIncludeDomains(),
              ElementsAre("test.com"));
  EXPECT_TRUE(content_filter->domains.GetExcludeDomains().empty());
}

TEST(AdblockContentFilterTest, ParseElemHideEmulationFilterNoIncludeDomain) {
  ASSERT_FALSE(ContentFilter::FromString("~foo.example.org",
                                         FilterType::ElemHideEmulation, "foo")
                   .has_value());
}

TEST(AdblockContentFilterTest,
     ParseElemHideEmulationFilterDomainsWithNoSubdomainRemoved) {
  auto content_filter =
      ContentFilter::FromString("org,example.org,~com,~example_too.org",
                                FilterType::ElemHideEmulation, "foo");
  ASSERT_TRUE(content_filter.has_value());
  EXPECT_EQ(content_filter->type, FilterType::ElemHideEmulation);
  EXPECT_EQ(content_filter->selector, "foo");
  EXPECT_THAT(content_filter->domains.GetIncludeDomains(),
              ElementsAre("example.org"));
  EXPECT_THAT(content_filter->domains.GetExcludeDomains(),
              ElementsAre("example_too.org"));
}

TEST(AdblockContentFilterTest,
     ParseElemHideEmulationFilterDomainsWithNoSubdomainRemovedButNotLocalhost) {
  auto content_filter =
      ContentFilter::FromString("org,example.org,~localhost,~example_too.org",
                                FilterType::ElemHideEmulation, "foo");
  ASSERT_TRUE(content_filter.has_value());
  EXPECT_EQ(content_filter->type, FilterType::ElemHideEmulation);
  EXPECT_EQ(content_filter->selector, "foo");
  EXPECT_THAT(content_filter->domains.GetIncludeDomains(),
              ElementsAre("example.org"));
  EXPECT_THAT(content_filter->domains.GetExcludeDomains(),
              ElementsAre("example_too.org", "localhost"));
}

}  // namespace adblock
