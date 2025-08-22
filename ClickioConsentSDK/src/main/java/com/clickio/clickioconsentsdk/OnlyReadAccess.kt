package com.clickio.clickioconsentsdk

import android.content.SharedPreferences
import android.webkit.JavascriptInterface

internal class OnlyReadAccess(val sharedPreferences: SharedPreferences, val logger: EventLogger) {

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
    fun write(key: String?): Boolean {
        return false
    }
}