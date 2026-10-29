plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

// Release signing comes from environment variables (set by GitHub Actions from repository secrets).
val keystorePath = providers.environmentVariable("TIPTOP_KEYSTORE_PATH").orNull
// GitHub Actions passes its run number so every build gets a higher version code.
val buildNumber = providers.environmentVariable("VERSION_CODE").orNull?.toInt() ?: 1

android {
  namespace = "be.jochem.tiptoptrein"
  compileSdk = 36
  defaultConfig {
    applicationId = "be.jochem.tiptoptrein"
    minSdk = 24
    targetSdk = 36
    versionCode = buildNumber
    versionName = "1.0.$buildNumber"
  }

  signingConfigs {
    if (keystorePath != null) {
      create("release") {
        storeFile = file(keystorePath)
        storePassword = providers.environmentVariable("TIPTOP_KEYSTORE_PASSWORD").get()
        keyAlias = providers.environmentVariable("TIPTOP_KEY_ALIAS").get()
        keyPassword = providers.environmentVariable("TIPTOP_KEY_PASSWORD").get()
      }
    }
  }

  buildTypes {
    release {
      // Without a keystore (local builds) fall back to the debug key so the release build can be tried out.
      signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
      isMinifyEnabled = true
      isShrinkResources = true
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

  packaging {
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

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
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
}
