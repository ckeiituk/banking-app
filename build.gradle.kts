plugins {
    kotlin("jvm") version "2.0.0" // Версия Kotlin должна совпадать с версиями библиотек
    kotlin("plugin.serialization") version "2.0.0"
    application
    id("org.openjfx.javafxplugin") version "0.0.10"
}

group = "ru.banking"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
    implementation("org.jetbrains.exposed:exposed-core:0.50.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.50.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.50.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.50.1")
    runtimeOnly("mysql:mysql-connector-java:8.0.33")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("no.tornado:tornadofx:1.7.20")
    implementation("org.openjfx:javafx-controls:17.0.1")
    implementation("org.openjfx:javafx-fxml:17.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.5.30")
    testImplementation("junit:junit:4.13.2")

    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("org.slf4j:slf4j-simple:1.7.30")
}

javafx {
    version = "17.0.1"
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("ru.banking.MainKt")
}
