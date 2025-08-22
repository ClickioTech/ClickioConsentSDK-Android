package com.clickio.clickioconsentsdk

data class WebViewConfig(
    val backgroundColor: Int = 0,// Default value - transparent, example of value: 0xFFFF0000 or android.graphics.Color.White
    val height: Int = -1, // Value in px, default value (-1) = match parent (max size)
    val width: Int = -1, // Value in px, default value (-1) = match parent (max size)
    val gravity: WebViewGravity = WebViewGravity.CENTER
)

enum class WebViewGravity {
    TOP, CENTER, BOTTOM
}