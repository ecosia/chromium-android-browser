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

#include "base/logging.h"

namespace adblock {

static constexpr char kDomainSeparator[] = ",";

// static
absl::optional<ContentFilter> ContentFilter::FromString(
    base::StringPiece domain_list,
    FilterType filter_type,
    base::StringPiece selector) {
  DCHECK(filter_type == FilterType::ElemHide ||
         filter_type == FilterType::ElemHideException ||
         filter_type == FilterType::ElemHideEmulation);
  if (selector.empty()) {
    VLOG(1) << "[eyeo] Content filters require selector";
    return {};
  }

  DomainOption domains =
      DomainOption::FromString(domain_list, kDomainSeparator);

  if (filter_type == FilterType::ElemHideEmulation) {
    // ElemHideEmulation filters require that the domains have
    // at least one subdomain or is localhost
    domains.RemoveDomainsWithNoSubdomain();
    if (domains.GetIncludeDomains().empty()) {
      VLOG(1) << "[eyeo] ElemHideEmulation "
                 "filters require include domains.";
      return {};
    }
  } else if (selector.size() < 3 && domains.UnrestrictedByDomain()) {
    VLOG(1) << "[eyeo] Content filter is not specific enough.  Must be longer "
               "than 2 characters or restricted by domain.";
    return {};
  }

  return ContentFilter(filter_type, selector, std::move(domains));
}

ContentFilter::ContentFilter(FilterType type,
                             base::StringPiece selector,
                             DomainOption domains)
    : type(type),
      selector(selector.data(), selector.size()),
      domains(std::move(domains)) {}
ContentFilter::~ContentFilter() = default;

}  // namespace adblock
