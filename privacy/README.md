# Privacy Module

Wrapping library around the Sourcepoint SDK to simplify the integration.

For any question/bug/feature request regarding the integration, slack me **@Hendri**

## Installation

### Add the following maven repositories to resolve the artifacts

```groovy
maven {
    setUrl("https://apps-sdk.voodoo-tech.io/android/")
    mavenContent {
        includeGroup("io.voodoo.apps")
    }
}
```

### Artifacts

```groovy
// GDPR/Consent flow
implementation("io.voodoo.apps:privacy:<latest_version>")
```

### Versioning

The SDK versioning is based on the ads module's versioning,
read [here](https://github.com/VoodooTeam/APPS-ad-tools-android?tab=readme-ov-file#versioning) for
more details

## Getting Started

### Instantiate the class VoodooPrivacyManager

We can instantiate the class lazily in the activity with the code below:

```kotlin
private val voodooConsentManager by lazy {
        VoodooPrivacyManager(
            lifecycleOwner = this,
            currentActivity = this,
            autoShowPopup = true,
            sourcepointConfiguration = SourcepointConfiguration(
                accountId = <ACCOUNT_ID>,
                propertyId = <PROPERTY_ID>,
                gdprPrivacyManagerId = "<GDPR_PRIVACY_MANAGER_ID>",
                usMspsPrivacyManagerId = "<MSPS_PRIVACY_MANAGER_ID>",
                propertyName = "<PROPERTY_NAME>"
            ),
            onUiReady = {
                        
            },
            onConsentReceived = {
                onReceiveConsent(it)
            },
            onError = {
                //Might be executed from background thread
                it.printStackTrace()
                onPrivacyError()
            }
        )
    }
```

### Instantiation Parameters

* `lifecycleOwner` : `LifecycleOwner` **(required)** will be used as the base for listening to the
  lifecycle
* `currentActivity` : `Activity` **(required)** will be used as the initialization parameter to
  Sourcepoint SDK
* `autoShowPopup` : `Boolean` **(required)** If **true** it will automatically show the dialog when
  the consent is loaded and needs an input from User. If **false**, we will need to handle the
  showing of consent dialog manually by calling `VoodooPrivacyManager.showDialog()`. We will need to
  listed to the callback `onUiReady`
* `sourcepointConfiguration` : `SourcepointConfiguration` **(required)** configuration that will be
  passed to Sourcepoint
* `onUiReady` : `(() -> Unit)` **(optional)** A callback that will be triggered once the dialog UI
  is properly loaded and ready to be shown by calling `VoodooPrivacyManager.showDialog()`
* `onConsentReceived` : `((VoodooPrivacyConsent) -> Unit)` **(optional)** will be called once the
  consent data is ready regardless a privacy dialog is shown or not
* `onError` : `((Throwable) -> Unit)` **(optional)** will be called if there are an error when
  getting the consent data.
* `onStatusUpdate` : `((ConsentStatus) -> Unit)` **(optional)** will be used as the base for
  listening to the VoodooPrivacyManager's status (LOADING, UI_READY, etc)

**Please note** that the callbacks might be called from background thread from the 3rd party code
hence if you need to do anything in the UI state directly, you'll need to wrap with `runOnUiThread`

### Initialize VoodooPrivacyManager

You can initialize the VoodooPrivacyManager class by
calling `voodooConsentManager.initializeConsent()`

### Change consent

If the user want to change consent, normally by clicking a privacy button, you can display the
change consent dialog by calling `voodooConsentManager.changePrivacyConsent()` but since the privacy
button is not available for every one you can hide or show it depends on this
method `voodooConsentManager.isPrivacyApplies()`

### `onConsentReceived` explained

The SDK will call this callback in this 3 scenarios below:

* User is in GDPR region. It will send back `VoodooPrivacyConsent` with value as below. For the
  subsequent session it will just trigger `onConsentReceived` without showing the dialog.
  * `adConsent` : Depend on what the user enter in the dialog
  * `analyticsConsent` : Depend on what the user enter in the dialog
  * `doNotSellDataEnabled` : false
  * `gdprApplicable` : true
* User is in US region and in the state of California, Colorado, Connecticut, Utah and Virginia . It
  will initially return `doNotSellDataEnabled` as **false** but user can change it to **true** if
  the user change the consent (calling `voodooConsentManager.changePrivacyConsent()`). Don't forget
  to pass the `doNotSellDataEnabled` to other 3rd party SDK that can receive the parameter.
  * `adConsent` : false
  * `analyticsConsent` : false
  * `doNotSellDataEnabled` : Depend on what the user set
  * `gdprApplicable` : false
* Consent is not applicable to the user (outside of the two above). It will send
  back `VoodooPrivacyConsent` with value as below
  * `adConsent` : false
  * `analyticsConsent` : false
  * `doNotSellDataEnabled` : false
  * `gdprApplicable` : false

## Privacy/Consent Impact on 3rd Party SDK Initialization

Below are the table of situation when to initialize ads or analytics related 3rd party SDK
| gdprApplicable | adConsent | analyticsConsent | Result |
|----------|----------|----------|----------|
|false|*|*| You can initialize any SDK|
| true | false | true | You can only initialize Analytics's related SDK|
| true | true | false |You can only initialize Ads's related SDK|
|true|true|true| You can initialize any SDK|
