package com.clickio.clickioconsentsdk

import android.util.Log

private const val CLICKIO_SDK_TAG = "CLICKIO_SDK"

enum class LogsMode {
    DISABLED,
    VERBOSE
}

internal enum class EventLevel {
    ERROR,
    DEBUG,
    INFO
}

internal class EventLogger {

    private var mode: LogsMode = LogsMode.DISABLED

    fun setMode(mode: LogsMode) {
        this.mode = mode
    }

    fun log(event: String, level: EventLevel) {
        if (mode == LogsMode.DISABLED) return
        when (level) {
            EventLevel.ERROR -> Log.e(CLICKIO_SDK_TAG, event)
            EventLevel.DEBUG -> Log.d(CLICKIO_SDK_TAG, event)
            EventLevel.INFO -> Log.i(CLICKIO_SDK_TAG, event)
        }
    }
}