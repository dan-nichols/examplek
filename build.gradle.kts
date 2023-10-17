import org.gradle.configurationcache.extensions.capitalized

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.ksp)
    `maven-publish`
}

val exampleKGroup: String by project
val exampleKVersion: String by project

project.group = exampleKGroup
project.version = "$exampleKVersion.${System.getenv("GITHUB_RUN_NUMBER") ?: "LOCAL"}"

publishing {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/rollvault/examplek")
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

        val commonTest by getting {
            dependencies {
                implementation(libs.test)
                kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.ksp.api)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.5.0")
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

dependencies {
    val testSourceSets = kotlin.sourceSets.filter {
        it.name.endsWith("Test")
    }

    val parents = testSourceSets.flatMap { sourceSet ->
        sourceSet.dependsOn.map { it.name }
    }

    testSourceSets.map { it.name }
        .filter { !parents.contains(it) }
        .forEach {
            add("ksp${it.capitalized()}", project)
        }
}

