package com.clickio.clickioconsentsdk

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import co.ab180.airbridge.Airbridge
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustThirdPartySharing
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import io.branch.referral.Branch
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class ClickioConsentSDK private constructor() {

    companion object {

        private const val SCOPE_GDPR = "gdpr"
        private const val SCOPE_US = "us"
        private const val SCOPE_OUT_OF_SCOPE = "out_of_scope"
        private const val BASE_CONSENT_STATUS_URL = "https://clickiocdn.com/sdk/consent-status?"
        private const val VERSION_KEY = "CLICKIO_CONSENT_server_request"

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

    private var isReady: Boolean = false
    private var config: Config? = null
    private var logger: EventLogger = EventLogger()
    private var onConsentUpdatedListener: (() -> Unit)? = null
    private var onReadyListener: (() -> Unit)? = null
    private var consentStatus: ConsentStatus? = null
    private var exportData: ExportData? = null

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
     * Data class for sdk/consent-status response
     */
    data class ConsentStatus(
        val scope: String? = null,
        val force: Boolean? = null,
        val error: String? = null,
    )

    // Common methods

    /**
     *  Init of SDK
     */
    fun initialize(context: Context, config: Config) {
        logger.log("Initialization stared", EventLevel.INFO)
        this.config = config
        exportData = ExportData(context)
        fetchConsentStatus(context)
        setConsentsIfApplicable()
    }

    fun setLogsMode(mode: LogsMode) {
        logger.setMode(mode)
    }

    fun onReady(listener: () -> Unit) {
        this.onReadyListener = listener
        if (isReady) listener.invoke()
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
                "Consent status is not loaded, possible reason: ${consentStatus?.error}",
                EventLevel.ERROR
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
                "Consent status is not loaded, possible reason: ${consentStatus?.error}",
                EventLevel.ERROR
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
    fun checkConsentForPurpose(purposeId: Int): Boolean? =
        exportData?.getConsentedTCFPurposes()?.contains(purposeId)

    /**
     * Verifies whether consent for a specific vendor has been granted.
     */
    fun checkConsentForVendor(vendorId: Int): Boolean? =
        exportData?.getConsentedTCFVendors()?.contains(vendorId)


    // WebView Screen Manipulations
    /**
     * Description from client's documentation:
     * Argument “mode”:
     * Resurface - check if user in consent scope (scope != out of scope) and open the dialog
     * Default - if scope = gdpr and force = true then open the dialog
     * Argument “language” (optional) - force UI language
     */
    fun openDialog(
        context: Context,
        mode: DialogMode = DialogMode.DEFAULT
    ) {
        logger.log("openDialog called with mode $mode", level = EventLevel.INFO)
        if (consentStatus?.scope == null) {
            logger.log(
                "Consent status is not loaded, possible reason: ${consentStatus?.error}",
                EventLevel.ERROR
            )
            return
        }
        when (mode) {
            DialogMode.DEFAULT -> {
                if (consentStatus?.scope == SCOPE_GDPR && consentStatus?.force == true) openWebViewActivity(
                    context
                )
            }

            DialogMode.RESURFACE -> {
                if (consentStatus?.scope != SCOPE_OUT_OF_SCOPE) openWebViewActivity(context)
            }
        }
    }

    internal fun getConfig() = config

    internal fun getConsentUpdatedCallback() = onConsentUpdatedListener

    internal fun getLogger() = logger

    internal fun updateConsentStatus() {
        consentStatus = consentStatus?.copy(force = false)
        setConsentsIfApplicable()
    }

    private fun setConsentsIfApplicable() {
        if (!isGoogleConsentModeIntegrationEnabled()) return
        if (isFirebaseAnalyticsAvailable()) setConsentsToFirebaseAnalytics()
        if (isAirBridgeAvailable()) setConsentsToAirbridge()
        if (isAdjustAvailable()) setConsentsToAdjust()
        if (isBranchAvailable()) setConsentsBranch()
    }

    private fun isGoogleConsentModeIntegrationEnabled(): Boolean =
        exportData?.getGoogleConsentMode() == null

    private fun mapToFirebaseConsentStatus(isGranted: Boolean?) =
        if (isGranted == true) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED

    private fun setConsentsToFirebaseAnalytics() {
        val consent = exportData?.getGoogleConsentMode()
        Firebase.analytics.setConsent(
            mapOf(
                FirebaseAnalytics.ConsentType.AD_STORAGE to
                        mapToFirebaseConsentStatus(consent?.adStorageGranted),
                FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to
                        mapToFirebaseConsentStatus(consent?.analyticsStorageGranted),
                FirebaseAnalytics.ConsentType.AD_USER_DATA to
                        mapToFirebaseConsentStatus(consent?.adUserDataGranted),
                FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to
                        mapToFirebaseConsentStatus(consent?.adPersonalizationGranted)
            )
        )
    }

    private fun setConsentsToAirbridge() {
        val consent = exportData?.getGoogleConsentMode()

        val eeaValue = if (consentStatus?.scope == SCOPE_GDPR) "1" else "0"
        val adPersonalizationValue = if (consent?.adPersonalizationGranted == true) "1" else "0"
        val adUserDataValue = if (consent?.adUserDataGranted == true) "1" else "0"

        Airbridge.setDeviceAlias("eea", eeaValue)
        Airbridge.setDeviceAlias("adPersonalization", adPersonalizationValue)
        Airbridge.setDeviceAlias("adUserData", adUserDataValue)
    }

    private fun setConsentsToAdjust() {
        val consent = exportData?.getGoogleConsentMode()

        val eeaValue = if (consentStatus?.scope == SCOPE_GDPR) "1" else "0"
        val adPersonalizationValue = if (consent?.adPersonalizationGranted == true) "1" else "0"
        val adUserDataValue = if (consent?.adUserDataGranted == true) "1" else "0"

        val adjustThirdPartySharing = AdjustThirdPartySharing(true)
        with(adjustThirdPartySharing) {
            addGranularOption("google_dma", "eea", eeaValue)
            addGranularOption("google_dma", "ad_personalization", adPersonalizationValue)
            addGranularOption("google_dma", "ad_user_data", adUserDataValue)
        }
        Adjust.trackThirdPartySharing(adjustThirdPartySharing)
    }

    private fun setConsentsBranch() {
        val consent = exportData?.getGoogleConsentMode()

        val eeaValue = consentStatus?.scope == SCOPE_GDPR
        val adPersonalizationValue = consent?.adPersonalizationGranted == true
        val adUserDataValue = consent?.adUserDataGranted == true

        Branch.getInstance().setDMAParamsForEEA(eeaValue, adPersonalizationValue, adUserDataValue)
    }

    private fun isFirebaseAnalyticsAvailable(): Boolean =
        isClassAvailable("com.google.firebase.analytics.FirebaseAnalytics")

    private fun isAirBridgeAvailable(): Boolean =
        isClassAvailable("co.ab180.airbridge.Airbridge")

    private fun isAdjustAvailable(): Boolean =
        isClassAvailable("com.adjust.sdk.Adjust")

    private fun isBranchAvailable(): Boolean =
        isClassAvailable("io.branch.referral.Branch")

    private fun isClassAvailable(className: String): Boolean =
        try {
            Class.forName(className)
            true
        } catch (e: ClassNotFoundException) {
            false
        }

    /**
     * Private method to fetch the current ConsentStatus
     */
    private fun fetchConsentStatus(context: Context) {
        logger.log("Fetching status", EventLevel.DEBUG)
        val siteId = config?.siteId
        val consentVersion: String? =
            PreferenceManager.getDefaultSharedPreferences(context).getString(VERSION_KEY, null)
        logger.log("Saved Consent Version $consentVersion", EventLevel.DEBUG)

        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                var urlString = BASE_CONSENT_STATUS_URL.plus("s=$siteId")
                consentVersion?.let { urlString = urlString.plus("&v=$it") }

                val connection = URL(urlString).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()

                val response = try {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } catch (e: IOException) {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "{}"
                }

                val json = JSONObject(response)

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    logger.log("The server returned response code \"OK\"", EventLevel.DEBUG)
                    logger.log("Starting parsing returned json: $json", EventLevel.DEBUG)

                    consentStatus = ConsentStatus(
                        scope = json.optString("scope", null),
                        force = json.optBoolean("force", false),
                    )
                    isReady = true
                    Handler(Looper.getMainLooper()).post {
                        logger.log("Calling onReady", EventLevel.DEBUG)
                        onReadyListener?.invoke()
                    }
                    return@execute
                }

                logger.log(
                    "Server response is not OK: ${connection.responseCode}",
                    EventLevel.ERROR
                )
                logger.log("Error response json: $json", EventLevel.DEBUG)
                consentStatus = ConsentStatus(error = json.optString("error"))

            } catch (e: Exception) {
                logger.log("Exception occurred: ${e.message}", EventLevel.ERROR)
            }
        }
    }

    /**
     *  Private method to open Screen with WebView
     */
    private fun openWebViewActivity(context: Context) {
        context.startActivity(Intent(context, ClickioWebActivity::class.java))
    }
}