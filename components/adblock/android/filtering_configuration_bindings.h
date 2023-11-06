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

#ifndef COMPONENTS_ADBLOCK_ANDROID_FILTERING_CONFIGURATION_BINDINGS_H_
#define COMPONENTS_ADBLOCK_ANDROID_FILTERING_CONFIGURATION_BINDINGS_H_

#include <map>
#include <utility>
#include <vector>
#include "base/android/jni_weak_ref.h"
#include "base/memory/raw_ptr.h"
#include "base/sequence_checker.h"
#include "components/adblock/core/configuration/filtering_configuration.h"
#include "components/adblock/core/subscription/subscription_service.h"
#include "components/keyed_service/core/keyed_service.h"
#include "components/prefs/pref_service.h"

namespace adblock {

class FilteringConfigurationBindings : public KeyedService,
                                       public FilteringConfiguration::Observer {
 public:
  explicit FilteringConfigurationBindings(
      SubscriptionService* subscription_service,
      PrefService* pref_service);
  ~FilteringConfigurationBindings() override;
  void Bind(const std::string& configuration_name,
            JavaObjectWeakGlobalRef filtering_configuration_java);
  void RemoveConfiguration(const std::string& configuration_name);
  std::vector<FilteringConfiguration*> GetConfigurations();
  FilteringConfiguration* GetInstalledConfigurationWithName(
      const std::string& name);

  // FilteringConfiguration::Observer:
  void OnEnabledStateChanged(FilteringConfiguration* config) override;
  void OnFilterListsChanged(FilteringConfiguration* config) override;
  void OnAllowedDomainsChanged(FilteringConfiguration* config) override;
  void OnCustomFiltersChanged(FilteringConfiguration* config) override;

 private:
  using JavaEventListener = void(JNIEnv* env,
                                 const base::android::JavaRef<jobject>& obj);
  void Notify(FilteringConfiguration* config,
              JavaEventListener event_listener_function);
  SEQUENCE_CHECKER(sequence_checker_);
  raw_ptr<SubscriptionService> subscription_service_;
  raw_ptr<PrefService> pref_service_;
  std::map<FilteringConfiguration*, JavaObjectWeakGlobalRef>
      bound_counterparts_;
};

}  // namespace adblock

#endif  // COMPONENTS_ADBLOCK_ANDROID_FILTERING_CONFIGURATION_BINDINGS_H_
