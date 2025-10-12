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
    suspend fun textSearch(query: String, maxResults: Int = 10): PlacesTextSearchResponse? {
        return googlePlacesClient.textSearch(query, maxResults)
    }

    /**
     * Place Details 조회 (DB 캐싱 포함)
     * 1. DB에서 googlePlaceId로 조회
     * 2. 없으면 API 호출 후 DB 저장
     */
    suspend fun getPlaceDetails(placeId: String, openNow: Boolean? = null, link: String? = null): PlaceDetailsResponse? {
        // 1. DB 조회
        val cachedPlace = placeJpaRepository.findByGooglePlaceId(placeId)
        if (cachedPlace != null && !cachedPlace.isDeleted) {
            // DB에 있으면 PlaceDetailsResponse로 변환하여 반환
            return convertToPlaceDetailsResponse(cachedPlace)
        }
        
        // 2. API 호출
        val apiResponse = googlePlacesClient.getPlaceDetails(placeId) ?: return null
        
        // 3. DB 저장 (비동기로 처리, 응답은 바로 반환)
        savePlaceToDb(apiResponse, openNow, link)
        
        return apiResponse
    }
    
    /**
     * 여러 Place Details 조회 (배치 DB 캐싱 + 병렬 API 호출)
     */
    suspend fun getPlaceDetailsBatch(
        placeIds: List<String>,
        openNowMap: Map<String, Boolean?> = emptyMap(),
        linkMap: Map<String, String?> = emptyMap()
    ): Map<String, PlaceDetailsResponse> = coroutineScope {
        // 1. DB에서 일괄 조회 (1번의 쿼리)
        val cachedPlaces = placeJpaRepository.findByGooglePlaceIdIn(placeIds)
            .filter { !it.isDeleted }
            .associateBy { it.googlePlaceId!! }
        
        // 2. DB에 없는 것만 병렬로 API 호출
        val uncachedIds = placeIds.filter { it !in cachedPlaces.keys }
        val apiResults = uncachedIds.map { placeId ->
            async(Dispatchers.IO) {
                googlePlacesClient.getPlaceDetails(placeId)?.let { response ->
                    Triple(response, openNowMap[placeId], linkMap[placeId])
                }
            }
        }.awaitAll().filterNotNull()
        
        // 3. API 결과를 한 번에 DB 저장 (배치 INSERT)
        if (apiResults.isNotEmpty()) {
            savePlacesToDbBatch(apiResults)
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
     */
    private fun savePlaceToDb(response: PlaceDetailsResponse, openNow: Boolean?, link: String?) {
        try {
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
            placeJpaRepository.save(entity)
        } catch (e: Exception) {
            // 저장 실패해도 API 응답은 정상 반환
            println("DB 저장 실패: ${e.message}")
        }
    }
    
    /**
     * 여러 Place를 한 번에 DB 저장 (배치 INSERT)
     */
    private fun savePlacesToDbBatch(results: List<Triple<PlaceDetailsResponse, Boolean?, String?>>) {
        try {
            val entities = results.map { (response, openNow, link) ->
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
            placeJpaRepository.saveAll(entities)
        } catch (e: Exception) {
            println("DB 배치 저장 실패: ${e.message}")
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
            reviews = if (entity.topReviewRating != null && entity.topReviewText != null) {
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
