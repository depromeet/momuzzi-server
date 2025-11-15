package org.depromeet.team3.place

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.depromeet.team3.place.client.GooglePlacesClient
import org.depromeet.team3.place.model.PlaceDetailsResponse
import org.depromeet.team3.place.util.PlaceAddressResolver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.Collections

/**
 * PlaceQuery 의 코루틴 병렬 처리 및 부분 실패 처리 통합 테스트
 */
class PlaceQueryRetryTest {

    private lateinit var googlePlacesClient: GooglePlacesClient
    private lateinit var placeJpaRepository: PlaceJpaRepository
    private lateinit var placeAddressResolver: PlaceAddressResolver
    private lateinit var placeQuery: PlaceQuery

    @BeforeEach
    fun setUp() {
        googlePlacesClient = mock()
        placeJpaRepository = mock()
        placeAddressResolver = mock()

        placeQuery = PlaceQuery(
            googlePlacesClient = googlePlacesClient,
            placeJpaRepository = placeJpaRepository,
            placeAddressResolver = placeAddressResolver
        )

        whenever(placeJpaRepository.saveAll(any<Iterable<PlaceEntity>>())).thenAnswer {
            (it.arguments[0] as Iterable<PlaceEntity>).toList()
        }
    }

    @Test
    fun `배치 조회 - 업데이트 필요한 장소 병렬 처리 성공`(): Unit = runBlocking {
        // given
        val placeIds = listOf("place_1", "place_2", "place_3")
        val cachedPlaces = listOf(
            createPlaceEntity("place_1", photos = null),
            createPlaceEntity("place_2", photos = null),
            createPlaceEntity("place_3", addressDescriptor = null)
        )

        whenever(placeJpaRepository.findByGooglePlaceIdIn(placeIds)).thenReturn(cachedPlaces)
        whenever(placeAddressResolver.isValidAddressDescriptor(any())).thenReturn(false)

        val updateResponse1 = createPlaceDetailsResponse("place_1", "맛집 1")
        val updateResponse2 = createPlaceDetailsResponse("place_2", "맛집 2")
        val updateResponse3 = createPlaceDetailsResponse("place_3", "맛집 3")

        whenever(googlePlacesClient.getPlaceDetails("place_1")).thenReturn(updateResponse1)
        whenever(googlePlacesClient.getPlaceDetails("place_2")).thenReturn(updateResponse2)
        whenever(googlePlacesClient.getPlaceDetails("place_3")).thenReturn(updateResponse3)

        whenever(placeAddressResolver.resolveAddressDescriptor(any())).thenReturn(null)

        // when
        val result = placeQuery.getPlaceDetailsBatch(placeIds)

        // then
        assertThat(result).hasSize(3)
        assertThat(result.keys).containsExactlyInAnyOrder("place_1", "place_2", "place_3")
        println("[PlaceQueryRetryTest] 업데이트 필요한 장소 처리 → keys=${result.keys}")

        verify(googlePlacesClient, times(3)).getPlaceDetails(any())
    }

    @Test
    fun `배치 조회 - 부분 실패 처리 (일부 장소만 실패)`(): Unit = runBlocking {
        val placeIds = listOf("place_1", "place_2", "place_3")

        whenever(placeJpaRepository.findByGooglePlaceIdIn(placeIds)).thenReturn(emptyList())

        val successResponse1 = createPlaceDetailsResponse("place_1", "맛집 1")
        val successResponse3 = createPlaceDetailsResponse("place_3", "맛집 3")

        whenever(googlePlacesClient.getPlaceDetails("place_1")).thenReturn(successResponse1)
        whenever(googlePlacesClient.getPlaceDetails("place_2")).thenThrow(
            org.depromeet.team3.place.exception.PlaceSearchException(
                org.depromeet.team3.common.exception.ErrorCode.PLACE_DETAILS_FETCH_FAILED
            )
        )
        whenever(googlePlacesClient.getPlaceDetails("place_3")).thenReturn(successResponse3)

        whenever(placeAddressResolver.resolveAddressDescriptor(any())).thenReturn(null)

        val result = placeQuery.getPlaceDetailsBatch(placeIds)

        assertThat(result).hasSize(2)
        assertThat(result.keys).containsExactlyInAnyOrder("place_1", "place_3")
        assertThat(result.keys).doesNotContain("place_2")
        println("[PlaceQueryRetryTest] 부분 실패 처리 → keys=${result.keys}")

        verify(googlePlacesClient, times(3)).getPlaceDetails(any())
    }

    @Test
    fun `배치 조회 - 모든 장소 실패 시 빈 결과 반환`(): Unit = runBlocking {
        val placeIds = listOf("place_1", "place_2", "place_3")

        whenever(placeJpaRepository.findByGooglePlaceIdIn(placeIds)).thenReturn(emptyList())
        whenever(googlePlacesClient.getPlaceDetails(any())).thenThrow(
            org.depromeet.team3.place.exception.PlaceSearchException(
                org.depromeet.team3.common.exception.ErrorCode.PLACE_DETAILS_FETCH_FAILED
            )
        )

        val result = placeQuery.getPlaceDetailsBatch(placeIds)

        assertThat(result).isEmpty()
        println("[PlaceQueryRetryTest] 모든 장소 실패 → size=${result.size}")
        verify(googlePlacesClient, times(3)).getPlaceDetails(any())
    }

