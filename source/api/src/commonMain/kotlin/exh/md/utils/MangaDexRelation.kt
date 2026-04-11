package exh.md.utils

enum class MangaDexRelation(val mdString: String?) {
    SIMILAR(null),
    MONOCHROME("monochrome"),
    MAIN_STORY("main_story"),
    ADAPTED_FROM("adapted_from"),
    BASED_ON("based_on"),
    PREQUEL("prequel"),
    SIDE_STORY("side_story"),
    DOUJINSHI("doujinshi"),
    SAME_FRANCHISE("same_franchise"),
    SHARED_UNIVERSE("shared_universe"),
    SEQUEL("sequel"),
    SPIN_OFF("spin_off"),
    ALTERNATE_STORY("alternate_story"),
    PRESERIALIZATION("preserialization"),
    COLORED("colored"),
    SERIALIZATION("serialization"),
    ALTERNATE_VERSION("alternate_version"),
    ;

    companion object {
        fun fromDex(mdString: String) = entries.find { it.mdString == mdString }
    }
}
