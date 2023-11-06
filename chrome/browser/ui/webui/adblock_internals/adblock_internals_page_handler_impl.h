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

#ifndef CHROME_BROWSER_UI_WEBUI_ADBLOCK_INTERNALS_ADBLOCK_INTERNALS_PAGE_HANDLER_IMPL_H_
#define CHROME_BROWSER_UI_WEBUI_ADBLOCK_INTERNALS_ADBLOCK_INTERNALS_PAGE_HANDLER_IMPL_H_

#include "base/memory/raw_ptr.h"
#include "chrome/browser/profiles/profile.h"
#include "chrome/browser/ui/webui/adblock_internals/adblock_internals.mojom.h"
#include "mojo/public/cpp/bindings/receiver.h"

class AdblockInternalsPageHandlerImpl
    : public mojom::adblock_internals::AdblockInternalsPageHandler {
 public:
  explicit AdblockInternalsPageHandlerImpl(
      Profile* profile,
      mojo::PendingReceiver<
          mojom::adblock_internals::AdblockInternalsPageHandler> receiver);
  AdblockInternalsPageHandlerImpl(const AdblockInternalsPageHandlerImpl&) =
      delete;
  AdblockInternalsPageHandlerImpl& operator=(
      const AdblockInternalsPageHandlerImpl&) = delete;
  ~AdblockInternalsPageHandlerImpl() override;

  // mojom::adblock_internals::AdblockInternalsPageHandler:
  void GetDebugInfo(GetDebugInfoCallback callback) override;

 private:
  static void OnTelemetryServiceInfoArrived(
      GetDebugInfoCallback callback,
      std::string content,
      std::vector<std::string> topic_provider_content);
  raw_ptr<Profile> profile_;
  mojo::Receiver<mojom::adblock_internals::AdblockInternalsPageHandler>
      receiver_;
};
#endif  // CHROME_BROWSER_UI_WEBUI_ADBLOCK_INTERNALS_ADBLOCK_INTERNALS_PAGE_HANDLER_IMPL_H_
