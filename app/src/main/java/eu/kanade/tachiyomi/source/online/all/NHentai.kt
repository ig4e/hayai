package eu.kanade.tachiyomi.source.online.all

import android.content.Context
import eu.kanade.tachiyomi.source.online.HttpSource
import exh.source.DelegatedHttpSource

/**
 * NHentai delegated source for EXH metadata handling.
 *
 * TODO: Full implementation will be provided by the EH sources agent.
 */
class NHentai(
    originalSource: HttpSource,
    context: Context,
) : DelegatedHttpSource(originalSource, context)
