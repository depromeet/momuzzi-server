package org.depromeet.team3.place

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.depromeet.team3.place.client.GooglePlacesClient
import org.depromeet.team3.place.model.PlaceDetailsResponse
import org.depromeet.team3.place.model.PlacesTextSearchResponse
import org.depromeet.team3.place.util.PlaceAddressResolver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class PlaceQuery(
    private val googlePlacesClient: GooglePlacesClient,
    private val placeJpaRepository: PlaceJpaRepository,
    private val placeAddressResolver: org.depromeet.team3.place.util.PlaceAddressResolver
) {
    private val logger = LoggerFactory.getLogger(PlaceQuery::class.java)
    /**
     * 텍스트 검색
     */
    suspend fun textSearch(
        query: String, 
        maxResults: Int = 10,
        latitude: Double? = null,
        longitude: Double? = null,
        radius: Double = 3000.0
    ): PlacesTextSearchResponse {
        return googlePlacesClient.textSearch(query, maxResults, latitude, longitude, radius)
    }

    /**
     * Google `placeIds` 목록을 대상으로 DB 캐싱과 병렬 API 호출을 결합해 상세 정보를 조회한다.
     *
     * 처리 순서:
     * - 캐시된 엔티티를 읽어 삭제되지 않은 항목만 선별한다.
     * - 누락되었거나 정보가 부족한 `placeId`만 병렬(최대 32개)로 상세 API 호출을 수행한다.
     * - 단일 `@Transactional` 메서드에서 조회한 상세 정보를 저장하고 캐시를 갱신한다.
     * - 요청 순서를 유지한 맵 형태로 `PlaceDetailsResponse`를 반환한다.
     *
     * 메서드 입력으로는 Google Places API가 반환한 `placeId`만 전달해야 캐시가 일관성을 유지한다.
     */
    suspend fun getPlaceDetailsBatch(
        placeIds: List<String>,
        openNowMap: Map<String, Boolean?> = emptyMap(),
        linkMap: Map<String, String?> = emptyMap()
    ): Map<String, PlaceDetailsResponse> = supervisorScope {
        val cachedPlaces: MutableMap<String, PlaceEntity> = withContext(Dispatchers.IO) {
            placeJpaRepository.findByGooglePlaceIdIn(placeIds)
                .filter { !it.isDeleted }
                .mapNotNull { entity -> entity.googlePlaceId?.let { id -> id to entity } }
                .toMap()
                .toMutableMap()
        }

        val needingUpdateIds = cachedPlaces.values
            .filter { it.photos.isNullOrBlank() || !placeAddressResolver.isValidAddressDescriptor(it.addressDescriptor) }
            .mapNotNull { it.googlePlaceId }

        val uncachedIds = placeIds.filter { it !in cachedPlaces.keys }

        val idsToFetch = (needingUpdateIds + uncachedIds).distinct()
        val fetchResults = idsToFetch.map { placeId ->
            async(Dispatchers.IO.limitedParallelism(32)) {
                try {
                    val response = googlePlacesClient.getPlaceDetails(placeId)
                    Triple(response, openNowMap[placeId], linkMap[placeId])
                } catch (e: org.depromeet.team3.place.exception.PlaceSearchException) {
                    logger.warn("Place 상세 정보 조회 실패: placeId=$placeId, errorCode=${e.errorCode.code}, message=${e.message}")
                    null
                } catch (e: Exception) {
                    logger.error("Place 상세 정보 조회 중 예상치 못한 오류: placeId=$placeId", e)
                    null
                }
            }
        }.awaitAll().filterNotNull()

        if (fetchResults.isNotEmpty()) {
            val persistedEntities = updatePhotosInDbTransactional(fetchResults)
            persistedEntities.forEach { entity ->
                entity.googlePlaceId?.let { cachedPlaces[it] = entity }
            }
        }

        return@supervisorScope buildResponseFromEntities(cachedPlaces, placeIds)
    }
    
    /**
     * 여러 Place를 한 번에 DB 저장 (배치 INSERT) 후 저장된 엔티티 반환
     * 이미 존재하는 googlePlaceId는 저장하지 않음
     */
    /**
     * Entity를 PlaceDetailsResponse로 변환
     */
    private fun convertToPlaceDetailsResponse(entity: PlaceEntity): PlaceDetailsResponse {
        return try {
            PlaceDetailsResponse(
                id = entity.googlePlaceId ?: "",
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
                } else null,
                photos = if (!entity.photos.isNullOrBlank()) {
                    val photoList = entity.photos.split(",").map { photoName ->
                        PlaceDetailsResponse.Photo(
                            name = photoName.trim(),
                            widthPx = 400,
                            heightPx = 400
                        )
                    }
                    photoList
                } else {
                    null
                },
                location = if (entity.latitude != null && entity.longitude != null) {
                    PlaceDetailsResponse.Location(
                        latitude = entity.latitude,
                        longitude = entity.longitude
                    )
                } else null
            )
        } catch (e: Exception) {
            // 변환 실패 시 기본값으로 반환
            PlaceDetailsResponse(
                id = entity.googlePlaceId ?: "",
                displayName = PlaceDetailsResponse.DisplayName(text = entity.name),
                formattedAddress = entity.address,
                rating = entity.rating,
                userRatingCount = entity.userRatingsTotal,
                currentOpeningHours = null,
                regularOpeningHours = null,
                reviews = null,
                priceRange = null,
                addressDescriptor = null,
                photos = null,
                location = if (entity.latitude != null && entity.longitude != null) {
                    PlaceDetailsResponse.Location(
                        latitude = entity.latitude,
                        longitude = entity.longitude
                    )
                } else null
            )
        }
    }

    /**
     * 상세 결과를 DB에 저장하는 단일 진입점.
     * - 트랜잭션 경계를 이 메서드로 모으고
     * - 내부에서만 JPA saveAll을 호출하며
     * - 저장된 엔티티를 그대로 돌려줘 재조회 없이 캐시를 갱신하게 한다.
     */
    @Transactional
    suspend fun updatePhotosInDbTransactional(
        rows: List<Triple<PlaceDetailsResponse, Boolean?, String?>>
    ): List<PlaceEntity> = withContext(Dispatchers.IO) {
        if (rows.isEmpty()) {
            return@withContext emptyList()
        }

        val placeIds = rows.map { it.first.id }
        val existingPlaces = placeJpaRepository.findByGooglePlaceIdIn(placeIds)
            .associateBy { it.googlePlaceId }

        val entities = mutableListOf<PlaceEntity>()
        for ((response, openNow, link) in rows) {
            val existing = existingPlaces[response.id]
            val descriptorResult = placeAddressResolver.resolveAddressDescriptor(response)
            entities += mapToEntity(response, existing, openNow, link, descriptorResult)
        }

        placeJpaRepository.saveAll(entities).toList()
    }

    private fun mapToEntity(
        response: PlaceDetailsResponse,
        existing: PlaceEntity?,
        openNow: Boolean?,
        link: String?,
        descriptorResult: PlaceAddressResolver.AddressDescriptorResult?
    ): PlaceEntity {
        return PlaceEntity(
            id = existing?.id,
            googlePlaceId = existing?.googlePlaceId ?: response.id,
            name = response.displayName?.text ?: existing?.name ?: "",
            address = response.formattedAddress ?: existing?.address ?: "",
            latitude = response.location?.latitude ?: existing?.latitude,
            longitude = response.location?.longitude ?: existing?.longitude,
            rating = response.rating ?: existing?.rating ?: 0.0,
            userRatingsTotal = response.userRatingCount ?: existing?.userRatingsTotal ?: 0,
            openNow = openNow ?: existing?.openNow,
            link = link ?: existing?.link,
            weekdayText = response.regularOpeningHours?.weekdayDescriptions?.joinToString("\n") ?: existing?.weekdayText,
            topReviewRating = response.reviews?.firstOrNull()?.rating ?: existing?.topReviewRating,
            topReviewText = response.reviews?.firstOrNull()?.text?.text ?: existing?.topReviewText,
            priceRangeStart = response.priceRange?.startPrice?.let { "${it.currencyCode} ${it.units ?: ""}" } ?: existing?.priceRangeStart,
            priceRangeEnd = response.priceRange?.endPrice?.let { "${it.currencyCode} ${it.units ?: ""}" } ?: existing?.priceRangeEnd,
            addressDescriptor = descriptorResult?.description ?: existing?.addressDescriptor,
            photos = response.photos?.take(5)?.joinToString(",") { it.name } ?: existing?.photos,
            isDeleted = existing?.isDeleted ?: false
        )
    }

    /**
     * 요청한 placeIds 순서를 유지하며 PlaceEntity → PlaceDetailsResponse로 변환한다.
     */
    private fun buildResponseFromEntities(
        cachedPlaces: Map<String, PlaceEntity>,
        placeIds: List<String>
    ): Map<String, PlaceDetailsResponse> {
        return placeIds.mapNotNull { placeId ->
            cachedPlaces[placeId]?.let { placeId to convertToPlaceDetailsResponse(it) }
        }.toMap()
    }
    
    /**
     * "KRW 10000" 형식을 Money로 파싱
     */
    private fun parseMoney(priceStr: String): PlaceDetailsResponse.PriceRange.Money {
        return try {
            val parts = priceStr.split(" ")
            PlaceDetailsResponse.PriceRange.Money(
                currencyCode = parts.getOrNull(0) ?: "KRW",
                units = parts.getOrNull(1)
            )
        } catch (e: Exception) {
            // 파싱 실패 시 기본값 반환
            PlaceDetailsResponse.PriceRange.Money(
                currencyCode = "KRW",
                units = null
            )
        }
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
