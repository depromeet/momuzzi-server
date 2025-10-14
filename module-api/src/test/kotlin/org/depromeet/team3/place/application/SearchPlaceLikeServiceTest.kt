package org.depromeet.team3.place.application

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.depromeet.team3.common.exception.ErrorCode
import org.depromeet.team3.meetingplace.MeetingPlace
import org.depromeet.team3.meetingplace.MeetingPlaceRepository
import org.depromeet.team3.meetingplace.exception.MeetingPlaceException
import org.depromeet.team3.placelike.PlaceLike
import org.depromeet.team3.placelike.PlaceLikeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

@ExtendWith(MockitoExtension::class)
class SearchPlaceLikeServiceTest {

    @Mock
    private lateinit var meetingPlaceRepository: MeetingPlaceRepository

    @Mock
    private lateinit var placeLikeRepository: PlaceLikeRepository

    private lateinit var searchPlaceLikeService: SearchPlaceLikeService

    @BeforeEach
    fun setUp() {
        searchPlaceLikeService = SearchPlaceLikeService(
            meetingPlaceRepository = meetingPlaceRepository,
            placeLikeRepository = placeLikeRepository
        )
    }

    @Test
    fun `좋아요 토글 - 처음 좋아요 추가`(): Unit = runBlocking {
        // given
        val meetingId = 1L
        val userId = 100L
        val placeId = 200L
        val meetingPlaceId = 10L

        val meetingPlace = MeetingPlace(
            id = meetingPlaceId,
            meetingId = meetingId,
            placeId = placeId
        )

        whenever(meetingPlaceRepository.findByMeetingIdAndPlaceId(meetingId, placeId))
            .thenReturn(meetingPlace)
        whenever(placeLikeRepository.findByMeetingPlaceIdAndUserIdForUpdate(meetingPlaceId, userId))
            .thenReturn(null)
        whenever(placeLikeRepository.countByMeetingPlaceId(meetingPlaceId))
            .thenReturn(1L)

        // when
        val result = searchPlaceLikeService.toggle(meetingId, userId, placeId)

        // then
        assertThat(result.isLiked).isTrue()
        assertThat(result.likeCount).isEqualTo(1)

        verify(placeLikeRepository).save(
            argThat { placeLike ->
                placeLike.meetingPlaceId == meetingPlaceId && placeLike.userId == userId 
            }
        )
        verify(placeLikeRepository, never()).deleteByMeetingPlaceIdAndUserId(any(), any())
    }

    @Test
    fun `좋아요 토글 - 기존 좋아요 취소`(): Unit = runBlocking {
        // given
        val meetingId = 1L
        val userId = 100L
        val placeId = 200L
        val meetingPlaceId = 10L

        val meetingPlace = MeetingPlace(
            id = meetingPlaceId,
            meetingId = meetingId,
            placeId = placeId
        )

        val existingLike = PlaceLike(
            id = 1L,
            meetingPlaceId = meetingPlaceId,
            userId = userId
        )

        whenever(meetingPlaceRepository.findByMeetingIdAndPlaceId(meetingId, placeId))
            .thenReturn(meetingPlace)
        whenever(placeLikeRepository.findByMeetingPlaceIdAndUserIdForUpdate(meetingPlaceId, userId))
            .thenReturn(existingLike)
        whenever(placeLikeRepository.countByMeetingPlaceId(meetingPlaceId))
            .thenReturn(0L)

        // when
        val result = searchPlaceLikeService.toggle(meetingId, userId, placeId)

        // then
        assertThat(result.isLiked).isFalse()
        assertThat(result.likeCount).isEqualTo(0)

        verify(placeLikeRepository).deleteByMeetingPlaceIdAndUserId(meetingPlaceId, userId)
        verify(placeLikeRepository, never()).save(any())
    }

    @Test
    fun `좋아요 토글 - MeetingPlace가 존재하지 않으면 예외 발생`(): Unit = runBlocking {
        // given
        val meetingId = 1L
        val userId = 100L
        val placeId = 200L

        whenever(meetingPlaceRepository.findByMeetingIdAndPlaceId(meetingId, placeId))
            .thenReturn(null)

        // when & then
        assertThatThrownBy {
            runBlocking {
                searchPlaceLikeService.toggle(meetingId, userId, placeId)
            }
        }.isInstanceOf(MeetingPlaceException::class.java)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEETING_PLACE_NOT_FOUND)

