@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jreleaser.model.Active

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.jreleaser)
    `maven-publish`
    signing
}

group = "org.oremif"
version = "0.3.1"

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
        val commonMain by getting {
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
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.coroutines.test)
            }
        }

        val jvmMain by getting {
            dependencies {
                api(libs.ktor.client.okhttp)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.slf4j.simple)
            }
        }

        val androidMain by getting {
            dependencies {
                api(libs.ktor.client.okhttp)
            }
        }

        val appleMain by creating {
            dependencies {
                api(libs.ktor.client.darwin)
            }
        }

        val wasmJsMain by getting {
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

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications.withType(MavenPublication::class).all {
        if (name.contains("jvm", ignoreCase = true)) {
            artifact(javadocJar)
        }
        pom.configureMavenCentralMetadata()
        signPublicationIfKeyPresent()
    }

    repositories {
        maven(url = layout.buildDirectory.dir("staging-deploy"))
    }
}

jreleaser {
    gitRootSearch = true
    strict.set(true)

    signing {
        active.set(Active.ALWAYS)
        armored = true
        artifacts = true
    }

    deploy {
        active.set(Active.ALWAYS)
        maven {
            active.set(Active.ALWAYS)
            mavenCentral {
                val ossrh by creating {
                    active.set(Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    applyMavenCentralRules = false
                    maxRetries = 240
                    stagingRepository(layout.buildDirectory.dir("staging-deploy").get().asFile.path)
                    // workaround: https://github.com/jreleaser/jreleaser/issues/1784
                    kotlin.targets.forEach { target ->
                        if (target !is KotlinJvmTarget && target !is KotlinAndroidTarget && target !is KotlinMetadataTarget) {
                            val klibArtifactId = if (target.platformType == KotlinPlatformType.wasm) {
                                "${name}-wasm-${target.name.lowercase().substringAfter("wasm")}"
                            } else {
                                "${name}-${target.name.lowercase()}"
                            }
                            artifactOverride {
                                artifactId = klibArtifactId
                                jar = false
                                verifyPom = false
                                sourceJar = false
                                javadocJar = false
                            }
                        }
                    }
                }
            }
        }
    }

    release {
        github {
            skipRelease = true
            skipTag = true
            overwrite = false
            token = "none"
        }
    }

    checksum {
        individual = false
        artifacts = false
        files = false
    }
}

fun MavenPom.configureMavenCentralMetadata() {
    name by project.name
    description by "DeepSeek Multiplatform Kotlin SDK"
    url by "https://github.com/Oremif/deepseek-kotlin"

    licenses {
        license {
            name by "The Apache Software License, Version 2.0"
            url by "https://github.com/Oremif/deepseek-kotlin/blob/master/LICENSE"
            distribution by "repo"
        }
    }

    developers {
        developer {
            id by "devcrocod"
            name by "Pavel Gorgulov"
            organization by "Oremif"
            organizationUrl by "https://oremif.org"
        }
    }

    scm {
        url by "https://github.com/Oremif/deepseek-kotlin"
        connection by "scm:git:git://github.com/Oremif/deepseek-kotlin.git"
        developerConnection by "scm:git:git@github.com/Oremif/deepseek-kotlin.git"
    }
}

fun MavenPublication.signPublicationIfKeyPresent() {
//    val keyId = project.getSensitiveProperty("GPG_PUBLIC_KEY")
    val signingKey = project.getSensitiveProperty("GPG_SECRET_KEY")
    val signingKeyPassphrase = project.getSensitiveProperty("SIGNING_PASSPHRASE")

    if (!signingKey.isNullOrBlank()) {
        the<SigningExtension>().apply {
            useInMemoryPgpKeys(signingKey, signingKeyPassphrase)

            sign(this@signPublicationIfKeyPresent)
        }
    }
}

fun Project.getSensitiveProperty(name: String?): String? {
    if (name == null) {
        error("Expected not null property '$name' for publication repository config")
    }

    return project.findProperty(name) as? String
        ?: System.getenv(name)
        ?: System.getProperty(name)
}

infix fun <T> Property<T>.by(value: T) {
    set(value)
}
