# Clickio Consent SDK for Android

This document provides information on integrating the Clickio Consent SDK into your Android application.

## Requirements

Before integrating the SDK, ensure that your application meets the following requirements:

- **Minimum SDK Version:** 26 (Android 8.0)
- **Target/Compile SDK Version:** The minimum required for Google Play compliance.
- **Internet Permission:** The SDK requires internet access, so include the following permission in your `AndroidManifest.xml`:
  
  ```xml
  <uses-permission android:name="android.permission.INTERNET" />
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
  ```
- **Maven Central Repository:** Ensure that `mavenCentral()` is included in your project's `build.gradle` file:
  
  ```gradle
  repositories {
      mavenCentral()
  }
  ```

## Installation

To integrate the SDK, add the following dependency to your project's `build.gradle` file:

```gradle
implementation("com.clickio:clickioconsentsdk:0.0.2")
```

## Initialization

To initialize the SDK, use the `initialize` method:

```kotlin
fun initialize(context: Context, config: Config)
```

### Configuration File

The SDK requires a configuration object with the following parameters:

```kotlin
data class Config(
    val siteId: String, // Clickio Site ID
    val appLanguage: String? = null // Optional: App language
)
```

- **`siteId`**: The Clickio Site ID. You can find it [here](https://docs.clickio.com/books/clickio-consent-cmp/page/google-consent-mode-v2-implementation#bkmrk-access-the-template%3A).
- **`appLanguage`**: (Optional) The language for the consent UI.

### Implementation Example

Wrap initialization in a try-catch block to handle any unexpected errors:

```kotlin
try {
    ClickioConsentSDK.getInstance()
        .initialize(
            context = this, // Use Activity context, not Application context
            config = ClickioConsentSDK.Config("241131")
        )
} catch (e: Exception) {
    e.printStackTrace()
}
```

## Handling SDK Readiness

After initialization, use the `onReady` callback to execute actions once the SDK is fully loaded. For example, you can open the consent dialog:

```kotlin
ClickioConsentSDK.getInstance().onReady {  
    ClickioConsentSDK.getInstance()
        .openDialog(
            context = this, // Use Activity context, not Application context
            mode = ClickioConsentSDK.DialogMode.RESURFACE
        )
}
```

Additionally, you can check the consent scope:

```kotlin
val scope = ClickioConsentSDK.getInstance().checkConsentScope()
```

## Quick Start

Here's the minimal implementation to get started:

```kotlin
ClickioConsentSDK.getInstance()
    .initialize(
        context = this, // Activity context required
        config = ClickioConsentSDK.Config("241131")
    )

ClickioConsentSDK.getInstance().onReady {  
    ClickioConsentSDK.getInstance()
        .openDialog(
            context = this, // Activity context required
            mode = ClickioConsentSDK.DialogMode.RESURFACE
        )
}
```

**What happens next?**

- The consent dialog will open, allowing the user to provide their preferences.
- Once accepted, the relevant consent parameters will be saved in `SharedPreferences`.

## Functionality Overview

### Consent Update Callback

The SDK provides an `onConsentUpdated` callback that is triggered whenever consent is updated:

```kotlin
ClickioConsentSDK.getInstance().onConsentUpdated { 
    // Handle consent update logic
}
```

### Opening the Consent Dialog

The SDK provides the `openDialog()` method to display the consent screen.

#### Method Signature:

```kotlin
fun openDialog(
    context: Context, 
    mode: ClickioConsentSDK.DialogMode = ClickioConsentSDK.DialogMode.DEFAULT
)
```

#### Parameters:

- **`context`** – Requires an `Activity` context (do not use `Application` context).
- **`mode`** – Defines when the dialog should be shown. Possible values:
  - `DialogMode.DEFAULT` – Opens the dialog if GDPR applies and user hasn't given consent.
  - `DialogMode.RESURFACE` – Opens the dialog if the user is within consent scope (not out of scope).

#### Example Usage:

```kotlin
ClickioConsentSDK.getInstance().onReady {  
    ClickioConsentSDK.getInstance().openDialog(
        context = this, // Must be an Activity context
        mode = ClickioConsentSDK.DialogMode.RESURFACE
    )
}
```

#### Behavior:

- The dialog will open, allowing users to provide or update their consent preferences.
- After submission, consent data is stored in `SharedPreferences`.
- The `onConsentUpdated` callback is triggered if any changes occur.

### Consent Checking Methods

The SDK offers several methods to retrieve consent-related data:

- **`checkConsentScope()`** – Returns the current consent scope (GDPR, US, or out of scope).
- **`checkConsentState()`** – Returns the current consent state:
  - `NOT_APPLICABLE`: If consent scope is out of scope.
  - `GDPR_NO_DECISION`: If GDPR applies, but the user has not made a decision.
  - `GDPR_DECISION_OBTAINED`: If GDPR applies and a decision has been made.
  - `US`: If the consent scope is US.
- **`checkConsentForPurpose(purposeId: Int)`** – Checks if consent is granted for a specific purpose.
- **`checkConsentForVendor(vendorId: Int)`** – Checks if consent is granted for a specific vendor.
- **`getTCString()`** – Returns the IAB TCF v2.2 consent string if available.
- **`getACString()`** – Returns the Google Additional Consent string if available.
- **`getGPPString()`** – Returns the Global Privacy Platform string if available.
- **`getGoogleConsentMode()`** – Returns the Google Consent Mode v2 status.
- **`getConsentedTCFVendors()`** – Returns a list of TCF vendors that have received consent.
- **`getConsentedTCFLiVendors()`** – Returns a list of TCF vendors with legitimate interest consent.
- **`getConsentedTCFPurposes()`** – Returns a list of TCF purposes that have been consented to.
- **`getConsentedTCFLiPurposes()`** – Returns a list of TCF purposes with legitimate interest consent.
- **`getConsentedGoogleVendors()`** – Returns a list of Google vendors that have received consent.
- **`getConsentedOtherVendors()`** – Returns a list of non-TCF vendors with consent.
- **`getConsentedOtherLiVendors()`** – Returns a list of non-TCF vendors with legitimate interest consent.
- **`getConsentedNonTcfPurposes()`** – Returns a list of non-TCF purposes that have received consent.

For more details, refer to the [Clickio documentation](https://docs.clickio.com/).

