plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
}

apply(from = "../config/quality.gradle")

android {
    namespace = "org.odk.collect.openrosa"

    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
            isReturnDefaultValues = true
        }
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(project(":entities"))
    coreLibraryDesugaring(libs.desugar)

    implementation(project(":shared"))
    implementation(project(":forms"))

    implementation(libs.kotlinStdlib)
    implementation(libs.timber)
    implementation(libs.okHttp)
    implementation(libs.okHttpDigest)
    implementation(libs.okHttpTls)
    implementation(libs.commonsIo)
    implementation(libs.slf4jApi)
    implementation(libs.javarosa) {
        exclude(group = "commons-io")
        exclude(group = "joda-time")
        exclude(group = "org.slf4j")
        exclude(group = "org.hamcrest", module = "hamcrest-all")
    }

    testImplementation(libs.junit)
    testImplementation(libs.hamcrest)
    testImplementation(libs.okhttp3Mockwebserver)
    testImplementation(libs.mockitoKotlin)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidxTestExtJunit)
}
