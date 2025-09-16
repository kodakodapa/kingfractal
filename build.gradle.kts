plugins {
    kotlin("jvm") version "2.2.0"
}

group = "com.github.kodakodapa"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jocl:jocl:2.0.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.8.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(24)
}