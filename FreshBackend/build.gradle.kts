import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.spring") version "1.9.20"
    kotlin("plugin.jpa") version "1.9.20"
    id("org.flywaydb.flyway") version "9.22.3" // для миграций БД
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.security:spring-security-crypto")
    implementation("org.postgresql:postgresql") // Уже должен быть
    implementation("org.flywaydb:flyway-core:10.18.0") // <-- Вот ЭТА версия
    implementation("org.flywaydb:flyway-database-postgresql:10.18.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin") // Уже должен быть
    implementation("org.jetbrains.kotlin:kotlin-reflect") // Уже должен быть
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8") // Уже должен быть
    implementation("com.auth0:java-jwt:4.4.0")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // PostgreSQL
    runtimeOnly("org.postgresql:postgresql")

    // Flyway

    // HTTP Client для Traccar
    implementation("org.springframework:spring-webflux") // WebClient

    // FCM (Firebase Admin SDK)
    implementation("com.google.firebase:firebase-admin:9.2.0")

    // Jackson Kotlin Module (для корректной работы с Kotlin Data Classes)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Kotlin Reflection
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Logging
    implementation("org.springframework.boot:spring-boot-starter-logging")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")

    // FaceCheck
    implementation("org.bytedeco:javacv-platform:1.5.10") // Укажите актуальную версию
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}


tasks.withType<Test> {
    useJUnitPlatform()
}



// Flyway task configuration (опционально, можно настроить через application.properties)
flyway {
    url = "jdbc:postgresql://localhost:5432/probationmob"
    user = "postgres"
    password = "556055"
}