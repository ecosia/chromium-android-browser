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

#include "chrome/browser/adblock/subscription_service_factory.h"

#include <fstream>
#include <memory>
#include <vector>

#include "base/files/file_util.h"
#include "base/functional/bind.h"
#include "base/task/thread_pool.h"
#include "base/trace_event/trace_event.h"
#include "chrome/browser/adblock/subscription_persistent_metadata_factory.h"
#include "chrome/browser/profiles/incognito_helpers.h"
#include "chrome/browser/profiles/profile.h"
#include "components/adblock/core/common/adblock_constants.h"
#include "components/adblock/core/configuration/persistent_filtering_configuration.h"
#include "components/adblock/core/converter/flatbuffer_converter.h"
#include "components/adblock/core/subscription/filtering_configuration_maintainer_impl.h"
#include "components/adblock/core/subscription/installed_subscription_impl.h"
#include "components/adblock/core/subscription/ongoing_subscription_request_impl.h"
#include "components/adblock/core/subscription/preloaded_subscription_provider_impl.h"
#include "components/adblock/core/subscription/subscription_config.h"
#include "components/adblock/core/subscription/subscription_downloader_impl.h"
#include "components/adblock/core/subscription/subscription_persistent_storage_impl.h"
#include "components/adblock/core/subscription/subscription_service_impl.h"
#include "components/adblock/core/subscription/subscription_updater.h"
#include "components/adblock/core/subscription/subscription_updater_impl.h"
#include "components/adblock/core/subscription/subscription_validator_impl.h"
#include "components/keyed_service/content/browser_context_dependency_manager.h"
#include "components/pref_registry/pref_registry_syncable.h"
#include "components/prefs/pref_service.h"
#include "content/public/browser/browser_context.h"
#include "content/public/browser/storage_partition.h"