    @Test
    fun `배치 조회 - DB 캐시와 API 호출 혼합`(): Unit = runBlocking {
        val placeIds = listOf("place_1", "place_2", "place_3", "place_4")

        val cachedPlaces = listOf(
            createPlaceEntity("place_1", photos = "photo1", recentlyUpdated = true),
            createPlaceEntity("place_2", photos = "photo2", recentlyUpdated = true)
        )

        whenever(placeJpaRepository.findByGooglePlaceIdIn(placeIds)).thenReturn(cachedPlaces)
        whenever(placeAddressResolver.isValidAddressDescriptor(any())).thenReturn(true)

        val apiResponse3 = createPlaceDetailsResponse("place_3", "맛집 3")
        val apiResponse4 = createPlaceDetailsResponse("place_4", "맛집 4")

        whenever(googlePlacesClient.getPlaceDetails("place_3")).thenReturn(apiResponse3)
        whenever(googlePlacesClient.getPlaceDetails("place_4")).thenReturn(apiResponse4)

        whenever(placeAddressResolver.resolveAddressDescriptor(any())).thenReturn(null)

        val result = placeQuery.getPlaceDetailsBatch(placeIds)

        assertThat(result).hasSize(4)
        assertThat(result.keys).containsExactlyInAnyOrder("place_1", "place_2", "place_3", "place_4")
        println("[PlaceQueryRetryTest] 캐시+API 혼합 → keys=${result.keys}")

        verify(googlePlacesClient, times(2)).getPlaceDetails(any())
        verify(googlePlacesClient, never()).getPlaceDetails("place_1")
        verify(googlePlacesClient, never()).getPlaceDetails("place_2")
    }

    @Test
    fun `배치 조회 - 업데이트 로직 병렬 처리 확인`(): Unit = runBlocking {
        val placeIds = (1..10).map { "place_$it" }
        val cachedPlaces = placeIds.map { createPlaceEntity(it, photos = null) }

        whenever(placeJpaRepository.findByGooglePlaceIdIn(placeIds)).thenReturn(cachedPlaces)
        whenever(placeAddressResolver.isValidAddressDescriptor(any())).thenReturn(false)
        val updateResponses = placeIds.associateWith { placeId ->
            createPlaceDetailsResponse(placeId, "맛집 ${placeId.removePrefix("place_")}")
        }

        val baseTime = System.currentTimeMillis()
        val callTimeline = Collections.synchronizedList(mutableListOf<String>())
        val delayMap = placeIds.withIndex().associate { (index, placeId) ->
            placeId to (100L + index * 40L)
        }

        updateResponses.keys.forEach { placeId ->
            whenever(googlePlacesClient.getPlaceDetails(placeId)).thenAnswer {
                val startElapsed = System.currentTimeMillis() - baseTime
                callTimeline += "start:$placeId:$startElapsed"
                Thread.sleep(delayMap.getValue(placeId))
                val endElapsed = System.currentTimeMillis() - baseTime
                callTimeline += "end:$placeId:$endElapsed"
                updateResponses.getValue(placeId)
            }
        }

        whenever(placeAddressResolver.resolveAddressDescriptor(any())).thenReturn(null)

        val startTime = System.currentTimeMillis()
        val result = placeQuery.getPlaceDetailsBatch(placeIds)
        val endTime = System.currentTimeMillis()

        assertThat(result).hasSize(10)
        verify(googlePlacesClient, times(10)).getPlaceDetails(any())
        assertThat(endTime - startTime).isLessThan(1200)

        val firstEndIndex = callTimeline.indexOfFirst { it.startsWith("end") }
        val startsBeforeFirstEnd = if (firstEndIndex == -1) 0 else callTimeline.subList(0, firstEndIndex)
            .count { it.startsWith("start") }
        assertThat(startsBeforeFirstEnd)
            .withFailMessage("병렬 처리 검증 실패 - 첫 완료 이전에 동시에 시작한 호출이 충분하지 않습니다. callTimeline=$callTimeline")
            .isGreaterThan(1)
        println("[PlaceQueryRetryTest] 병렬 업데이트 → keys=${result.keys}, timeline=${callTimeline.joinToString()}")
    }

    private fun createPlaceEntity(
        googlePlaceId: String,
        photos: String? = "photo1",
        addressDescriptor: String? = "신논현역 도보 약 5분",
        recentlyUpdated: Boolean = false
    ): PlaceEntity {
        return PlaceEntity(
            id = 1L,
            googlePlaceId = googlePlaceId,
            name = "맛집",
            address = "서울시 강남구",
            rating = 4.5,
            userRatingsTotal = 100,
            openNow = true,
            link = null,
            weekdayText = null,
            topReviewRating = null,
            topReviewText = null,
            priceRangeStart = null,
            priceRangeEnd = null,
            addressDescriptor = addressDescriptor,
            photos = photos,
            isDeleted = false
        ).apply {
            if (recentlyUpdated) {
                updateTimestamp()
            }
        }
    }

    private fun createPlaceDetailsResponse(placeId: String, name: String): PlaceDetailsResponse {
        return PlaceDetailsResponse(
            id = placeId,
            displayName = PlaceDetailsResponse.DisplayName(
                text = name,
                languageCode = "ko"
            ),
            formattedAddress = "서울시 강남구",
            rating = 4.5,
            userRatingCount = 100,
            regularOpeningHours = null,
            reviews = null,
            photos = listOf(
                PlaceDetailsResponse.Photo(
                    name = "places/$placeId/photos/photo1",
                    widthPx = 800,
                    heightPx = 800
                )
            ),
            priceRange = null,
            addressDescriptor = null,
            location = null
        )
    }
}

