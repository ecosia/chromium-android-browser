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

#include "components/adblock/content/browser/adblock_url_loader_factory_for_test.h"

#include "base/strings/string_split.h"
#include "base/strings/string_util.h"
#include "base/strings/utf_string_conversions.h"
#include "net/http/http_status_code.h"
#include "net/http/http_util.h"
#include "services/network/public/cpp/resource_request.h"
#include "services/network/public/mojom/url_loader.mojom.h"
#include "services/network/public/mojom/url_loader_factory.mojom.h"
#include "services/network/public/mojom/url_response_head.mojom.h"
#include "url/url_canon.h"
#include "url/url_util.h"

namespace {

std::string DecodePayload(const std::string& encoded) {
  // Example how to encode:
  // url::RawCanonOutputT<char> buffer;
  // url::EncodeURIComponent(base.c_str(), base.size(), &buffer);
  // std::string encoded(buffer.data(), buffer.length());
  VLOG(2) << "[eyeo] Encoded payload: " << encoded;
  url::RawCanonOutputT<char16_t> output;
  url::DecodeURLEscapeSequences(encoded.c_str(), encoded.size(),
                                url::DecodeURLMode::kUTF8OrIsomorphic, &output);
  std::string decoded =
      base::UTF16ToUTF8(std::u16string(output.data(), output.length()));
  VLOG(2) << "[eyeo] Decoded payload: " << decoded;
  return decoded;
}

std::string GetPayload(const std::string& input) {
  static const char kPayloadParam[] = "payload=";
  if (base::StartsWith(input, kPayloadParam)) {
    return input.substr(sizeof(kPayloadParam) - 1);
  }
  return "";
}

void ClearFilters(adblock::AdblockController* adblock_controller) {
  for (auto filter : adblock_controller->GetCustomFilters()) {
    adblock_controller->RemoveCustomFilter(filter);
  }
}

std::vector<std::string> ListFilters(
    adblock::AdblockController* adblock_controller) {
  return adblock_controller->GetCustomFilters();
}

void AddOrRemoveFilters(
    adblock::AdblockController* adblock_controller,
    void (adblock::AdblockController::*action)(const std::string&),
    std::string& filters) {
  static const char delimiter[] = "\n";
  auto filters_list = base::SplitString(
      filters, delimiter, base::WhitespaceHandling::TRIM_WHITESPACE,
      base::SplitResult::SPLIT_WANT_NONEMPTY);
  for (auto filter : filters_list) {
    (adblock_controller->*action)(filter);
  }
}

}  // namespace

namespace adblock {

// static
const std::string AdblockURLLoaderFactoryForTest::kAdblockDebugDataHostName =
    "adblock.test.data";

AdblockURLLoaderFactoryForTest::AdblockURLLoaderFactoryForTest(
    AdblockURLLoaderFactoryConfig config,
    content::GlobalRenderFrameHostId host_id,
    mojo::PendingReceiver<network::mojom::URLLoaderFactory> receiver,
    mojo::PendingRemote<network::mojom::URLLoaderFactory> target_factory,
    std::string user_agent_string,
    DisconnectCallback on_disconnect,
    AdblockController* adblock_controller)
    : AdblockURLLoaderFactory(std::move(config),
                              host_id,
                              std::move(receiver),
                              std::move(target_factory),
                              user_agent_string,
                              std::move(on_disconnect)),
      adblock_controller_(adblock_controller) {}

AdblockURLLoaderFactoryForTest::~AdblockURLLoaderFactoryForTest() = default;

void AdblockURLLoaderFactoryForTest::CreateLoaderAndStart(
    ::mojo::PendingReceiver<network::mojom::URLLoader> loader,
    int32_t request_id,
    uint32_t options,
    const network::ResourceRequest& request,
    ::mojo::PendingRemote<network::mojom::URLLoaderClient> client,
    const net::MutableNetworkTrafficAnnotationTag& traffic_annotation) {
  DCHECK(adblock_controller_);
  DCHECK(request.url.host() == kAdblockDebugDataHostName);
  VLOG(2) << "[eyeo] AdblockURLLoaderFactoryForTest handles: " << request.url;
  std::string response_body = HandleCommand(request.url);
  SendResponse(std::move(response_body), std::move(client));
}

void AdblockURLLoaderFactoryForTest::SendResponse(
    std::string response_body,
    ::mojo::PendingRemote<network::mojom::URLLoaderClient> client) const {
  auto response_head = network::mojom::URLResponseHead::New();
  response_head->mime_type = "text/plain";
  mojo::Remote<network::mojom::URLLoaderClient> client_remote(
      std::move(client));
  mojo::ScopedDataPipeProducerHandle producer;
  mojo::ScopedDataPipeConsumerHandle consumer;
  if (CreateDataPipe(nullptr, producer, consumer) != MOJO_RESULT_OK) {
    DLOG(ERROR)
        << "[eyeo] AdblockURLLoaderFactoryForTest fails to call CreateDataPipe";
    client_remote->OnComplete(
        network::URLLoaderCompletionStatus(net::ERR_INSUFFICIENT_RESOURCES));
    return;
  }
  uint32_t write_size = static_cast<uint32_t>(response_body.size());
  producer->WriteData(response_body.c_str(), &write_size,
                      MOJO_WRITE_DATA_FLAG_NONE);
  client_remote->OnReceiveResponse(std::move(response_head),
                                   std::move(consumer), absl::nullopt);
  network::URLLoaderCompletionStatus status;
  status.error_code = net::OK;
  status.decoded_body_length = write_size;
  client_remote->OnComplete(status);
}

std::string AdblockURLLoaderFactoryForTest::HandleCommand(
    const GURL& url) const {
  static const char kResponseOk[] = "OK";
  static const char kResponseInvalidCommand[] = "INVALID_COMMAND";
  static const char kResponseInvalidPayload[] = "INVALID_PAYLOAD";
  static const char kCommandAdd[] = "/add";
  static const char kCommandClear[] = "/clear";
  static const char kCommandList[] = "/list";
  static const char kCommandRemove[] = "/remove";

  std::string command = url.path();
  if (command == kCommandAdd || command == kCommandRemove) {
    std::string payload = DecodePayload(GetPayload(url.query()));
    if (payload.empty()) {
      return kResponseInvalidPayload;
    }
    if (command == kCommandAdd) {
      VLOG(2) << "[eyeo] Adding filter(s): " << payload;
      AddOrRemoveFilters(adblock_controller_,
                         &adblock::AdblockController::AddCustomFilter, payload);
    } else {
      VLOG(2) << "[eyeo] Removing filter(s): " << payload;
      AddOrRemoveFilters(adblock_controller_,
                         &adblock::AdblockController::RemoveCustomFilter,
                         payload);
    }
    return kResponseOk;
  } else if (command == kCommandClear) {
    VLOG(2) << "[eyeo] Clearing filters";
    ClearFilters(adblock_controller_);
    return kResponseOk;
  } else if (command == kCommandList) {
    VLOG(2) << "[eyeo] Listing filters";
    std::vector<std::string> filters = ListFilters(adblock_controller_);
    std::string response = kResponseOk;
    if (!filters.empty()) {
      response += "\n\n";
      for (auto filter : filters) {
        response += filter + "\n";
      }
    }
    return response;
  }
  return kResponseInvalidCommand;
}

}  // namespace adblock
