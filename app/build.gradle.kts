import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import javax.inject.Inject

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

abstract class GeneratePreviewSamplesTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDirectory: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val schemaDirectory: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val generator: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun generate() {
        execOperations.exec {
            commandLine(
                "python3",
                generator.get().asFile,
                "--database-source",
                sourceDirectory.file("es/igarciadelamo/txoka/data/received/TxokaDatabase.kt").get().asFile,
                "--schema-directory",
                schemaDirectory.get().asFile,
                "--appearance-source",
                sourceDirectory.file("es/igarciadelamo/txoka/domain/profile/CardAppearance.kt").get().asFile,
                "--output",
                outputDirectory.dir("preview-samples").get().asFile,
            )
        }
    }
}

val generatePreviewSamples = tasks.register<GeneratePreviewSamplesTask>("generatePreviewSamples") {
    sourceDirectory.set(layout.projectDirectory.dir("src/main/java"))
    schemaDirectory.set(layout.projectDirectory.dir("schemas/es.igarciadelamo.txoka.data.received.TxokaDatabase"))
    generator.set(rootProject.layout.projectDirectory.file("scripts/ci/preview_samples.py"))
    outputDirectory.set(layout.buildDirectory.dir("generated/previewSamples/assets"))
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

        buildConfigField("boolean", "ENABLE_RECEIVE_SIMULATOR", "false")
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
        debug {
            buildConfigField("boolean", "ENABLE_RECEIVE_SIMULATOR", "true")
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = true
                // Repackaging kotlin/kotlinx breaks package-private access in R8 (IllegalAccessError
                // on kotlin.sequences internals), so only androidx classes may move.
                packageScope = setOf("androidx.**")
            }
        }
        create("preview") {
            initWith(getByName("release"))
            buildConfigField("boolean", "ENABLE_RECEIVE_SIMULATOR", "true")
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
            // PR builds are signed separately and must never inherit a local release key.
            signingConfig = null
            matchingFallbacks += listOf("release")
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

androidComponents {
    beforeVariants(selector().withBuildType("preview")) { variant ->
        variant.hostTests[com.android.build.api.variant.HostTestBuilder.UNIT_TEST_TYPE]?.enable = true
    }
    onVariants(selector().withBuildType("preview")) { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            generatePreviewSamples,
            GeneratePreviewSamplesTask::outputDirectory,
        )
    }
    onVariants(selector().withBuildType("debug")) { variant ->
        // Exercise the preview installer in the test APK without adding it to the debug app.
        variant.deviceTests.values.forEach { test ->
            test.sources.java?.addStaticSourceDirectory("src/preview/java")
            test.sources.java?.addStaticSourceDirectory("src/androidTestPreviewSamples/java")
            test.sources.assets?.addGeneratedSourceDirectory(
                generatePreviewSamples,
                GeneratePreviewSamplesTask::outputDirectory,
            )
        }
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
