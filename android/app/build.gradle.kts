plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.thornburydental"
    compileSdk = 36

    // Pinned so every machine and CI runner builds libdentara_whisper.so with the same
    // toolchain. Bumping this changes generated machine code, so treat it as a real change.
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "com.dentara.dental"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        ndk {
            // arm64-v8a covers the clinical handsets; x86_64 keeps the emulator usable
            // for development. armeabi-v7a is omitted deliberately: 32-bit ARM is scarce
            // on devices sold since minSdk 24 and each extra ABI is a full whisper build.
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DCMAKE_BUILD_TYPE=Release",
                    "-DGGML_LTO=ON",
                    "-DWHISPER_BUILD_EXAMPLES=OFF",
                    "-DWHISPER_BUILD_TESTS=OFF"
                )
                cppFlags += listOf(
                    "-O3",
                    "-ffunction-sections",
                    "-fdata-sections",
                    "-fvisibility=hidden"
                )
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.31.6"
        }
    }

    val releaseStoreFile = providers.environmentVariable("PRACTIX_KEYSTORE_FILE").orNull
    val releaseStorePassword = providers.environmentVariable("PRACTIX_KEYSTORE_PASSWORD").orNull
    val releaseKeyAlias = providers.environmentVariable("PRACTIX_KEY_ALIAS").orNull
    val releaseKeyPassword = providers.environmentVariable("PRACTIX_KEY_PASSWORD").orNull
    val releaseSigningConfig = if (
        listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
            .all { !it.isNullOrBlank() }
    ) {
        signingConfigs.create("release") {
            storeFile = file(releaseStoreFile!!)
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    } else {
        null
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Never fall back to the publicly known debug signing key. With no
            // release credentials configured, produce no debug-signed release.
            signingConfig = releaseSigningConfig
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
      jniLibs {
        useLegacyPackaging = false
      }
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.process)

  // Biometric authentication & Fragment
  implementation(libs.androidx.biometric)
  implementation(libs.androidx.fragment.ktx)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Serialization
  implementation(libs.kotlinx.serialization.json)

  // Database Encryption & SQLite KTX
  implementation(libs.sqlcipher)
  implementation(libs.androidx.sqlite.ktx)

  // Google Filament 3D Engine
  implementation(libs.filament.android)
  implementation(libs.gltfio.android)
  implementation(libs.filament.utils)
}
