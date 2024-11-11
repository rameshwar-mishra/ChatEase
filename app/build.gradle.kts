plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.chatease"
    compileSdk = 34

    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.example.chatease"
        minSdk = 26 // Android 8.0
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.database)
    implementation(libs.firebase.messaging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Scalable Size Unit (Support for different screen sizes)
    implementation(libs.intuit.sdp)
    implementation(libs.intuit.ssp)

    // Ramen's Rounded ImageView
    implementation(libs.makeramen.roundedimageview)

    // Picasso (An easy image loading library)
    implementation("com.squareup.picasso:picasso:2.71828")

    // Glide (Advanced Image Loading Library)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Native version of uCrop (Image Cropping Library for Android) for enhanced image quality
    implementation("com.github.yalantis:ucrop:2.2.8-native")

    // Libraries of LifeCycle Observer
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1") // For Latest versions
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0") // For older versions
}
