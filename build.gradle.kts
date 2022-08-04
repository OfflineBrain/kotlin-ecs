plugins {
    kotlin("multiplatform") version "1.7.10"
}

group = "com.offlinebrain"
version = "0.1"

repositories {
    mavenCentral()
}

kotlin {
    js(BOTH) {
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
        }
    }

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    mingwX64 {
        binaries {
            sharedLib {
                baseName = "offlinebrain-ecs"
            }
        }
    }
    linuxX64 {
        binaries {
            sharedLib {
                baseName = "offlinebrain-ecs"
            }
        }
    }
    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}