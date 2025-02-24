package com.clickio.clickioconsentsdk

import android.content.Context
import android.util.Log

class ClickioConsentSDK private constructor() {

    companion object {

        private const val CLICKIO_SDK_TAG = "CLICKIO_SDK"

        private const val SCOPE_GDPR = "gdpr"
        private const val SCOPE_US = "us"
        private const val SCOPE_OUT_OF_SCOPE = "out_of_scope"

        private var instance: ClickioConsentSDK? = null

        /**
         * Singleton instance retrieval static method
         */
        fun getInstance(): ClickioConsentSDK {
            if (instance == null) {
                instance = ClickioConsentSDK()
            }
            return instance as ClickioConsentSDK
        }
    }

    private var config: Config? = null
    private var logger: EventLogger = EventLogger()
    private var onConsentUpdatedListener: (() -> Unit)? = null
    private var onReadyListener: (() -> Unit)? = null
    private var consentStatus: ConsentStatus? = null

    data class Config(
        val siteId: String,
        val appLanguage: String? = null,
    )

    enum class ConsentState {
        NOT_APPLICABLE,
        GDPR_NO_DECISION,
        GDPR_DECISION_OBTAINED,
        US
    }

    enum class DialogMode {
        DEFAULT,
        RESURFACE
    }

    /**
     *  Logging class
     */
    class EventLogger {

        enum class Mode {
            DISABLED,
            VERBOSE
        }

        enum class EventLevel {
            ERROR,
            DEBUG,
            INFO
        }

        private var mode: Mode = Mode.DISABLED

        fun setMode(mode: Mode) {
            this.mode = mode
        }

        fun log(event: String, level: EventLevel) {
            when (level) {
                EventLevel.ERROR -> Log.e(CLICKIO_SDK_TAG, event)
                EventLevel.DEBUG -> Log.d(CLICKIO_SDK_TAG, event)
                EventLevel.INFO -> Log.i(CLICKIO_SDK_TAG, event)
            }
        }
    }

    /**
     * Class for Exporting Data from Prefs
     */
    class ExportData(val context: Context) {

        /**
         * Description from client's documentation:
         * Return IAB TCF v2.2 string if exists
         */
        fun getTCString(): String {
            TODO()
        }

        /**
         * Description from client's documentation:
         * Return the Google additional consent ID if exists
         */
        fun getACString(): String {
            TODO()
        }

        /**
         * Description from client's documentation:
         * Return Global Privacy Platform String if exists
         */
        fun getGPPString(): String {
            TODO()
        }

        /**
         * Description from client's documentation:
         *  Return Google Consent Mode v2 flags
         */
        fun getGoogleConsentModeV2(): String {
            TODO()
        }

        /**
         * Description from client's documentation:
         * Return id's of TCF Vendors that given consent
         */
        fun getConsentedTCFVendors(): List<Int> {
            TODO()
        }

        /**
         * Description from client's documentation:
         * Return id's of TCF Vendors that given consent for legitimate interests
         */
        fun getConsentedTCFLiVendors(): List<Int> {
            TODO()
        }

        /**
         * Description from client's documentation:
         * Return id's of TCF purposes that given consent
         */
        fun getConsentedTCFPurposes(): List<Int> {
            TODO()
        }

        /**
         * Description from client's documentation:
         * Return id's of TCF purposes that given consent as Legitimate Interest
         */
        fun getConsentedTCFLiPurposes(): List<Int> {
            TODO()
        }

        /**
         * Description from client's documentation:
         * Return id's of Google Vendors that given consent
         */
        fun getConsentedGoogleVendors(): List<Int> {
            TODO()
        }

        /**
         * Description from client's documentation:
         * Return id's of non-TCF Vendors that given consent
         */
        fun getConsentedOtherVendors(): List<Int> {
            TODO()
        }

        /**
         * Description from client's documentation:
         * Return id's of non-TCF Vendors that given consent for legitimate interests
         */
        fun getConsentedOtherLiVendors(): List<Int> {
            TODO()
        }

        /**
         * Description from client's documentation:
         * Return id's of non-TCF purposes (simplified purposes) that given consent
         */
        fun getConsentedNonTcfPurposes(): List<Int> {
            TODO()
        }
    }

