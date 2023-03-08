// This file is part of eyeo Chromium SDK,
// Copyright (C) 2006-present eyeo GmbH
//
// eyeo Chromium SDK is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License version 3 as
// published by the Free Software Foundation.
//
// eyeo Chromium SDK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with eyeo Chromium SDK.  If not, see <http://www.gnu.org/licenses/>.

function findEnglishEasyList(item) {
  console.log(item.title + ' ' + item.url + ' ' + item.languages);
  return item.title.toLowerCase().includes('easylist') &&
      item.url.toLowerCase().includes('easylist') &&
      item.languages.includes('en');
}

function containsSubscription(subscriptions, url) {
  for (const subscription of subscriptions) {
    if (subscription.url === url) {
      return true;
    }
  }
  return false;
}

function verifySessionStats(sessionStats, expectedSubscription) {
  let index = -1;
  for (let i = 0; i < sessionStats.length; i++) {
    if (sessionStats[i].url === expectedSubscription) {
      index = i;
      break;
    }
  }
  if (index == -1) {
    chrome.test.fail('Failed: Missing entry in sessionStats');
    return;
  }
  if (sessionStats[index].count != 1) {
    chrome.test.fail(
        'Failed: Expected a single subscription hit number, got: ' +
        sessionStats[index].count);
    return;
  }
}

function verifyEventData(event, expectedSubscription) {
  if (event.tab_id < 1) {
    chrome.test.fail('Failed: Wrong tab_id value');
  }
  if (event.window_id < 1) {
    chrome.test.fail('Failed: Wrong window_id value');
  }
  if (event.content_type !== 'XMLHTTPREQUEST') {
    chrome.test.fail('Failed: Wrong content_type value: ' + event.content_type);
  }
  if (event.subscription !== expectedSubscription) {
    chrome.test.fail('Failed: Wrong subscription value: ' + event.subscription);
  }
}

