import de.undercouch.gradle.tasks.download.Download

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.androidx.navigation.safeargs)
    alias(libs.plugins.undercouch.download)
}

android {
    namespace = "com.xwurfel.echo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.xwurfel.echo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.kotlin.stdlib.jdk7)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.tensorflow.lite.task.audio)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Import DownloadModels task
val ASSET_DIR by extra { "$projectDir/src/main/assets" }
val TEST_ASSET_DIR by extra { "$projectDir/src/androidTest/assets" }

tasks.register<Download>("downloadAudioClassifierModel") {
    src("https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/audio_classification/android/lite-model_yamnet_classification_tflite_1.tflite")
    dest(file("$ASSET_DIR/yamnet.tflite"))
    overwrite(false)
}

tasks.register<Download>("downloadSpeechClassifierModel") {
    // This model is custom made using Model Maker. A detailed guide can be found here:
    // https://www.tensorflow.org/lite/models/modify/model_maker/speech_recognition
    src("https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/audio_classification/android/speech_commands.tflite")
    dest(file("$ASSET_DIR/speech.tflite"))
    overwrite(false)
}

tasks.named("preBuild") {
    dependsOn("downloadAudioClassifierModel", "downloadSpeechClassifierModel")
}
