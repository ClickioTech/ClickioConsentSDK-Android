package com.clickio.clickioconsentsdk

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import org.json.JSONObject
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
        val scope: String?,
        val force: Boolean?,
        val error: String?,
    )

    // Common methods

    /**
     *  Init of SDK
     */
    fun initialize(context: Context, config: Config) {
        logger.log("Initialization stared", EventLevel.INFO)
        this.config = config
        fetchConsentStatus(context)
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
                "Consent status is not loaded, possible reason:${consentStatus?.error}",
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
                "Consent status is not loaded, possible reason:${consentStatus?.error}",
                EventLevel.ERROR
            )
        }
        // TODO ask about what to do, when "force" is missing (when calling status with "version" getting only {"scope":"gdpr"})
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
    fun checkConsentForPurpose(purposeId: String): Boolean? {
        // TODO implement
        return null
    }

    /**
     * Verifies whether consent for a specific vendor has been granted.
     */
    fun checkConsentForVendor(vendorId: String): Boolean? {
        // TODO implement
        return null
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
        context: Context,
        mode: DialogMode = DialogMode.DEFAULT
    ) {
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

    /**
     * Private method to fetch the current ConsentStatus
     */
    private fun fetchConsentStatus(context: Context) {
        logger.log("Fetching status", EventLevel.INFO)
        val siteId = config?.siteId
        val consentVersion: String? =
            PreferenceManager.getDefaultSharedPreferences(context).getString(VERSION_KEY, null)
        logger.log("Saved Consent Version $consentVersion", EventLevel.INFO)
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                var urlString = BASE_CONSENT_STATUS_URL.plus("s=$siteId")
                consentVersion?.let {
                    urlString = urlString.plus("&v=$it")
                }
                val url = URL(urlString)

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }

                    val json = JSONObject(response)
                    logger.log("Server response is OK", EventLevel.INFO)
                    logger.log(json.toString(), EventLevel.INFO)
                    consentStatus = ConsentStatus(
                        scope = if (json.has("scope")) json.optString("scope") else null,
                        force = if (json.has("force")) json.optBoolean("force") else null,
                        error = if (json.has("error")) json.optString("error") else null
                    )
                    isReady = true
                    Handler(Looper.getMainLooper()).post {
                        onReadyListener?.invoke()
                    }
                } else {
                    logger.log("Server response is not OK", EventLevel.ERROR)
                }
            } catch (e: Exception) {
                logger.log(e.message.toString(), EventLevel.INFO)
                e.printStackTrace()
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