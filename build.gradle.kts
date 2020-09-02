import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    `maven-publish`
}

repositories {
    mavenLocal()
    maven("https://packages.confluent.io/maven/")
    mavenCentral()
}

tasks {
    jar {
        enabled = true
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:unchecked")
    options.compilerArgs.add("-Xlint:deprecation")
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events = mutableSetOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        info.events = events + mutableSetOf(TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR)
        showExceptions = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}

dependencies {
    implementation("org.apache.avro:avro:1.9.2")
}

publishing {
    publications {
        create<MavenPublication>("avro-json-encoder") {
            groupId = "com.camerongivler"
            artifactId = "avro-json-encoder"
            version = "1.5"

            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/camerongivler/avro-json-encoder")
            version = "1.5"
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

