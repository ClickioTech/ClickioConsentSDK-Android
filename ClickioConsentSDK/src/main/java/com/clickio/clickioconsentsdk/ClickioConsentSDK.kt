package com.clickio.clickioconsentsdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.preference.PreferenceManager
import co.ab180.airbridge.Airbridge
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustThirdPartySharing
import com.appsflyer.AppsFlyerConsent
import com.appsflyer.AppsFlyerLib
import com.clickio.clickioconsentsdk.ClickioConsentSDK.DialogMode.DEFAULT
import com.clickio.clickioconsentsdk.ClickioConsentSDK.DialogMode.RESURFACE
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

internal const val INTERNET_ERROR =
    "Bad network connection. Please ensure you are connected to the internet and try again"

class ClickioConsentSDK private constructor() {

    companion object {

        private const val SCOPE_GDPR = "gdpr"
        private const val SCOPE_US = "us"
        private const val SCOPE_OUT_OF_SCOPE = "out of scope"
        private const val BASE_CONSENT_STATUS_URL = "https://clickiocdn.com/sdk/consent-status?"
        private const val VERSION_KEY = "CLICKIO_CONSENT_server_request"

        private var instance: ClickioConsentSDK? = null

        /**
         * @return The singleton instance of [ClickioConsentSDK].
         */
        fun getInstance(): ClickioConsentSDK {
            if (instance == null) {
                instance = ClickioConsentSDK()
            }
            return instance as ClickioConsentSDK
        }
    }

    private var isReady: Boolean = false
    private var isReadyCalled: Boolean = false
    private var config: Config? = null
    private var logger: EventLogger = EventLogger()
    private var onConsentUpdatedListener: (() -> Unit)? = null
    private var onReadyListener: (() -> Unit)? = null
    private var consentStatus: ConsentStatus? = null
    private var exportData: ExportData? = null

    /**
     * Configuration data class.
     * @param siteId The site ID.
     * @param appLanguage The optional application language.
     */
    data class Config(
        val siteId: String,
        val appLanguage: String? = null,
    )

    /**
     * Enum representing different consent states.
     */
    enum class ConsentState {
        NOT_APPLICABLE,
        GDPR_NO_DECISION,
        GDPR_DECISION_OBTAINED,
        US
    }

    /**
     * Enum representing different dialog modes for opening the consent dialog.
     * @property DEFAULT Default logic for opening the consent dialog.
     * @property RESURFACE Forces the dialog to open, but only if the user's scope requires it (GDPR/US zone).
     */
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

    /**
     * Initializes the SDK.
     * @param context The application context.
     * @param config The configuration object.
     */
    fun initialize(context: Context, config: Config) {
        logger.log("Initialization stared", EventLevel.INFO)
        if (!isNetworkAvailable(context)) {
            logger.log(INTERNET_ERROR, EventLevel.ERROR)
            return
        }
        this.config = config
        exportData = ExportData(context)
        fetchConsentStatus(context)
        setConsentsIfApplicable()
    }

    /**
     * Sets the logging mode.
     * @param mode The logging mode.
     */
    fun setLogsMode(mode: LogsMode) {
        logger.setMode(mode)
    }

    /**
     * Registers a callback to be invoked when the SDK is ready.
     * @param listener The callback function.
     */
    fun onReady(listener: () -> Unit) {
        this.onReadyListener = listener
        if (isReady) callOnReadyCallback()
    }

    /**
     * Registers a callback to be invoked when consent is updated.
     * @param listener The callback function.
     */
    fun onConsentUpdated(listener: (() -> Unit)?) {
        this.onConsentUpdatedListener = listener
    }

    /**
     * Returns the applicable consent scope.
     * @return The consent scope or null if unavailable.
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
     * Determines the consent state based on the scope and force flag.
     * @return The [ConsentState] or null if unavailable.
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
     * Verifies whether consent for a specific purpose has been granted.
     * @param purposeId The ID of the purpose.
     * @return True if consent is granted, false otherwise, or null if unavailable.
     */
    fun checkConsentForPurpose(purposeId: Int): Boolean? =
        exportData?.getConsentedTCFPurposes()?.contains(purposeId)

    /**
     * Verifies whether consent for a specific vendor has been granted.
     * @param vendorId The ID of the vendor.
     * @return True if consent is granted, false otherwise, or null if unavailable.
     */
    fun checkConsentForVendor(vendorId: Int): Boolean? =
        exportData?.getConsentedTCFVendors()?.contains(vendorId)


