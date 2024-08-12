# APPS-ad-tools-android

Wrapping library around the AppLovin SDK to simplify the integration and not waste time debugging
known issues.

For any question/bug/feature request regarding the integration, slack me **@Yann Badoual**

## Changelog

[Changelog](https://github.com/VoodooTeam/APPS-ad-tools-android/releases)

## Privacy module (GDPR, ...)

[Privacy-Module](https://github.com/VoodooTeam/APPS-ad-tools-android/blob/feature/privacy-docs/privacy/README.md)

## Setup

#### Maven repositories

Add the following maven repositories to resolve the artifacts

```groovy
maven {
    setUrl("https://apps-sdk.voodoo-tech.io/android/")
    mavenContent {
        includeGroup("io.voodoo.apps")
    }
}
// jitpack required for apphrbr, feel free to add an mavenContent/includeGroup clause
maven { setUrl("https://jitpack.io") }
```

Note: you'll need the jitpack credentials to get apphrbr from jitpack

#### Artifacts

```groovy
// Ads basic api and compose utilities, no dependencies to applovin
// All your modules can use this lightweight artifacts
implementation("io.voodoo.apps:ads-api:<latest_version>")
implementation("io.voodoo.apps:ads-compose:<latest_version>")

// The actual implementation of the ads-api module, your app module needs it to initialize
// AppLovin and AppHrbr SDK, as well as provide AdClient/AdClientArbitrageur to your modules via DI
implementation("io.voodoo.apps:ads-applovin:<latest_version>")

// GDPR/Consent flow
implementation("io.voodoo.apps:privacy:<latest_version>")

// If using amazon network
implementation("io.voodoo.apps:ads-applovin-plugin-amazon:<latest_version>")
```

* `ads-api`: abstraction layer with no dependency to applovin/any network
* `ads-compose`: provides some useful classes/extensions to use the lib with compose.
    * `LazyListAdMediator` is a basic integration of the ads into a LazyList (used in sample app)
* `ads-applovin`: the implementation of the API with applovin SDK dependency
    * apphrbr is included until we find a clean API to extract it in another module like we did for
      amazon's integration
* `ads-applovin-plugin-*`: every extension plugin that might be required for a network to be
  integrated (eg: amazon for mrec in `AmazonMRECAdClientPlugin`)
* `ads-no-op`: a dummy implementation of `ads-api` to build your app without ads (eg: for faster
  debug build).
    * the objective is to replace `ads-applovin` module
    * note that this module doesn't provide a dummy implementation of `MaxNativeAdClient` (or mrec).
      Instead it just gives you a dummy AdClient implementation that will never return an ad
    * if you want to use this, you'll need to create two modules in your app, one to provide the
      actual clients to your arbitrageur/code, and one to provide the mock. Then you can import one
      or the other depending on what you want to build.

#### Versioning

The SDK versioning is based on applovin's version, and concatenates a version number
eg: if applovin version is `12.5.0`, the matching sdk versions will be `12.5.0.X`

The same thing applies for the amazon plugin, we use the applovin amazon adapter plugin,
which is itself using the amazon's SDK version
eg: if amazon sdk version is `9.9.3`, the applovin adapter will be `9.9.3.X` and the sdk plugin
version will be `9.9.3.X.Y`

## Demo app

The sample module is a demo app with integration of ads in a list (`LazyColumn`) of posts like
instagram.

* Add your GAID in `AdsInitiliazer` to the `setTestDeviceAdvertisingIds` call if you want to enable
  test mode
* [MainActivity](sample/src/main/java/io/voodoo/apps/ads/MainActivity.kt) handles getting user's
  consent to use collect data/use ads. Only after this consent is given we can initialize the
  various ads SDK.
* [AdsInitializer](sample/src/main/java/io/voodoo/apps/ads/feature/ads/AdsInitiliazer.kt): AppLovin
  SDK + Apphrbr (moderation) initialization
  (called from `MainActivity` after the consent is given)
* [FeedAdClientArbitrageurFactory](sample/src/main/java/io/voodoo/apps/ads/feature/ads/FeedAdClientArbitrageurFactory.kt):
  `AdClient` + `AdClientArbitrageur` instantiation/configuration
* [AdTracker](sample/src/main/java/io/voodoo/apps/ads/feature/ads/AdTracker.kt):
  Base tracking implementation that you probably need to implement
* [FeedScreen](sample/src/main/java/io/voodoo/apps/ads/feature/feed/FeedScreen.kt): main screen,
  list of post
* [FeedAdItem](sample/src/main/java/io/voodoo/apps/ads/feature/feed/component/FeedAdItem.kt):
  composable to display the ad item
    * For native ads, you need to implement the whole layout in an XML layout file with views for
      each element (title, body, icon, ...) (applovin requirement). You'll need to pass a view
      factory instance to your `MaxNativeAdClient`.
      See [MaxNativeAdViewFactory](sample/src/main/java/io/voodoo/apps/ads/feature/ads/nativ/AdMobNativeAdViewFactory.kt)
      for sample.
      see https://developers.applovin.com/en/android/ad-formats/native-ads#manual
    * For MREC ads, the applovin SDK provides us with a 300dp x 250dp view, and we can use it as we
      want.
      We can integrate this in a composable, like we do in the `FeedMRECAdItem` composable.
      see https://developers.applovin.com/en/android/ad-formats/banner-mrec-ads#mrecs

Because ads are loaded and can take time to be available, it creates a lot of edge cases, and we
need to insert them dynamically once loaded into the LazyList.

The [LazyListAdMediator](ads-compose/src/main/java/io/voodoo/apps/ads/compose/lazylist/LazyListAdMediator.kt)
class available in the `ads-compose` artifact provides a default integration of this behavior and
tries to handle most edge cases for the behavior wanted in an app like this.

For a seamless integration in an existing LazyList, you can use the overloads
of `LazyListScope.items` that takes a `adMediator: LazyListAdMediator` parameter
(see `FeedScreenContent` composable).

## Integration steps

* Add SDK dependencies (see Setup section above)
* Add the content of the [compose compiler stability config file](compose-compiler-config.conf) to
  your file
    *
  See https://developer.android.com/develop/ui/compose/performance/stability/fix#configuration-file
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
* Create a `MaxNativeAdClient` and/or `MaxMRECAdClient` (and potentially `AdClientArbitrageur`) and
  start loading/displaying ads following the example in the sample app

Don't forget to close your `AdClientArbitrageur`/`AdClient` when you're done to free resources. By
default
if the activity provided to the clients is a `LifecycleOwner`, the client will close itself when the
activity is destroyed.

To display the ad properly in your app, check the demo app (see README Demo app section)

## Updating AppLovin/network adapter

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

## Cool tips

* To test a specific network:
    * you might need to change your location with a VPN
    * some free VPN won't work, NordVPN seems to work
    * the location will depend of the network (some networks only serve in a few countries)
  * in the `configureSettings` block add the following call:

```kotlin
setExtraParameter("test_mode_network", "ADMOB_BIDDING")
```

* If loading ads starts to get slow or you get a lot of no-fill, try to reset your advertising ID

## Network specific

Some networks might require passing extra info when fetching an ad.
This can be done via the `localExtras`:

* When using `LazyListAdMediator` (and `DefaultScrollAdBehaviorEffect`) you can pass
  a `localExtrasProvider` to build this array when a request will be made.
* You can directly pass a `LocalExtrasProvider` instance to
  your `MaxNativeAdClient`/`MaxMRECAdClient` if it's more convenient than forwarding parameters to
  your ui

#### Bigo

* Bigo requires extra parameters when fetching an ad,
  see https://www.bigossp.com/guide/sdk/android/mediation/maxAdapter#5-load-and-show-an-ad

#### Amazon

```kotlin
AdRegistration.getInstance(AMAZON_APP_KEY, context)
AdRegistration.setAdNetworkInfo(DTBAdNetworkInfo(DTBAdNetwork.MAX))
AdRegistration.setMRAIDSupportedVersions(arrayOf("1.0", "2.0", "3.0"))
AdRegistration.setMRAIDPolicy(MRAIDPolicy.CUSTOM)
```

#### Facebook/Meta

```kotlin
AdSettings.setDataProcessingOptions(arrayOf())
```
