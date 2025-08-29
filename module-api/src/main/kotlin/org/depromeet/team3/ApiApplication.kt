package org.depromeet.team3

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = [
        "org.depromeet.team3"
    ]
)
class ApiApplication

fun main(args: Array<String>) {
    runApplication<ApiApplication>(*args)
}
