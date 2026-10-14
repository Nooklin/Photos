import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Release signing details come from keystore.properties in the project root, which is never committed (see
// keystore.properties.example). Without that file the release build is simply left unsigned.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "org.nooklin.photos"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "org.nooklin.photos"
        minSdk = 33
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (keystoreProperties.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            optimization {
                enable = false
            }
            if (keystoreProperties.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        // For judging scrolling speed on a phone: not debuggable, like release, but signed with the debug key so it
        // installs without a keystore. Debug builds make Compose lists look slower than they are.
        // To also try R8, change enable to true here (not tried, the release build keeps it off).
        create("perftest") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Required so the bundled .tflite model can be memory-mapped directly
    // from the APK rather than needing to be copied out first, standard
    // requirement for TFLite/LiteRT. androidResources{} + noCompress has
    // existed since AGP 8.1 in the plain (non-KMP) android{} block used
    // here; unlike the versions above, I couldn't find an AGP-9-specific
    // confirmation this exact syntax is unchanged, flagging as another
    // "verify in Android Studio if this fails" item.
    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    // Print one photo from the share sheet (open source AndroidX, no Google services)
    implementation(libs.androidx.print)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    // unverified versions, see gradle/libs.versions.toml note
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.datastore.preferences)
    // androidx.biometric has historically stayed in alpha for years with a
    // very stable API surface, version number here is a genuine guess,
    // more so than the others. If sync fails specifically here, search
    // "androidx.biometric" in Project Structure > Dependencies.
    implementation(libs.androidx.biometric)
    // androidx.exifinterface is an old, very stable AndroidX library (originated
    // in support-compat years ago), lower version-risk than the Compose deps.
    implementation(libs.androidx.exifinterface)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    // Room, backing the virtual-album system (album membership is stored
    // here, decoupled from the photo's actual file location).
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // LiteRT (TFLite), see gradle/libs.versions.toml note for the version
    // rationale and the classic-Interpreter-API decision.
    implementation(libs.litert) {
        // LiteRT drags in Google Play delivery code (Play Core, play-services-basement and tasks, WorkManager) for
        // a feature that downloads models from Google Play. The models here are bundled in assets, and nothing in
        // LiteRT calls that code (the only class using it, AiPackModelProvider, is referenced by no other class).
        // Excluded to keep Google libraries, background permissions and exported services out of the APK.
        exclude(group = "com.google.android.play")
        exclude(group = "com.google.android.gms")
        exclude(group = "androidx.work")
    }

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
