package com.clickio.clickioconsentsdk

import android.content.Context
import android.preference.PreferenceManager
import org.json.JSONArray

/**
 * Class for Exporting Data from Prefs
 */
class ExportData(context: Context) {

    private val sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)//  TODO check about deprecation

    /**
     * Description from client's documentation:
     * Return IAB TCF v2.2 string if exists
     */
    fun getTCString(): String? =
        sharedPreferences.getString("IABTCF_TCString", null)

    /**
     * Description from client's documentation:
     * Return the Google additional consent ID if exists
     */
    fun getACString(): String? =
        sharedPreferences.getString("IABTCF_AddtlConsent", null)

    /**
     * Description from client's documentation:
     * Return Global Privacy Platform String if exists
     */
    fun getGPPString(): String? =
        sharedPreferences.getString("IABGPP_HDR_GppString", null)

    /**
     * Description from client's documentation:
     *  Return Google Consent Mode v2 flags
     */
    fun getGoogleConsentMode(): Map<String, String> {
        // TODO recheck implementation
        return sharedPreferences.all
            .filterKeys { it.startsWith("CLICKIO_CONSENT_GOOGLE_ANALYTICS_") }
            .mapValues { it.value.toString() }
    }

    fun getConsentedTCFVendors(): List<Int>? {
        // TODO recheck implementation
        return parseBinaryString(sharedPreferences.getString("IABTCF_VendorConsents", null))
    }

    fun getConsentedTCFLiVendors(): List<Int>? {
        return parseBinaryString(
            sharedPreferences.getString(
                "IABTCF_VendorLegitimateInterests",
                null
            )
        )
    }

    fun getConsentedTCFPurposes(): List<Int>? {
        // TODO recheck implementation
        return parseBinaryString(sharedPreferences.getString("IABTCF_PurposeConsents", null))
    }

    fun getConsentedTCFLiPurposes(): List<Int>? {
        // TODO recheck implementation
        return parseBinaryString(
            sharedPreferences.getString(
                "IABTCF_PurposeLegitimateInterests",
                null
            )
        )
    }

    fun getConsentedGoogleVendors(): List<Int>? {
        // TODO recheck implementation
        val consentString = sharedPreferences.getString("IABTCF_AddtlConsent", null) ?: return null
        val parts = consentString.split("~")
        return if (parts.size > 1) parts[1].split(".").mapNotNull { it.toIntOrNull() } else null
    }

    fun getConsentedOtherVendors(): List<Int>? {
        // TODO recheck implementation
        return parseJsonArray(
            sharedPreferences.getString(
                "CLICKIO_CONSENT_other_vendors_consent",
                null
            )
        )
    }

    fun getConsentedOtherLiVendors(): List<Int>? {
        // TODO recheck implementation
        return parseJsonArray(
            sharedPreferences.getString(
                "CLICKIO_CONSENT_other_vendors_leg_int",
                null
            )
        )
    }

    fun getConsentedNonTcfPurposes(): List<Int>? {
        // TODO recheck implementation
        return parseJsonArray(
            sharedPreferences.getString(
                "CLICKIO_CONSENT_other_purposes_consent",
                null
            )
        )
    }

    private fun parseBinaryString(binaryString: String?): List<Int>? {
        return binaryString?.mapIndexedNotNull { index, char ->
            if (char == '1') index + 1 else null
        }
    }

    private fun parseJsonArray(jsonString: String?): List<Int>? {
        return try {
            jsonString?.let { JSONArray(it) }?.let { jsonArray ->
                List(jsonArray.length()) { index -> jsonArray.getInt(index) }
            }
        } catch (e: Exception) {
            null
        }
    }
}