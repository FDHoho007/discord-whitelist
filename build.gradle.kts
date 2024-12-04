plugins {
    kotlin("jvm") version "2.0.21"
}

group = "de.fdhoho007"
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.3-R0.1-SNAPSHOT")
    implementation("net.dv8tion:JDA:5.2.1") {
        exclude(module="opus-java")
    }
    implementation("club.minnced:jda-ktx:0.12.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}