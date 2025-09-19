package org.depromeet.team3.station.util

import org.depromeet.team3.station.StationEntity

/**
 * Station 관련 테스트 데이터 팩토리 클래스
 */
object StationTestDataFactory {

    fun createStationEntity(
        id: Long = 1L,
        name: String = "테스트역",
        locX: Double = 37.5665,
        locY: Double = 126.9780,
        isDeleted: Boolean = false
    ): StationEntity {
        return StationEntity(
            id = id,
            name = name,
            locX = locX,
            locY = locY,
            isDeleted = isDeleted
        )
    }
}
