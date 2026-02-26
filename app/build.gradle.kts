plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.github.av10086.smartalarm"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.av10086.smartalarm"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:${libs.versions.kotlin.get()}"))

    // LibXposed API - 我们手动处理初始化，所以只需要 api
    compileOnly(libs.libxposed.api)

    implementation(libs.libsu.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.constraintlayout)
}
