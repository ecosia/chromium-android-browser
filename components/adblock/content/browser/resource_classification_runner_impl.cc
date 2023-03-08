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

#include "components/adblock/content/browser/resource_classification_runner_impl.h"

#include "base/functional/bind.h"
#include "base/task/task_traits.h"
#include "base/task/thread_pool.h"
#include "base/trace_event/trace_event.h"
#include "components/adblock/core/common/adblock_utils.h"
#include "components/adblock/core/common/sitekey.h"
#include "content/public/browser/browser_thread.h"
#include "content/public/browser/render_frame_host.h"
#include "services/network/public/mojom/url_response_head.mojom.h"

namespace adblock {

using ClassificationDecision =
    ResourceClassifier::ClassificationResult::Decision;

namespace {

absl::optional<GURL> HasRewriteHelper(
    const SubscriptionService::Snapshot subscription_collections,
    const GURL& request_url,
    const std::vector<GURL>& frame_hierarchy) {
  absl::optional<GURL> rewrite_url = absl::nullopt;
  for (const auto& collection : subscription_collections) {
    rewrite_url = collection->GetRewriteUrl(request_url, frame_hierarchy);
    if (rewrite_url) {
      break;
    }
  }
  return rewrite_url;
}

absl::optional<GURL> CheckDocumentAllowlistedHelper(
    const SubscriptionService::Snapshot subscription_collections,
    const GURL& request_url) {
  absl::optional<GURL> subscription_url = absl::nullopt;
  // We check if page is allowlisted in every configuration but on success
  // return any matching subscription from any configuration
  for (const auto& collection : subscription_collections) {
    subscription_url = collection->FindBySpecialFilter(
        SpecialFilterType::Document, request_url, std::vector<GURL>(),
        SiteKey());
    if (!subscription_url) {
      return absl::nullopt;
    }
  }
  return subscription_url;
}

}  // namespace

ResourceClassificationRunnerImpl::CheckResourceFilterMatchResult::
    CheckResourceFilterMatchResult(FilterMatchResult status,
                                   const GURL& subscription)
    : status(status), subscription(subscription) {}
ResourceClassificationRunnerImpl::CheckResourceFilterMatchResult::
    ~CheckResourceFilterMatchResult() = default;
ResourceClassificationRunnerImpl::CheckResourceFilterMatchResult::
    CheckResourceFilterMatchResult(const CheckResourceFilterMatchResult&) =
        default;
ResourceClassificationRunnerImpl::CheckResourceFilterMatchResult&
ResourceClassificationRunnerImpl::CheckResourceFilterMatchResult::operator=(
    const CheckResourceFilterMatchResult&) = default;
ResourceClassificationRunnerImpl::CheckResourceFilterMatchResult::
    CheckResourceFilterMatchResult(CheckResourceFilterMatchResult&&) = default;
ResourceClassificationRunnerImpl::CheckResourceFilterMatchResult&
ResourceClassificationRunnerImpl::CheckResourceFilterMatchResult::operator=(
    CheckResourceFilterMatchResult&&) = default;

ResourceClassificationRunnerImpl::ResourceClassificationRunnerImpl(
    scoped_refptr<ResourceClassifier> resource_classifier,
    std::unique_ptr<FrameHierarchyBuilder> frame_hierarchy_builder,
    SitekeyStorage* sitekey_storage)
    : resource_classifier_(std::move(resource_classifier)),
      frame_hierarchy_builder_(std::move(frame_hierarchy_builder)),
      sitekey_storage_(sitekey_storage),
      weak_ptr_factory_(this) {}

ResourceClassificationRunnerImpl::~ResourceClassificationRunnerImpl() = default;

void ResourceClassificationRunnerImpl::AddObserver(Observer* observer) {
  DCHECK_CALLED_ON_VALID_SEQUENCE(sequence_checker_);
  observers_.AddObserver(observer);
}

void ResourceClassificationRunnerImpl::RemoveObserver(Observer* observer) {
  DCHECK_CALLED_ON_VALID_SEQUENCE(sequence_checker_);
  observers_.RemoveObserver(observer);
}

FilterMatchResult ResourceClassificationRunnerImpl::ShouldBlockPopup(
    const SubscriptionService::Snapshot& subscription_collections,
    const GURL& popup_url,
    content::RenderFrameHost* frame_host) {
  DCHECK_CALLED_ON_VALID_SEQUENCE(sequence_checker_);
  DCHECK(frame_host);
  TRACE_EVENT1("eyeo", "ResourceClassificationRunnerImpl::ShouldBlockPopup",
               "popup_url", popup_url.spec());

  const auto frame_hierarchy =
      frame_hierarchy_builder_->BuildFrameHierarchy(frame_host);
  const auto sitekey = sitekey_storage_->FindSiteKeyForAnyUrl(frame_hierarchy);

  auto classification_result = resource_classifier_->ClassifyPopup(
      subscription_collections, popup_url, frame_hierarchy,
      sitekey ? sitekey->second : SiteKey());
  if (classification_result.decision == ClassificationDecision::Ignored) {
    return FilterMatchResult::kNoRule;
  }
  const FilterMatchResult result =
      classification_result.decision == ClassificationDecision::Allowed
          ? FilterMatchResult::kAllowRule
          : FilterMatchResult::kBlockRule;
  if (result == FilterMatchResult::kBlockRule) {
    VLOG(1) << "[eyeo] Prevented loading of pop-up " << popup_url.spec();
  } else {
    VLOG(1) << "[eyeo] Pop-up allowed " << popup_url.spec();
  }
  const auto& opener_url =
      frame_hierarchy.empty() ? GURL() : frame_hierarchy.front();
  for (auto& observer : observers_) {
    observer.OnPopupMatched(popup_url, result, opener_url, frame_host,
                            classification_result.decisive_subscription);
  }
  return result;
}

void ResourceClassificationRunnerImpl::CheckRequestFilterMatchForWebSocket(
    SubscriptionService::Snapshot subscription_collections,
    const GURL& request_url,
    content::GlobalRenderFrameHostId render_frame_host_id,
    CheckFilterMatchCallback callback) {
  DCHECK_CALLED_ON_VALID_SEQUENCE(sequence_checker_);
  DCHECK(request_url.SchemeIsWSOrWSS());
  CheckRequestFilterMatchImpl(std::move(subscription_collections), request_url,
                              ContentType::Websocket, render_frame_host_id,
                              std::move(callback));
}

void ResourceClassificationRunnerImpl::CheckDocumentAllowlisted(
    SubscriptionService::Snapshot subscription_collections,
    const GURL& request_url,
    content::GlobalRenderFrameHostId render_frame_host_id) {
  // We pass main frame no matter what
  DVLOG(1) << "[eyeo] Main document. Passing it through: " << request_url;
  base::ThreadPool::PostTaskAndReplyWithResult(
      FROM_HERE, {},
      base::BindOnce(&CheckDocumentAllowlistedHelper,
                     std::move(subscription_collections), request_url),
      base::BindOnce(
          &ResourceClassificationRunnerImpl::ProcessDocumentAllowlistedResponse,
          weak_ptr_factory_.GetWeakPtr(), request_url, render_frame_host_id));
}

void ResourceClassificationRunnerImpl::ProcessDocumentAllowlistedResponse(
    const GURL request_url,
    content::GlobalRenderFrameHostId render_frame_host_id,
    absl::optional<GURL> allowing_subscription) {
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);
  content::RenderFrameHost* host =
      content::RenderFrameHost::FromID(render_frame_host_id);
  if (allowing_subscription && host) {
    VLOG(1) << "[eyeo] Calling OnPageAllowed() for " << request_url;
    for (auto& observer : observers_) {
      observer.OnPageAllowed(request_url, host, allowing_subscription.value());
    }
  }
}

