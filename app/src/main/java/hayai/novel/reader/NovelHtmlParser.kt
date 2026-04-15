package hayai.novel.reader

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities

object NovelHtmlParser {
    private val textBlockTags = setOf(
        "p",
        "h1",
        "h2",
        "h3",
        "h4",
        "h5",
        "h6",
        "blockquote",
        "pre",
        "code",
    )
    private val structuralTags = textBlockTags + setOf("ul", "ol", "li", "img", "hr", "table")
    private val removableTags = setOf(
        "script",
        "style",
        "noscript",
        "iframe",
        "form",
        "button",
        "input",
        "select",
        "textarea",
        "canvas",
        "object",
        "embed",
        "svg",
    )
    fun parse(
        html: String,
        baseUrl: String?,
        imageUrlResolver: ((String) -> String?)? = null,
    ): List<NovelBlock> {
        val document = Jsoup.parseBodyFragment(html, baseUrl.orEmpty())
        document.outputSettings()
            .prettyPrint(false)
            .escapeMode(Entities.EscapeMode.xhtml)
            .syntax(Document.OutputSettings.Syntax.html)

        sanitize(document)

        val blocks = buildList {
            document.body().children().forEach { element ->
                collectBlocks(element, this, imageUrlResolver)
            }
        }

        if (blocks.isNotEmpty()) {
            return blocks
        }

        val fallbackText = document.body().wholeText().cleanVisibleText()
        return if (fallbackText.isBlank()) {
            emptyList()
        } else {
            listOf(NovelBlock.Text(Entities.escape(fallbackText)))
        }
    }

    private fun sanitize(document: Document) {
        document.allElements
            .filter { element ->
                element.normalName() in removableTags ||
                    element.hasAttr("hidden") ||
                    element.attr("aria-hidden").equals("true", ignoreCase = true) ||
                    element.attr("style").contains("display:none", ignoreCase = true) ||
                    element.attr("style").contains("display: none", ignoreCase = true) ||
                    element.attr("style").contains("visibility:hidden", ignoreCase = true) ||
                    element.attr("style").contains("visibility: hidden", ignoreCase = true)
            }
            .toList()
            .forEach { it.remove() }
    }

    private fun collectBlocks(
        element: Element,
        output: MutableList<NovelBlock>,
        imageUrlResolver: ((String) -> String?)?,
    ) {
        when (element.normalName()) {
            "h1" -> addTextBlock(element, TextStyle.Heading1, output, imageUrlResolver)
            "h2" -> addTextBlock(element, TextStyle.Heading2, output, imageUrlResolver)
            "h3" -> addTextBlock(element, TextStyle.Heading3, output, imageUrlResolver)
            "h4" -> addTextBlock(element, TextStyle.Heading4, output, imageUrlResolver)
            "h5" -> addTextBlock(element, TextStyle.Heading5, output, imageUrlResolver)
            "h6" -> addTextBlock(element, TextStyle.Heading6, output, imageUrlResolver)
            "p" -> addParagraphWithImages(element, output, imageUrlResolver)
            "blockquote" -> addTextBlock(element, TextStyle.Quote, output, imageUrlResolver)
            "pre", "code" -> addTextBlock(element, TextStyle.Code, output, imageUrlResolver)
            "ul" -> addListBlock(element, ordered = false, output, imageUrlResolver)
            "ol" -> addListBlock(element, ordered = true, output, imageUrlResolver)
            "img" -> addImageBlock(element, output, imageUrlResolver)
            "hr" -> output.add(NovelBlock.Divider)
            "br" -> Unit
            else -> {
                if (element.hasStructuralChildren()) {
                    element.children().forEach { collectBlocks(it, output, imageUrlResolver) }
                } else {
                    addParagraphWithImages(element, output, imageUrlResolver)
                }
            }
        }
    }

    private fun addParagraphWithImages(
        element: Element,
        output: MutableList<NovelBlock>,
        imageUrlResolver: ((String) -> String?)?,
    ) {
        val clone = element.clone()
        clone.select("img").remove()
        val html = clone.html().trim()
        if (clone.text().cleanVisibleText().isNotBlank()) {
            output.add(NovelBlock.Text(html))
        }
        element.select("img").forEach { addImageBlock(it, output, imageUrlResolver) }
    }

    private fun addTextBlock(
        element: Element,
        style: TextStyle,
        output: MutableList<NovelBlock>,
        imageUrlResolver: ((String) -> String?)?,
    ) {
        val clone = element.clone()
        clone.select("img").remove()
        val html = when (style) {
            TextStyle.Code -> Entities.escape(clone.wholeText().cleanVisibleText())
            else -> clone.html().trim()
        }
        if (clone.text().cleanVisibleText().isNotBlank() || style == TextStyle.Code && html.isNotBlank()) {
            output.add(NovelBlock.Text(html, style))
        }
        element.select("img").forEach { addImageBlock(it, output, imageUrlResolver) }
    }

    private fun addListBlock(
        element: Element,
        ordered: Boolean,
        output: MutableList<NovelBlock>,
        imageUrlResolver: ((String) -> String?)?,
    ) {
        val items = element.children()
            .filter { it.normalName() == "li" }
            .mapNotNull { item ->
                val clone = item.clone()
                clone.select("img").remove()
                clone.html().trim().takeIf { clone.text().cleanVisibleText().isNotBlank() }
            }
        if (items.isNotEmpty()) {
            output.add(NovelBlock.ListItems(ordered, items))
        }
        element.select("img").forEach { addImageBlock(it, output, imageUrlResolver) }
    }

    private fun addImageBlock(
        element: Element,
        output: MutableList<NovelBlock>,
        imageUrlResolver: ((String) -> String?)?,
    ) {
        val url = resolveImageUrl(element, imageUrlResolver) ?: return
        val alt = element.attr("alt").trim().takeIf { it.isNotBlank() }
        output.add(NovelBlock.Image(url, alt))
    }

    private fun resolveImageUrl(element: Element, imageUrlResolver: ((String) -> String?)?): String? {
        val attrs = listOf("src", "data-src", "data-original", "data-lazy-src", "data-url")
        attrs.forEach { attr ->
            val raw = element.attr(attr).trim()
            if (raw.isNotBlank()) {
                val resolved = imageUrlResolver
                    ?.let { resolver -> runCatching { resolver(raw)?.trim() }.getOrNull() }
                if (resolved?.isHttpUrl() == true) return resolved
            }

            val absolute = element.absUrl(attr).trim()
            if (absolute.isHttpUrl()) return absolute

            when {
                raw.isHttpUrl() -> return raw
                raw.startsWith("//") -> return "https:$raw"
            }
        }
        return null
    }

    private fun Element.hasStructuralChildren(): Boolean {
        return children().any { child ->
            child.normalName() in structuralTags || child.hasStructuralChildren()
        }
    }

    private fun String.cleanVisibleText(): String {
        return replace('\u00A0', ' ').trim()
    }

    private fun String.isHttpUrl(): Boolean {
        return startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)
    }
}
