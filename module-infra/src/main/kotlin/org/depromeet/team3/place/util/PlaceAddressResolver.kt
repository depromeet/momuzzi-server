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
            placeDetails?.addressDescriptor?.let { descriptor ->
                // 1. addressDescriptor에서 역 정보 먼저 찾기
                val stationLandmark = findStationLandmark(descriptor)
                
                if (stationLandmark != null) {
                    // 역 정보가 있으면 바로 사용
                    createAddressDescriptor(
                        name = stationLandmark.displayName?.text,
                        distance = stationLandmark.straightLineDistanceMeters?.toInt()
                    )
                } else {
                    // 2. 역 정보가 없으면 Nearby Search API 호출 (fallback)
                    searchNearbyStation(placeDetails.location)
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
        return descriptor.landmarks
            ?.filter { landmark ->
                val types = landmark.types ?: emptyList()
                types.contains("transit_station") ||
                types.contains("subway_station") ||
                types.contains("train_station") ||
                landmark.displayName?.text?.endsWith("역") == true
            }
            ?.minByOrNull { it.straightLineDistanceMeters ?: Double.MAX_VALUE }
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
     * AddressDescriptor 생성
     */
    private fun createAddressDescriptor(
        name: String?,
        distance: Int?
    ): AddressDescriptorResult? {
        if (distance == null || name == null) return null
        
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
