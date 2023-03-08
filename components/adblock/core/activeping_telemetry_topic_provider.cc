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

#include "components/adblock/core/activeping_telemetry_topic_provider.h"

#include "base/guid.h"
#include "base/json/json_reader.h"
#include "base/json/json_writer.h"
#include "base/system/sys_info.h"
#include "base/time/time.h"
#include "components/adblock/core/common/adblock_prefs.h"

namespace adblock {
namespace {
int g_http_port_for_testing = 0;
std::optional<base::TimeDelta> g_time_delta_for_testing;

GURL GetUrl() {
  GURL url(EYEO_TELEMETRY_SERVER_URL);
  if (!g_http_port_for_testing) {
    return url;
  }
  DCHECK_EQ(url::kHttpsScheme, url.scheme());
  GURL::Replacements replacements;
  replacements.SetSchemeStr(url::kHttpScheme);
  const std::string port_str = base::NumberToString(g_http_port_for_testing);
  replacements.SetPortStr(port_str);
  return url.ReplaceComponents(replacements);
}

base::TimeDelta GetNormalPingInterval() {
  static base::TimeDelta kNormalPingInterval =
      g_time_delta_for_testing ? g_time_delta_for_testing.value()
                               : base::Hours(8);
  return kNormalPingInterval;
}

base::TimeDelta GetRetryPingInterval() {
  static base::TimeDelta kRetryPingInterval =
      g_time_delta_for_testing ? g_time_delta_for_testing.value()
                               : base::Hours(1);
  return kRetryPingInterval;
}

void AppendStringIfPresent(PrefService* pref_service,
                           const std::string& pref_name,
                           base::StringPiece payload_key,
                           base::Value& payload) {
  auto str = pref_service->GetString(pref_name);
  if (!str.empty()) {
    payload.SetStringKey(payload_key, std::move(str));
  }
}
}  // namespace

ActivepingTelemetryTopicProvider::ActivepingTelemetryTopicProvider(
    utils::AppInfo app_info,
    PrefService* pref_service,
    AdblockController* adblock_controller,
    const GURL& base_url,
    const std::string& auth_token)
    : app_info_(std::move(app_info)),
      pref_service_(pref_service),
      adblock_controller_(adblock_controller),
      base_url_(base_url),
      auth_token_(auth_token) {}

ActivepingTelemetryTopicProvider::~ActivepingTelemetryTopicProvider() = default;

// static
GURL ActivepingTelemetryTopicProvider::DefaultBaseUrl() {
#if !defined(EYEO_TELEMETRY_CLIENT_ID)
  LOG(WARNING)
      << "[eyeo] Using default Telemetry server since a Telemetry client ID "
         "was "
         "not provided. Users will not be counted correctly by eyeo. Please "
         "set an ID via \"eyeo_telemetry_client_id\" gn argument.";
#endif
  return GetUrl();
}

// static
std::string ActivepingTelemetryTopicProvider::DefaultAuthToken() {
#if defined(EYEO_TELEMETRY_ACTIVEPING_AUTH_TOKEN)
  VLOG(1) << "[eyeo] Using " << EYEO_TELEMETRY_ACTIVEPING_AUTH_TOKEN
          << " as Telemetry authentication token";
  return EYEO_TELEMETRY_ACTIVEPING_AUTH_TOKEN;
#else
  LOG(WARNING)
      << "[eyeo] No Telemetry authentication token defined. Users will "
         "not be counted correctly by eyeo. Please set a token via "
         "\"eyeo_telemetry_activeping_auth_token\" gn argument.";
  return "";
#endif
}

GURL ActivepingTelemetryTopicProvider::GetEndpointURL() const {
  return base_url_.Resolve("/topic/eyeochromium_activeping/version/1");
}

std::string ActivepingTelemetryTopicProvider::GetAuthToken() const {
  return auth_token_;
}

void ActivepingTelemetryTopicProvider::GetPayload(PayloadCallback callback) {
  base::Value payload(base::Value::Type::DICTIONARY);
  payload.SetStringKey("addon_name", "eyeo-chromium-sdk");
  payload.SetStringKey("addon_version", "2.0.0");
  payload.SetStringKey("application", app_info_.name);
  payload.SetStringKey("application_version", app_info_.version);
  payload.SetBoolKey("aa_active",
                     adblock_controller_->IsAcceptableAdsEnabled());
  payload.SetStringKey("platform", base::SysInfo::OperatingSystemName());
  payload.SetStringKey("platform_version",
                       base::SysInfo::OperatingSystemVersion());
  // Server requires the following parameters to either have a correct,
  // non-empty value, or not be present at all. We shall not send empty strings.
  AppendStringIfPresent(pref_service_, prefs::kTelemetryLastPingTag,
                        "last_ping_tag", payload);
  AppendStringIfPresent(pref_service_, prefs::kTelemetryFirstPingTime,
                        "first_ping", payload);
  AppendStringIfPresent(pref_service_, prefs::kTelemetryLastPingTime,
                        "last_ping", payload);
  AppendStringIfPresent(pref_service_, prefs::kTelemetryPreviousLastPingTime,
                        "previous_last_ping", payload);

  base::Value root(base::Value::Type::DICTIONARY);
  root.SetKey("payload", std::move(payload));
  std::string serialized;
  // The only way JSONWriter::Write() can return fail is then the Value
  // contains lists or dicts that are too deep (200 levels). We just built the
  // payload and root objects here, they should be really shallow.
  CHECK(base::JSONWriter::Write(root, &serialized));
  VLOG(1) << "[eyeo] Telemetry ping payload: " << serialized;
  std::move(callback).Run(std::move(serialized));
}

base::Time ActivepingTelemetryTopicProvider::GetTimeOfNextRequest() const {
  const auto next_ping_time =
      pref_service_->GetTime(prefs::kTelemetryNextPingTime);
  // Next ping time may be unset if this is a first run. Next request should
  // happen ASAP.
  if (next_ping_time.is_null())
    return base::Time::Now();

  return next_ping_time;
}

void ActivepingTelemetryTopicProvider::ParseResponse(
    std::unique_ptr<std::string> response_content) {
  if (!response_content) {
    VLOG(1) << "[eyeo] Telemetry ping failed, no response from server";
    ScheduleNextPing(GetRetryPingInterval());
    return;
  }

  VLOG(1) << "[eyeo] Response from Telemetry server: " << *response_content;
  auto parsed = base::JSONReader::Read(*response_content);
  if (!parsed || !parsed->is_dict()) {
    VLOG(1)
        << "[eyeo] Telemetry ping failed, response could not be parsed as JSON";
    ScheduleNextPing(GetRetryPingInterval());
    return;
  }

  auto* error_message = parsed->FindStringKey("error");
  if (error_message) {
    VLOG(1) << "[eyeo] Telemetry ping failed, error message: "
            << *error_message;
    ScheduleNextPing(GetRetryPingInterval());
    return;
  }

  // For legacy reasons, "ping_response_time" is sent to us as "token". This
  // should be the server time of when the ping was handled, possibly truncated
  // for anonymity. We don't parse it or interpret it, just send it back with
  // next ping.
  auto* ping_response_time = parsed->FindStringKey("token");
  if (!ping_response_time) {
    VLOG(1) << "[eyeo] Telemetry ping failed, response did not contain a last "
               "ping / token value";
    ScheduleNextPing(GetRetryPingInterval());
    return;
  }

  VLOG(1) << "[eyeo] Telemetry ping succeeded";
  ScheduleNextPing(GetNormalPingInterval());
  UpdatePrefs(*ping_response_time);
}

void ActivepingTelemetryTopicProvider::ScheduleNextPing(base::TimeDelta delay) {
  pref_service_->SetTime(prefs::kTelemetryNextPingTime,
                         base::Time::Now() + delay);
}

void ActivepingTelemetryTopicProvider::UpdatePrefs(
    const std::string& ping_response_time) {
  // First ping is only set once per client.
  if (pref_service_->GetString(prefs::kTelemetryFirstPingTime).empty()) {
    pref_service_->SetString(prefs::kTelemetryFirstPingTime,
                             ping_response_time);
  }
  // Previous-to-last becomes last, last becomes current.
  pref_service_->SetString(
      prefs::kTelemetryPreviousLastPingTime,
      pref_service_->GetString(prefs::kTelemetryLastPingTime));
  pref_service_->SetString(prefs::kTelemetryLastPingTime, ping_response_time);
  // Generate a new random tag that wil be sent along with ping times in the
  // next request.
  const auto tag = base::GUID::GenerateRandomV4();
  pref_service_->SetString(prefs::kTelemetryLastPingTag,
                           tag.AsLowercaseString());
}

// static
void ActivepingTelemetryTopicProvider::SetHttpPortForTesting(
    int http_port_for_testing) {
  g_http_port_for_testing = http_port_for_testing;
}

// static
void ActivepingTelemetryTopicProvider::SetIntervalsForTesting(
    base::TimeDelta time_delta) {
  g_time_delta_for_testing = time_delta;
}

}  // namespace adblock
