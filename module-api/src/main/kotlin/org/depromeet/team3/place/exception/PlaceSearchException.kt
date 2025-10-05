package org.depromeet.team3.place.exception

class PlaceSearchException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
