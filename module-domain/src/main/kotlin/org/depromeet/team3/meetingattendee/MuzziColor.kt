package org.depromeet.team3.meetingattendee

enum class MuzziColor {
    DEFAULT,
    STRAWBERRY,
    MATCHA,
    ORANGE,
    GRAPE,
    CHOCOLATE,
    MILK,
    MINT,
    SWEET_POTATO,
    PISTACHIO
    ;

    companion object {
        fun getOrDefault(name: String?): MuzziColor {
            return if (name.isNullOrBlank()) {
                DEFAULT
            } else {
                try {
                    valueOf(name.uppercase())
                } catch (e: IllegalArgumentException) {
                    DEFAULT
                }
            }
        }
    }
}