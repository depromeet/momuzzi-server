package org.depromeet.team3.place.util

import org.depromeet.team3.place.model.PlaceDetailsResponse
import org.depromeet.team3.place.model.PlacesSearchResponse

object PlaceTestDataFactory {
    
    fun createGooglePlacesSearchResponse(
        resultCount: Int = 5,
        status: String = "OK"
    ): PlacesSearchResponse {
        val results = (1..resultCount).map { index ->
            PlacesSearchResponse.PlaceResult(
                placeId = "place_id_$index",
                name = "맛집 $index",
                formattedAddress = "서울시 강남구 $index",
                rating = 4.5,
                userRatingsTotal = 100,
                openingHours = PlacesSearchResponse.PlaceResult.OpeningHours(openNow = true),
                url = "https://example.com/place_$index",
                photos = listOf(
                    PlacesSearchResponse.PlaceResult.Photo(
                        photoReference = "photo_ref_$index",
                        height = 400,
                        width = 400
                    )
                )
            )
        }
        
        return PlacesSearchResponse(
            results = results,
            status = status
        )
    }
    
    fun createGooglePlaceDetailsResponse(
        placeId: String = "place_id_1",
        status: String = "OK"
    ): PlaceDetailsResponse {
        return PlaceDetailsResponse(
            result = PlaceDetailsResponse.PlaceDetail(
                openingHours = PlaceDetailsResponse.PlaceDetail.OpeningHours(
                    weekdayText = listOf(
                        "월요일: 오전 10:00~오후 10:00",
                        "화요일: 오전 10:00~오후 10:00"
                    )
                ),
                reviews = listOf(
                    PlaceDetailsResponse.PlaceDetail.Review(
                        authorName = "리뷰어1",
                        rating = 5.0,
                        relativeTimeDescription = "1주 전",
                        text = "정말 맛있어요!",
                        time = System.currentTimeMillis() / 1000
                    ),
                    PlaceDetailsResponse.PlaceDetail.Review(
                        authorName = "리뷰어2",
                        rating = 4.0,
                        relativeTimeDescription = "2주 전",
                        text = "괜찮습니다",
                        time = System.currentTimeMillis() / 1000
                    )
                ),
                photos = listOf(
                    PlaceDetailsResponse.PlaceDetail.Photo(
                        photoReference = "detail_photo_ref_1",
                        height = 800,
                        width = 800
                    )
                )
            ),
            status = status
        )
    }
}
