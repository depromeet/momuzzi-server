package org.depromeet.team3.place.application

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.depromeet.team3.meetingplace.MeetingPlace
import org.depromeet.team3.meetingplace.MeetingPlaceRepository
import org.depromeet.team3.place.PlaceEntity
import org.depromeet.team3.place.PlaceQuery
import org.depromeet.team3.place.dto.request.PlacesSearchRequest
import org.depromeet.team3.place.exception.PlaceSearchException
import org.depromeet.team3.place.model.PlacesTextSearchResponse
import org.depromeet.team3.place.util.PlaceDetailsProcessor
import org.depromeet.team3.place.util.PlaceTestDataFactory
import org.depromeet.team3.placelike.PlaceLike
import org.depromeet.team3.placelike.PlaceLikeRepository
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
    private lateinit var placeDetailsProcessor: PlaceDetailsProcessor

    @Mock
    private lateinit var meetingPlaceRepository: MeetingPlaceRepository

    @Mock
    private lateinit var placeLikeRepository: PlaceLikeRepository
    
    @Mock
    private lateinit var meetingQuery: org.depromeet.team3.meeting.MeetingQuery
    
    private lateinit var searchPlacesService: SearchPlacesService

    @BeforeEach
    fun setUp() {
        searchPlacesService = SearchPlacesService(
            placeQuery = placeQuery,
            placeDetailsProcessor = placeDetailsProcessor,
            meetingPlaceRepository = meetingPlaceRepository,
            placeLikeRepository = placeLikeRepository,
            meetingQuery = meetingQuery
        )
    }

    @Test
    fun `맛집 검색 성공 - 기본 검색`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집")
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(resultCount = 10, placeId = "place1")
        
        whenever(placeQuery.textSearch(eq("강남역 맛집"), eq(15), anyOrNull(), anyOrNull(), any()))
            .thenReturn(googleResponse)
        
        // 전체 10개에 대한 Details
        val allPlaceDetails = googleResponse.places!!.map { place ->
            PlaceDetailsProcessor.PlaceDetailResult(
                placeId = place.id,
                name = place.displayName.text,
                address = place.formattedAddress,
                rating = place.rating ?: 0.0,
                userRatingsTotal = place.userRatingCount ?: 0,
                openNow = place.currentOpeningHours?.openNow,
                photos = listOf("https://example.com/photo.jpg"),
                link = "https://m.place.naver.com/place/list?query=${place.displayName.text}",
                weekdayText = listOf("월요일: 10:00~22:00"),
                topReview = PlaceDetailsProcessor.ReviewResult(
                    rating = 5,
                    text = "정말 맛있어요!"
                ),
                priceRange = null,
                addressDescriptor = null
            )
        }
        
        whenever(placeDetailsProcessor.fetchPlaceDetailsInParallel(googleResponse.places!!))
            .thenReturn(allPlaceDetails)
        
        // Google Place ID -> DB Place ID 매핑
        whenever(placeQuery.findByGooglePlaceIds(any()))
            .thenReturn(listOf(
                PlaceEntity(id = 100L, googlePlaceId = "place1", name = "맛집1", address = "주소1", rating = 4.5, userRatingsTotal = 100),
                PlaceEntity(id = 101L, googlePlaceId = "place2", name = "맛집2", address = "주소2", rating = 4.3, userRatingsTotal = 80),
                PlaceEntity(id = 102L, googlePlaceId = "place3", name = "맛집3", address = "주소3", rating = 4.7, userRatingsTotal = 120)
            ))

        // when
        val response = searchPlacesService.textSearch(request)

        // then
        assertThat(response.items).hasSize(10) // 전체 10개 반환
        assertThat(response.items[0].name).isEqualTo("맛집 1")
        assertThat(response.items[0].topReview).isNotNull
        assertThat(response.items[0].topReview?.rating).isEqualTo(5)
        
        // meetingId가 없으면 위도/경도도 null이어야 함
        verify(placeQuery).textSearch("강남역 맛집", 15, null, null, 3000.0)
        verify(placeDetailsProcessor).fetchPlaceDetailsInParallel(googleResponse.places!!)
        verify(placeQuery).findByGooglePlaceIds(any())
    }

    @Test
    fun `맛집 검색 성공 - 동일한 쿼리로 여러 번 검색`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집")
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(resultCount = 10, placeId = "place1")
        
        whenever(placeQuery.textSearch(any(), any(), anyOrNull(), anyOrNull(), any()))
            .thenReturn(googleResponse)
        
        // 전체 10개 Details
        val allPlaceDetails = googleResponse.places!!.map { place ->
            PlaceDetailsProcessor.PlaceDetailResult(
                placeId = place.id,
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
        
        whenever(placeDetailsProcessor.fetchPlaceDetailsInParallel(any()))
            .thenReturn(allPlaceDetails)
        
        // Google Place ID -> DB Place ID 매핑
        whenever(placeQuery.findByGooglePlaceIds(any()))
            .thenReturn(listOf(
                PlaceEntity(id = 100L, googlePlaceId = "place1", name = "맛집1", address = "주소1", rating = 4.5, userRatingsTotal = 100),
                PlaceEntity(id = 101L, googlePlaceId = "place2", name = "맛집2", address = "주소2", rating = 4.3, userRatingsTotal = 80),
                PlaceEntity(id = 102L, googlePlaceId = "place3", name = "맛집3", address = "주소3", rating = 4.7, userRatingsTotal = 120)
            ))

        // when - 첫 번째 요청
        val firstResponse = searchPlacesService.textSearch(request)
        
        // when - 두 번째 요청 (같은 쿼리)
        val secondResponse = searchPlacesService.textSearch(request)

        // then - 동일한 결과 반환 (페이지네이션 없음)
        assertThat(firstResponse.items).hasSize(10)
        assertThat(secondResponse.items).hasSize(10)
        
        // 두 응답이 동일한 결과를 반환
        assertThat(firstResponse.items.map { it.name })
            .isEqualTo(secondResponse.items.map { it.name })
    }

    @Test
    fun `맛집 검색 - 동시성 테스트`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집")
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(resultCount = 10, placeId = "place1")
        
        whenever(placeQuery.textSearch(any(), any(), anyOrNull(), anyOrNull(), any()))
            .thenReturn(googleResponse)
        
        val allPlaceDetails = googleResponse.places!!.map { place ->
            PlaceDetailsProcessor.PlaceDetailResult(
                placeId = place.id,
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
        
        whenever(placeDetailsProcessor.fetchPlaceDetailsInParallel(any()))
            .thenReturn(allPlaceDetails)
        
        // Google Place ID -> DB Place ID 매핑
        whenever(placeQuery.findByGooglePlaceIds(any()))
            .thenReturn(listOf(
                PlaceEntity(id = 100L, googlePlaceId = "place1", name = "맛집1", address = "주소1", rating = 4.5, userRatingsTotal = 100),
                PlaceEntity(id = 101L, googlePlaceId = "place2", name = "맛집2", address = "주소2", rating = 4.3, userRatingsTotal = 80),
                PlaceEntity(id = 102L, googlePlaceId = "place3", name = "맛집3", address = "주소3", rating = 4.7, userRatingsTotal = 120)
            ))

        // when - 동시에 2개의 요청 실행
        val results = listOf(
            async { searchPlacesService.textSearch(request) },
            async { searchPlacesService.textSearch(request) }
        ).awaitAll()

        // then - 두 응답은 동일한 결과를 반환해야 함
        val firstResult = results[0]
        val secondResult = results[1]
        
        assertThat(firstResult.items).hasSize(10)
        assertThat(secondResult.items).hasSize(10)
        
        // 두 응답이 동일한 결과를 반환
        assertThat(firstResult.items.map { it.name })
            .isEqualTo(secondResult.items.map { it.name })
    }

    @Test
    fun `맛집 검색 성공 - 상세 정보 조회 결과가 적으면 그만큼만 반환`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집")
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(resultCount = 10, placeId = "place1")
        
        whenever(placeQuery.textSearch(any(), any(), anyOrNull(), anyOrNull(), any()))
            .thenReturn(googleResponse)
        
        // 10개 요청했지만 4개만 성공적으로 조회됨 (API 에러 등)
        val placeDetails = googleResponse.places!!.take(4).map { place ->
            PlaceDetailsProcessor.PlaceDetailResult(
                placeId = place.id,
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
        
        whenever(placeDetailsProcessor.fetchPlaceDetailsInParallel(any()))
            .thenReturn(placeDetails)
        
        // Google Place ID -> DB Place ID 매핑 (4개만)
        whenever(placeQuery.findByGooglePlaceIds(any()))
            .thenReturn(listOf(
                PlaceEntity(id = 100L, googlePlaceId = "place1", name = "맛집1", address = "주소1", rating = 4.5, userRatingsTotal = 100),
                PlaceEntity(id = 101L, googlePlaceId = "place2", name = "맛집2", address = "주소2", rating = 4.3, userRatingsTotal = 80),
                PlaceEntity(id = 102L, googlePlaceId = "place3", name = "맛집3", address = "주소3", rating = 4.7, userRatingsTotal = 120),
                PlaceEntity(id = 103L, googlePlaceId = "place4", name = "맛집4", address = "주소4", rating = 4.1, userRatingsTotal = 90)
            ))

        // when
        val response = searchPlacesService.textSearch(request)

        // then - 4개만 반환
        assertThat(response.items).hasSize(4)
        assertThat(response.items.map { it.name }).doesNotContain("맛집 5")
    }

    @Test
    fun `맛집 검색 실패 - Google API 오류`() {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집")
        
        runBlocking {
            whenever(placeQuery.textSearch(any(), any(), anyOrNull(), anyOrNull(), any()))
                .thenThrow(RuntimeException("API Error"))
        }

        // when & then
        val exception = assertThrows<PlaceSearchException> {
            runBlocking {
                searchPlacesService.textSearch(request)
            }
        }
        
        assertThat(exception.errorCode.code).isEqualTo("P001")
        assertThat(exception.message).contains("맛집 검색 중 오류가 발생했습니다")
    }

    @Test
    fun `맛집 검색 실패 - 빈 검색어`() {
        // given
        val request = PlacesSearchRequest(query = "")

        // when & then
        val exception = assertThrows<PlaceSearchException> {
            runBlocking {
                searchPlacesService.textSearch(request)
            }
        }
        
        assertThat(exception.errorCode.code).isEqualTo("P007")
        assertThat(exception.message).contains("유효하지 않은 검색어입니다")
    }

    @Test
    fun `맛집 검색 - places가 null이면 빈 목록 반환`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(query = "강남역 맛집")
        val googleResponse = PlacesTextSearchResponse(places = null)
        
        whenever(placeQuery.textSearch(any(), any(), anyOrNull(), anyOrNull(), any()))
            .thenReturn(googleResponse)

        // when
        val response = searchPlacesService.textSearch(request)

        // then - 빈 목록 반환
        assertThat(response.items).isEmpty()
    }


    @Test
    fun `맛집 검색 - 미팅 ID가 있을 때 좋아요 정보 매핑`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(
            query = "강남역 맛집", 
            meetingId = 1L,
            userId = 100L
        )
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(resultCount = 3, placeId = null)
        
        whenever(meetingQuery.getStationCoordinates(1L))
            .thenReturn(null)
        
        whenever(placeQuery.textSearch(any(), any(), anyOrNull(), anyOrNull(), any()))
            .thenReturn(googleResponse)
        
        // Place Details
        val allPlaceDetails = googleResponse.places!!.map { place ->
            PlaceDetailsProcessor.PlaceDetailResult(
                placeId = place.id,
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
        
        whenever(placeDetailsProcessor.fetchPlaceDetailsInParallel(googleResponse.places!!))
            .thenReturn(allPlaceDetails)
        
        // MeetingPlace 조회 결과 (기존에는 없음 - 새로 생성됨)
        val existingMeetingPlaces = emptyList<MeetingPlace>()
        
        whenever(meetingPlaceRepository.findByMeetingId(1L))
            .thenReturn(existingMeetingPlaces)
        
        // 새로 생성될 MeetingPlace들 (저장 후 id가 설정됨)
        val newMeetingPlaces = listOf(
            MeetingPlace(id = 10L, meetingId = 1L, placeId = 100L), // placeId 100L
            MeetingPlace(id = 11L, meetingId = 1L, placeId = 101L), // placeId 101L
            MeetingPlace(id = 12L, meetingId = 1L, placeId = 102L)  // placeId 102L
        )
        
        whenever(meetingPlaceRepository.saveAll(any()))
            .thenReturn(newMeetingPlaces)
        
        // PlaceLike 조회 결과
        val placeLikes = listOf(
            PlaceLike(id = 1L, meetingPlaceId = 10L, userId = 100L), // 사용자 100L이 좋아요
            PlaceLike(id = 2L, meetingPlaceId = 10L, userId = 200L), // 사용자 200L이 좋아요
            PlaceLike(id = 3L, meetingPlaceId = 11L, userId = 200L)  // 사용자 200L이 좋아요
        )
        
        whenever(placeLikeRepository.findByMeetingPlaceIds(listOf(10L, 11L, 12L)))
            .thenReturn(placeLikes)
        
        // Google Place ID -> DB Place ID 매핑 (실제로는 PlaceQuery에서 조회)
        whenever(placeQuery.findByGooglePlaceIds(any()))
            .thenReturn(listOf(
                PlaceEntity(id = 100L, googlePlaceId = "place_id_1", name = "맛집1", address = "주소1", rating = 4.5, userRatingsTotal = 100),
                PlaceEntity(id = 101L, googlePlaceId = "place_id_2", name = "맛집2", address = "주소2", rating = 4.3, userRatingsTotal = 80),
                PlaceEntity(id = 102L, googlePlaceId = "place_id_3", name = "맛집3", address = "주소3", rating = 4.7, userRatingsTotal = 120)
            ))

        // when
        val response = searchPlacesService.textSearch(request)

        // then
        assertThat(response.items).hasSize(3)
        
        // saveAll이 호출되었는지 확인
        verify(meetingPlaceRepository).saveAll(any())
        
        // 좋아요 많은 순으로 정렬되어야 함
        val sortedByLikeCount = response.items.sortedByDescending { it.likeCount }
        assertThat(response.items).isEqualTo(sortedByLikeCount)
        
        // 각 장소별 좋아요 정보 확인
        val place1 = response.items.find { it.placeId == 100L }
        assertThat(place1).isNotNull()
        assertThat(place1?.likeCount).isEqualTo(2) // 2명이 좋아요
        assertThat(place1?.isLiked).isTrue() // 사용자 100L이 좋아요
        
        val place2 = response.items.find { it.placeId == 101L }
        assertThat(place2).isNotNull()
        assertThat(place2?.likeCount).isEqualTo(1) // 1명이 좋아요
        assertThat(place2?.isLiked).isFalse() // 사용자 100L은 좋아요 안함
        
        val place3 = response.items.find { it.placeId == 102L }
        assertThat(place3).isNotNull()
        assertThat(place3?.likeCount).isEqualTo(0) // 좋아요 없음
        assertThat(place3?.isLiked).isFalse() // 사용자 100L은 좋아요 안함
    }

    @Test
    fun `맛집 검색 - meetingId가 있을 때 Station 좌표를 사용한 검색`(): Unit = runBlocking {
        // given
        val meetingId = 1L
        val stationCoordinates = org.depromeet.team3.meeting.MeetingQuery.StationCoordinates(
            latitude = 37.5048,  // 강남역
            longitude = 127.0249
        )
        val request = PlacesSearchRequest(
            query = "맛집", 
            meetingId = meetingId,
            userId = 100L
        )
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(resultCount = 5, placeId = "place1")
        
        // Meeting의 Station 좌표 조회 성공
        whenever(meetingQuery.getStationCoordinates(meetingId))
            .thenReturn(stationCoordinates)
        
        // textSearch 호출 시 위도/경도가 전달되어야 함
        whenever(placeQuery.textSearch(
            eq("맛집"), 
            eq(15), 
            eq(37.5048),     // latitude
            eq(127.0249),    // longitude
            any()
        )).thenReturn(googleResponse)
        
        val allPlaceDetails = googleResponse.places!!.map { place ->
            PlaceDetailsProcessor.PlaceDetailResult(
                placeId = place.id,
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
        
        whenever(placeDetailsProcessor.fetchPlaceDetailsInParallel(googleResponse.places!!))
            .thenReturn(allPlaceDetails)
        
        whenever(placeQuery.findByGooglePlaceIds(any()))
            .thenReturn(listOf(
                PlaceEntity(id = 100L, googlePlaceId = "place1", name = "맛집1", address = "주소1", rating = 4.5, userRatingsTotal = 100)
            ))
        
        whenever(meetingPlaceRepository.findByMeetingId(meetingId))
            .thenReturn(emptyList())
        
        whenever(meetingPlaceRepository.saveAll(any()))
            .thenReturn(listOf(MeetingPlace(id = 10L, meetingId = meetingId, placeId = 100L)))
        
        whenever(placeLikeRepository.findByMeetingPlaceIds(any()))
            .thenReturn(emptyList())

        // when
        val response = searchPlacesService.textSearch(request)

        // then
        assertThat(response.items).isNotEmpty()
        
        // Station 좌표 조회 확인
        verify(meetingQuery).getStationCoordinates(meetingId)
        
        // 위도/경도가 textSearch에 제대로 전달되었는지 확인
        verify(placeQuery).textSearch("맛집", 15, 37.5048, 127.0249, 3000.0)
    }

    @Test
    fun `맛집 검색 - meetingId가 있지만 Station 좌표가 없을 때`(): Unit = runBlocking {
        // given
        val meetingId = 1L
        val request = PlacesSearchRequest(
            query = "맛집", 
            meetingId = meetingId,
            userId = 100L
        )
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(resultCount = 5, placeId = "place1")
        
        // Meeting의 Station 좌표 조회 실패 (null 반환)
        whenever(meetingQuery.getStationCoordinates(meetingId))
            .thenReturn(null)
        
        // 위도/경도 없이 textSearch 호출
        whenever(placeQuery.textSearch(eq("맛집"), eq(15), anyOrNull(), anyOrNull(), any()))
            .thenReturn(googleResponse)
        
        val allPlaceDetails = googleResponse.places!!.map { place ->
            PlaceDetailsProcessor.PlaceDetailResult(
                placeId = place.id,
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
        
        whenever(placeDetailsProcessor.fetchPlaceDetailsInParallel(googleResponse.places!!))
            .thenReturn(allPlaceDetails)
        
        whenever(placeQuery.findByGooglePlaceIds(any()))
            .thenReturn(listOf(
                PlaceEntity(id = 100L, googlePlaceId = "place1", name = "맛집1", address = "주소1", rating = 4.5, userRatingsTotal = 100)
            ))
        
        whenever(meetingPlaceRepository.findByMeetingId(meetingId))
            .thenReturn(emptyList())
        
        whenever(meetingPlaceRepository.saveAll(any()))
            .thenReturn(listOf(MeetingPlace(id = 10L, meetingId = meetingId, placeId = 100L)))
        
        whenever(placeLikeRepository.findByMeetingPlaceIds(any()))
            .thenReturn(emptyList())

        // when
        val response = searchPlacesService.textSearch(request)

        // then
        assertThat(response.items).isNotEmpty()
        
        // Station 좌표 조회 시도 확인
        verify(meetingQuery).getStationCoordinates(meetingId)
        
        // 위도/경도가 null로 전달되었는지 확인
        verify(placeQuery).textSearch("맛집", 15, null, null, 3000.0)
    }

    @Test
    fun `맛집 검색 - 미팅 ID가 없을 때 좋아요 정보 없음`(): Unit = runBlocking {
        // given
        val request = PlacesSearchRequest(
            query = "강남역 맛집", 
            // meetingId와 userId 없음
        )
        val googleResponse = PlaceTestDataFactory.createGooglePlacesSearchResponse(resultCount = 3, placeId = "place1")
        
        whenever(placeQuery.textSearch(any(), any(), anyOrNull(), anyOrNull(), any()))
            .thenReturn(googleResponse)
        
        val allPlaceDetails = googleResponse.places!!.map { place ->
            PlaceDetailsProcessor.PlaceDetailResult(
                placeId = place.id,
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
        
        whenever(placeDetailsProcessor.fetchPlaceDetailsInParallel(googleResponse.places!!))
            .thenReturn(allPlaceDetails)
        
        // Google Place ID -> DB Place ID 매핑
        whenever(placeQuery.findByGooglePlaceIds(any()))
            .thenReturn(listOf(
                PlaceEntity(id = 100L, googlePlaceId = "place1", name = "맛집1", address = "주소1", rating = 4.5, userRatingsTotal = 100),
                PlaceEntity(id = 101L, googlePlaceId = "place2", name = "맛집2", address = "주소2", rating = 4.3, userRatingsTotal = 80),
                PlaceEntity(id = 102L, googlePlaceId = "place3", name = "맛집3", address = "주소3", rating = 4.7, userRatingsTotal = 120)
            ))

        // when
        val response = searchPlacesService.textSearch(request)

        // then
        assertThat(response.items).hasSize(3)
        
        // 모든 장소의 좋아요 정보가 0이어야 함
        response.items.forEach { item ->
            assertThat(item.likeCount).isEqualTo(0)
            assertThat(item.isLiked).isFalse()
        }
        
        // 좋아요 순 정렬되지 않음 (구글 기본 순서 유지)
        verify(meetingPlaceRepository, never()).findByMeetingId(any())
        verify(placeLikeRepository, never()).findByMeetingPlaceIds(any())
    }
}