        verify(placeLikeRepository, never()).save(any())
        verify(placeLikeRepository, never()).deleteByMeetingPlaceIdAndUserId(any(), any())
    }

    @Test
    fun `좋아요 토글 - 여러 사용자가 좋아요한 경우 카운트 정확`(): Unit = runBlocking {
        // given
        val meetingId = 1L
        val userId = 100L
        val placeId = 200L
        val meetingPlaceId = 10L

        val meetingPlace = MeetingPlace(
            id = meetingPlaceId,
            meetingId = meetingId,
            placeId = placeId
        )

        whenever(meetingPlaceRepository.findByMeetingIdAndPlaceId(meetingId, placeId))
            .thenReturn(meetingPlace)
        whenever(placeLikeRepository.findByMeetingPlaceIdAndUserIdForUpdate(meetingPlaceId, userId))
            .thenReturn(null)
        whenever(placeLikeRepository.countByMeetingPlaceId(meetingPlaceId))
            .thenReturn(5L) // 다른 사용자들이 이미 4개 좋아요

        // when
        val result = searchPlaceLikeService.toggle(meetingId, userId, placeId)

        // then
        assertThat(result.isLiked).isTrue()
        assertThat(result.likeCount).isEqualTo(5) // 4 + 1 = 5
    }

    @Test
    fun `좋아요 토글 - findByMeetingPlaceIdAndUserIdForUpdate 메서드 호출 확인`(): Unit = runBlocking {
        // given
        val meetingId = 1L
        val userId = 100L
        val placeId = 200L
        val meetingPlaceId = 10L

        val meetingPlace = MeetingPlace(
            id = meetingPlaceId,
            meetingId = meetingId,
            placeId = placeId
        )

        whenever(meetingPlaceRepository.findByMeetingIdAndPlaceId(meetingId, placeId))
            .thenReturn(meetingPlace)
        whenever(placeLikeRepository.findByMeetingPlaceIdAndUserIdForUpdate(meetingPlaceId, userId))
            .thenReturn(null)
        whenever(placeLikeRepository.countByMeetingPlaceId(meetingPlaceId))
            .thenReturn(1L)

        // when
        searchPlaceLikeService.toggle(meetingId, userId, placeId)

        // then
        // findByMeetingPlaceIdAndUserIdForUpdate가 호출되었는지 확인 (락을 걸기 위한 메서드)
        verify(placeLikeRepository).findByMeetingPlaceIdAndUserIdForUpdate(meetingPlaceId, userId)
        // 기존의 findByMeetingPlaceIdAndUserId는 호출되지 않아야 함
        verify(placeLikeRepository, never()).findByMeetingPlaceIdAndUserId(any(), any())
    }

    @Test
    fun `좋아요 토글 - 동시성 테스트를 위한 락 메서드 사용 확인`(): Unit = runBlocking {
        // given
        val meetingId = 1L
        val userId = 100L
        val placeId = 200L
        val meetingPlaceId = 10L

        val meetingPlace = MeetingPlace(
            id = meetingPlaceId,
            meetingId = meetingId,
            placeId = placeId
        )

        val existingLike = PlaceLike(
            id = 1L,
            meetingPlaceId = meetingPlaceId,
            userId = userId
        )

        whenever(meetingPlaceRepository.findByMeetingIdAndPlaceId(meetingId, placeId))
            .thenReturn(meetingPlace)
        whenever(placeLikeRepository.findByMeetingPlaceIdAndUserIdForUpdate(meetingPlaceId, userId))
            .thenReturn(existingLike)
        whenever(placeLikeRepository.countByMeetingPlaceId(meetingPlaceId))
            .thenReturn(0L)

        // when
        searchPlaceLikeService.toggle(meetingId, userId, placeId)

        // then
        // 락을 걸기 위한 forUpdate 메서드가 호출되었는지 확인
        verify(placeLikeRepository).findByMeetingPlaceIdAndUserIdForUpdate(meetingPlaceId, userId)
        // 기존의 일반 조회 메서드는 호출되지 않아야 함
        verify(placeLikeRepository, never()).findByMeetingPlaceIdAndUserId(any(), any())
    }
}
