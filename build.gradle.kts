plugins {
    kotlin("multiplatform") version "1.7.10"
    id("maven-publish")
}

group = "com.offlinebrain"
version = "0.1.4"

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    jvm()
    js {
        browser()
        nodejs()
    }
    mingwX64()
    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0-RC")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}