package hayai.novel.reader

sealed interface NovelBlock {
    data class Text(
        val html: String,
        val style: TextStyle = TextStyle.Paragraph,
    ) : NovelBlock

    data class ListItems(
        val ordered: Boolean,
        val items: List<String>,
    ) : NovelBlock

    data class Image(
        val url: String,
        val alt: String?,
    ) : NovelBlock

    data object Divider : NovelBlock
}

enum class TextStyle {
    Paragraph,
    Heading1,
    Heading2,
    Heading3,
    Heading4,
    Heading5,
    Heading6,
    Quote,
    Code,
}
