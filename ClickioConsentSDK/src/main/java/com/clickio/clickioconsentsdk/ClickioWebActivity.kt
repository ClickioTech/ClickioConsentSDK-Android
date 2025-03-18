package com.clickio.clickioconsentsdk

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.preference.PreferenceManager
import com.clickio.clickioconsentsdk.utils.enableEdgeToEdge
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
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
        Handler(Looper.getMainLooper()).post {
            webView?.clearCache(true)
            webView?.clearHistory()
            logger.log("JS method [READY] was called", EventLevel.INFO)
            ClickioConsentSDK.getInstance().updateConsentStatus()
            if (isWriteCalled) consentUpdatedCallback?.invoke()
            finish()
        }
    }

    @JavascriptInterface
    fun read(key: String?): String? {
        logger.log("JS method [READ] was called with key:$key", EventLevel.INFO)
        if (key.isNullOrEmpty()) {
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
        synchronized(this) {
            val jsonObject = JSONObject(jsonString)
            with(sharedPreferences.edit()) {
                try {
                    jsonObject.keys().forEach { key ->
                        val value = jsonObject.get(key)
                        logger.log("$key $value", EventLevel.DEBUG)
                        when (value) {
                            is String -> putString(key, value)
                            is Int -> putInt(key, value)
                            is Boolean -> putBoolean(key, value)
                            is Float -> putFloat(key, value)
                            is Long -> putLong(key, value)
                            JSONObject.NULL -> remove(key)
                            null -> remove(key)
                            else -> putString(key, value.toString())
                        }
                    }
                } catch (e: JSONException) {
                    logger.log("JSON parse error: ${e.message}", EventLevel.ERROR)
                    return false
                }
                logger.log(
                    "Successfully finished parsing of the json",
                    EventLevel.DEBUG
                )
                apply()
            }
        }
        return true
    }

    private fun createTransparentViewWithWebView() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        val rootLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)
        }

        setContentView(rootLayout)

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
                topMargin = insets.top
            }
            WindowInsetsCompat.CONSUMED
        }

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
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(this@ClickioWebActivity, "clickioSDK")
            loadUrl(getConsentUrl())
        }
    }
}