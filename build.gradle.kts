import org.gradle.api.JavaVersion.VERSION_21
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.gradle.kotlin.dsl.getByType
import org.gradle.api.tasks.SourceSetContainer

plugins {
    kotlin("jvm") version "2.2.20"
    application
}

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass = "com.olliekennedy.PersonalSiteKt"
}

repositories {
    mavenCentral()
}

tasks {
    withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            allWarningsAsErrors = false
            jvmTarget.set(JVM_21)
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }

    java {
        sourceCompatibility = VERSION_21
        targetCompatibility = VERSION_21
    }

    // Dev hot-reload task
    register<JavaExec>("dev") {
        group = "application"
        description = "Run the hot reload development server on http://localhost:9000"
        val sourceSets = project.extensions.getByType<SourceSetContainer>()
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("com.olliekennedy.DevHotReloadKt")
    }
}

dependencies {
    implementation(platform("org.http4k:http4k-bom:6.18.1.0"))
    implementation("org.http4k.pro:http4k-tools-hotreload")
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-format-jackson")
    implementation("org.http4k:http4k-server-jetty")
    implementation("org.http4k:http4k-template-handlebars")
    testImplementation("org.http4k:http4k-testing-approval")
    testImplementation("org.http4k:http4k-testing-hamkrest")
    testImplementation("org.http4k:http4k-testing-playwright")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.13.3")
    testImplementation("org.junit.platform:junit-platform-launcher:1.13.4")
}
