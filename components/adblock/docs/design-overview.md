# Design overview

This section describes the general design of eyeo Chromium SDK, and how it fits into Chromium's design (classes, services, processes, etc).

## Services

The SDK is composed of the following `KeyedService`s:

* `AdblockController`: Allows to control ad-filtering settings.
* `AdblockJNI`: Provides JNI bindings for Android components.
* `AdblockTelemetryService`: Reports anonymous usage statistics to eyeo.
* `ContentSecurityPolicyInjector`: Injecting a Content Security Policy header into a HTTP response.
* `ElementHider`: Applies element hiding scripts and stylesheets on web pages.
* `ResourceClassificationRunner`: Decides whether to block or allow network requests.
* `SessionStats`: Stores statistics about blocked and allowed URLs in current session.
* `SitekeyStorage`: Extracts SiteKeys from response headers, validates and stores them.
* `SubscriptionPersistentMetadata`: Stores persistent subscription metadata in PrefService.
* `SubscriptionService`: Maintains a state of available `Subscription`s and synchronizes it with persistent storage.
* `SubscriptionUpdater`: Periodically updates installed subscriptions.

Following classes are also important to understand workflow:

* `AdblockURLLoaderFactory`: Processing network requests and responses.
* `AdblockWebContentObserver`: Listens to page load events to trigger frame-wide element hiding.

## Highlighted Chromium classes

The Chromium classes that are particularly important for our implementation are:

* `RenderFrameHost`: To find the [frame hierarchy](ad-filtering/README.md#frame-heirarchy), and to execute CSS and JavaScript.
* `WebContentsObserver`: To receive page load events, as well as injecting element hiding. Parent of `AdblockWebContentsObserver`.
* `ChromeContentBrowserClient`: For setup of URL loader factories for processing network requests, handle web sockets, popups, etc. Parent of `AdblockContentBrowserClient`.