void ResourceClassificationRunnerImpl::CheckRequestFilterMatch(
    SubscriptionService::Snapshot subscription_collections,
    const GURL& request_url,
    ContentType adblock_resource_type,
    content::GlobalRenderFrameHostId render_frame_host_id,
    CheckFilterMatchCallback callback) {
  DCHECK_CALLED_ON_VALID_SEQUENCE(sequence_checker_);
  DCHECK(!request_url.SchemeIsWSOrWSS())
      << "Use CheckRequestFilterMatchForWebSocket()";

  content::RenderFrameHost* host =
      frame_hierarchy_builder_->FindRenderFrameHost(render_frame_host_id);
  if (!host) {
    // We're unable to verify if the resource is allowed or blocked without a
    // frame hierarchy, so err on the side of safety and allow the load.
    // This happens for Ping-type requests, for reasons unknown.
    VLOG(1) << "[eyeo] Unable to build frame hierarchy for " << request_url
            << " \t: process id " << render_frame_host_id.child_id
            << ", render frame id " << render_frame_host_id.frame_routing_id
            << ", adblock_resource_type " << adblock_resource_type
            << ": Allowing load";
    std::move(callback).Run(FilterMatchResult::kNoRule);
    return;
  }

  CheckRequestFilterMatchImpl(std::move(subscription_collections), request_url,
                              adblock_resource_type, host->GetGlobalId(),
                              std::move(callback));
}

