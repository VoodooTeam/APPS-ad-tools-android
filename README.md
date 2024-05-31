# APPS-ad-tools-android

Wrapping library around the AppLovin SDK to simplify the integration and not waste time debugging
known issues.

For any question regarding the integration, slack me @Yann Badoual

### Setup

[Changelog](https://github.com/VoodooTeam/APPS-ad-tools-android/releases)

```groovy
maven {
    setUrl("https://apps-sdk.voodoo-tech.io/android/")
    mavenContent {
        includeGroup("io.voodoo.apps")
    }
}
// jitpack required for apphrbr, feel free to add an mavenContent/includeGroup clause
maven { setUrl("https://jitpack.io") }

// For every module using the ads
implementation("io.voodoo.apps:ads-api:<latest_version>")
implementation("io.voodoo.apps:ads-applovin:<latest_version>")
implementation("io.voodoo.apps:ads-applovin-compose:<latest_version>")

// If using amazon network
implementation("io.voodoo.apps:ads-applovin-plugin-amazon:<latest_version>")
```

Note: the SDK versioning is based on applovin's version, and concatenates a version number
eg: if applovin version is `12.5.0`, the matching sdk versions will be `12.5.0.X`

The same thing is true for the amazon plugin, we use the applovin amazon adapter plugin,
which is itself using the amazon's SDK version
eg: if amazon sdk version is `9.9.3`, the applovin adapter will be `9.9.3.X` and the sdk plugin
version will be `9.9.3.X.Y`

### demo app

The app module is a demo app with a list (`LazyColumn`) of posts like instagram.

* AppLovin SDK + Apphrbr (moderation) initialization in `AdsInitiliazer` class (called
  from `MainActivity` after the consent is received)
* `AdArbitrageurFactory`: `AdClient` + `AdArbitrageur` instantiation
* `FeedScreen`: main screen, list of post
* `FeedAdItem`: composable to display the ad item
* `layout_feed_ad_item`: native ad layout to emulate a post's design

Because ads are loaded and can take time to be available, it creates a lot of edge cases, and we
need to insert them dynamically once loaded into the LazyList. `FeedState` provides a default
integration of this behavior and tries to handle most edge cases for the behavior wanted in such an
app.

### library

* `ads-api`: abstraction layer with no dependency to applovin/any network
* `ads-applovin`: the implementation of the API with applovin SDK dependency
    * apphrbr is included until we find a clean API to extract it in another module like we did for
      amazon's integration
* `ads-applovin-compose`: provides some useful classes/extensions to use the lib with compose.
    * `LazyListAdMediator` is a basic integration of the ads into a LazyList (used in sample app)
* `ads-applovin-plugin-*`: every extension plugin that might be required for a network to be
  integrated (eg: amazon for mrec in `AmazonMRECAdClientPlugin`)
* TODO `ads-noop`: a dummy implementation of `ads-api` to build your app without ads (eg: for faster
  debug build)

## Integration steps

* Add SDK dependencies (see Setup section above)
* Check sample `AdsInitiliazer` + https://developers.applovin.com/en/android/overview/integration/
  for reference
* Add the dependency for each network in your app module (not in the `ads` module, cf above)
    * Check https://developers.applovin.com/en/android/preparing-mediated-networks for each network
      to see if you need additional steps
    * Note: by calling `AppLovinSdk.getInstance(context.applicationContext).showMediationDebugger()`
      you can launch the mediation debugger and check that every integration is working properly
      (and enable test mode to test a specific network,
      see https://developers.applovin.com/en/android/testing-networks/mediation-debugger/)
* Configure the ad review plugin by following the steps
  here https://developers.applovin.com/en/android/overview/integration#enable-ad-review
* Initialize AppLovin (+ Apphrbr, Amazon, ...) SDKs by following the official documentation and the
  demo app in `AdsInitiliazer`
    * Note: before initializing applovin/apphrbr and instanciating any clients, wait until you have
      the user's GDPR consents for ads
* Create an `MaxNativeAdClient` and/or `MaxMRECAdClient` and/or `AdArbitrageurFactory` and start
  loading/displaying ads following the example in the sample app

* Note: the applovin native ad format relies on you providing an xml layout file with
  pre-defined views and specifying the binding via `MaxNativeAdViewBinder`
    * see https://developers.applovin.com/en/android/ad-formats/native-ads#manual
    * see `layout_feed_ad_item` layout for a sample

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
  * in the `configureSettings` block add the following call:

```kotlin
setExtraParameter("test_mode_network", "ADMOB_BIDDING")
```

* If loading ads starts to get slow or you get a lot of no-fill, try to reset your advertising ID 
