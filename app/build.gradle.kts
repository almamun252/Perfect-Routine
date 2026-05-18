plugins {
    alias(libs.plugins.android.application)
    kotlin("android")
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt")
}


android {
    namespace = "com.almamun252.perfectroutine"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.almamun252.perfectroutine"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "2.1.1"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.0" // বা latest
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ===============================================
    // Perfect Routine অ্যাপের জন্য প্রয়োজনীয় নতুন লাইব্রেরি
    // ===============================================

    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    // --- Room Database ---
    val roomVersion = "2.7.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    add("kapt", "androidx.room:room-compiler:$roomVersion")

    // ⚠️ উইন্ডোজে Room 2.7.0 এর Native Library ক্র্যাশ ফিক্স করার জন্য এই লাইনটি যোগ করুন
    add("kapt", "org.xerial:sqlite-jdbc:3.46.0.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    //--- for icons
    implementation("androidx.compose.material:material-icons-extended")
    //--- for Notifications
    implementation("androidx.work:work-runtime-ktx:2.9.0")

}