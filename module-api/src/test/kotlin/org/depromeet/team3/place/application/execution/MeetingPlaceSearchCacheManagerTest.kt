package org.depromeet.team3.place.application.execution

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.servlet.function.ServerResponse.async

class MeetingPlaceSearchCacheManagerTest {

    private lateinit var cacheManager: MeetingPlaceSearchCacheManager

    @BeforeEach
    fun setUp() {
        cacheManager = MeetingPlaceSearchCacheManager()
    }

    @Test
    fun `캐시 MISS 후 저장하면 캐시 HIT로 동일한 결과를 반환한다`() = runTest {
        // Given
        val meetingId = 1L
        val placeIds = listOf("place1", "place2", "place3")
        val placeWeights = mapOf("place1" to 0.8, "place2" to 0.6, "place3" to 0.4)
        val usedKeywords = listOf("강남역 한식", "강남역 분위기")

        // When: 캐시 MISS
        val cacheMiss = cacheManager.getCachedAutomaticResult(meetingId)

        // Then
        assertThat(cacheMiss).isNull()

        // When: 캐시 저장
        cacheManager.cacheAutomaticResult(meetingId, placeIds, placeWeights, usedKeywords)

        // When: 캐시 HIT
        val cacheHit = cacheManager.getCachedAutomaticResult(meetingId)

        // Then
        assertThat(cacheHit).isNotNull
        assertThat(cacheHit!!.placeIds).isEqualTo(placeIds)
        assertThat(cacheHit.placeWeights).isEqualTo(placeWeights)
        assertThat(cacheHit.usedKeywords).isEqualTo(usedKeywords)
    }

    @Test
    fun `빈 결과는 캐시에 저장하지 않는다`() = runTest {
        // Given
        val meetingId = 1L
        val emptyPlaceIds = emptyList<String>()

        // When: 빈 결과 저장 시도
        cacheManager.cacheAutomaticResult(meetingId, emptyPlaceIds, emptyMap(), emptyList())

        // Then: 캐시에 저장되지 않음
        val result = cacheManager.getCachedAutomaticResult(meetingId)
        assertThat(result).isNull()
    }

    @Test
    fun `캐시를 먼저 저장하면 동시 요청 시 모두 캐시된 결과를 받는다`() = runTest {
        // Given: 캐시를 먼저 저장
        val meetingId = 1L
        val placeIds = listOf("place1", "place2")
        val placeWeights = mapOf("place1" to 0.8)
        val usedKeywords = listOf("keyword1")
        
        cacheManager.cacheAutomaticResult(meetingId, placeIds, placeWeights, usedKeywords)

        // When: 같은 meetingId로 동시에 10개 요청
        val results = (1..10).map {
            async {
                cacheManager.getCachedAutomaticResult(meetingId)
            }
        }.awaitAll()

        // Then: 모두 캐시된 결과를 받음
        assertThat(results).hasSize(10)
        assertThat(results).allMatch { it != null }
        assertThat(results).allMatch { it!!.placeIds == placeIds }
        assertThat(results).allMatch { it!!.placeWeights == placeWeights }
    }

    @Test
    fun `다른 모임은 독립적으로 캐싱된다`() = runTest {
        // Given
        val meeting1 = 1L
        val meeting2 = 2L
        val placeIds1 = listOf("place1", "place2")
        val placeIds2 = listOf("place3", "place4")

        // When: 각각 다른 모임에 캐싱
        cacheManager.cacheAutomaticResult(meeting1, placeIds1, mapOf("place1" to 0.8), listOf("keyword1"))
        cacheManager.cacheAutomaticResult(meeting2, placeIds2, mapOf("place3" to 0.7), listOf("keyword2"))

        // Then: 각각 독립적으로 조회됨
        val result1 = cacheManager.getCachedAutomaticResult(meeting1)
        val result2 = cacheManager.getCachedAutomaticResult(meeting2)

        assertThat(result1).isNotNull
        assertThat(result2).isNotNull
        assertThat(result1!!.placeIds).isEqualTo(placeIds1)
        assertThat(result2!!.placeIds).isEqualTo(placeIds2)
    }

    @Test
    fun `가중치와 키워드가 정확히 보존되어 저장되고 조회된다`() = runTest {
        // Given
        val meetingId = 1L
        val placeIds = listOf("place1", "place2", "place3")
        val placeWeights = mapOf(
            "place1" to 0.85,
            "place2" to 0.62,
            "place3" to 0.41
        )
        val usedKeywords = listOf("강남역 한식", "강남역 회식", "강남역 분위기")

        // When
        cacheManager.cacheAutomaticResult(meetingId, placeIds, placeWeights, usedKeywords)
        val result = cacheManager.getCachedAutomaticResult(meetingId)

        // Then: 가중치와 키워드가 정확히 보존됨
        assertThat(result).isNotNull
        assertThat(result!!.placeWeights).containsExactlyInAnyOrderEntriesOf(placeWeights)
        assertThat(result.usedKeywords).containsExactlyElementsOf(usedKeywords)
    }
}