void ResourceClassificationRunnerImpl::CheckRequestFilterMatchImpl(
    SubscriptionService::Snapshot subscription_collections,
    const GURL& request_url,
    ContentType adblock_resource_type,
    content::GlobalRenderFrameHostId frame_host_id,
    CheckFilterMatchCallback callback) {
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);
  DCHECK_CALLED_ON_VALID_SEQUENCE(sequence_checker_);
  DVLOG(1) << "[eyeo] CheckRequestFilterMatchImpl for " << request_url.spec();

  auto* host = content::RenderFrameHost::FromID(frame_host_id);
  if (!host) {
    // Host has died, likely because this is a deferred execution. It does not
    // matter anymore whether the resource is blocked, the page is gone.
    std::move(callback).Run(FilterMatchResult::kNoRule);
    return;
  }
  const std::vector<GURL> frame_hierarchy_chain =
      frame_hierarchy_builder_->BuildFrameHierarchy(host);

  DVLOG(1) << "[eyeo] Got " << frame_hierarchy_chain.size()
           << " frame_hierarchy for " << request_url.spec();

  auto site_key_pair =
      sitekey_storage_->FindSiteKeyForAnyUrl(frame_hierarchy_chain);
  SiteKey site_key;
  if (site_key_pair.has_value()) {
    site_key = site_key_pair->second;
    DVLOG(1) << "[eyeo] Found site key: " << site_key.value()
             << " for url: " << site_key_pair->first;
  }

  base::ThreadPool::PostTaskAndReplyWithResult(
      FROM_HERE, {},
      base::BindOnce(
          &ResourceClassificationRunnerImpl::CheckRequestFilterMatchInternal,
          resource_classifier_, std::move(subscription_collections),
          request_url, frame_hierarchy_chain, adblock_resource_type,
          std::move(site_key)),
      base::BindOnce(
          &ResourceClassificationRunnerImpl::OnCheckResourceFilterMatchComplete,
          weak_ptr_factory_.GetWeakPtr(), request_url, frame_hierarchy_chain,
          adblock_resource_type, frame_host_id, std::move(callback)));
}

// static
ResourceClassificationRunnerImpl::CheckResourceFilterMatchResult
ResourceClassificationRunnerImpl::CheckRequestFilterMatchInternal(
    const scoped_refptr<ResourceClassifier>& resource_classifier,
    SubscriptionService::Snapshot subscription_collections,
    const GURL request_url,
    const std::vector<GURL> frame_hierarchy,
    ContentType adblock_resource_type,
    const SiteKey sitekey) {
  TRACE_EVENT1("eyeo",
               "ResourceClassificationRunnerImpl::"
               "CheckRequestFilterMatchInternal",
               "url", request_url.spec());

  DVLOG(1) << "[eyeo] CheckRequestFilterMatchInternal start";

  auto classification_result = resource_classifier->ClassifyRequest(
      std::move(subscription_collections), request_url, frame_hierarchy,
      adblock_resource_type, sitekey);

  if (classification_result.decision == ClassificationDecision::Allowed) {
    VLOG(1) << "[eyeo] Document allowed due to allowing filter " << request_url;
    return CheckResourceFilterMatchResult(
        FilterMatchResult::kAllowRule,
        {classification_result.decisive_subscription});
  }

  if (classification_result.decision == ClassificationDecision::Blocked) {
    VLOG(1) << "[eyeo] Document blocked " << request_url;
    return CheckResourceFilterMatchResult(
        FilterMatchResult::kBlockRule,
        {classification_result.decisive_subscription});
  }

  return CheckResourceFilterMatchResult(FilterMatchResult::kNoRule, {});
}

void ResourceClassificationRunnerImpl::OnCheckResourceFilterMatchComplete(
    const GURL request_url,
    const std::vector<GURL> frame_hierarchy,
    ContentType adblock_resource_type,
    content::GlobalRenderFrameHostId render_frame_host_id,
    CheckFilterMatchCallback callback,
    const CheckResourceFilterMatchResult result) {
  // Notify |callback| as soon as we know whether we should block, as this
  // unblocks loading of network resources.
  std::move(callback).Run(result.status);
  auto* render_frame_host =
      content::RenderFrameHost::FromID(render_frame_host_id);
  if (render_frame_host) {
    // Only notify the UI if we explicitly blocked or allowed the resource, not
    // when there was NO_RULE.
    if (result.status == FilterMatchResult::kAllowRule ||
        result.status == FilterMatchResult::kBlockRule) {
      NotifyAdMatched(request_url, result.status, frame_hierarchy,
                      adblock_resource_type, render_frame_host,
                      result.subscription);
    }
  }
}

