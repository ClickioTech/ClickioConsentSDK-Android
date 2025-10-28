package com.clickio.clickioconsentsdk

import android.content.SharedPreferences
import android.webkit.JavascriptInterface
import org.json.JSONException
import org.json.JSONObject

/**
 * Separate read/write access for custom configured WebView for webViewLoadUrl
 */
internal class OnlyReadAndWriteAccess(val sharedPreferences: SharedPreferences, val logger: EventLogger) {

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
}