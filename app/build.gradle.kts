import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Release signing lives outside the repository: create `keystore.properties` at the project root
// with storeFile (relative to the root), storePassword, keyAlias and keyPassword. Without it the
// release build stays unsigned, which is what CI uses.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "es.igarciadelamo.txoka"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "es.igarciadelamo.txoka"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

ksp {
    // Schemas are committed so every database version bump can ship a real migration.
    arg("room.schemaLocation", "$projectDir/schemas")
}

kover {
    reports {
        filters {
            // Compose screens and Android-only adapters are exercised manually; the coverage gate
            // protects the logic that unit tests can reach.
            includes {
                classes(
                    "es.igarciadelamo.txoka.domain.*",
                    "es.igarciadelamo.txoka.nfc.HceCard*",
                    "es.igarciadelamo.txoka.nfc.HceResponder*",
                    "es.igarciadelamo.txoka.nfc.IncomingCards*",
                    "es.igarciadelamo.txoka.nfc.ShareRole*",
                    "es.igarciadelamo.txoka.nfc.FakeTransport*",
                    "es.igarciadelamo.txoka.nfc.NdefHceTransport*",
                    "es.igarciadelamo.txoka.nfc.NfcStatus",
                    "es.igarciadelamo.txoka.ui.*ViewModel*",
                    "es.igarciadelamo.txoka.ui.*UiState",
                    "es.igarciadelamo.txoka.ui.AppState",
                )
            }
            excludes {
                classes(
                    "*_Factory",
                    "*_Factory\$*",
                    "*_HiltModules*",
                    "Hilt_*",
                    "*_MembersInjector",
                    "*_LazyMapKey",
                    "*.BuildConfig",
                )
            }
        }
        total {
            verify {
                rule {
                    minBound(95)
                }
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.libphonenumber)
    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    parallel = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = JavaVersion.VERSION_11.toString()
}
