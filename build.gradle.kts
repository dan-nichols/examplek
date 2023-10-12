plugins {
    alias(libs.plugins.multiplatform)
    `maven-publish`
}

val exampleKGroup: String by project
val exampleKVersion: String by project

project.group = exampleKGroup
project.version = "$exampleKVersion.${System.getenv("GITHUB_RUN_NUMBER") ?: "LOCAL"}"

publishing {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/dan-nichols/examplek")
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}

kotlin {
    @Suppress("OPT_IN_USAGE")
    targetHierarchy.default()

    jvmToolchain(8)
    jvm {
        withJava()
    }
    ios()

    sourceSets {
        val commonMain by getting
        val commonTest by getting

        val jvmMain by getting {
            dependencies {
                implementation(libs.ksp.api)
            }
        }
    }
}

buildscript {
    dependencies {
        classpath(kotlin("gradle-plugin", version = libs.versions.kotlin.get()))
    }
}

tasks.create("printVersion") {
    doLast {
        println(project.version)
    }
}
