
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

#include "components/adblock/core/configuration/persistent_filtering_configuration.h"

#include <string>

#include "base/strings/string_piece_forward.h"
#include "base/strings/string_util.h"
#include "components/prefs/pref_service.h"
#include "components/prefs/scoped_user_pref_update.h"

namespace adblock {
namespace {

constexpr auto kConfigurationsPrefsPath =
    base::StringPiece("filtering.configurations");
constexpr auto kEnabledKey = base::StringPiece("enabled");
constexpr auto kDomainsKey = base::StringPiece("domains");
constexpr auto kCustomFiltersKey = base::StringPiece("filters");
constexpr auto kFilterListsKey = base::StringPiece("subscriptions");

base::Value ReadFromPrefs(PrefService* pref_service,
                          base::StringPiece configuration_name) {
  const auto& all_configurations =
      pref_service->GetValue(kConfigurationsPrefsPath);
  DCHECK(all_configurations.is_dict());
  const auto* this_config = all_configurations.FindKey(configuration_name);
  if (this_config && this_config->is_dict())
    return this_config->Clone();
  return base::Value{base::Value::Type::DICT};
}

void StoreToPrefs(const base::Value& configuration,
                  PrefService* pref_service,
                  base::StringPiece configuration_name) {
  // ScopedDictPrefUpdate requires an std::string for some reason:
  static std::string kConfigurationsPrefsPathString(kConfigurationsPrefsPath);
  ScopedDictPrefUpdate update(pref_service, kConfigurationsPrefsPathString);
  update.Get().Set(configuration_name, configuration.Clone());
}

void SetDefaultValuesIfNeeded(base::Value& configuration) {
  DCHECK(configuration.is_dict());
  auto& dict = configuration.GetDict();
  if (!dict.FindBool(kEnabledKey))
    dict.Set(kEnabledKey, true);
  dict.EnsureList(kDomainsKey);
  dict.EnsureList(kCustomFiltersKey);
  dict.EnsureList(kFilterListsKey);
}

bool AppendToList(base::Value& configuration,
                  base::StringPiece key,
                  const std::string& value) {
  DCHECK(configuration.FindListKey(key));  // see SetDefaultValuesIfNeeded().
  auto& list = configuration.FindListKey(key)->GetList();
  if (base::ranges::find(list, base::Value(value)) != list.end()) {
    // value already exists in the list.
    return false;
  }
  list.Append(value);
  return true;
}

bool RemoveFromList(base::Value& configuration,
                    base::StringPiece key,
                    const std::string& value) {
  DCHECK(configuration.FindListKey(key));  // see SetDefaultValuesIfNeeded().
  auto& list = configuration.FindListKey(key)->GetList();
  auto it = base::ranges::find(list, base::Value(value));
  if (it == list.end()) {
    // value was not on the list.
    return false;
  }
  list.erase(it);
  return true;
}

template <typename T>
std::vector<T> GetFromList(const base::Value& configuration,
                           base::StringPiece key) {
  DCHECK(configuration.FindListKey(key));  // see SetDefaultValuesIfNeeded().
  const auto& list = configuration.FindListKey(key)->GetList();
  std::vector<T> result;
  for (const auto& value : list)
    if (value.is_string())
      result.emplace_back(value.GetString());
  return result;
}

}  // namespace

PersistentFilteringConfiguration::PersistentFilteringConfiguration(
    PrefService* pref_service,
    std::string name)
    : pref_service_(pref_service),
      name_(std::move(name)),
      dictionary_(ReadFromPrefs(pref_service_, name_)) {
  SetDefaultValuesIfNeeded(dictionary_);
}

PersistentFilteringConfiguration::~PersistentFilteringConfiguration() = default;

void PersistentFilteringConfiguration::AddObserver(Observer* observer) {
  observers_.AddObserver(observer);
}
void PersistentFilteringConfiguration::RemoveObserver(Observer* observer) {
  observers_.RemoveObserver(observer);
}

const std::string& PersistentFilteringConfiguration::GetName() const {
  return name_;
}

void PersistentFilteringConfiguration::SetEnabled(bool enabled) {
  if (IsEnabled() == enabled)
    return;
  dictionary_.GetDict().Set(kEnabledKey, enabled);
  PersistToPrefs();
  NotifyEnabledStateChanged();
}

bool PersistentFilteringConfiguration::IsEnabled() const {
  const auto pref_value = dictionary_.GetDict().FindBool(kEnabledKey);
  DCHECK(pref_value);
  return *pref_value;
}

void PersistentFilteringConfiguration::AddFilterList(GURL url) {
  if (AppendToList(dictionary_, kFilterListsKey, url.spec())) {
    PersistToPrefs();
    NotifyFilterListsChanged();
  }
}

void PersistentFilteringConfiguration::RemoveFilterList(GURL url) {
  if (RemoveFromList(dictionary_, kFilterListsKey, url.spec())) {
    PersistToPrefs();
    NotifyFilterListsChanged();
  }
}

std::vector<GURL> PersistentFilteringConfiguration::GetFilterLists() const {
  return GetFromList<GURL>(dictionary_, kFilterListsKey);
}

void PersistentFilteringConfiguration::AddAllowedDomain(std::string domain) {
  if (AppendToList(dictionary_, kDomainsKey, domain)) {
    PersistToPrefs();
    NotifyAllowedDomainsChanged();
  }
}

void PersistentFilteringConfiguration::RemoveAllowedDomain(std::string domain) {
  if (RemoveFromList(dictionary_, kDomainsKey, domain)) {
    PersistToPrefs();
    NotifyAllowedDomainsChanged();
  }
}

std::vector<std::string> PersistentFilteringConfiguration::GetAllowedDomains()
    const {
  return GetFromList<std::string>(dictionary_, kDomainsKey);
}

void PersistentFilteringConfiguration::AddCustomFilter(std::string filter) {
  if (AppendToList(dictionary_, kCustomFiltersKey, filter)) {
    PersistToPrefs();
    NotifyCustomFiltersChanged();
  }
}

void PersistentFilteringConfiguration::RemoveCustomFilter(std::string filter) {
  if (RemoveFromList(dictionary_, kCustomFiltersKey, filter)) {
    PersistToPrefs();
    NotifyCustomFiltersChanged();
  }
}

std::vector<std::string> PersistentFilteringConfiguration::GetCustomFilters()
    const {
  return GetFromList<std::string>(dictionary_, kCustomFiltersKey);
}

void PersistentFilteringConfiguration::PersistToPrefs() {
  StoreToPrefs(dictionary_, pref_service_, name_);
}

void PersistentFilteringConfiguration::NotifyEnabledStateChanged() {
  for (auto& o : observers_)
    o.OnEnabledStateChanged(this);
}

void PersistentFilteringConfiguration::NotifyFilterListsChanged() {
  for (auto& o : observers_)
    o.OnFilterListsChanged(this);
}

void PersistentFilteringConfiguration::NotifyAllowedDomainsChanged() {
  for (auto& o : observers_)
    o.OnAllowedDomainsChanged(this);
}

void PersistentFilteringConfiguration::NotifyCustomFiltersChanged() {
  for (auto& o : observers_)
    o.OnCustomFiltersChanged(this);
}

// static
void PersistentFilteringConfiguration::RegisterProfilePrefs(
    PrefRegistrySimple* registry) {
  registry->RegisterDictionaryPref(std::string(kConfigurationsPrefsPath));
}

}  // namespace adblock
