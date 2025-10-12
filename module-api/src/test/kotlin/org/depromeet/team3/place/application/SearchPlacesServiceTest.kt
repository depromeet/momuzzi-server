package org.depromeet.team3.place.application

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.depromeet.team3.place.PlaceQuery
import org.depromeet.team3.place.dto.request.PlacesSearchRequest
import org.depromeet.team3.place.exception.PlaceSearchException
import org.depromeet.team3.place.model.PlacesTextSearchResponse
import org.depromeet.team3.place.util.PlaceDetailsAssembler
import org.depromeet.team3.place.util.PlaceTestDataFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

@ExtendWith(MockitoExtension::class)
class SearchPlacesServiceTest {

    @Mock
    private lateinit var placeQuery: PlaceQuery

    @Mock
    private lateinit var searchPlaceOffsetManager: SearchPlaceOffsetManager

    @Mock
    private lateinit var placeDetailsAssembler: PlaceDetailsAssembler
    
    private lateinit var searchPlacesService: SearchPlacesService

    @BeforeEach
    fun setUp() {
        searchPlacesService = SearchPlacesService(
            placeQuery = placeQuery,
            searchPlaceOffsetManager = searchPlaceOffsetManager,
            placeDetailsAssembler = placeDetailsAssembler
        )
    }

    @Test
    fun `맛집 검색 성공 - 첫 번째 요청`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집", maxResults = 5)
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(resultCount = 10)
        
        whenever(placeQuery.textSearch(any(), any()))
            .thenReturn(googleResponse)
        
