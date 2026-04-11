package exh.util

fun String?.nullIfBlank(): String? = if (isNullOrBlank()) null else this
fun String.trimAll(): String = trim().replace(Regex("\\s+"), " ")
fun String?.trimOrNull(): String? = this?.trim()?.ifBlank { null }
fun List<String>.dropBlank(): List<String> = filter { it.isNotBlank() }
inline fun <T> ignore(block: () -> T): T? = try { block() } catch (_: Exception) { null }
