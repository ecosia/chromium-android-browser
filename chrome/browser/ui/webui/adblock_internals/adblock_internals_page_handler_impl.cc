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

#include "chrome/browser/ui/webui/adblock_internals/adblock_internals_page_handler_impl.h"

#include "base/i18n/time_formatting.h"
#include "base/strings/utf_string_conversions.h"
#include "chrome/browser/adblock/adblock_telemetry_service_factory.h"
#include "chrome/browser/adblock/session_stats_factory.h"
#include "chrome/browser/adblock/subscription_service_factory.h"
#include "components/adblock/core/adblock_telemetry_service.h"
#include "components/adblock/core/session_stats.h"
#include "components/adblock/core/subscription/subscription_config.h"
#include "components/adblock/core/subscription/subscription_service.h"

namespace {

std::string SubscriptionInstallationStateToString(
    adblock::Subscription::InstallationState state) {
  using State = adblock::Subscription::InstallationState;
  switch (state) {
    case State::Installed:
      return "Installed";
    case State::Installing:
      return "Installing";
    case State::Preloaded:
      return "Preloaded";
    case State::Unknown:
      return "Unknown";
  }
  NOTREACHED();
  return "";
}

std::string DebugLine(std::string name, std::string value, int level) {
  return std::string(2 * level, ' ') + name + ": " + value + '\n';
}

std::string DebugLine(std::string name, int value, int level) {
  return DebugLine(name, std::to_string(value), level);
}

std::string FormatInstallationTime(base::Time time) {
  if (time.is_null()) {
    return "Never";
  }
  return base::UTF16ToUTF8(base::TimeFormatFriendlyDateAndTime(time));
}
}  // namespace

AdblockInternalsPageHandlerImpl::AdblockInternalsPageHandlerImpl(
    Profile* profile,
    mojo::PendingReceiver<mojom::adblock_internals::AdblockInternalsPageHandler>
        receiver)
    : profile_(profile), receiver_(this, std::move(receiver)) {}

AdblockInternalsPageHandlerImpl::~AdblockInternalsPageHandlerImpl() = default;

void AdblockInternalsPageHandlerImpl::GetDebugInfo(
    GetDebugInfoCallback callback) {
  CHECK(profile_);
  auto* service =
      adblock::SubscriptionServiceFactory::GetForBrowserContext(profile_);
  auto* stats = adblock::SessionStatsFactory::GetForBrowserContext(profile_);
  auto allowed = stats->GetSessionAllowedResourcesCount();
  auto blocked = stats->GetSessionBlockedResourcesCount();
  std::string content;
  for (auto* config : service->GetInstalledFilteringConfigurations()) {
    content += DebugLine("Configuration", config->GetName(), 0);
    content += DebugLine("Enabled", config->IsEnabled(), 1);
    for (const auto& it : config->GetAllowedDomains()) {
      content += DebugLine("Allowed domain", it, 1);
    }
    for (const auto& it : config->GetCustomFilters()) {
      content += DebugLine("Custom filter", it, 1);
    }
    for (auto it : service->GetCurrentSubscriptions(config)) {
      auto url = it->GetSourceUrl();
      content += DebugLine("Subscription", url.spec(), 1);
      content += DebugLine(
          "State",
          SubscriptionInstallationStateToString(it->GetInstallationState()), 2);
      content += DebugLine("Title", it->GetTitle(), 2);
      content += DebugLine("Version", it->GetCurrentVersion(), 2);
      content += DebugLine(
          "Last update", FormatInstallationTime(it->GetInstallationTime()), 2);
      content += DebugLine("Total allowed", allowed[url], 2);
      content += DebugLine("Total blocked", blocked[url], 2);
    }
  }

  auto* telemetry_service =
      adblock::AdblockTelemetryServiceFactory::GetForProfile(profile_);
  telemetry_service->GetTopicProvidersDebugInfo(base::BindOnce(
      &AdblockInternalsPageHandlerImpl::OnTelemetryServiceInfoArrived,
      std::move(callback), std::move(content)));
}

void AdblockInternalsPageHandlerImpl::OnTelemetryServiceInfoArrived(
    GetDebugInfoCallback callback,
    std::string content,
    std::vector<std::string> topic_provider_content) {
  for (auto& topic_provider_debug_info : topic_provider_content) {
    content +=
        DebugLine("Eyeometry topic provider", topic_provider_debug_info, 0);
  }
  std::move(callback).Run(std::move(content));
}
