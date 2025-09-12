@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

group = "org.oremif"
version = "0.3.3"

kotlin {
    explicitApi()

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    macosX64()
    macosArm64()
    linuxX64()
    linuxArm64()
    mingwX64()

    wasmJs {
        nodejs() {
            testTask {
                useMocha {
                    timeout = "30s"
                }
            }
        }
        browser() {
            testTask {
                useMocha {
                    timeout = "30s"
                }
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api(libs.coroutines.core)
                api(libs.ktor.client.core)
                api(libs.ktor.client.auth)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.serialization.json)
                implementation(libs.serialization.json)
                implementation(libs.ktor.client.logging)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.coroutines.test)
            }
        }

        jvmMain {
            dependencies {
                api(libs.ktor.client.okhttp)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.slf4j.simple)
            }
        }

        androidMain {
            dependencies {
                api(libs.ktor.client.okhttp)
            }
        }

        appleMain {
            dependencies {
                api(libs.ktor.client.darwin)
            }
        }

        linuxMain.dependencies {
            api(libs.ktor.client.cio)
        }

        macosMain.dependencies {
            api(libs.ktor.client.cio)
        }

        mingwMain.dependencies {
            api(libs.ktor.client.cio)
        }

        wasmJsMain {
            dependencies {
                api(libs.ktor.client.js)
            }
        }
    }
}

android {
    namespace = "org.oremif.deepseek"
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}


dokka {
    moduleName.set("DeepSeek Kotlin SDK")

    pluginsConfiguration.html {
        footerMessage = "© 2025 Oremif. All rights reserved."
        customAssets.from(listOf(file("../dokka/images/logo-icon.svg")))
        customStyleSheets.from(listOf(file("../dokka/styles/logo-styles.css")))
    }

    dokkaSourceSets.configureEach {
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/Oremif/deepseek-kotlin")
            remoteLineSuffix.set("#L")
            documentedVisibilities(VisibilityModifier.Public)
        }
    }
    dokkaPublications.html {
        outputDirectory = project.rootProject.layout.projectDirectory.dir("docs")
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    pom {
        name = "DeepSeek Kotlin SDK"
        description = "DeepSeek Multiplatform Kotlin SDK"
        inceptionYear = "2025"
        url = "https://github.com/Oremif/deepseek-kotlin"

        licenses {
            license {
                name = "The Apache Software License, Version 2.0"
                url = "https://github.com/Oremif/deepseek-kotlin/blob/master/LICENSE"
                distribution = "repo"
            }
        }

        developers {
            developer {
                id = "devcrocod"
                name = "Pavel Gorgulov"
                organization = "Oremif"
                organizationUrl = "https://oremif.org"
            }
        }

        scm {
            url = "https://github.com/Oremif/deepseek-kotlin"
            connection = "scm:git:git://github.com/Oremif/deepseek-kotlin.git"
            developerConnection = "scm:git:git@github.com/Oremif/deepseek-kotlin.git"
        }
    }
}
