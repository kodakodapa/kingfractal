plugins {
    kotlin("jvm") version "2.2.0"
    application
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

application {
    mainClass.set("com.github.kodakodapa.kingfractal.MainKt")
}


tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}