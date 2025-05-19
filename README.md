# Clickio Consent SDK for Android
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Setup and Usage](#setup-and-usage)
- [ExportData](#exportdata)
- [Integration with Third-Party Libraries for Google Consent Mode](#integration-with-third-party-libraries-for-google-consent-mode)
- [Integration with Third-Party Libraries when Google Consent Mode is disabled](#integration-with-third-party-libraries-when-google-consent-mode-is-disabled)

## Requirements

Before integrating the SDK, ensure that your application meets the following requirements:

-   **Minimum SDK Version:** `21` (Android 5.0)

-   **Target/Compile SDK Version:** The minimum required for Google Play compliance.

-   **Internet Permission:** The SDK requires internet access, so include the following permission in your `AndroidManifest.xml`:

    ```xml
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    ```

## Installation
**Maven Central Repository:** Ensure that `mavenCentral()` is included in your project's `build.gradle` file:

  ```gradle
  repositories {
      mavenCentral()
  }
```

To integrate the SDK, add the following dependency to your project's `build.gradle` file:

```gradle
 implementation("com.clickio:clickioconsentsdk:1.0.0-rc6")
```

## Quick Start

Here's the minimal implementation to get started:

**Make sure to replace string "Clickio Site ID" with yours [Site id](https://docs.clickio.com/books/clickio-consent-cmp/page/google-consent-mode-v2-implementation#bkmrk-access-the-template%3A)**.
```kotlin
with(ClickioConsentSDK.getInstance()) {
    initialize(
        context = context,
        config = ClickioConsentSDK.Config("Clickio Site ID") 
    )
    onReady { openDialog(context) }
}
```


In this code after successful initialization, the SDK will open the Consent dialog (a transparent `Activity` with a `WebView`).

## Setup and Usage

### Singleton Access

All interactions with the Clickio SDK should be done using the `ClickioConsentSDK.getInstance()` method to obtain the singleton instance of the SDK.

### Initialization

To initialize the SDK, use the `initialize` method:

```kotlin
ClickioConsentSDK.getInstance().initialize(context: Context, config: Config)
```

The SDK requires a configuration object with the following parameters:

```kotlin
data class Config(
    val siteId: String, // Your Clickio Site ID
    val appLanguage: String? = null // Optional, two-letter language code in ISO 639-1
)
```
[ISO 639-1](https://en.wikipedia.org/wiki/List_of_ISO_639_language_codes)
### Handling SDK Readiness

Use the `onReady` callback to execute actions once the SDK is fully loaded:

```kotlin
ClickioConsentSDK.getInstance().onReady {  
    ClickioConsentSDK.getInstance().openDialog(context)
}
```

The SDK should not be used before `onReady` is triggered, as it may lead to outdated data or errors.

### Functionality Overview

#### Opening the Consent Dialog

The SDK provides the `openDialog()` method to display the consent screen.

```kotlin
ClickioConsentSDK.getInstance().openDialog(
    context: Context, 
    mode: ClickioConsentSDK.DialogMode = ClickioConsentSDK.DialogMode.DEFAULT
)
```

##### Parameters:

-   **`context`** – Requires an `Activity` or `Appilcation` context.
-   **`mode`** – Defines when the dialog should be shown. Possible values:
    -   `DialogMode.DEFAULT` – Opens the dialog if GDPR applies and user hasn't given consent.
    -   `DialogMode.RESURFACE` – Always forces dialog to open, regardless of the user’s jurisdiction, allowing users to modify settings for GDPR compliance or to opt out under US regulations.

----------
#### Consent Update Callback

The SDK provides an `onConsentUpdated` callback that is triggered whenever consent is updated:

```kotlin
ClickioConsentSDK.getInstance().onConsentUpdated { 
    // Handle consent update logic
}
```
----------
### Logging

To enable logging, use the following method:

```kotlin
ClickioConsentSDK.getInstance().setLogsMode(mode: LogsMode)
```
-   **`mode`** – Defines whether logging is enabled or not:
    -   `LogsMode.DISABLED` – Disables logging, default value
    -   `LogsMode.VERBOSE` – Enables logging

   ----------


### Checking Consent Scope

```kotlin
ClickioConsentSDK.getInstance().checkConsentScope(): String?
```

Returns the applicable consent scope as String.

#### Returns:
-   **"gdpr"** – The user is subject to GDPR requirements.
-   **"us"** – The user is subject to US requirements.
-   **"out of scope"** – The user is not subject to GDPR/US, other cases.

----------

### Checking Consent State

 ```kotlin
ClickioConsentSDK.getInstance().checkConsentState(): ConsentState?
 ```

Determines the consent state based on the scope and force flag and returns ConsentState.

#### Returns:

-   **`ConsentState.NOT_APPLICABLE`** – The user is not subject to GDPR/US.
-   **`ConsentState.GDPR_NO_DECISION`** – The user is subject to GDPR but has not made a decision.
-   **`ConsentState.GDPR_DECISION_OBTAINED`** – The user is subject to GDPR and has made a decision.
-   **`ConsentState.US`** – The user is subject to US regulations.

----------

### Checking Consent for a Purpose

 ```kotlin
ClickioConsentSDK.getInstance().checkConsentForPurpose(purposeId: Int): Boolean?
 ```

Verifies whether consent for a specific [TCF purpose](https://iabeurope.eu/iab-europe-transparency-consent-framework-policies/#headline-24-18959) has been granted by using `IABTCF_PurposeConsents` string.


----------

### Checking Consent for a Vendor

 ```kotlin
ClickioConsentSDK.getInstance().checkConsentForVendor(vendorId: Int): Boolean?
```

Verifies whether consent for a specific [TCF vendor](https://iabeurope.eu/vendor-list-tcf/) has been granted by using `IABTCF_VendorConsents` string.

----------

## ExportData

`ExportData` is a class designed to retrieve consent values from `SharedPreferences`. It provides methods to obtain various types of consent, including TCF, Google Consent Mode, and others.

*Example of use*
 ```kotlin
val exportData = ExportData(context)
val valueOfTCString = exportData.getTCString()
val listOfconsentedTCFPurposes = exportData.getConsentedTCFPurposes()
```

### Constructor
```kotlin
ExportData(context: Context)
```
##### Parameters:

-   **`context`**  – Requires an  `Activity`  or  `Appilcation`  context.

----------

### Methods

### `getTCString`

```kotlin
fun getTCString(): String?
```

Returns the IAB TCF v2.2 string if it exists.

----------

### `getACString`

```kotlin
fun getACString(): String?
```

Returns the Google additional consent string if it exists.

----------


### `getGPPString`

```kotlin
fun getGPPString(): String?
```

Returns the Global Privacy Platform (GPP) string if it exists.

----------

### `getConsentedTCFVendors`

```kotlin
fun getConsentedTCFVendors(): List<Int>?
```

Returns the IDs of TCF vendors that have given consent.

----------

### `getConsentedTCFLiVendors`

```kotlin
fun getConsentedTCFLiVendors(): List<Int>?
```

Returns the IDs of TCF vendors that have given consent for legitimate interests.

----------

### `getConsentedTCFPurposes`

```kotlin
fun getConsentedTCFPurposes(): List<Int>?
```

Returns the IDs of TCF purposes that have given consent.

----------

### `getConsentedTCFLiPurposes`

```kotlin
fun getConsentedTCFLiPurposes(): List<Int>?
```

Returns the IDs of TCF purposes that have given consent as Legitimate Interest.

----------

### `getConsentedGoogleVendors`

```kotlin
fun getConsentedGoogleVendors(): List<Int>?
```

Returns the IDs of Google vendors that have given consent.

----------

### `getConsentedOtherVendors`

```kotlin
fun getConsentedOtherVendors(): List<Int>?
```

Returns the IDs of non-TCF vendors that have given consent.

----------

### `getConsentedOtherLiVendors`

```kotlin
fun getConsentedOtherLiVendors(): List<Int>?
```

Returns the IDs of non-TCF vendors that have given consent for legitimate interests.

----------

### `getConsentedNonTcfPurposes`

```kotlin
fun getConsentedNonTcfPurposes(): List<Int>?
```

Returns the IDs of non-TCF purposes (simplified purposes) that have given consent.

----------

### `getGoogleConsentMode`

```kotlin
fun getGoogleConsentMode(): GoogleConsentStatus?
```

Returns Google Consent Mode v2 flags wrapped into `GoogleConsentStatus` class if Google Consent Mode enabled, otherwise will return `null`.

```kotlin
data class GoogleConsentStatus(
    val analyticsStorageGranted: Boolean?,
    val adStorageGranted: Boolean?,
    val adUserDataGranted: Boolean?,
    val adPersonalizationGranted: Boolean?
)
```
Represents the status of Google Consent Mode.

-   `analyticsStorageGranted` — Consent for analytics storage.
-   `adStorageGranted` — Consent for ad storage.
-   `adUserDataGranted` — Consent for processing user data for ads.
-   `adPersonalizationGranted` — Consent for ad personalization.


# Integration with Third-Party Libraries for Google Consent Mode

Clickio Consent SDK supports automatic integration with external analytics and advertising platforms for Google Consent Mode V2 if enabled:

-   [Firebase Analytics](https://firebase.google.com/docs/analytics)
-   [Adjust](https://www.adjust.com/)
-   [Airbridge](https://www.airbridge.io/)
-   [AppsFlyer](https://www.appsflyer.com/)

### Firebase Analytics
If the Firebase Analytics SDK is present in the project, the Clickio SDK will automatically send Google Consent flags to Firebase if *Clickio Google Consent Mode* integration **enabled**.

ClickioConsentSDK transmits consent flags immediately if they were updated after showing the consent dialog (when `onConsentUpdated` is called) or during initialization if the consent has been accepted.

Also you might need to set default consent values in the app's `AndroidManifest.xml` as described [here](https://developers.google.com/tag-platform/security/guides/app-consent?consentmode=advanced&platform=android#default-consent).

After successfully transmitting the flags, a log message will be displayed (if logging is enabled) confirming the successful transmission. In case of an error, an error message will appear in the logs. You may need to update Firebase Analytics to a newer version in your project.

----------

### Adjust,  Airbridge,  AppsFlyer

If any of these SDKs (**Adjust, Airbridge, AppsFlyer**) are present in the project, `ClickioConsentSDK` will automatically send Google Consent flags to them if *Clickio Google Consent Mode* integration **enabled**.

However, interactions with `ClickioConsentSDK` should be performed after initializing the SDK since `ClickioConsentSDK` only transmits consent flags, while the initialization and configuration of the libraries are the responsibility of the app developer.

After successfully transmitting the flags, a log message will be displayed (if logging is enabled) to confirm the successful transmission. In case of an error, an error message will appear in the logs. You may need to update the SDK you are using (Adjust, Airbridge, or AppsFlyer) to a newer version in your project.

## Integration with other libraries

For other libraries, you can use the `getGoogleConsentMode` method from the `ExportData` class to retrieve the `GoogleConsentStatus`.

For example, you can subscribe to the `onConsentUpdated` callback and call `getGoogleConsentMode` within it.

```kotlin
val exportData = ExportData(context)
ClickioConsentSDK.getInstance().onConsentUpdated { 
	val googleConsentFlags = exportData.getGoogleConsentMode()
	if (googleConsentFlags != null){
		// Send values to other SDK
	}
}
```
If you need to send consent data on each subsequent app launch, it is recommended to wait for the `onReady` callback and then call `getGoogleConsentMode`.

**Keep in mind:** `getGoogleConsentMode` can return `null`  if Google Consent Mode is disabled or unavailable.

# Integration with Third-Party libraries when Google Consent Mode is disabled
If _Clickio Google Consent Mode_ integration is **disabled** you can set consent flags manually.

*Firebase Analytics example:*
```kotlin
with(ClickioConsentSDK.getInstance()){ 
	onConsentUpdated {  
		val purpose1 = checkConsentForPurpose(1)  
		val purpose3 = checkConsentForPurpose(3)  
		val purpose4 = checkConsentForPurpose(4)  
		val purpose7 = checkConsentForPurpose(7)  
		val purpose8 = checkConsentForPurpose(8)  
		val purpose9 = checkConsentForPurpose(9)  
		
		val adStorage =  if  (purpose1) ConsentStatus.GRANTED else ConsentStatus.DENIED 
		val adUserData =  if  (purpose1 && purpose7) ConsentStatus.GRANTED else ConsentStatus.DENIED 
		val adPersonalization =  if  (purpose3 && purpose4) ConsentStatus.GRANTED else ConsentStatus.DENIED 
		val analyticsStorage =  if  (purpose8 && purpose9) ConsentStatus.GRANTED else ConsentStatus.DENIED 
		
		val consentSettings =  mapOf(
			ConsentType.AD_STORAGE to adStorage, 
			ConsentType.AD_USER_DATA to adUserData, 
			ConsentType.AD_PERSONALIZATION to adPersonalization, 
			ConsentType.ANALYTICS_STORAGE to analyticsStorage 
		) 
		Firebase.analytics.setConsent(consentSettings)  
	}  
}
```
[More about Consent Mode flags mapping with TCF and non-TCF purposes](https://docs.clickio.com/books/clickio-consent-cmp/page/google-consent-mode-v2-implementation#bkmrk-5.1.-tcf-mode)