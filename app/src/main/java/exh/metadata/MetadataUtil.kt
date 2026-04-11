package exh.metadata

import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utility functions for metadata parsing.
 * TODO: This stub will be replaced by the full implementation from the metadata agent.
 */
object MetadataUtil {
    val EX_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm",
        Locale.US,
    )

    fun parseHumanReadableByteCount(text: String): Double? {
        val regex = Regex("([\\d.]+)\\s*(\\w+)")
        val match = regex.find(text) ?: return null
        val value = match.groupValues[1].toDoubleOrNull() ?: return null
        val unit = match.groupValues[2].lowercase()
        return when (unit) {
            "b", "bytes" -> value
            "kb", "kib" -> value * 1024
            "mb", "mib" -> value * 1024 * 1024
            "gb", "gib" -> value * 1024 * 1024 * 1024
            else -> null
        }
    }
}
