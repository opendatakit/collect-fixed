plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
}

apply(from = "../config/quality.gradle")

android {
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    namespace = "org.odk.collect.audioclips"
}

dependencies {
    coreLibraryDesugaring(libs.desugar)

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(project(":androidshared"))
    implementation(project(":async"))
    implementation(libs.kotlinStdlib)
    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxLifecycleViewmodelKtx)
    implementation(libs.androidxLifecycleLivedataKtx)

    testImplementation(project(":androidtest"))
    testImplementation(project(":test-shared"))
    testImplementation(libs.junit)
    testImplementation(libs.mockitoKotlin)
    testImplementation(libs.androidxTestExtJunit)
    testImplementation(libs.androidxArchCoreTesting)
    testImplementation(libs.hamcrest)
}
