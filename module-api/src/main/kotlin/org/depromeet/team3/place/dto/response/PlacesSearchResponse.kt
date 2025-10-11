package org.depromeet.team3.place.dto.response

data class PlacesSearchResponse(
    val items: List<PlaceItem>
) {
    data class PlaceItem(
        val name: String,
        val address: String,
        val rating: Double?,
        val userRatingsTotal: Int?,
        val openNow: Boolean?,
        val photos: List<String>?,
        val link: String,
        val weekdayText: List<String>?,
        val topReview: Review?,
        val generativeSummary: String?,
        val priceRange: PriceRange?,
        val addressDescriptor: AddressDescriptor?
    ) {
        data class Review(
            val rating: Int,
            val text: String
        )
        
        data class PriceRange(
            val startPrice: String?,
            val endPrice: String?
        )
        
        data class AddressDescriptor(
            val description: String
        )
    }
}