    /**
     * Opens the consent dialog based on the specified mode.
     * @param context Android context from Activity of subclass.
     * @param mode The dialog mode.
     */
    fun openDialog(
        context: Context,
        mode: DialogMode = DEFAULT
    ) {
        logger.log("openDialog called with mode $mode", level = EventLevel.INFO)
        if (!isReady || consentStatus == null) {
            logger.log("ClickioSDK is not ready", level = EventLevel.INFO)
            return
        }
        if (consentStatus?.scope == null) {
            logger.log(
                "Consent status is not loaded, possible reason: ${consentStatus?.error}",
                EventLevel.ERROR
            )
            return
        }

        when (mode) {
            DEFAULT -> {
                if (consentStatus?.scope == SCOPE_GDPR && consentStatus?.force == true) openWebViewActivity(
                    context
                ) else {
                    logger.log(
                        "Dialog not shown: decision already saved or user is located outside the EEA, GB, or CH regions",
                        EventLevel.INFO
                    )
                }
            }

            RESURFACE -> {
                if (consentStatus?.scope != SCOPE_OUT_OF_SCOPE) {
                    openWebViewActivity(context)
                } else {
                    logger.log(
                        "Dialog not shown: decision already saved or user is located outside the EEA, GB, or CH regions",
                        EventLevel.INFO
                    )
                }
            }
        }
    }

