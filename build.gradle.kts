plugins {
    id("org.springframework.boot") version "3.4.9" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("jvm") version "1.9.25" apply false
    kotlin("plugin.spring") version "1.9.25" apply false
    kotlin("plugin.jpa") version "1.9.25" apply false
    kotlin("kapt") version "1.9.25" apply false
}

// 모든 프로젝트 공통 설정
allprojects {
    group = "org.depromeet"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

// 하위 프로젝트에만 적용
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.1")
        }
    }

    // tasks 설정
    tasks.withType<JavaCompile> {
        targetCompatibility = "21"
        sourceCompatibility = "21"
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "21"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    afterEvaluate {
        dependencies {
            add("testImplementation", "org.junit.jupiter:junit-jupiter:5.9.2")
            add("testImplementation", "org.jetbrains.kotlin:kotlin-test-junit5:1.8.20")
            add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")
        }
    }
}

// 루트 프로젝트 빌드 비활성화
tasks.configureEach {
    onlyIf { false }
}
