plugins {
    kotlin("jvm") version "2.4.20"
    id("com.ncorti.ktfmt.gradle") version "0.27.0"
}

group = "org.oremif.deepseek"

version = "1.0-SNAPSHOT"

ktfmt { kotlinLangStyle() }

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.oremif:deepseek-kotlin:0.4.0")
    implementation("org.slf4j:slf4j-simple:2.0.19")
}

tasks.test { useJUnitPlatform() }

kotlin { jvmToolchain(17) }
