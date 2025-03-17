package com.clickio.clickioconsentsdk

import android.content.Context
import androidx.preference.PreferenceManager

private const val GRANTED = "granted"

/**
 * Class for exporting data from shared preferences.
 * @param context Application context used to access shared preferences.
 */
class ExportData(context: Context) {

    private val sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    /**
     * Returns the IAB TCF v2.2 string if it exists.
     * @return TCF string or `null` if not found.
     */
    fun getTCString(): String? =
        sharedPreferences.getString("IABTCF_TCString", null)

    /**
     * Returns the Google additional consent ID if it exists.
     * @return Additional consent string or `null` if not found.
     */
    fun getACString(): String? =
        sharedPreferences.getString("IABTCF_AddtlConsent", null)

    /**
     * Returns the Global Privacy Platform (GPP) string if it exists.
     * @return GPP string or `null` if not found.
     */
    fun getGPPString(): String? =
        sharedPreferences.getString("IABGPP_HDR_GppString", null)

    /**
     * Returns Google Consent Mode v2 flags.
     * @return [GoogleConsentStatus] object containing consent flags, or `null` if no data is found.
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

        if (adStorageString.isNullOrEmpty() && analyticsStorageString.isNullOrEmpty() && adUserDataString.isNullOrEmpty() && adPersonalizationString.isNullOrEmpty()) return null

        return GoogleConsentStatus(
            adStorageGranted = adStorageString == GRANTED,
            analyticsStorageGranted = analyticsStorageString == GRANTED,
            adUserDataGranted = adUserDataString == GRANTED,
            adPersonalizationGranted = adPersonalizationString == GRANTED
        )
    }

    /**
     * Returns IDs of TCF Vendors that have given consent.
     * @return List of vendor IDs or `null` if not available.
     */
    fun getConsentedTCFVendors(): List<Int>? =
        parseBinaryString(sharedPreferences.getString("IABTCF_VendorConsents", null))

    /**
     * Returns IDs of TCF Vendors that have given consent for legitimate interests.
     * @return List of vendor IDs or `null` if not available.
     */
    fun getConsentedTCFLiVendors(): List<Int>? =
        parseBinaryString(sharedPreferences.getString("IABTCF_VendorLegitimateInterests", null))

    /**
     * Returns IDs of TCF purposes that have given consent.
     * @return List of purpose IDs or `null` if not available.
     */
    fun getConsentedTCFPurposes(): List<Int>? =
        parseBinaryString(sharedPreferences.getString("IABTCF_PurposeConsents", null))

    /**
     * Returns IDs of TCF purposes that have given consent as Legitimate Interest.
     *
     * @return List of purpose IDs or `null` if not available.
     */
    fun getConsentedTCFLiPurposes(): List<Int>? =
        parseBinaryString(sharedPreferences.getString("IABTCF_PurposeLegitimateInterests", null))

    /**
     * Returns IDs of Google Vendors that have given consent.
     * @return List of Google Vendor IDs or `null` if not available.
     */
    fun getConsentedGoogleVendors(): List<Int>? {
        val consentString = sharedPreferences.getString("IABTCF_AddtlConsent", null) ?: return null
        val parts = consentString.split("~")
        return if (parts.size > 1) parts[1].split(".").mapNotNull { it.toIntOrNull() } else null
    }

    /**
     * Returns IDs of non-TCF Vendors that have given consent.
     * @return List of vendor IDs or `null` if not available.
     */
    fun getConsentedOtherVendors(): List<Int>? =
        sharedPreferences.getString("CLICKIO_CONSENT_other_vendors_consent", null)
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }

    /**
     * Returns IDs of non-TCF Vendors that have given consent for legitimate interests.
     * @return List of vendor IDs or `null` if not available.
     */
    fun getConsentedOtherLiVendors(): List<Int>? =
        sharedPreferences.getString("CLICKIO_CONSENT_other_vendors_leg_int", null)
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }

    /**
     * Returns IDs of non-TCF purposes (simplified purposes) that have given consent.
     * @return List of purpose IDs or `null` if not available.
     */
    fun getConsentedNonTcfPurposes(): List<Int>? =
        sharedPreferences.getString("CLICKIO_CONSENT_other_purposes_consent", null)
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }

    /**
     * Parses a binary string representing consented IDs.
     * @param binaryString A binary string where '1' indicates consent for the corresponding index.
     * @return List of integers representing consented IDs or `null` if input is empty.
     */
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