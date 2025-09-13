dependencies {
    implementation(project(":module-global-utils"))
    implementation(project(":module-domain"))
    implementation(project(":module-infra"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework:spring-jdbc:6.2.3")

    // JWT 관련
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")
    
    runtimeOnly("com.mysql:mysql-connector-j")
}

tasks {
    jar {
        enabled = false
    }
    bootJar {
        enabled = true
    }
}
