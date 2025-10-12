package org.depromeet.team3.place

import org.depromeet.team3.place.client.GooglePlacesClient
import org.depromeet.team3.place.model.PlaceDetailsResponse
import org.depromeet.team3.place.model.PlacesTextSearchResponse
import org.springframework.stereotype.Repository

@Repository
class PlaceQuery(
    private val googlePlacesClient: GooglePlacesClient
) {
    /**
     * 텍스트 검색
     */
    suspend fun textSearch(query: String, maxResults: Int = 10): PlacesTextSearchResponse? {
        return googlePlacesClient.textSearch(query, maxResults)
    }

    /**
     * Place Details 조회
     */
    suspend fun getPlaceDetails(placeId: String): PlaceDetailsResponse? {
        return googlePlacesClient.getPlaceDetails(placeId)
    }
    
    /**
     * 주변 지하철역 검색
     */
    suspend fun searchNearbyStation(latitude: Double, longitude: Double): NearbyStationInfo? {
        val response = googlePlacesClient.searchNearby(latitude, longitude)
        val station = response?.places?.firstOrNull() ?: return null
        
        // 거리 계산 (Haversine formula)
        val distance = calculateDistance(
            lat1 = latitude,
            lon1 = longitude,
            lat2 = station.location.latitude,
            lon2 = station.location.longitude
        )
        
        return NearbyStationInfo(
            name = station.displayName.text,
            distance = distance.toInt()
        )
    }
    
    /**
     * 두 지점 간의 거리 계산 (Haversine formula, 결과는 미터 단위)
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // 지구 반지름 (미터)
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    data class NearbyStationInfo(
        val name: String,
        val distance: Int
    )
}
