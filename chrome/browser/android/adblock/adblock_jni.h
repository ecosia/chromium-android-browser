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

#ifndef CHROME_BROWSER_ANDROID_ADBLOCK_ADBLOCK_JNI_H_
#define CHROME_BROWSER_ANDROID_ADBLOCK_ADBLOCK_JNI_H_

#include <string>

#include "base/android/jni_weak_ref.h"
#include "components/adblock/core/adblock_controller.h"
#include "components/keyed_service/core/keyed_service.h"

namespace content {
class BrowserContext;
}

namespace adblock {

class AdblockJNI : public AdblockController::Observer, public KeyedService {
 public:
  explicit AdblockJNI(AdblockController* controller);
  ~AdblockJNI() override;

  void Bind(JavaObjectWeakGlobalRef weak_java_controller);
  // AdblockController::Observer:
  void OnSubscriptionUpdated(const GURL& url) override;

 private:
  SEQUENCE_CHECKER(sequence_checker_);
  AdblockController* controller_;
  JavaObjectWeakGlobalRef weak_java_controller_;
};

}  // namespace adblock

#endif  // CHROME_BROWSER_ANDROID_ADBLOCK_ADBLOCK_JNI_H_
