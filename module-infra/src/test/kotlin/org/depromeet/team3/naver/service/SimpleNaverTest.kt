package org.depromeet.team3.naver.service

import org.depromeet.team3.naver.dto.NaverLocalSearchResponse
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient
import kotlin.test.assertTrue

class SimpleNaverTest {

    @Test
    fun `Naver API 간단 테스트`() {
        val clientId = ""
        val clientSecret = ""
        val baseUrl = "https://openapi.naver.com/v1/search"
        
        val restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeaders { headers ->
                headers.set("X-Naver-Client-Id", clientId)
                headers.set("X-Naver-Client-Secret", clientSecret)
                headers.set("Accept", "application/json")
            }
            .build()

        try {
            val response = restClient
                .get()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/local.json")
                        .queryParam("query", "한식 강남역")
                        .queryParam("display", 3)
                        .queryParam("sort", "comment")
                        .build()
                }
                .retrieve()
                .body(NaverLocalSearchResponse::class.java)

            assertTrue(response != null, "정상 응답 실패")
            
            response.items.forEachIndexed { index, item ->
                println("${index + 1}. ${item.title}")
                println("   카테고리: ${item.category}")
                println("   주소: ${item.address}")
                println("   도로명주소: ${item.roadAddress}")
                println("   전화번호: ${item.telephone ?: "정보 없음"}")
                println("   설명: ${item.description}")
                println("   좌표: (${item.mapX}, ${item.mapY})")
                println("   링크: ${item.link}")
                println()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