void ResourceClassificationRunnerImpl::NotifyAdMatched(
    const GURL& url,
    FilterMatchResult result,
    const std::vector<GURL>& parent_frame_urls,
    ContentType content_type,
    content::RenderFrameHost* render_frame_host,
    const GURL& subscription) {
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);
  VLOG(1) << "[eyeo] NotifyAdMatched() called for " << url;

  for (auto& observer : observers_) {
    observer.OnAdMatched(url, result, parent_frame_urls,
                         static_cast<ContentType>(content_type),
                         render_frame_host, {subscription});
  }
}

void ResourceClassificationRunnerImpl::CheckResponseFilterMatch(
    SubscriptionService::Snapshot subscription_collections,
    const GURL& response_url,
    ContentType adblock_resource_type,
    content::GlobalRenderFrameHostId render_frame_host_id,
    const scoped_refptr<net::HttpResponseHeaders>& headers,
    CheckFilterMatchCallback callback) {
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);
  DCHECK_CALLED_ON_VALID_SEQUENCE(sequence_checker_);
  DVLOG(1) << "[eyeo] CheckResponseFilterMatch for " << response_url.spec();
  content::RenderFrameHost* host =
      frame_hierarchy_builder_->FindRenderFrameHost(render_frame_host_id);
  if (!host) {
    // This request is likely dead, since there's no associated RenderFrameHost.
    std::move(callback).Run(FilterMatchResult::kNoRule);
    return;
  }

  auto frame_hierarchy = frame_hierarchy_builder_->BuildFrameHierarchy(host);
  // ResponseFilterMatch might take a while, let it run in the background.
  base::ThreadPool::PostTaskAndReplyWithResult(
      FROM_HERE, {},
      base::BindOnce(
          &ResourceClassificationRunnerImpl::CheckResponseFilterMatchInternal,
          resource_classifier_, std::move(subscription_collections),
          response_url, frame_hierarchy, adblock_resource_type,
          std::move(headers)),
      base::BindOnce(
          &ResourceClassificationRunnerImpl::OnCheckResourceFilterMatchComplete,
          weak_ptr_factory_.GetWeakPtr(), response_url, frame_hierarchy,
          adblock_resource_type, host->GetGlobalId(), std::move(callback)));
}

// static
ResourceClassificationRunnerImpl::CheckResourceFilterMatchResult
ResourceClassificationRunnerImpl::CheckResponseFilterMatchInternal(
    const scoped_refptr<ResourceClassifier> resource_classifier,
    SubscriptionService::Snapshot subscription_collections,
    const GURL response_url,
    const std::vector<GURL> frame_hierarchy,
    ContentType adblock_resource_type,
    const scoped_refptr<net::HttpResponseHeaders> response_headers) {
  auto classification_result = resource_classifier->ClassifyResponse(
      std::move(subscription_collections), response_url, frame_hierarchy,
      adblock_resource_type, response_headers);

  if (classification_result.decision == ClassificationDecision::Allowed) {
    VLOG(1) << "[eyeo] Document allowed due to allowing filter "
            << response_url;
    return CheckResourceFilterMatchResult(
        FilterMatchResult::kAllowRule,
        {classification_result.decisive_subscription});
  }

  if (classification_result.decision == ClassificationDecision::Blocked) {
    VLOG(1) << "[eyeo] Document blocked " << response_url;
    return CheckResourceFilterMatchResult(
        FilterMatchResult::kBlockRule,
        {classification_result.decisive_subscription});
  }

  return CheckResourceFilterMatchResult(FilterMatchResult::kNoRule, {});
}

void ResourceClassificationRunnerImpl::CheckRewriteFilterMatch(
    SubscriptionService::Snapshot subscription_collections,
    const GURL& request_url,
    content::GlobalRenderFrameHostId render_frame_host_id,
    base::OnceCallback<void(const absl::optional<GURL>&)> callback) {
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);
  DVLOG(1) << "[eyeo] CheckRewriteFilterMatch for " << request_url.spec();

  content::RenderFrameHost* host =
      frame_hierarchy_builder_->FindRenderFrameHost(render_frame_host_id);
  if (!host) {
    std::move(callback).Run(absl::optional<GURL>{});
    return;
  }

  const std::vector<GURL> frame_hierarchy =
      frame_hierarchy_builder_->BuildFrameHierarchy(host);
  base::ThreadPool::PostTaskAndReplyWithResult(
      FROM_HERE, {},
      base::BindOnce(&HasRewriteHelper, std::move(subscription_collections),
                     request_url, frame_hierarchy),
      std::move(callback));
}

}  // namespace adblock
