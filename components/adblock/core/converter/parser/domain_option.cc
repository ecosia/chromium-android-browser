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

#include "base/ranges/algorithm.h"
#include "base/strings/string_split.h"
#include "base/strings/string_util.h"

namespace adblock {

static constexpr char kExcludeSymbol = '~';

DomainOption::DomainOption() {}

// static
DomainOption DomainOption::FromString(base::StringPiece domains_list,
                                      base::StringPiece separator) {
  auto lower_domains_list = base::ToLowerASCII(domains_list);
  auto domains =
      base::SplitStringPiece(lower_domains_list, separator,
                             base::TRIM_WHITESPACE, base::SPLIT_WANT_NONEMPTY);

  const auto first_include_domain_it = std::partition(
      domains.begin(), domains.end(), [](base::StringPiece domain) {
        return !domain.empty() && domain.front() == kExcludeSymbol;
      });

  std::vector<std::string> exclude_domains(domains.begin(),
                                           first_include_domain_it);
  std::vector<std::string> include_domains(first_include_domain_it,
                                           domains.end());

  // Remove the ~ prefix that indicates an exclude domain.
  for (auto& domain : exclude_domains) {
    base::RemoveChars(domain, base::StringPiece(&kExcludeSymbol), &domain);
  }

  // TODO(DPD-1795): Don't allow duplicated domains

  return DomainOption(std::move(exclude_domains), std::move(include_domains));
}

const std::vector<std::string>& DomainOption::GetExcludeDomains() const {
  return exclude_domains_;
}

const std::vector<std::string>& DomainOption::GetIncludeDomains() const {
  return include_domains_;
}

void DomainOption::RemoveDomainsWithNoSubdomain() {
  exclude_domains_.erase(
      base::ranges::remove_if(exclude_domains_, &HasSubdomainOrLocalhost),
      exclude_domains_.end());

  include_domains_.erase(
      base::ranges::remove_if(include_domains_, &HasSubdomainOrLocalhost),
      include_domains_.end());
}

DomainOption::DomainOption(std::vector<std::string> exclude_domains,
                           std::vector<std::string> include_domains)
    : exclude_domains_(std::move(exclude_domains)),
      include_domains_(std::move(include_domains)) {}
DomainOption::DomainOption(const DomainOption& other) = default;
DomainOption::DomainOption(DomainOption&& other) = default;
DomainOption& DomainOption::operator=(const DomainOption& other) = default;
DomainOption& DomainOption::operator=(DomainOption&& other) = default;
DomainOption::~DomainOption() = default;

// static
bool DomainOption::HasSubdomainOrLocalhost(base::StringPiece domain) {
  return (domain != "localhost") &&
         (domain.find(".") == base::StringPiece::npos);
}

}  // namespace adblock
