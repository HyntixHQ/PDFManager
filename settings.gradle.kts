pluginManagement {
    repositories {
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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "PDF Manager"
include(":app")
include(":HyntixPdfViewer")
project(":HyntixPdfViewer").projectDir = file("../../libs/HyntixPdfViewer")
include(":KotlinPdfium")
project(":KotlinPdfium").projectDir = file("../../libs/KotlinPdfium")
