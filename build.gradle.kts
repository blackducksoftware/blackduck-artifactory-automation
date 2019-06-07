import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        jcenter()
        mavenCentral()
        maven(url = "https://plugins.gradle.org/m2/")
    }

    dependencies {
        classpath("com.blackducksoftware.integration:common-gradle-plugin:0.0.+")
    }
}

plugins {
    kotlin("jvm") version "1.3.31"
}

group = "com.synopsys.integration"
version = "0.0.1-SNAPSHOT"

apply(plugin = "com.blackducksoftware.integration.simple")

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}