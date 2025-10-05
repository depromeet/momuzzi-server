package org.depromeet.team3.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.depromeet.team3.common.NaverApiProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestClient

/**
 *  혹시 모르니 일단 둘게요
 */
@Configuration
@EnableConfigurationProperties(NaverApiProperties::class)
class NaverMapRestClientConfiguration(
    private val properties: NaverApiProperties
) {

    private val logger = KotlinLogging.logger { NaverMapRestClientConfiguration::class.java.name }

    companion object {
        const val MAX_CONNECTION_TOTAL = 3
        const val MAX_PER_ROUTE = 5
    }

    @Bean
    fun naverMapLocalSearchRestClient(): RestClient {
        return RestClient.builder()
            .requestFactory(httpRequestFactory())
            .baseUrl(properties.localSearchBaseUrl)
            .defaultHeaders { headers ->
                headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                headers.set("X-Naver-Client-Id", properties.clientId)
                headers.set("X-Naver-Client-Secret", properties.clientSecret)
            }
            .defaultStatusHandler(HttpStatusCode::is5xxServerError) { request, response ->
                logger.error {
                    "Naver Map Local Search API request failed. " +
                        "Status: ${response.statusCode}, " +
                        "URL: ${request.uri}, " +
                        "Method: ${request.method}"
                }
            }
            .build()
    }

    @Bean
    fun httpRequestFactory(): ClientHttpRequestFactory {
        val httpClient = HttpClients.custom()
            .setConnectionManager(pollConnectionManager())
            .build()

        return HttpComponentsClientHttpRequestFactory(httpClient)
    }

    @Bean
    fun pollConnectionManager(): PoolingHttpClientConnectionManager {
        val manager = PoolingHttpClientConnectionManager()
        manager.maxTotal = MAX_CONNECTION_TOTAL
        manager.defaultMaxPerRoute = MAX_PER_ROUTE

        return manager
    }
}
