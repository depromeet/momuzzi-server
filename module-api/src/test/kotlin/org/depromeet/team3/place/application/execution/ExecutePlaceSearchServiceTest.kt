package org.depromeet.team3.place.application.execution

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.depromeet.team3.meetingplace.MeetingPlace
import org.depromeet.team3.meetingplace.MeetingPlaceRepository
import org.depromeet.team3.place.PlaceEntity
import org.depromeet.team3.place.PlaceQuery
import org.depromeet.team3.place.application.model.PlaceSearchPlan
import org.depromeet.team3.place.application.plan.CreateSurveyKeywordService
import org.depromeet.team3.place.dto.request.PlacesSearchRequest
import org.depromeet.team3.place.model.PlacesTextSearchResponse
import org.depromeet.team3.place.util.PlaceDetailsProcessor
import org.depromeet.team3.placelike.PlaceLikeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import java.util.ArrayDeque

class ExecutePlaceSearchServiceTest {

    private lateinit var placeQuery: FakePlaceQuery
    private lateinit var placeDetailsProcessor: FakePlaceDetailsProcessor
    private lateinit var meetingPlaceRepository: MeetingPlaceRepository
    private lateinit var placeLikeRepository: PlaceLikeRepository
    private lateinit var cacheManager: MeetingPlaceSearchCacheManager

    private lateinit var service: ExecutePlaceSearchService

    @BeforeEach
    fun setup() {
        placeQuery = FakePlaceQuery()
        placeDetailsProcessor = FakePlaceDetailsProcessor()
        meetingPlaceRepository = mock()
        placeLikeRepository = mock()
        cacheManager = mock()

        service = ExecutePlaceSearchService(
            placeQuery = placeQuery,
            placeDetailsProcessor = placeDetailsProcessor,
            meetingPlaceRepository = meetingPlaceRepository,
            placeLikeRepository = placeLikeRepository,
            cacheManager = cacheManager
        )

        cacheManager.stub {
            onBlocking { getCachedAutomaticResult(any()) }.doReturn(null)
        }

        meetingPlaceRepository.stub {
            onBlocking { findByMeetingId(any()) }.doReturn(emptyList())
            onBlocking { saveAll(any()) }.doAnswer { invocation ->
                invocation.getArgument<List<MeetingPlace>>(0)
            }
        }

        placeLikeRepository.stub {
            onBlocking { findByMeetingPlaceIds(any()) }.doReturn(emptyList())
        }
    }

