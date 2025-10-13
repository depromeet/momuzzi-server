dependencies {
    implementation(project(":module-global-utils"))
    implementation(project(":module-domain"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.linecorp.kotlin-jdsl:jpql-dsl:3.5.5")
    implementation("com.linecorp.kotlin-jdsl:jpql-render:3.5.5")
    implementation("com.linecorp.kotlin-jdsl:hibernate-support:3.5.5")
    implementation("com.linecorp.kotlin-jdsl:spring-data-jpa-support:3.5.5")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")

    // HTTP Client
    implementation("org.apache.httpcomponents.client5:httpclient5:5.3.1")
    implementation("org.apache.httpcomponents.core5:httpcore5:5.2.4")
    
    // Kotlin Logging
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    
    // Test Dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
}

tasks {
    jar {
        enabled = true
    }
    bootJar {
        enabled = false
    }
}
