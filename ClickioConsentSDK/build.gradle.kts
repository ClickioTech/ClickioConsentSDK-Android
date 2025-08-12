import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
    id("com.vanniktech.maven.publish") version "0.31.0"
}

android {
    namespace = "com.clickio.clickioconsentsdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.preference.ktx)

    compileOnly(libs.firebase.analytics.ktx)
    compileOnly(libs.airbridge)
    compileOnly(libs.adjust)
    compileOnly(libs.appsflyer)
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)
    signAllPublications()
}

mavenPublishing {
    coordinates("com.clickio", "clickioconsentsdk", "1.0.0-rc13")

    pom {
        name.set("ClickioConsentSDK")
        description.set("A library for managing consent by Clickio")
        url.set("https://clickio.com/")

        developers {
            developer {
                email.set("app-dev@clickio.com")
            }
        }

        licenses {
            license {
                name.set("The MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/ClickioTech/ClickioConsentSDK-Android.git")
            developerConnection.set("scm:git:ssh://git@github.com/ClickioTech/ClickioConsentSDK-Android.git")
            url.set("https://github.com/ClickioTech/ClickioConsentSDK-Android")
        }
    }
}