    /**
     * Creates and returns a configured WebView to handle saved consent.
     * @param context Android context from current Activity
     * @param url yours site url
     * @param webViewConfig optional webview configuration
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun webViewLoadUrl(
        context: Context,
        url: String,
        webViewConfig: WebViewConfig = WebViewConfig()
    ): WebView {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val customWebView = WebView(context).apply {

            layoutParams = FrameLayout.LayoutParams(
                webViewConfig.width,
                webViewConfig.height
            ).apply {
                gravity = when (webViewConfig.gravity) {
                    WebViewGravity.TOP -> Gravity.TOP
                    WebViewGravity.CENTER -> Gravity.CENTER
                    WebViewGravity.BOTTOM -> Gravity.BOTTOM
                }
            }
            setBackgroundColor(webViewConfig.backgroundColor)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = false
            }

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            val readAccess = OnlyReadAccess(sharedPreferences, logger)

            addJavascriptInterface(readAccess, "clickioSDK")
            loadUrl(url)
        }
        return customWebView
    }

    internal fun getConfig() = config

    internal fun getConsentUpdatedCallback() = onConsentUpdatedListener

    internal fun getLogger() = logger

    internal fun updateConsentStatus() {
        consentStatus = consentStatus?.copy(force = false)
        setConsentsIfApplicable()
    }

    private fun setConsentsIfApplicable() {
        logger.log(
            "Is Google Consent Mode Enabled = ${isGoogleConsentModeIntegrationEnabled()}",
            EventLevel.DEBUG
        )
        if (!isGoogleConsentModeIntegrationEnabled()) return
        if (isFirebaseAnalyticsAvailable()) setConsentsToFirebaseAnalytics()
        if (isAirBridgeAvailable()) setConsentsToAirbridge()
        if (isAdjustAvailable()) setConsentsToAdjust()
        if (isAppsFlyerAvailable()) setConsentsToAppsFlyer()
    }

    private fun isGoogleConsentModeIntegrationEnabled(): Boolean =
        exportData?.getGoogleConsentMode() != null

    private fun mapToFirebaseConsentStatus(isGranted: Boolean?) =
        if (isGranted == true) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED

    private fun setConsentsToFirebaseAnalytics() {
        logger.log("Setting consent to Firebase", EventLevel.INFO)
        val consent = exportData?.getGoogleConsentMode()
        try {
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
            logger.log("Successful finished setting consent to Firebase", EventLevel.INFO)
        } catch (e: Exception) {
            logger.log("Failed setting consent to Firebase: $e", EventLevel.ERROR)
        }
    }

    private fun setConsentsToAdjust() {
        logger.log("Setting consent to Adjust", EventLevel.INFO)
        val consent = exportData?.getGoogleConsentMode()
        val eeaValue = if (consentStatus?.scope == SCOPE_GDPR) "1" else "0"
        val adPersonalizationValue = if (consent?.adPersonalizationGranted == true) "1" else "0"
        val adUserDataValue = if (consent?.adUserDataGranted == true) "1" else "0"

        try {
            val adjustThirdPartySharing = AdjustThirdPartySharing(true)
            with(adjustThirdPartySharing) {
                addGranularOption("google_dma", "eea", eeaValue)
                addGranularOption("google_dma", "ad_personalization", adPersonalizationValue)
                addGranularOption("google_dma", "ad_user_data", adUserDataValue)
            }
            Adjust.trackThirdPartySharing(adjustThirdPartySharing)

            logger.log("Successful finished setting consent to Adjust", EventLevel.INFO)
        } catch (e: Throwable) {
            logger.log("Failed setting consent to Adjust: $e", EventLevel.ERROR)
        }
    }

    private fun setConsentsToAirbridge() {
        logger.log("Setting consent to Airbridge", EventLevel.INFO)

        val consent = exportData?.getGoogleConsentMode()
        val eeaValue = if (consentStatus?.scope == SCOPE_GDPR) "1" else "0"
        val adPersonalizationValue = if (consent?.adPersonalizationGranted == true) "1" else "0"
        val adUserDataValue = if (consent?.adUserDataGranted == true) "1" else "0"

        try {
            Airbridge.setDeviceAlias("eea", eeaValue)
            Airbridge.setDeviceAlias("adPersonalization", adPersonalizationValue)
            Airbridge.setDeviceAlias("adUserData", adUserDataValue)

            logger.log("Successful finished setting consent to Airbridge", EventLevel.INFO)
        } catch (e: Throwable) {
            logger.log("Failed setting consent to Airbridge: $e", EventLevel.ERROR)
        }
    }

    private fun setConsentsToAppsFlyer() {
        logger.log("Setting consent to AppsFlyer", EventLevel.INFO)

        val consent = exportData?.getGoogleConsentMode()

        var adPersonalizationValue: Boolean? = null
        var adUserDataValue: Boolean? = null
        var adStorageValue: Boolean? = null

        val eeaValue = consentStatus?.scope == SCOPE_GDPR

        if (eeaValue) {
            adPersonalizationValue = consent?.adPersonalizationGranted == true
            adUserDataValue = consent?.adUserDataGranted == true
            adStorageValue = consent?.adStorageGranted == true
        }

        try {
            val gdprUser = AppsFlyerConsent(
                eeaValue, adUserDataValue, adPersonalizationValue, adStorageValue
            )
            AppsFlyerLib.getInstance().setConsentData(gdprUser)
            logger.log("Successful finished setting consent to AppsFlyer", EventLevel.INFO)
        } catch (e: Throwable) {
            logger.log("Failed manually setting consent to AppsFlyer: $e", EventLevel.ERROR)

            // The current version of the AppsFlyer library for Flutter uses an outdated version
            // of the native library under the hood, which does not support setting manual mode.
            try {
                logger.log("Setting consent to AppsFlyer through TCF", EventLevel.INFO)
                AppsFlyerLib.getInstance().enableTCFDataCollection(true)
            } catch (e: Throwable) {
                logger.log("Failed setting consent to AppsFlyer through TCF: $e", EventLevel.ERROR)
            }
        }
    }

    private fun isFirebaseAnalyticsAvailable(): Boolean =
        isClassAvailable("com.google.firebase.analytics.FirebaseAnalytics")

    private fun isAirBridgeAvailable(): Boolean =
        isClassAvailable("co.ab180.airbridge.Airbridge")

    private fun isAdjustAvailable(): Boolean =
        isClassAvailable("com.adjust.sdk.Adjust")

    private fun isAppsFlyerAvailable(): Boolean =
        isClassAvailable("com.appsflyer.AppsFlyerLib")

    private fun isClassAvailable(className: String): Boolean =
        try {
            Class.forName(className)
            true
        } catch (e: ClassNotFoundException) {
            false
        }

    /**
     * Fetches the current consent status from the server.
     * @param context The application/activity context.
     */
    private fun fetchConsentStatus(context: Context) {
        logger.log("Started fetching consent status", EventLevel.DEBUG)
        val siteId = config?.siteId
        val consentVersion: String? =
            PreferenceManager.getDefaultSharedPreferences(context).getString(VERSION_KEY, null)
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                var urlString = BASE_CONSENT_STATUS_URL.plus("s=$siteId")
                consentVersion?.let { urlString = urlString.plus("&v=$it") }
                logger.log("Fetching URL: $urlString", EventLevel.DEBUG)
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
                        scope = json.optString("scope", "").ifEmpty { null },
                        force = json.optBoolean("force", false),
                    )

                    Handler(Looper.getMainLooper()).post {
                        logger.log("Calling onReady", EventLevel.DEBUG)
                        isReady = true
                        callOnReadyCallback()
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
     * Opens the WebView activity for managing consent.
     * @param context The application/activity context.
     */
    private fun openWebViewActivity(context: Context) {
        if (!isNetworkAvailable(context)) {
            logger.log(INTERNET_ERROR, EventLevel.ERROR)
            return
        }
        val intent = Intent(context, ClickioWebActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        context.startActivity(intent)
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo != null && networkInfo.isConnected
        }
    }

    private fun callOnReadyCallback() {
        synchronized(this) {
            if (!isReadyCalled) {
                isReadyCalled = true
                onReadyListener?.invoke()
            }
        }
    }
}