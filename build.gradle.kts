group = "io.github.ifropc.kotomo"
version = "0.0.1-SNAPSHOT"

val jvmVersion = "11"

repositories {
    mavenCentral()
}

plugins {
    kotlin("multiplatform") version "1.6.21"
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = jvmVersion
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js(BOTH) {
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
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
        val jvmMain by getting {
            dependencies {
                implementation("com.esotericsoftware:kryo:5.0.0")
                implementation("org.imgscalr:imgscalr-lib:4.2")
            }
        }
        val jvmTest by getting
        val jsMain by getting
        val jsTest by getting
    }
}
