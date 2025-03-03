@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.dokka)
//    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "io.github.oremif"
version = "0.0.1"

kotlin {
    explicitApi()

    jvm()

    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    linuxX64()

    wasmJs {
        browser()
        nodejs()
        d8()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.serialization.core)
                implementation(libs.serialization.json)
                implementation(libs.coroutines.core)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.auth)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.serialization.json)
                implementation(libs.kotlinx.datetime)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.coroutines.test)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
    }
}

android {
    namespace = "org.jetbrains.kotlinx.multiplatform.library.template"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

//mavenPublishing {
//    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
//
//    signAllPublications()
//
//    coordinates(group.toString(), "library", version.toString())
//
//    pom {
//        name = "deepseek-kotlin"
//        description = "DeepSeek Kotlin SDK"
//        inceptionYear = "2025"
//        url = "https://github.com/oremif/deepseek-kotlin/"
//        licenses {
//            license {
//                name = "XXX"
//                url = "YYY"
//                distribution = "ZZZ"
//            }
//        }
//        developers {
//            developer {
//                id = "devcrocod"
//                name = "Pavel Gorgulov"
//                url = "https://github.com/devcrocod"
//            }
//        }
//        scm {
//            url = "XXX" // TODO
//            connection = "YYY"
//            developerConnection = "ZZZ"
//        }
//    }
//}