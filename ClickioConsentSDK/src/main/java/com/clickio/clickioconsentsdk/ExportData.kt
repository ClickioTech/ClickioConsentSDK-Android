package com.clickio.clickioconsentsdk

import android.content.Context
import androidx.preference.PreferenceManager

private const val GRANTED = "granted"

/**
 * Class for Exporting Data from Prefs
 */
class ExportData(context: Context) {

    private val sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    /**
     * Return IAB TCF v2.2 string if exists
     */
    fun getTCString(): String? =
        sharedPreferences.getString("IABTCF_TCString", null)

    /**
     * Return the Google additional consent ID if exists
     */
    fun getACString(): String? =
        sharedPreferences.getString("IABTCF_AddtlConsent", null)

    /**
     * Return Global Privacy Platform String if exists
     */
    fun getGPPString(): String? =
        sharedPreferences.getString("IABGPP_HDR_GppString", null)

    /**
     *  Description from client's documentation:
     *  Return Google Consent Mode v2 flags
     */
    fun getGoogleConsentMode(): GoogleConsentStatus? {
        val adStorageString =
            sharedPreferences.getString("CLICKIO_CONSENT_GOOGLE_ANALYTICS_adStorage", null)
        val analyticsStorageString =
            sharedPreferences.getString("CLICKIO_CONSENT_GOOGLE_ANALYTICS_analyticsStorage", null)
        val adUserDataString =
            sharedPreferences.getString("CLICKIO_CONSENT_GOOGLE_ANALYTICS_adUserData", null)
        val adPersonalizationString =
            sharedPreferences.getString("CLICKIO_CONSENT_GOOGLE_ANALYTICS_adPersonalization", null)

        if (adStorageString == null && analyticsStorageString == null && adUserDataString == null && adPersonalizationString == null) return null

        return GoogleConsentStatus(
            adStorageGranted = adStorageString == GRANTED,
            analyticsStorageGranted = analyticsStorageString == GRANTED,
            adUserDataGranted = adUserDataString == GRANTED,
            adPersonalizationGranted = adPersonalizationString == GRANTED
        )
    }

    /**
     * Return id's of TCF Vendors that given consent
     */
    fun getConsentedTCFVendors(): List<Int>? =
        parseBinaryString(sharedPreferences.getString("IABTCF_VendorConsents", null))


    /**
     * Return id's of TCF Vendors that given consent for legitimate interests
     */
    fun getConsentedTCFLiVendors(): List<Int>? =
        parseBinaryString(sharedPreferences.getString("IABTCF_VendorLegitimateInterests", null))

    /**
     * Return id's of TCF purposes that given consent
     */
    fun getConsentedTCFPurposes(): List<Int>? =
        parseBinaryString(sharedPreferences.getString("IABTCF_PurposeConsents", null))

    /**
     * Return id's of TCF purposes that given consent as Legitimate Interest
     */
    fun getConsentedTCFLiPurposes(): List<Int>? =
        parseBinaryString(sharedPreferences.getString("IABTCF_PurposeLegitimateInterests", null))

    fun getConsentedGoogleVendors(): List<Int>? {
        val consentString = sharedPreferences.getString("IABTCF_AddtlConsent", null) ?: return null
        val parts = consentString.split("~")
        return if (parts.size > 1) parts[1].split(".").mapNotNull { it.toIntOrNull() } else null
    }

    /**
     * Return id's of non-TCF Vendors that given consent
     */
    fun getConsentedOtherVendors(): List<Int>? =
        sharedPreferences.getString(
            "CLICKIO_CONSENT_other_vendors_consent",
            null
        )?.split(",")?.mapNotNull { it.toIntOrNull() }

    /**
     * Return id's of non-TCF Vendors that given consent for legitimate interests
     */
    fun getConsentedOtherLiVendors(): List<Int>? =
        sharedPreferences.getString(
            "CLICKIO_CONSENT_other_vendors_leg_int",
            null
        )?.split(",")?.mapNotNull { it.toIntOrNull() }

    /**
     * Return id's of non-TCF purposes (simplified purposes) that given consent
     */
    fun getConsentedNonTcfPurposes(): List<Int>? =
        sharedPreferences.getString(
            "CLICKIO_CONSENT_other_purposes_consent",
            null
        )?.split(",")?.mapNotNull { it.toIntOrNull() }


    private fun parseBinaryString(binaryString: String?): List<Int>? {
        if (binaryString.isNullOrEmpty()) return null
        return binaryString.mapIndexedNotNull { index, char ->
            if (char == '1') index + 1 else null
        }
    }
}

data class GoogleConsentStatus(
    val analyticsStorageGranted: Boolean?,
    val adStorageGranted: Boolean?,
    val adUserDataGranted: Boolean?,
    val adPersonalizationGranted: Boolean?
)