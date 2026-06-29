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
    }
}

rootProject.name = "Zinema"

// --- Application module ---
include(":app")

// --- Core modules ---
include(":core:network")
include(":core:data")
include(":core:domain")
include(":core:ui")
include(":core:security")

// --- Feature modules ---
include(":feature:auth")
include(":feature:home")
include(":feature:detail")
include(":feature:player")
include(":feature:search")
include(":feature:shorttv")
