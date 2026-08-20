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
        google()
        mavenCentral()
        maven {
            setUrl("https://dl.frostwire.com/maven")
            content { includeGroup("com.frostwire") }
        }
    }
}
rootProject.name = "BluStream"
include(":app")
