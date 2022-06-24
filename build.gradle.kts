group = "io.github.ifropc.kotomo"
version = "0.1"

val jvmVersion = "1.8"

repositories {
    mavenCentral()
}

plugins {
    kotlin("multiplatform") version "1.6.21"
    kotlin("plugin.serialization") version "1.6.21"
    id("org.jetbrains.dokka") version "1.6.21"
    `maven-publish`
    signing
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
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
            webpackTask {
                output.libraryTarget = "commonjs2"
            }
            testTask {
                useMocha { }
//                useKarma {
//                    useFirefox()
//                    // TODO: enable chrome
////                    useChrome()
//                }
            }
            binaries.executable()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
                implementation("io.github.microutils:kotlin-logging:2.1.23")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.1")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.imgscalr:imgscalr-lib:4.2")
                implementation("ch.qos.logback:logback-classic:1.0.13")
            }
        }
        val jvmTest by getting
        val jsMain by getting {
            dependencies {
                implementation(npm("image-js", "0.34.1"))
            }
        }
        val jsTest by getting
    }
}

val sonatypeUsername: String = extra["ossUser"] as String
val sonatypePassword: String = extra["ossToken"] as String

val dokkaOutputDir = buildDir.resolve("dokka")

tasks.dokkaHtml {
    outputDirectory.set(dokkaOutputDir)
}

val deleteDokkaOutputDir by tasks.register<Delete>("deleteDokkaOutputDirectory") {
    delete(dokkaOutputDir)
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    dependsOn(deleteDokkaOutputDir, tasks.dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaOutputDir)
}

publishing {
    repositories {
        maven {
            name="oss"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = sonatypeUsername
                password = sonatypePassword
            }
        }
    }

    publications {
        withType<MavenPublication> {
            artifact(javadocJar)
            pom {
                name.set("Kotomo")
                description.set("Kotomo OCR Core")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                url.set("https://github.com/Ifropc/Kotomo")
                issueManagement {
                    system.set("Github")
                    url.set("https://github.com/Ifropc/Kotomo/issues")
                }
                scm {
                    connection.set("https://github.com/Ifropc/Kotomo.git")
                    url.set("https://github.com/Ifropc/Kotomo")
                }
                developers {
                    developer {
                        name.set("Gleb")
                        email.set("ifropc@apache.org")
                    }
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}
