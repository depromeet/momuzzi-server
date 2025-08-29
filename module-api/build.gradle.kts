dependencies {
    implementation(project(":module-global-utils"))
    implementation(project(":module-domain"))
    implementation(project(":module-infra"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework:spring-jdbc:6.2.3")
    implementation("org.springframework.cloud:spring-cloud-starter-config")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")
    
    runtimeOnly("com.h2database:h2")
}

tasks {
    jar {
        enabled = false
    }
    bootJar {
        enabled = true
    }
}
