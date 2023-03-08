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

#ifndef COMPONENTS_ADBLOCK_CONTENT_BROWSER_ADBLOCK_URL_LOADER_FACTORY_FOR_TEST_H_
#define COMPONENTS_ADBLOCK_CONTENT_BROWSER_ADBLOCK_URL_LOADER_FACTORY_FOR_TEST_H_

#include "components/adblock/content/browser/adblock_url_loader_factory.h"
#include "components/adblock/core/adblock_controller.h"

namespace adblock {

// A simple class which handles following commands passed via intercepted url:
// - add filter, f.e. `adblock.test.data/add?payload=filer_text_to_add`
// - remove filter, f.e. `adblock.test.data/remove?payload=filer_text_to_remove`
// - clear all filter, f.e. `adblock.test.data/clear`
// - list all filter, f.e. ``adblock.test.data/list`
// `filer_text_to_add` is expected to be an url encoded string.
// When adding or removing filter one can encode several entries splitting them
// by a new line character. Real example:
// - call `adblock.test.data/add?payload=%2FadsPlugin%2F%2A%0A%2Fadsponsor.`
// - then calling `adblock.test.data/list` returns:
//   OK
//
//   /adsPlugin/*
//   /adsponsor.
class AdblockURLLoaderFactoryForTest final : public AdblockURLLoaderFactory {
 public:
  AdblockURLLoaderFactoryForTest(
      AdblockURLLoaderFactoryConfig config,
      content::GlobalRenderFrameHostId host_id,
      mojo::PendingReceiver<network::mojom::URLLoaderFactory> receiver,
      mojo::PendingRemote<network::mojom::URLLoaderFactory> target_factory,
      std::string user_agent_string,
      DisconnectCallback on_disconnect,
      AdblockController* adblock_controller);
  ~AdblockURLLoaderFactoryForTest() final;

  void CreateLoaderAndStart(
      ::mojo::PendingReceiver<::network::mojom::URLLoader> loader,
      int32_t request_id,
      uint32_t options,
      const ::network::ResourceRequest& request,
      ::mojo::PendingRemote<::network::mojom::URLLoaderClient> client,
      const ::net::MutableNetworkTrafficAnnotationTag& traffic_annotation)
      final;

  static const std::string kAdblockDebugDataHostName;

 private:
  std::string HandleCommand(const GURL& url) const;
  void SendResponse(
      std::string response_body,
      ::mojo::PendingRemote<network::mojom::URLLoaderClient> client) const;
  AdblockController* adblock_controller_;
};

}  // namespace adblock

#endif  // COMPONENTS_ADBLOCK_CONTENT_BROWSER_ADBLOCK_URL_LOADER_FACTORY_FOR_TEST_H_
