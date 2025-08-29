dependencies {
    implementation(project(":module-global-utils"))
    implementation(project(":module-domain"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}

tasks {
    jar {
        enabled = true
    }
    bootJar {
        enabled = false
    }
}
