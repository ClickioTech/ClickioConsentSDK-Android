package com.clickio.clickioconsentsdk

import android.content.Context
import android.util.Log

class ClickioConsentSDK private constructor() {

    companion object {

        private const val CLICKIO_SDK_TAG = "CLICKIO_SDK"

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


    enum class Scope {
        GDPR,
        US,
        OUT_OF_SCOPE
    }

    enum class ConsentState {
        CONSENT_NOT_APPLICABLE,
        EU_USER_NOT_DECIDED_YET,
        ER_USER_CONSENTED
    }

    enum class ConsentChoiceMade {
        AGREE_ALL,
        REJECT_ALL,
        PARTIAL_AGREE
    }

    enum class DialogMode {
        DEFAULT,
        RESURFACE
    }

    data class Config(
        val clientId: String,
        val appLanguage: String? = null,
    )

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

    private data class ConsentStatus(
        val scope: Scope,
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
     * Return the scope that applies to the user (may return GDPR or US or False).
     */
    fun checkConsentScopeApplies() =
        consentStatus?.scope //(Unless, of course, it is not important that it returns False instead of OUT_OF_SCOPE))

    /**
     * Description from client's documentation:
     * Return:
     * consent not applicable (non-eu or US user)
     * eu user, user has not decided yet
     * eu user, user consented (does not mean, that user consented to everything)
     */
    fun checkConsentState(): ConsentState {
        // TODO: Declare whether force = user has not decided yet
        return when (consentStatus.scope) {
            Scope.GDPR -> if (consentStatus.force) ConsentState.ER_USER_CONSENTED else ConsentState.EU_USER_NOT_DECIDED_YET
            Scope.US -> return ConsentState.CONSENT_NOT_APPLICABLE
            Scope.OUT_OF_SCOPE -> return ConsentState.CONSENT_NOT_APPLICABLE
        }
    }

    /**
     * Description from client's documentation:
     * Checks if consent choice is required from the user
     * Return true if the user in the GDPR consent scope and no previous decision
     */
    fun checkConsentRequired(): Boolean {
        // TODO: Declare whether force = user has not decided yet
        return consentStatus == Scope.GDPR && consentStatus.force
    }

    /**
     * Description from client's documentation:
     * Determines whether the user has made a consent choice (agree all, reject all or partial agree).
     */
    fun checkIfConsentChoiceMade(): ConsentChoiceMade {
        TODO("Determine whether how declare Agree All from Partial Agree")
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
     * Resurface - check if user in consent scope (not “out of scope”) and open the dialog
     * Default - Checks if user's consent (GDPR) is required (user is in scope, no previous decision or previous decision is out of date) and open the dialog
     * Argument “language” (optional) - force UI language
     */
    fun openDialog(
        context: Context,
        mode: DialogMode = DialogMode.DEFAULT,
        language: String? = null
    ) {
        when (mode) {
            DialogMode.DEFAULT -> {
                // TODO: Declare whether force = no previous decision and how to check out of date
                if (consentStatus?.scope == Scope.GDPR && consentStatus?.force == true) openWebViewActivity()
            }

            DialogMode.RESURFACE -> {
                if (consentStatus?.scope == Scope.GDPR) openWebViewActivity()
            }
        }
    }

    /**
     * Description from client's documentation:
     * Clears all consent data, effectively simulating the complete removal of the app from the device.
     */
    fun clearData() {
        // Wiping of SharedPreferences
    }

    /**
     * Private method to fetch the current ConsentStatus
     */
    private fun fetchConsentStatus(): ConsentStatus {
        if (config == null) {
            logger.log("Missed configuration", EventLogger.EventLevel.ERROR)
            return
        }
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