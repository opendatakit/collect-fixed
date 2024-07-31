import dependencies.Dependencies
import dependencies.Versions

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

apply(from = "../config/quality.gradle")

android {
    namespace = "org.odk.collect.draw"
    compileSdk = Versions.android_compile_sdk

    defaultConfig {
        minSdk = Versions.android_min_sdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    coreLibraryDesugaring(Dependencies.desugar)

    implementation(project(":shared"))
    implementation(project(":androidshared"))
    implementation(project(":strings"))
    implementation(project(":async"))
    implementation(project(":settings"))
    implementation(project(":icons"))

    implementation(Dependencies.rarepebble_colorpicker)
    implementation(Dependencies.androidx_lifecycle_livedata_ktx)
    implementation(Dependencies.androidx_lifecycle_viewmodel_ktx)
    implementation(Dependencies.androidx_fragment_ktx)
    implementation(Dependencies.android_material)
    implementation(Dependencies.timber)

    implementation(Dependencies.dagger)
    kapt(Dependencies.dagger_compiler)

    debugImplementation(project(":fragments-test"))

    testImplementation(Dependencies.androidx_test_ext_junit)
    testImplementation(Dependencies.mockito_kotlin)
    testImplementation(Dependencies.androidx_test_espresso_core)
    testImplementation(Dependencies.robolectric)
}
