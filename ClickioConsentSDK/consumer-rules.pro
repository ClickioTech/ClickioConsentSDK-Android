-if class co.ab180.airbridge.Airbridge
-keep class co.ab180.airbridge.** { *; }

-if class com.adjust.sdk.Adjust
-keep class com.adjust.sdk.** { *; }

-if class com.appsflyer.AppsFlyerLib
-keep class com.appsflyer.** { *; }

-dontwarn co.ab180.airbridge.**
-dontwarn com.adjust.sdk.**
-dontwarn com.appsflyer.**
