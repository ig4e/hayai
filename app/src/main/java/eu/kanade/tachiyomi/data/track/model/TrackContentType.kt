package eu.kanade.tachiyomi.data.track.model

/**
 * Tracker content-type discriminator. Lets the tracker-selection UI filter services by what
 * the entry actually is — manga sources show only manga trackers, novel sources show only novel
 * trackers (NovelUpdates / NovelList).
 */
enum class TrackContentType {
    MANGA,
    NOVEL,
}
