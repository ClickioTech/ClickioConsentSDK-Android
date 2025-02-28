package com.clickio.clickioconsentsdk

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONException
import org.json.JSONObject

private const val BASE_CONSENT_URL = "https://clickiocmp.com/t/static/consent_app.html?"

internal class ClickioWebActivity : AppCompatActivity() {

    private val logger = ClickioConsentSDK.getInstance().getLogger()
    private val consentUpdatedCallback = ClickioConsentSDK.getInstance().getConsentUpdatedCallback()
    private val config = ClickioConsentSDK.getInstance().getConfig()
    private var webView: WebView? = null
    private var isWriteCalled: Boolean = false
    private val sharedPreferences by lazy {
        //  TODO check about deprecation,
        //  Perhaps - implementation("androidx.preference:preference-ktx:1.1.1")
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createTransparentViewWithWebView()
        configureWebView()
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        backPressedCallback.remove()
    }

    @JavascriptInterface
    fun ready() {
        //TODO figure out: whether webView need somehow to be cleaned (I suppose - no)
        logger.log("JS method [READY] was called", EventLevel.INFO)
        if (isWriteCalled) consentUpdatedCallback?.invoke()
        finish()
    }

    @JavascriptInterface
    fun read(key: String?): String? {
        logger.log("JS method [READ] was called with key:$key", EventLevel.INFO)
        if (key.isNullOrEmpty()) {
            logger.log("Attempted to read with null or empty key", EventLevel.ERROR)
            return null
        }
        val value = sharedPreferences.getString(key, null)
        logger.log("Value:${value} was return for Key:$key", EventLevel.DEBUG)
        return value
    }

    @JavascriptInterface
    fun write(jsonString: String): Boolean {
        logger.log(
            "JS method [WRITE] was called with json: $jsonString",
            EventLevel.INFO
        )
        isWriteCalled = true
        synchronized(this) { // TODO perhaps no need
            try {
                val jsonObject = JSONObject(jsonString)
                val editor = sharedPreferences.edit()

                jsonObject.keys().forEach { key ->
                    when (val value = jsonObject.get(key)) {
                        is String -> editor.putString(key, value)
                        is Int -> editor.putInt(key, value)
                        is Boolean -> editor.putBoolean(key, value)
                        is Float -> editor.putFloat(key, value)
                        is Long -> editor.putLong(key, value)
                        else -> editor.putString(key, value.toString())
                    }
                }

                editor.apply()
            } catch (e: JSONException) {
                logger.log("JSON parse error: ${e.message}", EventLevel.ERROR)
                return false
            }
        }
        return true
    }

    private fun createTransparentViewWithWebView() {
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val rootLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)
        }

        setContentView(rootLayout)

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }

        rootLayout.addView(webView)
    }

    private fun getConsentUrl(): String {
        return if (config?.appLanguage.isNullOrEmpty())
            BASE_CONSENT_URL.plus("sid=${config?.siteId}")
        else
            BASE_CONSENT_URL.plus("sid=${config?.siteId}&lang=${config?.appLanguage}")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView?.apply {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(this@ClickioWebActivity, "clickioSDK")
            loadUrl(getConsentUrl())
        }
    }
}