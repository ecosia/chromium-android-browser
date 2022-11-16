// Copyright 2014 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "components/search_engines/template_url_prepopulate_data.h"

#include "base/logging.h"
#include "base/ranges/algorithm.h"
#include "build/build_config.h"
#include "components/country_codes/country_codes.h"
#include "components/pref_registry/pref_registry_syncable.h"
#include "components/prefs/pref_service.h"
#include "components/search_engines/prepopulated_engines.h"
#include "components/search_engines/search_engines_pref_names.h"
#include "components/search_engines/template_url_data.h"
#include "components/search_engines/template_url_data_util.h"

namespace TemplateURLPrepopulateData {

// Helpers --------------------------------------------------------------------

namespace {
// NOTE: You should probably not change the data in this file without changing
// |kCurrentDataVersion| in prepopulated_engines.json. See comments in
// GetDataVersion() below!

// Put the engines within each country in order with most interesting/important
// first.  The default will be the first engine.

/* Ecosia: retain Ecosia monopoly
// Default (for countries with no better engine set)
const PrepopulatedEngine* const engines_default[] = {
    &google,
    &bing,
    &yahoo,
};

// Note, the below entries are sorted by country code, not the name in comment.
// Engine selection by country ------------------------------------------------
// clang-format off
// United Arab Emirates
const PrepopulatedEngine* const engines_AE[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &yandex_ru,
};

// Albania
const PrepopulatedEngine* const engines_AL[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &yandex_tr,
};

// Argentina
const PrepopulatedEngine* const engines_AR[] = {
    &google,
    &bing,
    &yahoo_ar,
    &duckduckgo,
    &ecosia,
};

// Austria
const PrepopulatedEngine* const engines_AT[] = {
    &google,
    &bing,
    &yahoo_at,
    &duckduckgo,
    &ecosia,
};

// Australia
const PrepopulatedEngine* const engines_AU[] = {
    &google,
    &bing,
    &yahoo_au,
    &duckduckgo,
    &ecosia,
};

// Bosnia and Herzegovina
const PrepopulatedEngine* const engines_BA[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &yandex_com,
};

// Belgium
const PrepopulatedEngine* const engines_BE[] = {
    &google,
    &bing,
    &yahoo,
    &ecosia,
    &duckduckgo,
};

// Bulgaria
const PrepopulatedEngine* const engines_BG[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &yandex_ru,
};

// Bahrain
const PrepopulatedEngine* const engines_BH[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &ecosia,
};

// Burundi
const PrepopulatedEngine* const engines_BI[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &yandex_ru,
};

// Brunei
const PrepopulatedEngine* const engines_BN[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &ecosia,
};

// Bolivia
const PrepopulatedEngine* const engines_BO[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &ecosia,
};

// Brazil
const PrepopulatedEngine* const engines_BR[] = {
    &google,
    &bing,
    &yahoo_br,
    &duckduckgo,
    &yandex_com,
};

// Belarus
const PrepopulatedEngine* const engines_BY[] = {
    &google,
    &yandex_by,
    &mail_ru,
    &bing,
    &duckduckgo,
};

// Belize
const PrepopulatedEngine* const engines_BZ[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &naver,
};

// Canada
const PrepopulatedEngine* const engines_CA[] = {
    &google,
    &bing,
    &yahoo_ca,
    &duckduckgo,
    &ecosia,
};

// Switzerland
const PrepopulatedEngine* const engines_CH[] = {
    &google,
    &bing,
    &duckduckgo,
    &yahoo_ch,
    &ecosia,
};

// Chile
const PrepopulatedEngine* const engines_CL[] = {
    &google,
    &bing,
    &yahoo_cl,
    &duckduckgo,
    &ecosia,
};

// China
const PrepopulatedEngine* const engines_CN[] = {
    &baidu,
    &bing,
    &sogou,
    &so_360,
    &google,
};

// Colombia
const PrepopulatedEngine* const engines_CO[] = {
    &google,
    &bing,
    &yahoo_co,
    &duckduckgo,
    &ecosia,
};

// Costa Rica
const PrepopulatedEngine* const engines_CR[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &ecosia,
};

// Czech Republic
const PrepopulatedEngine* const engines_CZ[] = {
    &google,
    &seznam_cz,
    &bing,
    &yahoo,
    &duckduckgo,
};

// Germany
const PrepopulatedEngine* const engines_DE[] = {
    &google,
    &bing,
    &yahoo_de,
    &duckduckgo,
    &ecosia,
};

// Denmark
const PrepopulatedEngine* const engines_DK[] = {
    &google,
    &bing,
    &yahoo_dk,
    &duckduckgo,
    &ecosia,
};

// Dominican Republic
const PrepopulatedEngine* const engines_DO[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &ecosia,
};

// Algeria
const PrepopulatedEngine* const engines_DZ[] = {
    &google,
    &bing,
    &yahoo_fr,
    &yandex_com,
    &duckduckgo,
};

// Ecuador
const PrepopulatedEngine* const engines_EC[] = {
    &google,
    &bing,
    &yahoo_es,
    &ecosia,
    &duckduckgo,
};

// Estonia
const PrepopulatedEngine* const engines_EE[] = {
    &google,
    &bing,
    &yandex_ru,
    &duckduckgo,
    &mail_ru,
};

// Egypt
const PrepopulatedEngine* const engines_EG[] = {
    &google,
    &bing,
    &yahoo,
    &yandex_com,
    &duckduckgo,
};

// Spain
const PrepopulatedEngine* const engines_ES[] = {
    &google,
    &bing,
    &yahoo_es,
    &duckduckgo,
    &ecosia,
};

// Finland
const PrepopulatedEngine* const engines_FI[] = {
    &google,
    &bing,
    &yahoo_fi,
    &duckduckgo,
    &yandex_ru,
};

// Faroe Islands
const PrepopulatedEngine* const engines_FO[] = {
    &google,
    &bing,
    &yahoo_uk,
    &duckduckgo,
    &ecosia,
};

// France
const PrepopulatedEngine* const engines_FR[] = {
    &google,
    &bing,
    &yahoo_fr,
    &qwant,
    &ecosia,
};

// United Kingdom
const PrepopulatedEngine* const engines_GB[] = {
    &google,
    &bing,
    &yahoo_uk,
    &duckduckgo,
    &ecosia,
};

// Greece
const PrepopulatedEngine* const engines_GR[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &yandex_ru,
};

// Guatemala
const PrepopulatedEngine* const engines_GT[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &ecosia,
};

// Hong Kong
const PrepopulatedEngine* const engines_HK[] = {
    &google,
    &bing,
    &yahoo_hk,
    &baidu,
    &so_360,
};

// Honduras
const PrepopulatedEngine* const engines_HN[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &ecosia,
};

// Croatia
const PrepopulatedEngine* const engines_HR[] = {
    &google,
    &bing,
    &duckduckgo,
    &yahoo,
    &ecosia,
};

// Hungary
const PrepopulatedEngine* const engines_HU[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &yandex_com,
};

// Indonesia
const PrepopulatedEngine* const engines_ID[] = {
    &google,
    &bing,
    &yahoo_id,
    &duckduckgo,
    &yandex_com,
};

// Ireland
const PrepopulatedEngine* const engines_IE[] = {
    &google,
    &bing,
    &yahoo_uk,
    &duckduckgo,
    &ecosia,
};

// Israel
const PrepopulatedEngine* const engines_IL[] = {
    &google,
    &bing,
    &yandex_ru,
    &yahoo,
    &duckduckgo,
};

// India
const PrepopulatedEngine* const engines_IN[] = {
    &google,
    &bing,
    &yahoo_in,
    &duckduckgo,
    &info_com,
};

// Iraq
const PrepopulatedEngine* const engines_IQ[] = {
    &google,
    &bing,
    &yahoo,
    &yandex_tr,
    &petal_search,
};

// Iran
const PrepopulatedEngine* const engines_IR[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &ask,
};

// Iceland
const PrepopulatedEngine* const engines_IS[] = {
    &google,
    &bing,
    &duckduckgo,
    &yahoo,
    &ecosia,
};

// Italy
const PrepopulatedEngine* const engines_IT[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &ecosia,
};

// Jamaica
const PrepopulatedEngine* const engines_JM[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &ecosia,
};

// Jordan
const PrepopulatedEngine* const engines_JO[] = {
    &google,
    &bing,
    &yahoo,
    &petal_search,
    &duckduckgo,
};

// Japan
const PrepopulatedEngine* const engines_JP[] = {
    &google,
    &bing,
    &yahoo_jp,
    &duckduckgo,
    &baidu,
};

// Kenya
const PrepopulatedEngine* const engines_KE[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &yandex_com,
};

// South Korea
const PrepopulatedEngine* const engines_KR[] = {
    &google,
    &naver,
    &bing,
    &daum,
    &yahoo,
};

// Kuwait
const PrepopulatedEngine* const engines_KW[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &yandex_tr,
};

// Kazakhstan
const PrepopulatedEngine* const engines_KZ[] = {
    &google,
    &yandex_kz,
    &mail_ru,
    &bing,
    &duckduckgo,
};

// Lebanon
const PrepopulatedEngine* const engines_LB[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &ecosia,
};

// Liechtenstein
const PrepopulatedEngine* const engines_LI[] = {
    &google,
    &bing,
    &duckduckgo,
    &yahoo,
    &ecosia,
};

// Lithuania
const PrepopulatedEngine* const engines_LT[] = {
    &google,
    &bing,
    &yandex_ru,
    &duckduckgo,
    &yahoo,
};

// Luxembourg
const PrepopulatedEngine* const engines_LU[] = {
    &google,
    &bing,
    &duckduckgo,
    &yahoo,
    &ecosia,
};

// Latvia
const PrepopulatedEngine* const engines_LV[] = {
    &google,
    &bing,
    &yandex_ru,
    &yahoo,
    &duckduckgo,
};

// Libya
const PrepopulatedEngine* const engines_LY[] = {
    &google,
    &bing,
    &yahoo,
    &yandex_tr,
    &duckduckgo,
};

// Morocco
const PrepopulatedEngine* const engines_MA[] = {
    &google,
    &bing,
    &yahoo_fr,
    &yandex_com,
    &duckduckgo,
};

// Monaco
const PrepopulatedEngine* const engines_MC[] = {
    &google,
    &bing,
    &yahoo_fr,
    &duckduckgo,
    &qwant,
};

// Moldova
const PrepopulatedEngine* const engines_MD[] = {
    &google,
    &yandex_ru,
    &bing,
    &mail_ru,
    &duckduckgo,
};

// Montenegro
const PrepopulatedEngine* const engines_ME[] = {
    &google,
    &bing,
    &yandex_ru,
    &yahoo,
    &duckduckgo,
};

// Macedonia
const PrepopulatedEngine* const engines_MK[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &yandex_com,
};

// Mexico
const PrepopulatedEngine* const engines_MX[] = {
    &google,
    &bing,
    &yahoo_mx,
    &duckduckgo,
    &ecosia,
};

// Malaysia
const PrepopulatedEngine* const engines_MY[] = {
    &google,
    &bing,
    &yahoo_my,
    &duckduckgo,
    &ecosia,
};

// Nicaragua
const PrepopulatedEngine* const engines_NI[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &ecosia,
};

// Netherlands
const PrepopulatedEngine* const engines_NL[] = {
    &google,
    &bing,
    &yahoo_nl,
    &duckduckgo,
    &yandex_ru,
};

// Norway
const PrepopulatedEngine* const engines_NO[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &ecosia,
};

// New Zealand
const PrepopulatedEngine* const engines_NZ[] = {
    &google,
    &bing,
    &yahoo_nz,
    &duckduckgo,
    &ecosia,
};

// Oman
const PrepopulatedEngine* const engines_OM[] = {
    &google,
    &bing,
    &yahoo,
    &petal_search,
    &duckduckgo,
};

// Panama
const PrepopulatedEngine* const engines_PA[] = {
    &google,
    &bing,
    &yahoo_es,
    &duckduckgo,
    &ecosia,
};

// Peru
const PrepopulatedEngine* const engines_PE[] = {
    &google,
    &bing,
    &yahoo_pe,
    &ecosia,
    &duckduckgo,
};

// Philippines
const PrepopulatedEngine* const engines_PH[] = {
    &google,
    &bing,
    &yahoo_ph,
    &ecosia,
    &duckduckgo,
};

// Pakistan
const PrepopulatedEngine* const engines_PK[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &yandex_com,
};

// Poland
const PrepopulatedEngine* const engines_PL[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &yandex_ru,
};

// Puerto Rico
const PrepopulatedEngine* const engines_PR[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &ecosia,
};

// Portugal
const PrepopulatedEngine* const engines_PT[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &ecosia,
};

// Paraguay
const PrepopulatedEngine* const engines_PY[] = {
    &google,
    &bing,
    &yahoo_es,
    &duckduckgo,
    &ecosia,
};

// Qatar
const PrepopulatedEngine* const engines_QA[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &yandex_tr,
};

// Romania
const PrepopulatedEngine* const engines_RO[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &yandex_tr,
};

// Serbia
const PrepopulatedEngine* const engines_RS[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &yandex_ru,
};

// Russia
const PrepopulatedEngine* const engines_RU[] = {
    &yandex_ru,
    &google,
    &duckduckgo,
    &bing,
    &mail_ru,
};

// Rwanda
const PrepopulatedEngine* const engines_RW[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &ecosia,
};

// Saudi Arabia
const PrepopulatedEngine* const engines_SA[] = {
    &google,
    &bing,
    &yahoo,
    &yandex_com,
    &duckduckgo,
};

// Sweden
const PrepopulatedEngine* const engines_SE[] = {
    &google,
    &bing,
    &yahoo_se,
    &duckduckgo,
    &yandex_ru,
};

// Singapore
const PrepopulatedEngine* const engines_SG[] = {
    &google,
    &bing,
    &yahoo_sg,
    &baidu,
    &duckduckgo,
};

// Slovenia
const PrepopulatedEngine* const engines_SI[] = {
    &google,
    &bing,
    &duckduckgo,
    &yahoo,
    &yandex_com,
};

// Slovakia
const PrepopulatedEngine* const engines_SK[] = {
    &google,
    &bing,
    &duckduckgo,
    &yahoo,
    &yandex_ru,
};

// El Salvador
const PrepopulatedEngine* const engines_SV[] = {
    &google,
    &bing,
    &yahoo_es,
    &duckduckgo,
    &ecosia,
};

// Syria
const PrepopulatedEngine* const engines_SY[] = {
    &google,
    &bing,
    &yandex_ru,
    &yahoo,
    &duckduckgo,
};

// Thailand
const PrepopulatedEngine* const engines_TH[] = {
    &google,
    &bing,
    &yahoo_th,
    &naver,
    &duckduckgo,
};

// Tunisia
const PrepopulatedEngine* const engines_TN[] = {
    &google,
    &bing,
    &yahoo_fr,
    &duckduckgo,
    &yandex_com,
};

// Turkey
const PrepopulatedEngine* const engines_TR[] = {
    &google,
    &yandex_tr,
    &yahoo_tr,
    &bing,
    &duckduckgo,
};

// Trinidad and Tobago
const PrepopulatedEngine* const engines_TT[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &yandex_com,
};

// Taiwan
const PrepopulatedEngine* const engines_TW[] = {
    &google,
    &yahoo_tw,
    &bing,
    &baidu,
    &duckduckgo,
};

// Tanzania
const PrepopulatedEngine* const engines_TZ[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &ecosia,
};

// Ukraine
const PrepopulatedEngine* const engines_UA[] = {
    &google,
    &yandex_ru,
    &bing,
    &duckduckgo,
    &mail_ru,
};

// United States
const PrepopulatedEngine* const engines_US[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &ecosia,
};

// Uruguay
const PrepopulatedEngine* const engines_UY[] = {
    &google,
    &bing,
    &yahoo_es,
    &duckduckgo,
    &ecosia,
};

// Venezuela
const PrepopulatedEngine* const engines_VE[] = {
    &google,
    &bing,
    &yahoo_es,
    &duckduckgo,
    &yandex_com,
};

// Vietnam
const PrepopulatedEngine* const engines_VN[] = {
    &google,
    &coccoc,
    &bing,
    &yahoo,
    &baidu,
};

// Yemen
const PrepopulatedEngine* const engines_YE[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &yandex_com,
};

// South Africa
const PrepopulatedEngine* const engines_ZA[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &ecosia,
};

// Zimbabwe
const PrepopulatedEngine* const engines_ZW[] = {
    &google,
    &bing,
    &yahoo,
    &duckduckgo,
    &ecosia,
};

// clang-format on
// ----------------------------------------------------------------------------

*/
std::vector<std::unique_ptr<TemplateURLData>> GetPrepopulationSetFromCountryID(
    int country_id) {
  std::vector<std::unique_ptr<TemplateURLData>> t_urls;
  // Ecosia: have Ecosia as only search engine
  t_urls.push_back(TemplateURLDataFromPrepopulatedEngine(ecosia));
  return t_urls;
}

std::vector<std::unique_ptr<TemplateURLData>> GetPrepopulatedTemplateURLData(
    PrefService* prefs) {
  std::vector<std::unique_ptr<TemplateURLData>> t_urls;
  if (!prefs)
    return t_urls;

  const base::Value::List& list =
      prefs->GetList(prefs::kSearchProviderOverrides);

  for (const base::Value& engine : list) {
    if (engine.is_dict()) {
      auto t_url = TemplateURLDataFromOverrideDictionary(engine);
      if (t_url)
        t_urls.push_back(std::move(t_url));
    }
  }
  return t_urls;
}

}  // namespace

// Global functions -----------------------------------------------------------

void RegisterProfilePrefs(user_prefs::PrefRegistrySyncable* registry) {
  country_codes::RegisterProfilePrefs(registry);
  registry->RegisterListPref(prefs::kSearchProviderOverrides);
  registry->RegisterIntegerPref(prefs::kSearchProviderOverridesVersion, -1);
}

int GetDataVersion(PrefService* prefs) {
  // Allow tests to override the local version.
  return (prefs && prefs->HasPrefPath(prefs::kSearchProviderOverridesVersion)) ?
      prefs->GetInteger(prefs::kSearchProviderOverridesVersion) :
      kCurrentDataVersion;
}

std::vector<std::unique_ptr<TemplateURLData>> GetPrepopulatedEngines(
    PrefService* prefs,
    size_t* default_search_provider_index) {
  // If there is a set of search engines in the preferences file, it overrides
  // the built-in set.
  std::vector<std::unique_ptr<TemplateURLData>> t_urls =
      GetPrepopulatedTemplateURLData(prefs);
  if (t_urls.empty()) {
    t_urls = GetPrepopulationSetFromCountryID(
        country_codes::GetCountryIDFromPrefs(prefs));
  }
  if (default_search_provider_index) {
    const auto itr =
        base::ranges::find(t_urls, google.id, &TemplateURLData::prepopulate_id);
    *default_search_provider_index =
        itr == t_urls.end() ? 0 : std::distance(t_urls.begin(), itr);
  }
  return t_urls;
}

std::unique_ptr<TemplateURLData> GetPrepopulatedEngine(PrefService* prefs,
                                                       int prepopulated_id) {
  auto engines =
      TemplateURLPrepopulateData::GetPrepopulatedEngines(prefs, nullptr);
  for (auto& engine : engines) {
    if (engine->prepopulate_id == prepopulated_id)
      return std::move(engine);
  }
  return nullptr;
}

#if BUILDFLAG(IS_ANDROID)

std::vector<std::unique_ptr<TemplateURLData>> GetLocalPrepopulatedEngines(
    const std::string& locale) {
  int country_id = country_codes::CountryStringToCountryID(locale);
  if (country_id == country_codes::kCountryIDUnknown) {
    LOG(ERROR) << "Unknown country code specified: " << locale;
    return std::vector<std::unique_ptr<TemplateURLData>>();
  }

  return GetPrepopulationSetFromCountryID(country_id);
}

#endif

std::vector<const PrepopulatedEngine*> GetAllPrepopulatedEngines() {
  return std::vector<const PrepopulatedEngine*>(
      &kAllEngines[0], &kAllEngines[0] + kAllEnginesLength);
}

void ClearPrepopulatedEnginesInPrefs(PrefService* prefs) {
  if (!prefs)
    return;

  prefs->ClearPref(prefs::kSearchProviderOverrides);
  prefs->ClearPref(prefs::kSearchProviderOverridesVersion);
}

std::unique_ptr<TemplateURLData> GetPrepopulatedDefaultSearch(
    PrefService* prefs) {
  size_t default_search_index;
  // This could be more efficient.  We load all URLs but keep only the default.
  std::vector<std::unique_ptr<TemplateURLData>> loaded_urls =
      GetPrepopulatedEngines(prefs, &default_search_index);

  return (default_search_index < loaded_urls.size())
             ? std::move(loaded_urls[default_search_index])
             : nullptr;
}

}  // namespace TemplateURLPrepopulateData
