package org.depromeet.team3.place.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import org.depromeet.team3.place.application.SearchPlacesService
import org.depromeet.team3.place.dto.response.PlacesSearchResponse
import org.depromeet.team3.place.exception.PlaceSearchException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlacesSearchControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean(relaxed = true)
    private lateinit var searchPlacesService: SearchPlacesService

    @Test
    fun `맛집 검색 성공`() {
        // given
        val query = "강남역 맛집"
        val mockResponse = PlacesSearchResponse(
            items = listOf(
                PlacesSearchResponse.PlaceItem(
                    name = "맛집 1",
                    address = "서울시 강남구",
                    rating = 4.5,
                    userRatingsTotal = 100,
                    openNow = true,
                    photos = listOf("https://example.com/photo1.jpg"),
                    link = "https://m.place.naver.com/place/list?query=맛집 1",
                    weekdayText = listOf("월요일: 10:00~22:00"),
                    topReview = PlacesSearchResponse.PlaceItem.Review(
                        rating = 5,
                        text = "정말 맛있어요!"
                    ),
                    priceRange = null,
                    addressDescriptor = null
                )
            )
        )

        coEvery { searchPlacesService.textSearch(any()) } returns mockResponse

        // when & then
        val mvcResult = mockMvc.perform(get("/api/v1/places").param("query", query))
            .andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].name").value("맛집 1"))
    }


    @Test
    fun `맛집 검색 - 빈 결과 반환`() {
        // given
        val query = "존재하지않는맛집"
        val mockResponse = PlacesSearchResponse(items = emptyList())

        coEvery { searchPlacesService.textSearch(any()) } returns mockResponse

        // when & then
        val mvcResult = mockMvc.perform(get("/api/v1/places").param("query", query))
            .andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items").isEmpty)
    }

    @Test
    fun `맛집 검색 실패 - 필수 파라미터 누락`() {
        // when & then
        mockMvc.perform(get("/api/v1/places"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `맛집 검색 실패 - 서비스 에러 발생`() {
        // given
        val query = "강남역 맛집"

        coEvery { searchPlacesService.textSearch(any()) } throws PlaceSearchException("맛집 검색 중 오류가 발생했습니다")

        // when & then
        val mvcResult = mockMvc.perform(get("/api/v1/places").param("query", query))
            .andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().is5xxServerError)
    }

    @Test
    fun `맛집 검색 - 여러 항목 반환`() {
        // given
        val query = "강남역 맛집"
        val mockResponse = PlacesSearchResponse(
            items = (1..5).map { index ->
                PlacesSearchResponse.PlaceItem(
                    name = "맛집 $index",
                    address = "서울시 강남구 $index",
                    rating = 4.0 + (index * 0.1),
                    userRatingsTotal = 100 * index,
                    openNow = index % 2 == 0,
                    photos = listOf("https://example.com/photo$index.jpg"),
                    link = "https://m.place.naver.com/place/list?query=맛집 $index",
                    weekdayText = listOf("월요일: 10:00~22:00"),
                    topReview = PlacesSearchResponse.PlaceItem.Review(
                        rating = 4 + index % 2,
                        text = "리뷰 $index"
                    ),
                    priceRange = PlacesSearchResponse.PlaceItem.PriceRange(
                        startPrice = "KRW ${10000 * index}",
                        endPrice = "KRW ${20000 * index}"
                    ),
                    addressDescriptor = PlacesSearchResponse.PlaceItem.AddressDescriptor(
                        description = "강남역 근처"
                    )
                )
            }
        )

        coEvery { searchPlacesService.textSearch(any()) } returns mockResponse

        // when & then
        val mvcResult = mockMvc.perform(get("/api/v1/places").param("query", query))
            .andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items.length()").value(5))
            .andExpect(jsonPath("$.data.items[0].name").value("맛집 1"))
            .andExpect(jsonPath("$.data.items[4].name").value("맛집 5"))
    }

    @Test
    fun `맛집 검색 - 특수문자 포함 쿼리`() {
        // given
        val query = "강남역 맛집 & 카페"
        val mockResponse = PlacesSearchResponse(
            items = listOf(
                PlacesSearchResponse.PlaceItem(
                    name = "카페 & 맛집",
                    address = "서울시 강남구",
                    rating = 4.5,
                    userRatingsTotal = 50,
                    openNow = true,
                    photos = listOf("https://example.com/photo.jpg"),
                    link = "https://m.place.naver.com/place/list?query=카페 & 맛집",
                    weekdayText = null,
                    topReview = null,
                    priceRange = null,
                    addressDescriptor = null
                )
            )
        )

        coEvery { searchPlacesService.textSearch(any()) } returns mockResponse

        // when & then
        val mvcResult = mockMvc.perform(get("/api/v1/places").param("query", query))
            .andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].name").value("카페 & 맛집"))
    }
}