var availableTests = [
  function setEnabled_isEnabled() {
    chrome.adblockPrivate.setEnabled(true);
    chrome.adblockPrivate.isEnabled(function(enabled) {
      if (enabled)
        chrome.test.succeed();
      else
        chrome.test.fail('Failed: Adblocking should be enabled');
    });
    chrome.adblockPrivate.setEnabled(false);
    chrome.adblockPrivate.isEnabled(function(enabled) {
      if (!enabled)
        chrome.test.succeed();
      else
        chrome.test.fail('Failed: Adblocking should be enabled');
    });
  },
  function setAAEnabled_isAAEnabled() {
    chrome.adblockPrivate.setAcceptableAdsEnabled(true);
    chrome.adblockPrivate.isAcceptableAdsEnabled(function(enabled) {
      if (enabled)
        chrome.test.succeed();
      else
        chrome.test.fail('Failed: AA should be enabled');
    });
    chrome.adblockPrivate.setAcceptableAdsEnabled(false);
    chrome.adblockPrivate.isAcceptableAdsEnabled(function(enabled) {
      if (!enabled)
        chrome.test.succeed();
      else
        chrome.test.fail('Failed: AA should be enabled');
    });
  },
  function getBuiltInSubscriptions() {
    chrome.adblockPrivate.getBuiltInSubscriptions(function(recommended) {
      let found = recommended.find(findEnglishEasyList);
      if (found) {
        chrome.test.succeed();
      } else {
        chrome.test.fail('Failed: invalid built-in subscriptions');
      }
    });
  },
  // This test works because at the beginning getInstalledSubscriptions returns
  // just default entries from recommended subscriptions.
  function installedSubscriptionsDataSchema() {
    chrome.adblockPrivate.getInstalledSubscriptions(function(installed) {
      for (const subscription of installed) {
        if (!subscription.current_version)
          chrome.test.fail('Failed: Must contain "current_version" property');
        if (!subscription.installation_state)
          chrome.test.fail(
              'Failed: Must contain "installation_state" property');
        if (!subscription.last_installation_time)
          chrome.test.fail(
              'Failed: Must contain "last_installation_time" property');
        if (!subscription.title)
          chrome.test.fail('Failed: Must contain "title" property');
        if (!subscription.url)
          chrome.test.fail('Failed: Must contain "url" property');
        chrome.test.succeed();
      }
    });
  },
  function installSubscriptionInvalidURL() {
    chrome.adblockPrivate.installSubscription('http://', function() {
      if (!chrome.runtime.lastError)
        chrome.test.fail('Failed: invalid input accepted');
      else
        chrome.test.succeed();
    });
  },
  function uninstallSubscriptionInvalidURL() {
    chrome.adblockPrivate.uninstallSubscription('http://', function() {
      if (!chrome.runtime.lastError)
        chrome.test.fail('Failed: invalid input accepted');
      else
        chrome.test.succeed();
    });
  },
  function subscriptionsManagement() {
    const urlParams = new URLSearchParams(window.location.search);
    let kEasylist = urlParams.get('easylist');
    let kExceptionrules = urlParams.get('exceptions');
    let kABPFilters = urlParams.get('snippets');
    let kCustom = 'https://example.com/subscription.txt';

    function containsDefaultSubscriptions(subscriptions) {
      return containsSubscription(subscriptions, kEasylist) &&
          containsSubscription(subscriptions, kExceptionrules) &&
          containsSubscription(subscriptions, kABPFilters);
    }

    chrome.adblockPrivate.getInstalledSubscriptions(function(installed) {
      if (installed.length) {
        if (!containsDefaultSubscriptions(installed)) {
          chrome.test.fail('Failed: Should contain all default subscriptions');
        }
        for (const subscription of installed) {
          chrome.adblockPrivate.uninstallSubscription(subscription.url);
        }
      } else {
        chrome.test.fail('Failed: Should contain default subscriptions');
      }
      chrome.adblockPrivate.getInstalledSubscriptions(function(installed) {
        if (installed.length) {
          chrome.test.fail(
              'Failed: There shouldn\'t be any installed subscriptions');
        }
        chrome.adblockPrivate.installSubscription(kEasylist);
        chrome.adblockPrivate.installSubscription(kExceptionrules);
        chrome.adblockPrivate.installSubscription(kABPFilters);
        chrome.adblockPrivate.getInstalledSubscriptions(function(installed) {
          if (installed.length) {
            if (!containsDefaultSubscriptions(installed)) {
              chrome.test.fail(
                  'Failed: Should contain all default subscriptions');
            }
          } else {
            chrome.test.fail('Failed: Should contain default subscriptions');
          }
          chrome.adblockPrivate.installSubscription(kCustom);
          chrome.adblockPrivate.getInstalledSubscriptions(function(installed) {
            if (!containsSubscription(installed, kCustom)) {
              chrome.test.fail('Failed: Should contain custom subscription');
            }
            chrome.adblockPrivate.uninstallSubscription(kCustom);
            chrome.adblockPrivate.getInstalledSubscriptions(function(
                installed) {
              if (containsSubscription(installed, kCustom)) {
                chrome.test.fail(
                    'Failed: Should not contain custom subscription');
              } else {
                chrome.test.succeed();
              }
            });
          });
        });
      });
    });
  },
  function allowedDomainsManagement() {
    chrome.adblockPrivate.getAllowedDomains(function(domains) {
      if (domains.length) {
        for (const domain of domains)
          chrome.adblockPrivate.removeAllowedDomain(domain);
      }

      chrome.adblockPrivate.getAllowedDomains(function(domains) {
        if (domains.length) {
          chrome.test.fail('Failed: There shouldn\'t be any allowed domains');
          return;
        }

        chrome.adblockPrivate.addAllowedDomain('foo.bar');
        chrome.adblockPrivate.addAllowedDomain('bar.baz');
        chrome.adblockPrivate.getAllowedDomains(function(domains) {
          if (domains.length != 2) {
            chrome.test.fail('Failed: There should be 2 allowed domains');
            return;
          }

          if (domains.indexOf('foo.bar') == -1 ||
              domains.indexOf('bar.baz') == -1) {
            chrome.test.fail('Failed: Didn\'t find expected allowed domains');
            return;
          }

          chrome.adblockPrivate.removeAllowedDomain('foo.bar');
          chrome.adblockPrivate.removeAllowedDomain('bar.baz');

          chrome.adblockPrivate.getAllowedDomains(function(domains) {
            if (domains.length)
              chrome.test.fail('Failed: Still have allowed domains');
            else
              chrome.test.succeed();
          });
        });
      });
    });
  },
  function customFiltersManagement() {
    chrome.adblockPrivate.getCustomFilters(function(filters) {
      if (filters.length) {
        for (const filter of filters)
          chrome.adblockPrivate.removeCustomFilter(filter);
      }

      chrome.adblockPrivate.getCustomFilters(function(filters) {
        if (filters.length) {
          chrome.test.fail('Failed: There shouldn\'t be any custom filters');
          return;
        }

        chrome.adblockPrivate.addCustomFilter('foo.bar');
        chrome.adblockPrivate.addCustomFilter('bar.baz');
        chrome.adblockPrivate.getCustomFilters(function(filters) {
          if (filters.length != 2) {
            chrome.test.fail('Failed: There should be 2 custom filters');
            return;
          }

          if (filters.indexOf('foo.bar') == -1 ||
              filters.indexOf('bar.baz') == -1) {
            chrome.test.fail('Failed: Didn\'t find expected custom filters');
            return;
          }

          chrome.adblockPrivate.removeCustomFilter('foo.bar');
          chrome.adblockPrivate.removeCustomFilter('bar.baz');

          chrome.adblockPrivate.getCustomFilters(function(filters) {
            if (filters.length)
              chrome.test.fail('Failed: Still have custom filters');
            else
              chrome.test.succeed();
          });
        });
      });
    });
  },
  function adBlockedEvents() {
    const urlParams = new URLSearchParams(window.location.search);
    let expectedSubscription = urlParams.get('subscription_url');
    chrome.adblockPrivate.onSubscriptionUpdated.addListener(function(
        subscription) {
      if (subscription !== expectedSubscription)
        chrome.test.fail('Failed: Unexpected subscription: ' + subscription);

      chrome.adblockPrivate.onAdBlocked.addListener(function(e) {
        verifyEventData(e, expectedSubscription);
        if (e.url.includes('test1.png')) {
          chrome.test.succeed();
        }
      });
      let request = new XMLHttpRequest();
      let handler = function() {};
      request.onload = handler;
      request.onerror = handler;
      request.open('GET', 'https://example.com/test1.png', true);
      request.send();
    });
    chrome.adblockPrivate.installSubscription(expectedSubscription);
  },
  function adAllowedEvents() {
    const urlParams = new URLSearchParams(window.location.search);
    let expectedSubscription = urlParams.get('subscription_url');
    chrome.adblockPrivate.onSubscriptionUpdated.addListener(function(
        subscription) {
      if (subscription !== expectedSubscription)
        chrome.test.fail('Failed: Unexpected subscription: ' + subscription);

      chrome.adblockPrivate.onAdAllowed.addListener(function(e) {
        verifyEventData(e, expectedSubscription);
        if (e.url.includes('test2.png')) {
          chrome.test.succeed();
        }
      });
      let request = new XMLHttpRequest();
      let handler = function() {};
      request.onload = handler;
      request.onerror = handler;
      request.open('GET', 'https://example.com/test2.png', true);
      request.send();
    });
    chrome.adblockPrivate.installSubscription(expectedSubscription);
  },
  function sessionStats() {
    const urlParams = new URLSearchParams(window.location.search);
    let expectedSubscription = urlParams.get('subscription_url');
    // Verify that subscription was downloaded and update event triggered,
    // then verify correct number of a session blocked and allowed ads count
    // for this subscription.
    chrome.adblockPrivate.onSubscriptionUpdated.addListener(function(
        subscription) {
      if (subscription !== expectedSubscription)
        chrome.test.fail('Failed: Unexpected subscription: ' + subscription);

      let handler = function() {
        chrome.adblockPrivate.getSessionBlockedAdsCount(function(sessionStats) {
          verifySessionStats(sessionStats, expectedSubscription);
          let handler = function() {
            chrome.adblockPrivate.getSessionAllowedAdsCount(function(
                sessionStats) {
              verifySessionStats(sessionStats, expectedSubscription);
              chrome.test.succeed();
            });
          };
          let request = new XMLHttpRequest();
          request.onload = handler;
          request.onerror = handler;
          request.open('GET', 'https://example.com/test4.png', true);
          request.send();
        });
      };
      let request = new XMLHttpRequest();
      request.onload = handler;
      request.onerror = handler;
      request.open('GET', 'https://example.com/test3.png', true);
      request.send();
    });
    chrome.adblockPrivate.installSubscription(expectedSubscription);
  },
];

var testToRun;
if (window.location.search.indexOf('&') > 1)
  testToRun =
      window.location.search.substring(1, window.location.search.indexOf('&'));
else
  testToRun = window.location.search.substring(1);
chrome.test.runTests(availableTests.filter(function(op) {
  return op.name == testToRun;
}));
