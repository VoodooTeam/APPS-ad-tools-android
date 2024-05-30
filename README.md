# APPS-ad-tools-android

Wrapping library around the AppLovin SDK to simplify the integration and not waste time debugging
known issues.

For any question regarding the integration, slack me @Yann Badoual

### demo app

The app module is a demo app with a list (`LazyColumn`) of posts like instagram.

* AppLovin SDK + Apphrbr (moderation) initialization in `AdsInitiliazer` class (called
  from `Application.onCreate`)
* `AdArbitrageurFactory`: `AdClient` + `AdArbitrageur` instantiation
* `FeedScreen`: main screen, list of post
* `FeedState`: demo integration of ads in a `LazyList`
* `FeedAdItem`: composable to display the ad item
* `layout_feed_ad_item`: native ad layout to emulate a post's design

Because ads are loaded and can take time to be available, it creates a lot of edge cases, and we
need to insert them dynamically once loaded into the LazyList. `FeedState` provides a default
integration of this behavior and tries to handle most edge cases for the behavior wanted in such an
app.

### library

This is a WIP, on the long term, the `ads` module will be split in several modules:

* `ads-api` (api subpackage): abstraction layer with no dependency to applovin/any network
* `ads-applovin` (applovin subpackage): the implementation of the API with applovin SDK dependency
    * apphrbr is included until we find a clean API to extract it in another module like we did for
      amazon's integration
* `ads-noop`: a dummy implementation of `ads-api` to build your app without ads (eg: for faster
  debug build)
* `ads-plugin-*`: every extension plugin that might be required for a network to be integrated (eg:
  amazon for mrec in `AmazonMRECAdClientPlugin`)

## Integration steps

* Add jitpack as a dependency resolution strategy (see https://jitpack.io/)
    * this is needed for apphrbr SDK (moderation)
* Check https://developers.applovin.com/en/android/overview/integration/ for reference
* Pull the `ads` module in your project
    * this is temporary until we deploy a maven artifact, so I don't recommend making changes in it.
      If you need to make changes in it, slack me at @Yann Badoual and we'll do it in the repo
* Pull the `MaxNativeAdContent` + `MRECAdContent` composable in your app for convenience
    * Note: the applovin native ad format relies on you providing an xml layout file with
      pre-defined views and specifying the binding via `MaxNativeAdViewBinder`
    * see https://developers.applovin.com/en/android/ad-formats/native-ads#manual
    * see `layout_feed_ad_item` layout
* Add the dependency for each network in your app module (not in the `ads` module, cf above)
    * Check https://developers.applovin.com/en/android/preparing-mediated-networks to see if you
      need additional steps beside adding the dependency
    * Note: by calling `AppLovinSdk.getInstance(context.applicationContext).showMediationDebugger()`
      you can launch the mediation debugger and check that every integration is working properly (
      and enable test mode to test a specific network,
      see https://developers.applovin.com/en/android/testing-networks/mediation-debugger/)
* Optional: if you don't need the amazon network, remove the dependencies from the `ads` module and
  delete the `AmazonMRECAdClientPlugin` class. For now we need those in the module until we split
  the module.
* Optional (not recommended): if you don't want the apphrbr moderation layer:
    * remove the `com.github.appharbr:appharbr-android-sdk` dependency
    * in `MaxNativeAdClient` and `MaxMRECAdClient` remove any call to the SDK
* Configure the ad review plugin by following the steps
  here https://developers.applovin.com/en/android/overview/integration#enable-ad-review
* Initialize AppLovin (+ Apphrbr, Amazon, ...) SDKs by following the official documentation and the
  demo app in `AdsInitiliazer`
    * Note: before initializing applovin/apphrbr and instanciating any clients, wait until you have
      the user's GDPR consents for ads
* Create an `MaxNativeAdClient` and/or `MaxMRECAdClient` and/or `AdArbitrageurFactory` and start
  loading/displaying ads following the example in the sample app

To display the ad properly in your app, check the demo app (see README Demo app section)

### Updating AppLovin/network adapter

Applovin's adapters versioning is the concat of the SDK version + an adapter version. eg: if the SDK
version is `9.9.3`, the applovin adapter version will be `9.9.3.X`.

To update applovin or a network adapter, you first need to check that apphrbr is supporting this
version (they tend to be very slow to support new versions). Check apphrbr support
here https://helpcenter.appharbr.com/hc/en-us/articles/17039424125457-Before-Starting

Once you checked the latest supported version of a network SDK, you'll need to check the latest
applovin adapter for this network. The easiest way is to check on mvnrepository.com
(eg for amazon: https://mvnrepository.com/artifact/com.applovin.mediation/amazon-tam-adapter).

To see the latest adreview plugin version, check
here https://artifacts.applovin.com/android/com/applovin/quality/AppLovinQualityServiceGradlePlugin/maven-metadata.xml

### Cool tips

* To test a specific network:
    * you might need to change your location with a VPN
    * some free VPN won't work, NordVPN seems to work
    * the location will depend of the network (some networks only serve in a few countries)
    * in the `configureSettings` add the following call:

```kotlin
setExtraParameter("test_mode_network", "ADMOB_BIDDING")
```

* If loading ads starts to get slow or you get a lot of no-fill, try to reset your advertising ID 