    @Test
    fun `캐시된 결과에서 사진이 있는 항목을 우선 정렬한다`() = runTest {
        val plan = PlaceSearchPlan.Automatic(
            keywords = emptyList(),
            stationCoordinates = null,
            fallbackKeyword = "fallback"
        )

        placeQuery.stubFindByGooglePlaceIds(
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

        placeDetailsProcessor.stubDetails(
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

        cacheManager.stub {
            onBlocking { getCachedAutomaticResult(1L) }.doReturn(
                MeetingPlaceSearchCacheManager.AutomaticSearchResult(
                    placeIds = listOf("PHOTOLESS", "PHOTO"),
                    placeWeights = mapOf("PHOTOLESS" to 1.0, "PHOTO" to 1.0),
                    usedKeywords = listOf("cached")
                )
            )
        }

        val request = PlacesSearchRequest(meetingId = 1L, userId = null)

        val response = service.search(request, plan)

        assertThat(response.items.map { it.name }).contains("사진 있음")
        assertThat(response.items.first().photos).isNotEmpty()
    }

    @Test
    fun `캐시 미스 시 사진 없는 결과를 fallback 후보 중 사진 있는 항목으로 보완한다`() = runTest {
        val keywordCandidate = CreateSurveyKeywordService.KeywordCandidate(
            keyword = "키워드 맛집",
            weight = 1.0,
            type = CreateSurveyKeywordService.KeywordType.GENERAL,
            matchKeywords = setOf("키워드", "keyword", "맛집")
        )
        val plan = PlaceSearchPlan.Automatic(
            keywords = listOf(keywordCandidate),
            stationCoordinates = null,
            fallbackKeyword = "fallback 맛집"
        )

        val textSearchPlaces = (0 until 15).map { index ->
            PlacesTextSearchResponse.Place(
                id = "P$index",
                displayName = PlacesTextSearchResponse.Place.DisplayName("키워드 맛집 $index"),
                formattedAddress = "주소 $index",
                rating = 5.0 - index * 0.1,
                userRatingCount = 10 - index
            )
        }

        placeQuery.stubTextSearch("키워드 맛집", PlacesTextSearchResponse(textSearchPlaces))
        placeQuery.stubTextSearch("fallback 맛집", PlacesTextSearchResponse(emptyList()))
        placeQuery.stubFindByGooglePlaceIds { ids ->
            ids.mapIndexed { idx, id ->
                PlaceEntity(
                    id = idx.toLong() + 1,
                    googlePlaceId = id,
                    name = "장소 $id",
                    address = "주소 $id",
                    rating = 4.0,
                    userRatingsTotal = 100
                )
            }
        }

        placeDetailsProcessor.stubDetails { requested ->
            requested.map { place ->
                val idx = place.id.removePrefix("P").toIntOrNull() ?: 0
                PlaceDetailsProcessor.PlaceDetailResult(
                    placeId = place.id,
                    name = "장소 ${place.id}",
                    address = place.formattedAddress,
                    rating = place.rating ?: 0.0,
                    userRatingsTotal = place.userRatingCount ?: 0,
                    openNow = true,
                    photos = if (idx < 10) emptyList() else listOf("photo-$idx"),
                    link = "link-$idx",
                    weekdayText = emptyList(),
                    topReview = null,
                    priceRange = null,
                    addressDescriptor = null
                )
            }
        }

        val request = PlacesSearchRequest(meetingId = 2L, userId = null)

        val response = service.search(request, plan)

        assertThat(response.items).hasSize(10)
        assertThat(response.items.first().photos).isNotEmpty()
        assertThat(response.items.any { it.name == "장소 P10" }).isTrue()
    }
}

private class FakePlaceQuery : PlaceQuery(
    googlePlacesClient = mock(),
    placeJpaRepository = mock(),
    placeAddressResolver = mock()
) {
    private val textSearchResponses = mutableMapOf<String, ArrayDeque<PlacesTextSearchResponse>>()
    private var findByIdsProvider: (List<String>) -> List<PlaceEntity> = { emptyList() }

    fun stubTextSearch(query: String, vararg responses: PlacesTextSearchResponse) {
        textSearchResponses[query] = ArrayDeque(responses.asList())
    }

    fun stubFindByGooglePlaceIds(provider: (List<String>) -> List<PlaceEntity>) {
        findByIdsProvider = provider
    }

    fun stubFindByGooglePlaceIds(entities: List<PlaceEntity>) {
        findByIdsProvider = { ids ->
            ids.mapNotNull { id -> entities.find { it.googlePlaceId == id } }
        }
    }

    override suspend fun textSearch(
        query: String,
        maxResults: Int,
        latitude: Double?,
        longitude: Double?,
        radius: Double
    ): PlacesTextSearchResponse {
        val deque = textSearchResponses[query]
            ?: error("No stubbed response for query=$query")
        if (deque.isEmpty()) {
            error("No remaining stubbed responses for query=$query")
        }
        return deque.removeFirst()
    }

    override fun findByGooglePlaceIds(googlePlaceIds: List<String>): List<PlaceEntity> =
        findByIdsProvider(googlePlaceIds)
}

private class FakePlaceDetailsProcessor : PlaceDetailsProcessor(
    placeQuery = mock(),
    placeAddressResolver = mock(),
    googlePlacesApiProperties = mock()
) {
    private var provider: (List<PlacesTextSearchResponse.Place>) -> List<PlaceDetailResult> =
        { emptyList() }

    fun stubDetails(results: List<PlaceDetailResult>) {
        provider = { results }
    }

    fun stubDetails(provider: (List<PlacesTextSearchResponse.Place>) -> List<PlaceDetailResult>) {
        this.provider = provider
    }

    override suspend fun fetchPlaceDetailsInParallel(
        places: List<PlacesTextSearchResponse.Place>
    ): List<PlaceDetailResult> = provider(places)
}

