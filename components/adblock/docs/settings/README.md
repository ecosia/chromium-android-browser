# Settings

eyeo Chromium SDK can be configured to provide the experience most adequate for the end-user.

At the moment, the following settings are supported:

| Setting                 | Type    | Description |
| ----------------------- | ------- | ----------- |
| Ad-blocking             | Boolean | Whether ad-blocking is enabled or disabled. |
| Acceptable Ads          | Boolean | Whether Acceptable Ads are displayed or hidden. |
| Subscriptions           | List    | Subscriptions to filter lists. |
| Allowed domains         | List    | Domains on which no ads will be blocked, even when ad-blocking is enabled. |
| Custom filters          | List    | Additional filters implemented via [filter language](https://help.eyeo.com/adblockplus/how-to-write-filters) |

## Implementation details

Settings can be configured via the C++ class `AdblockController` or its Java counterpart, `org.chromium.components.adblock.AdblockController`.

The Browser Extension API defined and documented in `chrome/common/extensions/api/adblock_private.idl` uses the C++ implementation. Android UI fragments consume the Java one.

The [user setting sequence diagram](user-setting-sequence.png) describes the flow of updating a setting, and how it varies depending on the API being used.
