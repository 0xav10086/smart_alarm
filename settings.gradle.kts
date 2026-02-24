pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral() // 优先从中央库下载 libsu
        google()
        // LibXposed 专属快照库
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
        // 阿里云镜像用不了，https://maven.aliyun.com/mvn/guide告诉我还差前置条件
//        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "SmartAlarm"
include(":app")