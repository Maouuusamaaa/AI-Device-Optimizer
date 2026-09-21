plugins {
    id("com.android.application")
}

val ciKeystoreFile = System.getenv("CI_ANDROID_KEYSTORE_FILE")
val ciKeystorePassword = System.getenv("CI_ANDROID_KEYSTORE_PASSWORD")
val ciKeyAlias = System.getenv("CI_ANDROID_KEY_ALIAS")
val ciKeyPassword = System.getenv("CI_ANDROID_KEY_PASSWORD")
val ciSigningConfigured = listOf(
    ciKeystoreFile,
    ciKeystorePassword,
    ciKeyAlias,
    ciKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.maouuusama.ai.device.optimizer"
    compileSdk = 36
    ndkVersion = "27.2.12479018"

    signingConfigs {
        if (ciSigningConfigured) {
            create("ciStable") {
                storeFile = file(ciKeystoreFile!!)
                storePassword = ciKeystorePassword
                keyAlias = ciKeyAlias
                keyPassword = ciKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.maouuusama.ai.device.optimizer"
        minSdk = 29
        targetSdk = 36
        versionCode = 4
        versionName = "0.1.4"

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        getByName("debug") {
            if (ciSigningConfigured) {
                signingConfig = signingConfigs.getByName("ciStable")
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-ktx:1.12.0")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    testImplementation("junit:junit:4.13.2")
}