namespace adblock {
namespace {

std::optional<base::TimeDelta> g_update_check_interval_for_testing;
std::optional<base::TimeDelta> g_update_initial_delay_for_testing;

base::TimeDelta GetUpdateInitialDelay() {
  static base::TimeDelta kInitialDelay =
      g_update_initial_delay_for_testing
          ? g_update_initial_delay_for_testing.value()
          : base::Seconds(30);
  return kInitialDelay;
}

base::TimeDelta GetUpdateCheckInterval() {
  static base::TimeDelta kCheckInterval =
      g_update_check_interval_for_testing
          ? g_update_check_interval_for_testing.value()
          : base::Hours(1);
  return kCheckInterval;
}

constexpr net::BackoffEntry::Policy kRetryBackoffPolicy = {
    0,               // Number of initial errors to ignore.
    5000,            // Initial delay in ms.
    2.0,             // Factor by which the waiting time will be multiplied.
    0.2,             // Fuzzing percentage.
    60 * 60 * 1000,  // Maximum delay in ms.
    -1,              // Never discard the entry.
    false,           // Use initial delay.
};

std::unique_ptr<OngoingSubscriptionRequest> MakeOngoingSubscriptionRequest(
    scoped_refptr<network::SharedURLLoaderFactory> url_loader_factory) {
  return std::make_unique<OngoingSubscriptionRequestImpl>(&kRetryBackoffPolicy,
                                                          url_loader_factory);
}

ConversionResult ConvertFilterFile(const GURL& subscription_url,
                                   const base::FilePath& path) {
  TRACE_EVENT1("eyeo", "ConvertFileToFlatbuffer", "url",
               subscription_url.spec());
  ConversionResult result;
  std::ifstream input_stream(path.AsUTF8Unsafe());
  if (!input_stream.is_open() || !input_stream.good()) {
    result = ConversionError("Could not open filter file");
  } else {
    result = FlatbufferConverter::Convert(
        input_stream, subscription_url,
        config::AllowPrivilegedFilters(subscription_url));
  }
  base::DeleteFile(path);
  return result;
}

std::unique_ptr<SubscriptionUpdater> MakeSubscriptionUpdater() {
  return std::make_unique<SubscriptionUpdaterImpl>(GetUpdateInitialDelay(),
                                                   GetUpdateCheckInterval());
}

std::unique_ptr<FilteringConfigurationMaintainer>
MakeFilterConfigurationMaintainer(
    content::BrowserContext* context,
    FilteringConfiguration* configuration,
    FilteringConfigurationMaintainerImpl::SubscriptionUpdatedCallback
        observer) {
  auto* prefs = Profile::FromBrowserContext(context)->GetPrefs();
  auto main_thread_task_runner = base::SequencedTaskRunner::GetCurrentDefault();
  auto* persistent_metadata =
      SubscriptionPersistentMetadataFactory::GetForBrowserContext(context);
  scoped_refptr<network::SharedURLLoaderFactory> url_loader_factory =
      context->GetDefaultStoragePartition()
          ->GetURLLoaderFactoryForBrowserProcess();

  const std::string storage_dir = configuration->GetName() + "_subscriptions";

  auto storage = std::make_unique<SubscriptionPersistentStorageImpl>(
      context->GetPath().AppendASCII(storage_dir),
      std::make_unique<SubscriptionValidatorImpl>(prefs,
                                                  CurrentSchemaVersion()),
      persistent_metadata);

  ConversionExecutors* conversion_executors =
      SubscriptionServiceFactory::GetInstance();

  auto downloader = std::make_unique<SubscriptionDownloaderImpl>(
      utils::GetAppInfo(),
      base::BindRepeating(&MakeOngoingSubscriptionRequest, url_loader_factory),
      conversion_executors, persistent_metadata);

  auto maintainer = std::make_unique<FilteringConfigurationMaintainerImpl>(
      configuration, std::move(storage), std::move(downloader),
      std::make_unique<PreloadedSubscriptionProviderImpl>(),
      MakeSubscriptionUpdater(), conversion_executors, persistent_metadata,
      observer);
  maintainer->InitializeStorage();
  return maintainer;
}

}  // namespace

scoped_refptr<InstalledSubscription>
SubscriptionServiceFactory::ConvertCustomFilters(
    const std::vector<std::string>& filters) const {
  auto raw_data =
      FlatbufferConverter::Convert(filters, CustomFiltersUrl(), true);
  return base::MakeRefCounted<InstalledSubscriptionImpl>(
      std::move(raw_data), Subscription::InstallationState::Installed,
      base::Time());
}

void SubscriptionServiceFactory::ConvertFilterListFile(
    const GURL& subscription_url,
    const base::FilePath& path,
    base::OnceCallback<void(ConversionResult)> result_callback) const {
  base::ThreadPool::PostTaskAndReplyWithResult(
      FROM_HERE, {base::MayBlock()},
      base::BindOnce(&ConvertFilterFile, subscription_url, path),
      std::move(result_callback));
}

// static
SubscriptionService* SubscriptionServiceFactory::GetForBrowserContext(
    content::BrowserContext* context) {
  return static_cast<SubscriptionService*>(
      GetInstance()->GetServiceForBrowserContext(context, true));
}
// static
SubscriptionServiceFactory* SubscriptionServiceFactory::GetInstance() {
  static base::NoDestructor<SubscriptionServiceFactory> instance;
  return instance.get();
}

SubscriptionServiceFactory::SubscriptionServiceFactory()
    : BrowserContextKeyedServiceFactory(
          "AdblockSubscriptionService",
          BrowserContextDependencyManager::GetInstance()) {
  DependsOn(SubscriptionPersistentMetadataFactory::GetInstance());
}
SubscriptionServiceFactory::~SubscriptionServiceFactory() = default;

KeyedService* SubscriptionServiceFactory::BuildServiceInstanceFor(
    content::BrowserContext* context) const {
  return new SubscriptionServiceImpl(
      base::BindRepeating(&MakeFilterConfigurationMaintainer, context));
}

content::BrowserContext* SubscriptionServiceFactory::GetBrowserContextToUse(
    content::BrowserContext* context) const {
  return chrome::GetBrowserContextRedirectedInIncognito(context);
}

void SubscriptionServiceFactory::RegisterProfilePrefs(
    user_prefs::PrefRegistrySyncable* registry) {
  adblock::PersistentFilteringConfiguration::RegisterProfilePrefs(registry);
}

// static
void SubscriptionServiceFactory::SetUpdateCheckAndDelayIntervalsForTesting(
    base::TimeDelta check_interval,
    base::TimeDelta initial_delay) {
  g_update_check_interval_for_testing = check_interval;
  g_update_initial_delay_for_testing = initial_delay;
}

}  // namespace adblock
