plugins {
    kotlin("jvm") version "2.2.0"
}

group = "com.github.kodakodapa"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jocl:jocl:2.0.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}


tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(24)
}