plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "io.github.av10086.smartalarm"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.av10086.smartalarm"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
    // 强制所有 org.jetbrains.kotlin 相关的库使用 2.1.10，这是可行的方案，对于 Kotlin 1.8+ 版本依赖冲突问题
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:${libs.versions.kotlin.get()}"))

    // 使用 libs.xxx 引用，简洁且有自动补全
    compileOnly(libs.libxposed.api)
    ksp(libs.libxposed.api)

    implementation(libs.libsu.core)
    implementation(libs.libsu.nio)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.constraintlayout)
}

// 添加以下代码来强制解决冲突，这是可行的方案，对于 Kotlin 1.8+ 版本依赖冲突问题
//configurations.all {
//    resolutionStrategy.eachDependency {
//        if (requested.group == "org.jetbrains.kotlin") {
//            useVersion(libs.versions.kotlin.get())
//        }
//    }
//}

// Kotlin 1.8+ 版本依赖冲突问题
// 从 Kotlin 1.8.0 开始，官方将 kotlin-stdlib-jdk7 和 kotlin-stdlib-jdk8 的功能直接整合进了 kotlin-stdlib 中。由于当前项目使用的是 Kotlin 2.1.10，它会自动引入新的 kotlin-stdlib 。然而，依赖的某些第三方库（可能是旧版 libsu 或其他库）还在显式引用旧版的 kotlin-stdlib-jdk8:1.6.0，导致同一个类在两个不同的 Jar 包里都存在，从而触发了 Duplicate class 错误 。