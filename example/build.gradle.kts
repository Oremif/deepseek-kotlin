plugins {
    kotlin("jvm") version "2.1.20"
}

group = "org.oremif.deepseek"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.oremif:deepseek-kotlin:0.3.1")
    implementation("org.slf4j:slf4j-simple:2.0.16")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}