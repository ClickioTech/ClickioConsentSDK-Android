plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.clickio.clickioconsentsdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 26

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
    compileOnly(libs.sdk.android)
    compileOnly(libs.adjust.android)
    compileOnly("io.branch.sdk.android:library:5.15.2")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.clickio"
            artifactId = "clickioconsentsdk"
            version = "0.0.2"

            afterEvaluate {
                from(components["release"])
            }
        }
    }

    repositories {
        mavenLocal()
    }
}