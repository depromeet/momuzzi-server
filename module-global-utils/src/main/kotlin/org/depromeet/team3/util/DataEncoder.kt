package org.depromeet.team3.util

import java.util.*

object DataEncoder {

    fun encodeWithSeparator(separator: String, vararg data: String): String {
        val combined = data.joinToString(separator)
        return Base64.getEncoder().encodeToString(combined.toByteArray())
    }

    fun decodeWithSeparator(encodedData: String, separator: String): List<String>? {
        val decoded = try {
            String(Base64.getDecoder().decode(encodedData))
        } catch (e: Exception) {
            return null
        }
        return decoded.split(separator)
    }
}
