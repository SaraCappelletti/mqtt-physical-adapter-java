/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    `java-library`
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    api("com.github.spullara.mustache.java:compiler:0.9.10")
    api("ch.qos.logback:logback-classic:1.2.9")
    api("com.google.code.gson:gson:2.10")
    api("io.github.wldt:wldt-core:0.2.1")
    testImplementation("junit:junit:4.13.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}

group = "io.github.wldt"
version = "0.1.0"
description = "Physical adapter to connect with the MQTT protocol"
java.sourceCompatibility = JavaVersion.VERSION_1_8


publishing {
    repositories {
        maven {
            name = "Sonatype"
            val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            credentials {
                username = (properties["ossrhUsername"] as String?)
                password = (properties["ossrhPassword"] as String?)
            }
        }
        maven {
            name = "Local"
            val releasesRepoUrl = layout.buildDirectory.dir("repos/releases")
            val snapshotsRepoUrl = layout.buildDirectory.dir("repos/snapshots")
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
        }
    }
    publications.create<MavenPublication>("WLDTRelease") {
        from(components["java"])

        pom {
            name = "MQTT Physical Adapter"
            url = "https://wldt.github.io/"
            description = project.description;
            licenses {
                license {
                    name = "Apache-2.0 license"
                    url = "https://raw.githubusercontent.com/wldt/mqtt-physical-adapter-java/main/LICENSE"
                }
            }

            developers {
                developer {
                    id = "samubura"
                    name = "Samuele Burattini"
                    email = "samuele.burattini@unibo.it"
                }
                developer {
                    id = "piconem"
                    name = "Marco Picone"
                    email = "picone.m@gmail.com"
                }
            }

            scm {
                connection = "scm:git:https://github.com/wldt/mqtt-physical-adapter-java"
                url = "https://github.com/wldt/mqtt-physical-adapter-java"
            }
        }
    }
}

signing {
    sign(publishing.publications["WLDTRelease"])
}
