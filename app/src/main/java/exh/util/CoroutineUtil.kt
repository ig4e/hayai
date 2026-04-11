package exh.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

suspend fun <T> withIOContext(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }
suspend fun <T> withNonCancellableContext(block: suspend () -> T): T = withContext(NonCancellable) { block() }
