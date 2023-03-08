
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

#ifndef COMPONENTS_ADBLOCK_CORE_CONFIGURATION_FAKE_FILTERING_CONFIGURATION_H_
#define COMPONENTS_ADBLOCK_CORE_CONFIGURATION_FAKE_FILTERING_CONFIGURATION_H_

#include <string>
#include <vector>

#include "base/observer_list.h"
#include "components/adblock/core/configuration/filtering_configuration.h"

namespace adblock {

class FakeFilteringConfiguration : public FilteringConfiguration {
 public:
  FakeFilteringConfiguration();
  ~FakeFilteringConfiguration() override;

  void AddObserver(Observer* observer) override;
  void RemoveObserver(Observer* observer) override;

  const std::string& GetName() const override;

  void SetEnabled(bool enabled) override;
  bool IsEnabled() const override;

  void AddFilterList(GURL url) override;
  void RemoveFilterList(GURL url) override;
  std::vector<GURL> GetFilterLists() const override;

  void AddAllowedDomain(std::string domain) override;
  void RemoveAllowedDomain(std::string domain) override;
  std::vector<std::string> GetAllowedDomains() const override;

  void AddCustomFilter(std::string filter) override;
  void RemoveCustomFilter(std::string filter) override;
  std::vector<std::string> GetCustomFilters() const override;

  Observer* observer = nullptr;
  std::string name;
  bool is_enabled = true;
  std::vector<GURL> filter_lists;
  std::vector<std::string> allowed_domains;
  std::vector<std::string> custom_filters;
};

}  // namespace adblock

#endif  // COMPONENTS_ADBLOCK_CORE_CONFIGURATION_FAKE_FILTERING_CONFIGURATION_H_
