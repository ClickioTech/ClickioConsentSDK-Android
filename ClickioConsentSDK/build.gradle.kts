plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.clickio.clickioconsentsdk"
    compileSdk = 35

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
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.activity.ktx)

    compileOnly("com.google.firebase:firebase-analytics-ktx:21.5.0")
    compileOnly("io.airbridge:sdk-android:2.27.1")
    compileOnly("com.adjust.sdk:adjust-android:5.1.0")
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