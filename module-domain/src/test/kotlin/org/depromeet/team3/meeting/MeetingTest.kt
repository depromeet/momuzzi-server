package org.depromeet.team3.meeting

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MeetingTest {

    @Test
    fun `Meeting 생성한다`() {
        // given
        val now = LocalDateTime.now()
        val hostUserId = 1L
        val attendeeCount = 5

        // when
        val meeting = Meeting(
            hostUserId = hostUserId,
            attendeeCount = attendeeCount,
            createdAt = now
        )

        // then
        assertNotNull(meeting)
        assertEquals(hostUserId, meeting.hostUserId)
        assertEquals(attendeeCount, meeting.attendeeCount)
        assertEquals(false, meeting.isClosed)
        assertNull(meeting.endAt)
        assertEquals(now, meeting.createdAt)
        assertNull(meeting.updatedAt)
    }

    @Test
    fun `data class의 equals와 hashCode가 올바르게 작동한다`() {
        // given
        val now = LocalDateTime.now()
        val meeting1 = Meeting(
            id = 1L,
            hostUserId = 1L,
            attendeeCount = 5,
            createdAt = now
        )

        val meeting2 = Meeting(
            id = 1L,
            hostUserId = 1L,
            attendeeCount = 5,
            createdAt = now
        )

        // when & then
        assertEquals(meeting1, meeting2)
        assertEquals(meeting1.hashCode(), meeting2.hashCode())
    }
}
