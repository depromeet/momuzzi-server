package org.depromeet.team3.place.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.depromeet.team3.place.client.GooglePlacesClient
import org.depromeet.team3.place.model.PlaceDetailsResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 주소 및 역 정보 처리 담당
 */
@Component
class PlaceAddressResolver(
    private val googlePlacesClient: GooglePlacesClient
) {
    private val logger = LoggerFactory.getLogger(PlaceAddressResolver::class.java)

    /**
     * AddressDescriptor 처리
     * 1. addressDescriptor에서 역 정보 찾기
     * 2. 없으면 Nearby Search API 호출 (fallback)
     */
    suspend fun resolveAddressDescriptor(
        placeDetails: PlaceDetailsResponse?
    ): AddressDescriptorResult? {
        return try {
            val placeName = placeDetails?.displayName?.text ?: "unknown"

            // 1. addressDescriptor에서 역 정보 먼저 찾기
            val fromDescriptor = placeDetails?.addressDescriptor?.let { descriptor ->
                val stationLandmark = findStationLandmark(descriptor)
                if (stationLandmark != null) {
                    createAddressDescriptor(
                        name = stationLandmark.displayName?.text,
                        distance = stationLandmark.straightLineDistanceMeters?.toInt()
                    )
                } else {
                    null
                }
            }
            
            // 2. addressDescriptor에 역 정보가 없으면 Nearby Search API 호출
            if (fromDescriptor != null) {
                return fromDescriptor
            }
            
            val fromNearbySearch = searchNearbyStation(placeDetails?.location)
            if (fromNearbySearch != null) {
                return fromNearbySearch
            }
            
            // 3. Nearby Search에도 없으면 주소에서 역 정보 추출 시도
            extractStationFromAddress(placeDetails?.formattedAddress)
        } catch (e: Exception) {
            logger.warn("addressDescriptor 처리 실패", e)
            null
        }
    }
    
    /**
     * AddressDescriptor에서 역 정보 찾기
     * 역으로 끝나는 이름만 허용 (일반 건물 제외)
     */
    private fun findStationLandmark(
        descriptor: PlaceDetailsResponse.AddressDescriptor
    ): PlaceDetailsResponse.AddressDescriptor.Landmark? {
        return try {
            descriptor.landmarks
                ?.filter { landmark ->
                    val displayName = landmark.displayName?.text ?: return@filter false
                    
                    // isValidStationName으로 통합 검증
                    isValidStationName(displayName)
                }
                ?.minByOrNull { it.straightLineDistanceMeters ?: Double.MAX_VALUE }
        } catch (e: Exception) {
            logger.warn("역 정보 찾기 실패: ${e.message}")
            null
        }
    }
    
    /**
     * Nearby Search API를 통해 가까운 역 찾기
     * 반경 2000m까지 검색 (도보 25분 거리)
     */
    private suspend fun searchNearbyStation(
        location: PlaceDetailsResponse.Location?
    ): AddressDescriptorResult? {
        return location?.let {
            withContext(Dispatchers.IO) {
                try {
                    val response = googlePlacesClient.searchNearby(location.latitude, location.longitude, radius = 2000.0)
                    val station = response.places?.firstOrNull() ?: return@withContext null
                    
                    // 거리 계산 (Haversine formula)
                    val distance = calculateDistance(
                        lat1 = location.latitude,
                        lon1 = location.longitude,
                        lat2 = station.location.latitude,
                        lon2 = station.location.longitude
                    )
                    
                    createAddressDescriptor(
                        name = station.displayName.text,
                        distance = distance.toInt()
                    )
                } catch (e: Exception) {
                    logger.warn("주변 역 검색 실패: ${e.message}")
                    null
                }
            }
        }
    }
    
    /**
     * 두 지점 간의 거리 계산 (Haversine formula, 결과는 미터 단위)
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // 지구 반지름 (미터)
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * 주소에서 역 정보 추출
     * "역" 뒤에 다른 글자가 붙으면 매장명으로 간주하고 제외 (예: 경복궁역점)
     */
    private fun extractStationFromAddress(address: String?): AddressDescriptorResult? {
        return try {
            if (address.isNullOrBlank()) return null
            
            // 주소에서 역 정보 패턴 찾기
            // "역" 뒤에 공백, 숫자, "번", "출구", "m", "도보", "분"만 허용 (역점, 역삼 등 제외)
            val stationPatterns = listOf(
                Regex("([가-힣]+역)(?=\\s|[0-9]|번|출구|도보|$)"),  // 역 뒤에 특정 문자만 허용
                Regex("([가-힣]+역)\\s*[0-9]*번?출구"),
                Regex("([가-힣]+역)\\s*도보")
            )

            for (pattern in stationPatterns) {
                val match = pattern.find(address)
                if (match != null) {
                    val stationName = match.groupValues[1].trim()

                    // "역점", "역전", "역삼" 등 매장명/지명 제외
                    if (isValidStationName(stationName)) {
                        return AddressDescriptorResult(
                            description = stationName
                        )
                    }
                }
            }
            
            // 역 정보를 찾지 못하면 null 반환
            null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 유효한 역 이름인지 검증
     */
    private fun isValidStationName(name: String): Boolean {
        // "역"으로 끝나지 않으면 false
         if (!name.endsWith("역")) return false

        // 너무 짧으면 false (예: "역")
        if (name.length <= 1) return false
        
        // 일반적인 매장명/건물명 패턴 제외
        val invalidPatterns = listOf(
            "점", "동", "관", "타운", "마트",
            "빌딩", "센터", "건물"
        )
        if (invalidPatterns.any { name.contains(it) }) return false
        
        return true
    }
    
    /**
     * AddressDescriptor 생성
     * 역 이름이 맞는지 한 번 더 검증
     */
    private fun createAddressDescriptor(
        name: String?,
        distance: Int?
    ): AddressDescriptorResult? {
        if (distance == null || name == null) return null
        
        // 유효한 역 이름인지 검증
        if (!isValidStationName(name)) {
            return null
        }

        val walkingMinutes = maxOf(1, distance / 67)
        return AddressDescriptorResult(
            description = "${name} 도보 약 ${walkingMinutes}분"
        )
    }
    
    /**
     * AddressDescriptor 결과 DTO
     */
    data class AddressDescriptorResult(
        val description: String
    )
}