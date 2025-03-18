plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
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
    compileOnly(libs.branch)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.clickio"
            artifactId = "clickioconsentsdk"
            version = "0.0.8"

            afterEvaluate {
                from(components["release"])
            }
        }
    }

    repositories {
        mavenLocal()
    }
}