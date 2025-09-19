package org.depromeet.team3.meeting.util

import org.depromeet.team3.meeting.MeetingEntity
import org.depromeet.team3.station.StationEntity
import org.depromeet.team3.station.util.StationTestDataFactory
import org.depromeet.team3.user.UserEntity
import org.depromeet.team3.user.util.UserTestDataFactory

/**
 * Meeting 관련 테스트 데이터 팩토리 클래스
 */
object MeetingTestDataFactory {

    fun createMeetingEntity(
        id: Long = 1L,
        attendeeCount: Int = 2,
        isClosed: Boolean = false,
        endAt: java.time.LocalDateTime? = null,
        hostUser: UserEntity = UserTestDataFactory.createUserEntity(),
        station: StationEntity = StationTestDataFactory.createStationEntity()
    ): MeetingEntity {
        return MeetingEntity(
            id = id,
            attendeeCount = attendeeCount,
            isClosed = isClosed,
            endAt = endAt,
            hostUser = hostUser,
            station = station
        )
    }
}
