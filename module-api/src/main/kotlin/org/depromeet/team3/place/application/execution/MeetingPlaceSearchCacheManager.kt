package org.depromeet.team3.place.application.execution

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Meeting별 Place 검색 결과 캐싱
 * 
 * 같은 모임에서 재요청 시:
 * - Google Places API 호출 없이 캐싱된 결과 반환
 * - 좋아요 정보만 실시간으로 업데이트
 * 
 * Automatic 검색(설문 기반)에 주로 사용:
 * - 같은 모임 → 설문 결과 동일 → 생성되는 키워드 조합 동일
 * - 첫 요청 결과(Place IDs + 가중치)를 통째로 캐싱
 */
@Component
class MeetingPlaceSearchCacheManager {
    private val logger = LoggerFactory.getLogger(MeetingPlaceSearchCacheManager::class.java)
    
    /**
     * Automatic 검색 결과 캐시
     * 캐시 키: meetingId
     * 캐시 값: AutomaticSearchResult (Place IDs + 가중치 + 키워드 목록)
     */
    private val automaticSearchCache = Caffeine.newBuilder()
        .expireAfterWrite(3, TimeUnit.DAYS)  // 3일 후 만료
        .maximumSize(100)                    // 최대 100개 모임 캐싱
        .removalListener<Long, AutomaticSearchResult> { key, _, _ ->
            if (key != null) {
                meetingMutexMap.remove(key)
            }
        }
        .build<Long, AutomaticSearchResult>()
    
    /**
     * meetingId별 Mutex
     * 같은 모임에 대한 동시 요청을 직렬화하여 중복 API 호출 방지
     */
    private val meetingMutexMap = ConcurrentHashMap<Long, Mutex>()
    
    /**
     * Automatic 검색 결과 조회
     * 
     * @param meetingId 모임 ID
     * @return 캐시된 검색 결과 (Place IDs + 가중치 + 키워드), 없으면 null
     */
    suspend fun getCachedAutomaticResult(meetingId: Long): AutomaticSearchResult? {
        val mutex = meetingMutexMap.computeIfAbsent(meetingId) { Mutex() }
        
        return mutex.withLock {
            val cached = automaticSearchCache.getIfPresent(meetingId)
            if (cached != null) {
                logger.info("자동 검색 캐시 HIT: meetingId=$meetingId, places=${cached.placeIds.size}개, keywords=${cached.usedKeywords}")
            } else {
                logger.debug("자동 검색 캐시 MISS: meetingId=$meetingId")
            }
            cached
        }
    }
    
    /**
     * Automatic 검색 결과를 캐시에 저장
     * 
     * @param meetingId 모임 ID
     * @param placeIds Google Place ID 리스트 (가중치 순서 보존)
     * @param placeWeights Place별 가중치 맵
     * @param usedKeywords 사용된 키워드 목록
     */
    suspend fun cacheAutomaticResult(
        meetingId: Long,
        placeIds: List<String>,
        placeWeights: Map<String, Double>,
        usedKeywords: List<String>
    ) {
        if (placeIds.isEmpty()) {
            logger.debug("빈 검색 결과는 캐싱하지 않음: meetingId=$meetingId")
            return
        }
        
        val mutex = meetingMutexMap.computeIfAbsent(meetingId) { Mutex() }
        
        mutex.withLock {
            val result = AutomaticSearchResult(
                placeIds = placeIds,
                placeWeights = placeWeights,
                usedKeywords = usedKeywords
            )
            automaticSearchCache.put(meetingId, result)
            logger.info("자동 검색 캐시 저장: meetingId=$meetingId, places=${placeIds.size}개, keywords=$usedKeywords")
        }
    }

    
    /**
     * Automatic 검색 결과
     */
    data class AutomaticSearchResult(
        val placeIds: List<String>,           // Google Place ID 리스트 (가중치 순서 보존)
        val placeWeights: Map<String, Double>, // Place별 가중치
        val usedKeywords: List<String>        // 사용된 키워드 목록
    )
    
    data class CacheStats(
        val size: Long,
        val hitCount: Long,
        val missCount: Long,
        val hitRate: Double
    )
}

