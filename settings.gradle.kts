pluginManagement {repositories {
    google()
    mavenCentral() // 必须有这一行
    gradlePluginPortal()
}
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 添加阿里云公共仓库镜像，这通常能解决很多依赖下载失败的问题
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "SmartAlarm"
include(":app")