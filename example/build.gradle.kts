plugins {
    kotlin("jvm") version "2.3.20"
}

group = "org.oremif.deepseek"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.oremif:deepseek-kotlin:0.4.0")
    implementation("org.slf4j:slf4j-simple:2.0.17")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}