package org.depromeet.team3.place

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.depromeet.team3.place.client.GooglePlacesClient
import org.depromeet.team3.place.model.PlaceDetailsResponse
import org.depromeet.team3.place.model.PlacesTextSearchResponse
import org.springframework.stereotype.Repository

@Repository
class PlaceQuery(
    private val googlePlacesClient: GooglePlacesClient,
    private val placeJpaRepository: PlaceJpaRepository
) {
    /**
     * 텍스트 검색
     */
    suspend fun textSearch(query: String, maxResults: Int = 10): PlacesTextSearchResponse {
        return googlePlacesClient.textSearch(query, maxResults)
    }

    /**
     * Place Details 조회 (DB 캐싱 포함)
     * 1. DB에서 googlePlaceId로 조회
     * 2. 없으면 API 호출 후 DB 저장
     */
    suspend fun getPlaceDetails(placeId: String, openNow: Boolean? = null, link: String? = null): PlaceDetailsResponse {
        // 1. DB 조회
        val cachedPlace = placeJpaRepository.findByGooglePlaceId(placeId)
        if (cachedPlace != null && !cachedPlace.isDeleted) {
            // DB에 있으면 PlaceDetailsResponse로 변환하여 반환
            return convertToPlaceDetailsResponse(cachedPlace)
        }
        
        // 2. API 호출
        val apiResponse = googlePlacesClient.getPlaceDetails(placeId)
        
        // 3. DB 저장 (비동기로 처리, 응답은 바로 반환)
        savePlaceToDb(apiResponse, openNow, link)
        
        return apiResponse
    }
    
    /**
     * 여러 Place Details 조회 (배치 DB 캐싱 + 병렬 API 호출)
     * 
     * 주의: DB 캐싱은 placeIds 목록에 있는 것만 조회하므로, 
     * Google API에서 반환한 placeId만 전달해야 함
     */
    suspend fun getPlaceDetailsBatch(
        placeIds: List<String>,
        openNowMap: Map<String, Boolean?> = emptyMap(),
        linkMap: Map<String, String?> = emptyMap()
    ): Map<String, PlaceDetailsResponse> = coroutineScope {
        // 1. DB에서 일괄 조회 (1번의 쿼리) - placeIds에 포함된 것만 조회
        val cachedPlaces = placeJpaRepository.findByGooglePlaceIdIn(placeIds)
            .filter { !it.isDeleted }
            .mapNotNull { entity -> entity.googlePlaceId?.let { id -> id to entity } }
            .toMap()

        // 2. DB에 없는 것만 병렬로 API 호출
        val uncachedIds = placeIds.filter { it !in cachedPlaces.keys }
        val apiResults = uncachedIds.map { placeId ->
            async(Dispatchers.IO) {
                try {
                    val response = googlePlacesClient.getPlaceDetails(placeId)
                    Triple(response, openNowMap[placeId], linkMap[placeId])
                } catch (e: Exception) {
                    // 개별 place 조회 실패는 로깅만 하고 계속 진행
                    null
                }
            }
        }.awaitAll().filterNotNull()

        // 3. API 결과를 한 번에 DB 저장 (배치 INSERT) 후 저장된 엔티티 받기
        val savedEntities = if (apiResults.isNotEmpty()) {
            savePlacesToDbBatch(apiResults)
        } else {
            emptyList()
        }
        
        // 4. 결과 합치기
        val result = mutableMapOf<String, PlaceDetailsResponse>()
        cachedPlaces.forEach { (id, entity) ->
            result[id] = convertToPlaceDetailsResponse(entity)
        }
        apiResults.forEach { (response, _, _) ->
            result[response.id] = response
        }

        return@coroutineScope result
    }
    
    /**
     * DB에 저장 (API 응답 기반)
     * googlePlaceId가 이미 존재하면 저장하지 않음 (unique 제약조건)
     */
    private fun savePlaceToDb(response: PlaceDetailsResponse, openNow: Boolean?, link: String?): PlaceEntity? {
        try {
            // 이미 존재하는지 체크 (unique 제약조건 위반 방지)
            val existing = placeJpaRepository.findByGooglePlaceId(response.id)
            if (existing != null) {
                return existing
            }
            
            val entity = PlaceEntity(
                googlePlaceId = response.id,
                name = response.displayName?.text ?: "",
                address = response.formattedAddress ?: "",
                rating = response.rating ?: 0.0,
                userRatingsTotal = response.userRatingCount ?: 0,
                openNow = openNow,
                link = link,
                weekdayText = response.regularOpeningHours?.weekdayDescriptions?.joinToString("\n"),
                topReviewRating = response.reviews?.firstOrNull()?.rating,
                topReviewText = response.reviews?.firstOrNull()?.text?.text,
                priceRangeStart = response.priceRange?.startPrice?.let { "${it.currencyCode} ${it.units ?: ""}" },
                priceRangeEnd = response.priceRange?.endPrice?.let { "${it.currencyCode} ${it.units ?: ""}" },
                addressDescriptor = response.addressDescriptor?.landmarks?.firstOrNull()?.let {
                    "${it.displayName?.text ?: ""} 도보 약 ${(it.straightLineDistanceMeters ?: 0.0).toInt()}m"
                }
            )
            return placeJpaRepository.save(entity)
        } catch (e: Exception) {
            // 저장 실패해도 API 응답은 정상 반환
            println("DB 저장 실패: ${e.message}")
            return null
        }
    }
    
    /**
     * 여러 Place를 한 번에 DB 저장 (배치 INSERT) 후 저장된 엔티티 반환
     * 이미 존재하는 googlePlaceId는 저장하지 않음
     */
    private fun savePlacesToDbBatch(results: List<Triple<PlaceDetailsResponse, Boolean?, String?>>): List<PlaceEntity> {
        try {
            // 1. 이미 존재하는 placeId 확인
            val placeIds = results.map { it.first.id }
            val existingPlaces = placeJpaRepository.findByGooglePlaceIdIn(placeIds)
                .associateBy { it.googlePlaceId }
            
            // 2. 존재하지 않는 것만 필터링
            val newResults = results.filter { (response, _, _) ->
                response.id !in existingPlaces.keys
            }
            
            if (newResults.isEmpty()) {
                return existingPlaces.values.toList()
            }

            val entities = newResults.map { (response, openNow, link) ->
                PlaceEntity(
                    googlePlaceId = response.id,
                    name = response.displayName?.text ?: "",
                    address = response.formattedAddress ?: "",
                    rating = response.rating ?: 0.0,
                    userRatingsTotal = response.userRatingCount ?: 0,
                    openNow = openNow,
                    link = link,
                    weekdayText = response.regularOpeningHours?.weekdayDescriptions?.joinToString("\n"),
                    topReviewRating = response.reviews?.firstOrNull()?.rating,
                    topReviewText = response.reviews?.firstOrNull()?.text?.text,
                    priceRangeStart = response.priceRange?.startPrice?.let { "${it.currencyCode} ${it.units ?: ""}" },
                    priceRangeEnd = response.priceRange?.endPrice?.let { "${it.currencyCode} ${it.units ?: ""}" },
                    addressDescriptor = response.addressDescriptor?.landmarks?.firstOrNull()?.let {
                        "${it.displayName?.text ?: ""} 도보 약 ${(it.straightLineDistanceMeters ?: 0.0).toInt()}m"
                    }
                )
            }
            
            val savedEntities = placeJpaRepository.saveAll(entities).toList()
            return existingPlaces.values.toList() + savedEntities
        } catch (e: Exception) {
            println("DB 배치 저장 실패: ${e.message}")
            return emptyList()
        }
    }
    
    /**
     * Entity를 PlaceDetailsResponse로 변환
     */
    private fun convertToPlaceDetailsResponse(entity: PlaceEntity): PlaceDetailsResponse {
        return PlaceDetailsResponse(
            id = entity.googlePlaceId!!,
            displayName = PlaceDetailsResponse.DisplayName(text = entity.name),
            formattedAddress = entity.address,
            rating = entity.rating,
            userRatingCount = entity.userRatingsTotal,
            currentOpeningHours = if (entity.openNow != null || !entity.weekdayText.isNullOrBlank()) {
                PlaceDetailsResponse.CurrentOpeningHours(
                    openNow = entity.openNow,
                    weekdayDescriptions = entity.weekdayText?.split("\n")
                )
            } else null,
            regularOpeningHours = if (!entity.weekdayText.isNullOrBlank()) {
                PlaceDetailsResponse.OpeningHours(
                    weekdayDescriptions = entity.weekdayText.split("\n")
                )
            } else null,
            reviews = if (entity.topReviewRating != null && !entity.topReviewText.isNullOrBlank()) {
                listOf(
                    PlaceDetailsResponse.Review(
                        authorAttribution = PlaceDetailsResponse.Review.AuthorAttribution(""),
                        rating = entity.topReviewRating,
                        relativePublishTimeDescription = "",
                        text = PlaceDetailsResponse.Review.TextContent(entity.topReviewText)
                    )
                )
            } else null,
            priceRange = if (entity.priceRangeStart != null || entity.priceRangeEnd != null) {
                PlaceDetailsResponse.PriceRange(
                    startPrice = entity.priceRangeStart?.let { parseMoney(it) },
                    endPrice = entity.priceRangeEnd?.let { parseMoney(it) }
                )
            } else null,
            addressDescriptor = if (!entity.addressDescriptor.isNullOrBlank()) {
                PlaceDetailsResponse.AddressDescriptor(
                    landmarks = listOf(
                        PlaceDetailsResponse.AddressDescriptor.Landmark(
                            name = "",
                            displayName = PlaceDetailsResponse.AddressDescriptor.Landmark.TextContent(
                                text = entity.addressDescriptor
                            )
                        )
                    )
                )
            } else null
        )
    }
    
    /**
     * "KRW 10000" 형식을 Money로 파싱
     */
    private fun parseMoney(priceStr: String): PlaceDetailsResponse.PriceRange.Money {
        val parts = priceStr.split(" ")
        return PlaceDetailsResponse.PriceRange.Money(
            currencyCode = parts.getOrNull(0) ?: "KRW",
            units = parts.getOrNull(1)
        )
    }
    
    /**
     * 주변 지하철역 검색
     */
    suspend fun searchNearbyStation(latitude: Double, longitude: Double): NearbyStationInfo? {
        return try {
            val response = googlePlacesClient.searchNearby(latitude, longitude)
            val station = response.places?.firstOrNull() ?: return null
            
            // 거리 계산 (Haversine formula)
            val distance = calculateDistance(
                lat1 = latitude,
                lon1 = longitude,
                lat2 = station.location.latitude,
                lon2 = station.location.longitude
            )
            
            NearbyStationInfo(
                name = station.displayName.text,
                distance = distance.toInt()
            )
        } catch (e: Exception) {
            // 주변 역 검색 실패 시 null 반환 (선택적 기능)
            null
        }
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
    
    /**
     * Google Place ID 목록으로 Place 엔티티 조회
     */
    fun findByGooglePlaceIds(googlePlaceIds: List<String>): List<PlaceEntity> {
        return placeJpaRepository.findByGooglePlaceIdIn(googlePlaceIds)
    }
    
    data class NearbyStationInfo(
        val name: String,
        val distance: Int
    )
}
