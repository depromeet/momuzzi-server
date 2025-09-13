dependencies {
    implementation(project(":module-global-utils"))
    implementation(project(":module-domain"))
    implementation(project(":module-infra"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework:spring-jdbc:6.2.3")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")
    
    testImplementation("org.mockito:mockito-core:5.1.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.1.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.8.20")
    
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
