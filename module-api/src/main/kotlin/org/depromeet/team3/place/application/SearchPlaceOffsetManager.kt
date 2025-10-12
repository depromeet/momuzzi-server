package org.depromeet.team3.place.application

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 검색어별 offset 관리 및 동시성 제어
 */
@Component
class SearchPlaceOffsetManager(
    private val maxCallCount: Int = 2
) {
    private val logger = LoggerFactory.getLogger(SearchPlaceOffsetManager::class.java)
    
    /**
     * 검색어별 현재 offset을 관리하는 메모리 캐시
     */
    private val queryOffsetCache = Caffeine.newBuilder()
        .expireAfterAccess(60, TimeUnit.MINUTES)
        .maximumSize(500)
        .build<String, Int>()
    
    /**
     * 검색어별 Mutex를 관리하는 맵
     */
    private val queryMutexMap = ConcurrentHashMap<String, Mutex>()

    /**
     * Offset 관리 + Mutex 보호
     * 같은 query에 대한 동시 요청을 Mutex로 직렬화하여 offset을 원자적으로 읽고 갱신
     * 
     * @return Pair<startIndex, shouldReturnEmpty>
     */
    suspend fun <T> selectWithOffset(
        queryKey: String,
        maxResults: Int,
        items: List<T>
    ): List<T>? {
        val mutex = queryMutexMap.computeIfAbsent(queryKey) { Mutex() }
        
        var startIndex = 0
        var shouldReturnEmpty = false
        
        try {
            mutex.withLock {
                val currentOffset = queryOffsetCache.getIfPresent(queryKey) ?: 0
                val currentCallCount = currentOffset / maxResults
                
                when {
                    currentCallCount >= maxCallCount -> {
                        logger.debug("검색어의 최대 호출 횟수 도달: query=$queryKey")
                        queryOffsetCache.invalidate(queryKey)
                        shouldReturnEmpty = true
                    }
                    currentOffset >= items.size -> {
                        logger.debug("검색어의 offset이 결과 크기 초과: query=$queryKey")
                        queryOffsetCache.invalidate(queryKey)
                        shouldReturnEmpty = true
                    }
                    else -> {
                        startIndex = currentOffset
                        queryOffsetCache.put(queryKey, currentOffset + maxResults)
                        logger.debug("검색어 offset 갱신: query=$queryKey, offset: $currentOffset -> ${currentOffset + maxResults}")
                    }
                }
            }
        } finally {
            // 캐시가 만료되면 mutex도 제거 (메모리 누수 방지)
            if (queryOffsetCache.getIfPresent(queryKey) == null) {
                queryMutexMap.remove(queryKey)
            }
        }
        
        if (shouldReturnEmpty) {
            return null
        }
        
        val endIndex = minOf(startIndex + maxResults, items.size)
        return items.subList(startIndex, endIndex)
    }
}
