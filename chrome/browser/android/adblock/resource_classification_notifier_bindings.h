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

#ifndef CHROME_BROWSER_ANDROID_ADBLOCK_RESOURCE_CLASSIFICATION_NOTIFIER_BINDINGS_H_
#define CHROME_BROWSER_ANDROID_ADBLOCK_RESOURCE_CLASSIFICATION_NOTIFIER_BINDINGS_H_

#include <utility>
#include <vector>
#include "base/android/jni_weak_ref.h"
#include "base/sequence_checker.h"
#include "components/adblock/content/browser/resource_classification_runner.h"
#include "components/keyed_service/core/keyed_service.h"
#include "components/prefs/pref_service.h"

namespace adblock {

class ResourceClassificationNotifierBindings
    : public KeyedService,
      public ResourceClassificationRunner::Observer {
 public:
  explicit ResourceClassificationNotifierBindings(
      ResourceClassificationRunner* classification_runner);
  ~ResourceClassificationNotifierBindings() override;

  void Bind(JavaObjectWeakGlobalRef resource_classifier_java);

  // ResourceClassificationRunner::Observer
  void OnAdMatched(const GURL& url,
                   FilterMatchResult match_result,
                   const std::vector<GURL>& parent_frame_urls,
                   ContentType content_type,
                   content::RenderFrameHost* render_frame_host,
                   const GURL& subscription) override;
  void OnPageAllowed(const GURL& url,
                     content::RenderFrameHost* render_frame_host,
                     const GURL& subscription) override;
  void OnPopupMatched(const GURL& url,
                      FilterMatchResult match_result,
                      const GURL& opener_url,
                      content::RenderFrameHost* render_frame_host,
                      const GURL& subscription) override;

 private:
  SEQUENCE_CHECKER(sequence_checker_);
  ResourceClassificationRunner* classification_runner_;
  JavaObjectWeakGlobalRef bound_counterpart_;
};

}  // namespace adblock

#endif  // CHROME_BROWSER_ANDROID_ADBLOCK_RESOURCE_CLASSIFICATION_NOTIFIER_BINDINGS_H_