        val selectedPlaces = googleResponse.places!!.take(5)
        whenever(searchPlaceOffsetManager.selectWithOffset<PlacesTextSearchResponse.Place>(anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(selectedPlaces)
        
        val placeDetails = selectedPlaces.map { place ->
            PlaceDetailsAssembler.PlaceDetailResult(
                name = place.displayName.text,
                address = place.formattedAddress,
                rating = place.rating ?: 0.0,
                userRatingsTotal = place.userRatingCount ?: 0,
                openNow = place.currentOpeningHours?.openNow,
                photos = listOf("https://example.com/photo.jpg"),
                link = "https://m.place.naver.com/place/list?query=${place.displayName.text}",
                weekdayText = listOf("월요일: 10:00~22:00"),
                topReview = PlaceDetailsAssembler.ReviewResult(
                    rating = 5,
                    text = "정말 맛있어요!"
                ),
                priceRange = null,
                addressDescriptor = null
            )
        }
        
        whenever(placeDetailsAssembler.fetchPlaceDetailsInParallel(any()))
            .thenReturn(placeDetails)

        // when
        val response = searchPlacesService.textSearch(request)

        // then
        assertThat(response.items).hasSize(5)
        assertThat(response.items[0].name).isEqualTo("맛집 1")
        assertThat(response.items[0].topReview).isNotNull
        assertThat(response.items[0].topReview?.rating).isEqualTo(5)
        
        verify(placeQuery).textSearch("강남역 맛집", 10)
        verify(searchPlaceOffsetManager).selectWithOffset<PlacesTextSearchResponse.Place>(eq("강남역 맛집"), eq(5), anyOrNull())
        verify(placeDetailsAssembler).fetchPlaceDetailsInParallel(any())
    }

    @Test
    fun `맛집 검색 성공 - 두 번째 요청은 다음 페이지 반환`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집", maxResults = 5)
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(resultCount = 10)
        
        whenever(placeQuery.textSearch(any(), any()))
            .thenReturn(googleResponse)
        
        // 첫 번째 요청은 0-4번 인덱스
        val firstPagePlaces = googleResponse.places!!.subList(0, 5)
        // 두 번째 요청은 5-9번 인덱스
        val secondPagePlaces = googleResponse.places!!.subList(5, 10)
        
        whenever(searchPlaceOffsetManager.selectWithOffset<PlacesTextSearchResponse.Place>(anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(firstPagePlaces)
            .thenReturn(secondPagePlaces)
        
        val firstPageDetails = firstPagePlaces.map { place ->
            PlaceDetailsAssembler.PlaceDetailResult(
                name = place.displayName.text,
                address = place.formattedAddress,
                rating = place.rating ?: 0.0,
                userRatingsTotal = place.userRatingCount ?: 0,
                openNow = place.currentOpeningHours?.openNow,
                photos = listOf("https://example.com/photo.jpg"),
                link = "https://m.place.naver.com/place/list?query=${place.displayName.text}",
                weekdayText = null,
                topReview = null,
                priceRange = null,
                addressDescriptor = null
            )
        }
        
        val secondPageDetails = secondPagePlaces.map { place ->
            PlaceDetailsAssembler.PlaceDetailResult(
                name = place.displayName.text,
                address = place.formattedAddress,
                rating = place.rating ?: 0.0,
                userRatingsTotal = place.userRatingCount ?: 0,
                openNow = place.currentOpeningHours?.openNow,
                photos = listOf("https://example.com/photo.jpg"),
                link = "https://m.place.naver.com/place/list?query=${place.displayName.text}",
                weekdayText = null,
                topReview = null,
                priceRange = null,
                addressDescriptor = null
            )
        }
        
        whenever(placeDetailsAssembler.fetchPlaceDetailsInParallel(any()))
            .thenReturn(firstPageDetails)
            .thenReturn(secondPageDetails)

        // when - 첫 번째 요청
        val firstResponse = searchPlacesService.textSearch(request)
        
        // when - 두 번째 요청 (같은 쿼리)
        val secondResponse = searchPlacesService.textSearch(request)

        // then
        assertThat(firstResponse.items).hasSize(5)
        assertThat(firstResponse.items[0].name).isEqualTo("맛집 1")
        
        assertThat(secondResponse.items).hasSize(5)
        assertThat(secondResponse.items[0].name).isEqualTo("맛집 6")
        
        // 두 응답이 서로 다른 결과를 반환
        assertThat(firstResponse.items.map { it.name })
            .doesNotContainAnyElementsOf(secondResponse.items.map { it.name })
    }

    @Test
    fun `맛집 검색 - 동시성 테스트 Mutex 동작 확인`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집", maxResults = 5)
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(resultCount = 10)
        
        whenever(placeQuery.textSearch(any(), any()))
            .thenReturn(googleResponse)
        
        // 첫 번째와 두 번째 요청에 대해 다른 페이지 반환
        val firstPagePlaces = googleResponse.places!!.subList(0, 5)
        val secondPagePlaces = googleResponse.places!!.subList(5, 10)
        
        whenever(searchPlaceOffsetManager.selectWithOffset<PlacesTextSearchResponse.Place>(anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(firstPagePlaces)
            .thenReturn(secondPagePlaces)
        
        val firstPageDetails = firstPagePlaces.map { place ->
            PlaceDetailsAssembler.PlaceDetailResult(
                name = place.displayName.text,
                address = place.formattedAddress,
                rating = place.rating ?: 0.0,
                userRatingsTotal = place.userRatingCount ?: 0,
                openNow = place.currentOpeningHours?.openNow,
                photos = listOf("https://example.com/photo.jpg"),
                link = "https://m.place.naver.com/place/list?query=${place.displayName.text}",
                weekdayText = null,
                topReview = null,
                priceRange = null,
                addressDescriptor = null
            )
        }
        
        val secondPageDetails = secondPagePlaces.map { place ->
            PlaceDetailsAssembler.PlaceDetailResult(
                name = place.displayName.text,
                address = place.formattedAddress,
                rating = place.rating ?: 0.0,
                userRatingsTotal = place.userRatingCount ?: 0,
                openNow = place.currentOpeningHours?.openNow,
                photos = listOf("https://example.com/photo.jpg"),
                link = "https://m.place.naver.com/place/list?query=${place.displayName.text}",
                weekdayText = null,
                topReview = null,
                priceRange = null,
                addressDescriptor = null
            )
        }
        
        whenever(placeDetailsAssembler.fetchPlaceDetailsInParallel(any()))
            .thenReturn(firstPageDetails)
            .thenReturn(secondPageDetails)

        // when - 동시에 2개의 요청 실행
        val results = listOf(
            async { searchPlacesService.textSearch(request) },
            async { searchPlacesService.textSearch(request) }
        ).awaitAll()

        // then - 두 응답은 서로 다른 결과를 반환해야 함 (중복 없음)
        val firstResult = results[0]
        val secondResult = results[1]
        
        assertThat(firstResult.items).hasSize(5)
        assertThat(secondResult.items).hasSize(5)
        
        val firstNames = firstResult.items.map { it.name }.toSet()
        val secondNames = secondResult.items.map { it.name }.toSet()
        
        // 교집합이 없어야 함 (중복된 페이지가 없음)
        assertThat(firstNames.intersect(secondNames)).isEmpty()
    }

    @Test
    fun `맛집 검색 실패 - 최대 호출 횟수 초과`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집", maxResults = 5)
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(resultCount = 10)
        
        whenever(placeQuery.textSearch(any(), any()))
            .thenReturn(googleResponse)
        
        // 처음 두 번은 정상 반환, 세 번째는 null 반환 (최대 호출 횟수 초과)
        whenever(searchPlaceOffsetManager.selectWithOffset<PlacesTextSearchResponse.Place>(anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(googleResponse.places!!.subList(0, 5))
            .thenReturn(googleResponse.places!!.subList(5, 10))
            .thenReturn(null)
        
        whenever(placeDetailsAssembler.fetchPlaceDetailsInParallel(any()))
            .thenReturn(emptyList())

        // when - 첫 번째, 두 번째 요청
        searchPlacesService.textSearch(request)
        searchPlacesService.textSearch(request)
        
        // when - 세 번째 요청 (최대 호출 횟수 초과)
        val thirdResponse = searchPlacesService.textSearch(request)

        // then - 빈 목록 반환
        assertThat(thirdResponse.items).isEmpty()
    }

    @Test
    fun `맛집 검색 실패 - Google API 오류`() {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집", maxResults = 5)
        
        runBlocking {
            whenever(placeQuery.textSearch(any(), any()))
                .thenThrow(RuntimeException("API Error"))
        }

        // when & then
        val exception = assertThrows<PlaceSearchException> {
            runBlocking {
                searchPlacesService.textSearch(request)
            }
        }
        
        assertThat(exception.message).isEqualTo("맛집 검색 중 오류가 발생했습니다")
    }

    @Test
    fun `맛집 검색 - places가 null이면 빈 목록 반환`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집", maxResults = 5)
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(resultCount = 0)
        
        whenever(placeQuery.textSearch(any(), any()))
            .thenReturn(googleResponse.copy(places = null))

        // when
        val response = searchPlacesService.textSearch(request)

        // then - 빈 목록 반환
        assertThat(response.items).isEmpty()
    }

    @Test
    fun `맛집 검색 - places가 empty이면 빈 목록 반환`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집", maxResults = 5)
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(resultCount = 0)
        
        whenever(placeQuery.textSearch(any(), any()))
            .thenReturn(googleResponse.copy(places = emptyList()))

        // when
        val response = searchPlacesService.textSearch(request)

        // then - 빈 목록 반환
        assertThat(response.items).isEmpty()
    }

    @Test
    fun `맛집 검색 - 상세 정보 조회 결과가 적으면 그만큼만 반환`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집", maxResults = 5)
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(resultCount = 10)
        
        whenever(placeQuery.textSearch(any(), any()))
            .thenReturn(googleResponse)
        
        val selectedPlaces = googleResponse.places!!.take(5)
        whenever(searchPlaceOffsetManager.selectWithOffset<PlacesTextSearchResponse.Place>(anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(selectedPlaces)
        
        // 5개 요청했지만 4개만 성공적으로 조회됨
        val placeDetails = selectedPlaces.take(4).map { place ->
            PlaceDetailsAssembler.PlaceDetailResult(
                name = place.displayName.text,
                address = place.formattedAddress,
                rating = place.rating ?: 0.0,
                userRatingsTotal = place.userRatingCount ?: 0,
                openNow = place.currentOpeningHours?.openNow,
                photos = listOf("https://example.com/photo.jpg"),
                link = "https://m.place.naver.com/place/list?query=${place.displayName.text}",
                weekdayText = null,
                topReview = null,
                priceRange = null,
                addressDescriptor = null
            )
        }
        
        whenever(placeDetailsAssembler.fetchPlaceDetailsInParallel(any()))
            .thenReturn(placeDetails)

        // when
        val response = searchPlacesService.textSearch(request)

        // then - 4개만 반환
        assertThat(response.items).hasSize(4)
        assertThat(response.items.map { it.name }).doesNotContain("맛집 5")
    }
}
