dependencies {
    implementation(project(":module-global-utils"))
    implementation(project(":module-domain"))
    implementation(project(":module-infra"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework:spring-jdbc:6.2.3")

    // JWT 관련
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")
    
    // Jackson Kotlin 모듈
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Caffeine Cache
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    testImplementation("org.mockito:mockito-core:5.1.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.1.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.8.20")
    
    runtimeOnly("com.mysql:mysql-connector-j")
    
    // 테스트용 H2 데이터베이스
    testRuntimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.security:spring-security-test")
}

tasks {
    jar {
        enabled = false
    }
    bootJar {
        enabled = true
    }
}