    /**
     * Data class for sdk/consent-status response
     */
    data class ConsentStatus(
        val scope: String,
        val force: Boolean,
        val error: String,
    )

    // Common methods

    /**
     *  Init of SDK
     */
    fun initialize(config: Config) {
        this.config = config
        consentStatus = fetchConsentStatus()
        onReadyListener?.invoke()
    }

    fun setLogsMode(mode: EventLogger.Mode) {
        logger.setMode(mode)
    }

    fun onReady(listener: () -> Unit) {
        this.onReadyListener = listener
    }

    fun onConsentUpdated(listener: (() -> Unit)?) {
        this.onConsentUpdatedListener = listener
    }

    /**
     * Description from client's documentation:
     * Return the scope that applies to the user (return the sdk/consent-status scope output).
     */
    fun checkConsentScope(): String? {
        if (consentStatus?.scope == null) {
            logger.log(
                "Consent status is not loaded, possible reason:${consentStatus?.error}",
                EventLogger.EventLevel.ERROR
            )
        }
        return consentStatus?.scope
    }


    /**
     * Description from client's documentation:
     * Return:
     * not_applicable (if scope = ‘out of scope’)
     * gdpr_no_decision - scope = gdpr and force = true and force state is not changed during app session
     * gdpr_decision_obtained - scope = gdpr and force = false
     * us - scope = us
     */
    fun checkConsentState(): ConsentState? {
        if (consentStatus?.scope == null) {
            logger.log(
                "Consent status is not loaded, possible reason:${consentStatus?.error}",
                EventLogger.EventLevel.ERROR
            )
        }
        if (consentStatus?.scope == SCOPE_OUT_OF_SCOPE) return ConsentState.NOT_APPLICABLE
        if (consentStatus?.scope == SCOPE_GDPR && consentStatus?.force == true) return ConsentState.GDPR_NO_DECISION
        if (consentStatus?.scope == SCOPE_GDPR && consentStatus?.force == false) return ConsentState.GDPR_DECISION_OBTAINED
        if (consentStatus?.scope == SCOPE_US) return ConsentState.US
        return null
    }

    /**
     * Description from client's documentation:
     * Verifies whether consent for a specific purpose has been granted.
     */
    fun checkConsentForPurpose(purposeId: String): Boolean {
        TODO()
    }

    /**
     * Verifies whether consent for a specific vendor has been granted.
     */
    fun checkConsentForVendor(vendorId: String): Boolean {
        TODO()
    }


    // WebView Screen Manipulations
    /**
     * Description from client's documentation:
     * Argument “mode”:
     * Resurface - check if user in consent scope (scope != out of scope) and open the dialog
     * Default - if scope = gdpr and force = true then open the dialog
     * Argument “language” (optional) - force UI language
     */
    fun openDialog(
        context: Context, // Android specific
        mode: DialogMode = DialogMode.DEFAULT,
        language: String? = null
    ) {
        when (mode) {
            DialogMode.DEFAULT -> {
                if (consentStatus?.scope == SCOPE_GDPR && consentStatus?.force == true) openWebViewActivity()
            }

            DialogMode.RESURFACE -> {
                if (consentStatus?.scope != SCOPE_OUT_OF_SCOPE) openWebViewActivity()
            }
        }
    }

    /**
     * Description from client's documentation:
     * Clears all consent data, effectively simulating the complete removal of the app from the device.
     */
    private fun clearData() {
        // Wiping of SharedPreferences
        // Available only for SDK developers for debugging
    }

    /**
     * Private method to fetch the current ConsentStatus
     */
    private fun fetchConsentStatus(): ConsentStatus {
        // Calling GET https://clickiocdn.com/sdk/consent-status?s={config.clientId}&v={sharedPreferences.CLICKIO_CONSENT_server_request}
        // Getting Scope(GDPR, US, OUT_OF_SCOPE), force (True/False), error - parsing to ConsentStatus model
        return ConsentStatus()
    }

    /**
     *  Private method to open Screen with WebView
     */
    private fun openWebViewActivity() {
        // Launch a transparent activity with WebView to which we pass config, onConsentUpdated callback, logger
        // In this Activity we do not forget to read/write/ready realization
    }
}