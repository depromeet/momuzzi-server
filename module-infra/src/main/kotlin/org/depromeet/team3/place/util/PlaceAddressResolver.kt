package org.depromeet.team3.place.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.depromeet.team3.place.PlaceQuery
import org.depromeet.team3.place.model.PlaceDetailsResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 주소 및 역 정보 처리 담당
 */
@Component
class PlaceAddressResolver(
    private val placeQuery: PlaceQuery
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
            logger.debug("addressDescriptor 처리 시작: placeId=${placeDetails?.id}")
            placeDetails?.addressDescriptor?.let { descriptor ->
                logger.debug("addressDescriptor 발견: landmarks=${descriptor.landmarks?.size ?: 0}개")
                // 1. addressDescriptor에서 역 정보 먼저 찾기
                val stationLandmark = findStationLandmark(descriptor)
                
                if (stationLandmark != null) {
                    logger.debug("역 정보 발견: ${stationLandmark.displayName?.text}, 거리: ${stationLandmark.straightLineDistanceMeters}m")
                    // 역 정보가 있으면 바로 사용
                    createAddressDescriptor(
                        name = stationLandmark.displayName?.text,
                        distance = stationLandmark.straightLineDistanceMeters?.toInt()
                    )
                } else {
                    logger.debug("역 정보 없음, Nearby Search API 호출")
                    // 2. 역 정보가 없으면 Nearby Search API 호출 (fallback)
                    searchNearbyStation(placeDetails.location)
                }
            } ?: run {
                logger.debug("addressDescriptor가 null입니다, 주소에서 역 정보 추출 시도")
                // 3. addressDescriptor가 없으면 주소에서 역 정보 추출 시도
                extractStationFromAddress(placeDetails?.formattedAddress) ?: run {
                    // 4. 역 정보도 없으면 기본 주소 정보 제공
                    logger.debug("역 정보도 없음, 기본 주소 정보 제공")
                    placeDetails?.formattedAddress?.let { address ->
                        AddressDescriptorResult(
                            description = address.split(",").firstOrNull()?.trim() ?: address
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("addressDescriptor 처리 실패", e)
            null
        }
    }
    
    /**
     * AddressDescriptor에서 역 정보 찾기
     */
    private fun findStationLandmark(
        descriptor: PlaceDetailsResponse.AddressDescriptor
    ): PlaceDetailsResponse.AddressDescriptor.Landmark? {
        return try {
            descriptor.landmarks
                ?.filter { landmark ->
                    val types = landmark.types ?: emptyList()
                    types.contains("transit_station") ||
                    types.contains("subway_station") ||
                    types.contains("train_station") ||
                    landmark.displayName?.text?.endsWith("역") == true
                }
                ?.minByOrNull { it.straightLineDistanceMeters ?: Double.MAX_VALUE }
        } catch (e: Exception) {
            logger.warn("역 정보 찾기 실패: ${e.message}")
            null
        }
    }
    
    /**
     * Nearby Search API를 통해 가까운 역 찾기
     */
    private suspend fun searchNearbyStation(
        location: PlaceDetailsResponse.Location?
    ): AddressDescriptorResult? {
        return location?.let {
            withContext(Dispatchers.IO) {
                val nearbyStation = placeQuery.searchNearbyStation(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
                nearbyStation?.let { station ->
                    createAddressDescriptor(
                        name = station.name,
                        distance = station.distance
                    )
                }
            }
        }
    }
    
    /**
     * 주소에서 역 정보 추출
     */
    private fun extractStationFromAddress(address: String?): AddressDescriptorResult? {
        return try {
            if (address.isNullOrBlank()) return null
            
            logger.debug("주소에서 역 정보 추출 시도: $address")
            
            // 주소에서 역 정보 패턴 찾기 (더 간단하고 확실한 패턴)
            val stationPatterns = listOf(
                Regex("([가-힣]+역)"),
                Regex("([가-힣]+역\\s*[0-9]*번?출구?)"),
                Regex("([가-힣]+역\\s*[0-9]*번?출구?\\s*[0-9]*m)"),
                Regex("([가-힣]+역\\s*[0-9]*m)"),
                Regex("([가-힣]+역\\s*도보\\s*[0-9]*분?)")
            )
            
            for (pattern in stationPatterns) {
                val match = pattern.find(address)
                if (match != null) {
                    val stationName = match.groupValues[1].trim()
                    logger.debug("주소에서 역 정보 발견: $stationName")
                    return AddressDescriptorResult(
                        description = stationName
                    )
                }
            }
            
            // 역 정보를 찾지 못했으면 기본 주소 정보라도 제공
            logger.debug("역 정보를 찾을 수 없음, 기본 주소 정보 제공")
            val basicAddress = address.split(",").firstOrNull()?.trim()
            if (!basicAddress.isNullOrBlank()) {
                return AddressDescriptorResult(
                    description = basicAddress
                )
            }
            
            null
        } catch (e: Exception) {
            logger.warn("주소에서 역 정보 추출 실패: ${e.message}")
            null
        }
    }
    
    /**
     * AddressDescriptor 생성
     */
    private fun createAddressDescriptor(
        name: String?,
        distance: Int?
    ): AddressDescriptorResult? {
        return try {
            if (distance == null || name == null) return null
            
            val walkingMinutes = maxOf(1, distance / 67)
            AddressDescriptorResult(
                description = "${name} 도보 약 ${walkingMinutes}분"
            )
        } catch (e: Exception) {
            logger.warn("AddressDescriptor 생성 실패: ${e.message}")
            null
        }
    }
    
    /**
     * AddressDescriptor 결과 DTO
     */
    data class AddressDescriptorResult(
        val description: String
    )
}
