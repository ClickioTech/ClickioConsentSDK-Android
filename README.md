# Clickio Consent SDK for Android

## Requirements

Before integrating the SDK, ensure that your application meets the following requirements:

-   **Minimum SDK Version:** `21` (Android 5.0)

-   **Target/Compile SDK Version:** The minimum required for Google Play compliance.

-   **Internet Permission:** The SDK requires internet access, so include the following permission in your `AndroidManifest.xml`:

    ```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
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
implementation("com.clickio:clickioconsentsdk:1.0.0")
```

## Quick Start

Here's the minimal implementation to get started:

```kotlin
with(ClickioConsentSDK.getInstance()) {
    initialize(
        context = context,
        config = ClickioConsentSDK.Config("Clickio Site ID") 
    )
    onReady { openDialog(context) }
}

```
In this code after successful initialization, the SDK will open the Consent Window (a transparent `Activity` with a `WebView`).

## Setup and Usage

### Singleton Access

All interactions with the Clickio SDK should be done using the `ClickioConsentSDK.getInstance()` method to obtain the singleton instance of the SDK

### Initialization

To initialize the SDK, use the `initialize` method:

```kotlin
ClickioConsentSDK.getInstance().initialize(context: Context, config: Config)
```

#### Configuration Object

The SDK requires a configuration object with the following parameters:

```kotlin
data class Config(
    val siteId: String, // Clickio Site ID
    val appLanguage: String? = null // Optional: App language
)
```

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
  -   `DialogMode.RESURFACE` – Opens the dialog if GDPR applies.

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
  -   `LogsMode.DISABLED` – Default value
  -   `LogsMode.VERBOSE` – Enables logging

   ----------


### Checking Consent Scope

```kotlin
ClickioConsentSDK.getInstance().checkConsentScope(): String?
```

Returns the applicable consent scope as String.

#### Returns:
-   **"gdpr"** – The user is subject to GDPR requirements.
-   **"us"** – The user is subject to US (GPP National) requirements.
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

Checks if consent has been granted for a specific purpose.

----------

### Checking Consent for a Vendor

 ```kotlin
ClickioConsentSDK.getInstance().fun checkConsentForVendor(vendorId: Int): Boolean?
```

Checks if consent has been granted for a specific vendor.

----------

# ExportData

`ExportData` is a class designed to retrieve consent values from `SharedPreferences`. It provides methods to obtain various types of consent, including TCF, Google Consent Mode, and others.

## Constructor

```kotlin
ExportData(context: Context)
```


## Methods

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

Returns the Google additional consent ID if it exists.

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

Returns Google Consent Mode v2 flags.

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


# Integration with Third-Party Libraries

Clickio Consent SDK supports integration with external analytics and advertising platforms for Google Consent Mode V2:

-   [Firebase Analytics](https://firebase.google.com/docs/analytics)
-   [Adjust](https://www.adjust.com/)
-   [Airbridge](https://www.airbridge.io/)
-   [AppsFlyer](https://www.appsflyer.com/)

### Firebase
If Firebase Analytics is present in the project, you can set default consent values in the app's `AndroidManifest.xml` before displaying the consent window, as described [here](https://developers.google.com/tag-platform/security/guides/app-consent?consentmode=advanced&platform=android). ClickioConsentSDK will automatically send Google Consent flags to Firebase Analytics if applicable.

----------

### Adjust
If the Adjust SDK is present in the project, ClickioConsentSDK will automatically send Google Consent flags to Adjust if applicable.

----------

### Airbridge
If the Airbridge SDK is present in the project, ClickioConsentSDK will automatically send Google Consent flags to Airbridge if applicable.

----------

### AppsFlyer
If the AppsFlyer SDK is present in the project, ClickioConsentSDK will automatically send Google Consent flags to Airbridge if applicable.

----------
# Other libraries

For other libraries, you can use the `getGoogleConsentMode` method from the `ExportData` class to retrieve the `GoogleConsentStatus`, which includes the following fields:

```kotlin
val analyticsStorageGranted: Boolean?
val adStorageGranted: Boolean?
val adUserDataGranted: Boolean?
val adPersonalizationGranted: Boolean?

```

These values indicate whether the corresponding consent options have been granted (`true`) or denied (`false`).