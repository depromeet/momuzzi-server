package org.depromeet.team3.place.application.execution

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.depromeet.team3.meetingplace.MeetingPlace
import org.depromeet.team3.meetingplace.MeetingPlaceRepository
import org.depromeet.team3.place.PlaceEntity
import org.depromeet.team3.place.PlaceQuery
import org.depromeet.team3.place.application.model.PlaceSearchPlan
import org.depromeet.team3.place.dto.request.PlacesSearchRequest
import org.depromeet.team3.place.model.PlacesTextSearchResponse
import org.depromeet.team3.place.util.PlaceDetailsProcessor
import org.depromeet.team3.placelike.PlaceLikeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub

class ExecutePlaceSearchServiceTest {

    private val placeQuery: PlaceQuery = mock()
    private val placeDetailsProcessor: PlaceDetailsProcessor = mock()
    private val meetingPlaceRepository: MeetingPlaceRepository = mock()
    private val placeLikeRepository: PlaceLikeRepository = mock()
    private val cacheManager: MeetingPlaceSearchCacheManager = mock()

    private lateinit var service: ExecutePlaceSearchService

    @BeforeEach
    fun setup() {
        service = ExecutePlaceSearchService(
            placeQuery = placeQuery,
            placeDetailsProcessor = placeDetailsProcessor,
            meetingPlaceRepository = meetingPlaceRepository,
            placeLikeRepository = placeLikeRepository,
            cacheManager = cacheManager
        )
    }

    @Test
    fun `사진이 없는 항목이 있으면 fallback 후보 중 사진 있는 항목으로 대체한다`() = runTest {
        // given
        val plan = PlaceSearchPlan.Automatic(
            keywords = emptyList(),
            stationCoordinates = null,
            fallbackKeyword = "fallback"
        )

        val photoLessPlace = PlacesTextSearchResponse.Place(
            id = "PHOTOLESS",
            displayName = PlacesTextSearchResponse.Place.DisplayName("사진 없음"),
            formattedAddress = "주소",
            rating = 4.5
        )

        val photoPlace = PlacesTextSearchResponse.Place(
            id = "PHOTO",
            displayName = PlacesTextSearchResponse.Place.DisplayName("사진 있음"),
            formattedAddress = "주소",
            rating = 4.0
        )

        placeQuery.stub {
            on { findByGooglePlaceIds(any<List<String>>()) }.doReturn(
                listOf(
                    PlaceEntity(
                        id = 1L,
                        googlePlaceId = "PHOTOLESS",
                        name = "사진 없음",
                        address = "주소",
                        rating = 4.5,
                        userRatingsTotal = 10
                    ),
                    PlaceEntity(
                        id = 2L,
                        googlePlaceId = "PHOTO",
                        name = "사진 있음",
                        address = "주소",
                        rating = 4.0,
                        userRatingsTotal = 8
                    )
                )
            )
        }

        cacheManager.stub {
            onBlocking { getCachedAutomaticResult(1L) }.doReturn(
                MeetingPlaceSearchCacheManager.AutomaticSearchResult(
                    placeIds = listOf("PHOTOLESS", "PHOTO"),
                    placeWeights = mapOf("PHOTOLESS" to 1.0, "PHOTO" to 1.0),
                    usedKeywords = listOf("cached")
                )
            )
        }

        meetingPlaceRepository.stub {
            onBlocking { findByMeetingId(any<Long>()) }.doReturn(emptyList())
            onBlocking { saveAll(any<List<MeetingPlace>>()) }.doAnswer { invocation ->
                invocation.getArgument<List<MeetingPlace>>(0)
            }
        }

        placeLikeRepository.stub {
            onBlocking { findByMeetingPlaceIds(any<List<Long>>()) }.doReturn(emptyList())
        }

        placeDetailsProcessor.stub {
            onBlocking { fetchPlaceDetailsInParallel(any()) }.doReturn(
                listOf(
                    PlaceDetailsProcessor.PlaceDetailResult(
                        placeId = "PHOTOLESS",
                        name = "사진 없음",
                        address = "주소",
                        rating = 4.5,
                        userRatingsTotal = 10,
                        openNow = true,
                        photos = emptyList(),
                        link = "link",
                        weekdayText = emptyList(),
                        topReview = null,
                        priceRange = null,
                        addressDescriptor = null
                    ),
                    PlaceDetailsProcessor.PlaceDetailResult(
                        placeId = "PHOTO",
                        name = "사진 있음",
                        address = "주소",
                        rating = 4.0,
                        userRatingsTotal = 8,
                        openNow = true,
                        photos = listOf("image"),
                        link = "link",
                        weekdayText = emptyList(),
                        topReview = null,
                        priceRange = null,
                        addressDescriptor = null
                    )
                )
            )
        }

        val request = PlacesSearchRequest(meetingId = 1L, userId = null)

        // when
        val response = service.search(request, plan)

        // then
        val placeNames = response.items.map { it.name }
        assertThat(placeNames).contains("사진 있음")
        assertThat(response.items.first().photos).isNotEmpty()
    }
}

