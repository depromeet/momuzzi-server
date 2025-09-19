package org.depromeet.team3.meeting.application

import org.depromeet.team3.meeting.Meeting
import org.depromeet.team3.meeting.MeetingRepository
import org.depromeet.team3.meeting.util.MeetingTestDataFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

class InviteTokenServiceTest {
    
    @Mock
    private lateinit var meetingRepository: MeetingRepository
    
    private lateinit var inviteTokenService: InviteTokenService
    
    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        inviteTokenService = InviteTokenService(meetingRepository)
    }
    
    @Test
    fun `초대 토큰 생성 성공 테스트`() {
        // Given
        val meetingId = 1L
        val baseUrl = "https://app.momuzzi.com"
        val meeting = Meeting(
            id = meetingId,
            hostUserId = 1L,
            attendeeCount = 5,
            isClosed = false,
            stationId = 1L,
            endAt = LocalDateTime.now().plusHours(2),
            createdAt = LocalDateTime.now(),
            updatedAt = null
        )
        
        whenever(meetingRepository.findById(meetingId)).thenReturn(meeting)
        
        // When
        val result = inviteTokenService.generateInviteToken(meetingId, baseUrl)
        
        // Then
        assertNotNull(result)
        assertTrue(result.inviteUrl.startsWith(baseUrl))
        assertTrue(result.inviteUrl.contains("token="))
        assertNotNull(result.token)
    }
    
    @Test
    fun `존재하지 않는 모임으로 초대 토큰 생성 시 예외 발생`() {
        // Given
        val meetingId = 999L
        val baseUrl = "https://app.momuzzi.com"
        
        whenever(meetingRepository.findById(meetingId)).thenReturn(null)
        
        // When & Then
        assertThrows<IllegalArgumentException> {
            inviteTokenService.generateInviteToken(meetingId, baseUrl)
        }
    }
    
    @Test
    fun `종료된 모임으로 초대 토큰 생성 시 예외 발생`() {
        // Given
        val meetingId = 1L
        val baseUrl = "https://app.momuzzi.com"
        val meeting = Meeting(
            id = meetingId,
            hostUserId = 1L,
            attendeeCount = 5,
            isClosed = true,
            stationId = 1L,
            endAt = LocalDateTime.now().minusHours(1),
            createdAt = LocalDateTime.now(),
            updatedAt = null
        )
        
        whenever(meetingRepository.findById(meetingId)).thenReturn(meeting)
        
        // When & Then
        assertThrows<IllegalStateException> {
            inviteTokenService.generateInviteToken(meetingId, baseUrl)
        }
    }
    
    @Test
    fun `유효한 토큰 검증 성공 테스트`() {
        // Given
        val meetingId = 1L
        val baseUrl = "https://app.momuzzi.com"
        val meeting = Meeting(
            id = meetingId,
            hostUserId = 1L,
            attendeeCount = 5,
            isClosed = false,
            stationId = 1L,
            endAt = LocalDateTime.now().plusHours(2),
            createdAt = LocalDateTime.now(),
            updatedAt = null
        )
        
        whenever(meetingRepository.findById(meetingId)).thenReturn(meeting)

        val tokenResponse = inviteTokenService.generateInviteToken(meetingId, baseUrl)
        
        // When
        val result = inviteTokenService.validateInviteToken(tokenResponse.token)
        
        // Then
        assertNotNull(result)
        assertTrue(result.isValid)
        assertFalse(result.isExpired)
        assertEquals(meetingId, result.meetingId)
        assertEquals("유효한 토큰입니다.", result.message)
    }
    
    @Test
    fun `잘못된 토큰 검증 실패 테스트`() {
        // Given
        val invalidToken = "invalid_token"
        
        // When
        val result = inviteTokenService.validateInviteToken(invalidToken)
        
        // Then
        assertNotNull(result)
        assertFalse(result.isValid)
        assertTrue(result.isExpired)
        assertNull(result.meetingId)
        assertEquals("유효하지 않은 토큰입니다.", result.message)
    }
    
    @Test
    fun `존재하지 않는 모임 ID로 토큰 검증 실패 테스트`() {
        // Given
        val meetingId = 999L
        val baseUrl = "https://app.momuzzi.com"
        
        whenever(meetingRepository.findById(meetingId)).thenReturn(null)

        assertThrows<IllegalArgumentException> {
            inviteTokenService.generateInviteToken(meetingId, baseUrl)
        }

        val fakeToken = "fake_token_for_testing"
        
        // When
        val result = inviteTokenService.validateInviteToken(fakeToken)
        
        // Then
        assertNotNull(result)
        assertFalse(result.isValid)
        assertTrue(result.isExpired)
        assertNull(result.meetingId)
        assertEquals("유효하지 않은 토큰입니다.", result.message)
    }
}
