package org.depromeet.team3.naver.service

import org.depromeet.team3.naver.dto.NaverLocalSearchItem
import org.depromeet.team3.naver.dto.NaverLocalSearchResponse
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NaverMapServiceTest {

    @Test
    fun `Mock 테스트`() {
        // given
        val mockResponse = createMockResponse()

        // when & then
        assertNotNull(mockResponse)
        assertEquals(2, mockResponse.items.size)
        assertEquals("한식당1", mockResponse.items[0].title)
        assertEquals("한식", mockResponse.items[0].category)
        assertEquals("강남구 테헤란로 123", mockResponse.items[0].address)
        assertEquals("02-1234-5678", mockResponse.items[0].telephone)
        assertEquals("127.027619", mockResponse.items[0].mapX)
        assertEquals("37.497952", mockResponse.items[0].mapY)
        
        mockResponse.items.forEachIndexed { index, item ->
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
    }

    @Test
    fun `NaverLocalSearchItem 필드 검증 테스트`() {
        // given
        val item = NaverLocalSearchItem(
            title = "테스트 식당",
            link = "https://map.naver.com/v5/entry/place/1234567890",
            category = "한식",
            description = "테스트용 식당입니다",
            telephone = "02-1234-5678",
            address = "서울시 강남구 테스트로 123",
            roadAddress = "서울특별시 강남구 테스트로 123",
            mapX = "127.027619",
            mapY = "37.497952"
        )

        // when & then
        assertNotNull(item)
        assertEquals("테스트 식당", item.title)
        assertEquals("한식", item.category)
        assertEquals("서울시 강남구 테스트로 123", item.address)
        assertEquals("서울특별시 강남구 테스트로 123", item.roadAddress)
        assertEquals("02-1234-5678", item.telephone)
        assertEquals("127.027619", item.mapX)
        assertEquals("37.497952", item.mapY)

        println("식당명: ${item.title}")
        println("카테고리: ${item.category}")
        println("주소: ${item.address}")
        println("도로명주소: ${item.roadAddress}")
        println("전화번호: ${item.telephone}")
        println("좌표: (${item.mapX}, ${item.mapY})")
        println("설명: ${item.description}")
        println("링크: ${item.link}")
        println()
    }

    @Test
    fun `음식 카테고리별 검색 시나리오 테스트`() {
        // given
        val categories = listOf("한식", "중식", "일식", "양식", "카페")
        
        println("=== 음식 카테고리별 검색 시나리오 테스트 ===")
        
        // when & then
        categories.forEach { category ->
            val response = createMockResponseForCategory(category)
            
            assertNotNull(response)
            assertNotNull(response.items)
            assertTrue(response.items.isNotEmpty(), "$category 카테고리 검색 결과 없음")
            
            response.items.forEach { item ->
                assertNotNull(item.title, "식당 이름 없음")
                assertNotNull(item.category, "카테고리 없음")
                assertNotNull(item.address, "주소 없음")
                assertNotNull(item.mapX, "X 좌표 없음")
                assertNotNull(item.mapY, "Y 좌표 없음")
                
                println("   🍽️  ${item.title}")
                println("      카테고리: ${item.category}")
                println("      주소: ${item.address}")
                println("      전화번호: ${item.telephone ?: "정보 없음"}")
                println("      좌표: (${item.mapX}, ${item.mapY})")
            }
        }
    }

    private fun createMockResponse(): NaverLocalSearchResponse {
        return NaverLocalSearchResponse(
            lastBuildDate = "2024-01-01T00:00:00.000+09:00",
            total = 2,
            start = 1,
            display = 2,
            items = listOf(
                NaverLocalSearchItem(
                    title = "한식당1",
                    link = "https://map.naver.com/v5/entry/place/1234567890",
                    category = "한식",
                    description = "맛있는 한식당입니다",
                    telephone = "02-1234-5678",
                    address = "강남구 테헤란로 123",
                    roadAddress = "서울특별시 강남구 테헤란로 123",
                    mapX = "127.027619",
                    mapY = "37.497952"
                ),
                NaverLocalSearchItem(
                    title = "한식당2",
                    link = "https://map.naver.com/v5/entry/place/0987654321",
                    category = "한식",
                    description = "전통 한식당입니다",
                    telephone = "02-9876-5432",
                    address = "강남구 역삼동 456",
                    roadAddress = "서울특별시 강남구 역삼동 456",
                    mapX = "127.028000",
                    mapY = "37.498000"
                )
            )
        )
    }

    private fun createMockResponseForCategory(category: String): NaverLocalSearchResponse {
        return NaverLocalSearchResponse(
            lastBuildDate = "2024-01-01T00:00:00.000+09:00",
            total = 1,
            start = 1,
            display = 1,
            items = listOf(
                NaverLocalSearchItem(
                    title = "${category}당",
                    link = "https://map.naver.com/v5/entry/place/1234567890",
                    category = category,
                    description = "맛있는 ${category}당입니다",
                    telephone = "02-1234-5678",
                    address = "강남구 테헤란로 123",
                    roadAddress = "서울특별시 강남구 테헤란로 123",
                    mapX = "127.027619",
                    mapY = "37.497952"
                )
            )
        )
    }
}
