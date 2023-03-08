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

package org.chromium.components.adblock;

import org.junit.Assert;
import org.junit.Rule;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

public class TestAdBlockedObserver implements ResourceClassificationNotifier.AdBlockedObserver {
    @Override
    public void onAdAllowed(AdblockCounters.ResourceInfo info) {
        if (countDownLatch != null) {
            countDownLatch.countDown();
        }
        // to be remove after DPD-961
        if (alreadyCounted.contains(info.getRequestUrl())) {
            return;
        }
        alreadyCounted.add(info.getRequestUrl());
        allowedInfos.add(info);
        Assert.assertTrue(info.getSubscription().equals(getExpectedSubscriptionUrl()));
    }

    @Override
    public void onAdBlocked(AdblockCounters.ResourceInfo info) {
        if (countDownLatch != null) {
            countDownLatch.countDown();
        }
        // to be remove after DPD-961
        if (alreadyCounted.contains(info.getRequestUrl())) {
            return;
        }
        alreadyCounted.add(info.getRequestUrl());
        blockedInfos.add(info);
        Assert.assertTrue(info.getSubscription().equals(getExpectedSubscriptionUrl()));
    }

    @Override
    public void onPageAllowed(AdblockCounters.ResourceInfo info) {
        allowedPageInfos.add(info);
        Assert.assertTrue(info.getSubscription().equals(getExpectedSubscriptionUrl()));
    }

    @Override
    public void onPopupAllowed(AdblockCounters.ResourceInfo info) {
        allowedPopupsInfos.add(info);
        Assert.assertTrue(info.getSubscription().equals(getExpectedSubscriptionUrl()));
    }

    @Override
    public void onPopupBlocked(AdblockCounters.ResourceInfo info) {
        blockedPopupsInfos.add(info);
        Assert.assertTrue(info.getSubscription().equals(getExpectedSubscriptionUrl()));
    }

    public boolean isBlocked(String url) {
        for (AdblockCounters.ResourceInfo info : blockedInfos) {
            if (info.getRequestUrl().contains(url)) return true;
        }

        return false;
    }

    public boolean isPopupBlocked(String url) {
        for (AdblockCounters.ResourceInfo info : blockedPopupsInfos) {
            if (info.getRequestUrl().contains(url)) return true;
        }

        return false;
    }

    public int numBlockedByType(AdblockContentType type) {
        int result = 0;
        for (AdblockCounters.ResourceInfo info : blockedInfos) {
            if (info.getAdblockContentType() == type) ++result;
        }
        return result;
    }

    public int numBlockedPopups() {
        int result = 0;
        for (AdblockCounters.ResourceInfo info : blockedPopupsInfos) {
            ++result;
        }
        return result;
    }

    public boolean isAllowed(String url) {
        for (AdblockCounters.ResourceInfo info : allowedInfos) {
            if (info.getRequestUrl().contains(url)) return true;
        }

        return false;
    }

    public boolean isPageAllowed(String url) {
        for (AdblockCounters.ResourceInfo info : allowedPageInfos) {
            if (info.getRequestUrl().contains(url)) return true;
        }

        return false;
    }

    public boolean isPopupAllowed(String url) {
        for (AdblockCounters.ResourceInfo info : allowedPopupsInfos) {
            if (info.getRequestUrl().contains(url)) return true;
        }

        return false;
    }

    public int numAllowedByType(AdblockContentType type) {
        int result = 0;
        for (AdblockCounters.ResourceInfo info : allowedInfos) {
            if (info.getAdblockContentType() == type) ++result;
        }
        return result;
    }

    public int numAllowedPopups() {
        int result = 0;
        for (AdblockCounters.ResourceInfo info : allowedPopupsInfos) {
            ++result;
        }
        return result;
    }

    public void setExpectedSubscriptionUrl(URL url) {
        mTestSubscriptionUrl = url;
    }

    String getExpectedSubscriptionUrl() {
        if (mTestSubscriptionUrl != null) return mTestSubscriptionUrl.toString();
        return "adblock:custom";
    }

    public URL mTestSubscriptionUrl;
    public List<AdblockCounters.ResourceInfo> blockedInfos = new CopyOnWriteArrayList<>();
    public List<AdblockCounters.ResourceInfo> allowedInfos = new CopyOnWriteArrayList<>();
    public List<AdblockCounters.ResourceInfo> allowedPageInfos = new CopyOnWriteArrayList<>();
    public List<AdblockCounters.ResourceInfo> blockedPopupsInfos = new CopyOnWriteArrayList<>();
    public List<AdblockCounters.ResourceInfo> allowedPopupsInfos = new CopyOnWriteArrayList<>();
    public CountDownLatch countDownLatch;
    // public remove after DPD-961
    Set<String> alreadyCounted = new HashSet<String>();
};
