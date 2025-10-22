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
        targetCompatibility = "17"
        sourceCompatibility = "17"
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "17"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        
        // 테스트 리포트 설정
        reports {
            junitXml.required.set(true)
            html.required.set(true)
        }
        
        // 테스트 실패해도 계속 진행 (전체 리포트 생성을 위해)
        ignoreFailures = true
        
        // CI 환경에서만 테스트 캐시 비활성화 (로컬 개발 성능 저하 방지)
        val isCI = System.getenv("CI")?.toBoolean() ?: false
        if (isCI) {
            outputs.upToDateWhen { false }
        }
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
