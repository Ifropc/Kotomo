import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "io.github.ifropc.kotomo"
version = "0.0.1-SNAPSHOT"

val jvmVersion = "11"

plugins {
    application
    kotlin("jvm") version "1.6.21"
}


dependencies {
    implementation("com.esotericsoftware:kryo:5.0.0")
    implementation("org.imgscalr:imgscalr-lib:4.2")
//    testImplementation("kotlin-test-junit")
}

repositories {
    mavenLocal()
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = jvmVersion
}

kotlin {
    sourceSets {
        val test by getting {
            dependencies {
                implementation(kotlin("test")) // This brings all the platform dependencies automatically
            }
        }
    }
}
