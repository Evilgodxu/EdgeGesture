pluginManagement {
    repositories {
        maven { url = uri("https://mirrors.cloud.tencent.com/repository/maven-google/") }
        maven { url = uri("https://mirrors.cloud.tencent.com/repository/maven/") }
        maven { url = uri("https://mirrors.cloud.tencent.com/repository/gradle-plugin/") }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("de.fayard.refreshVersions") version "0.60.6"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://mirrors.cloud.tencent.com/repository/maven-google/") }
        maven { url = uri("https://mirrors.cloud.tencent.com/repository/maven/") }
        google()
        mavenCentral()
    }
}

rootProject.name = "边缘手势"
include(":app")
