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
    private val removableClassTokens = setOf(
        "ad",
        "ads",
        "advert",
        "advertisement",
        "breadcrumb",
        "chapter-nav",
        "footer",
        "header",
        "nav",
        "navigation",
        "pagination",
        "share",
        "social",
    )

    fun parse(html: String, baseUrl: String?): List<NovelBlock> {
        val document = Jsoup.parseBodyFragment(html, baseUrl.orEmpty())
        document.outputSettings()
            .prettyPrint(false)
            .escapeMode(Entities.EscapeMode.xhtml)
            .syntax(Document.OutputSettings.Syntax.html)

        sanitize(document)

        val blocks = buildList {
            document.body().children().forEach { element ->
                collectBlocks(element, this)
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
                    element.attr("style").contains("visibility: hidden", ignoreCase = true) ||
                    element.classNames().any { className ->
                        val normalized = className.lowercase()
                        normalized in removableClassTokens ||
                            normalized.startsWith("ad-") ||
                            normalized.endsWith("-ad") ||
                            normalized.contains("advert")
                    }
            }
            .toList()
            .forEach { it.remove() }
    }

    private fun collectBlocks(element: Element, output: MutableList<NovelBlock>) {
        when (element.normalName()) {
            "h1" -> addTextBlock(element, TextStyle.Heading1, output)
            "h2" -> addTextBlock(element, TextStyle.Heading2, output)
            "h3" -> addTextBlock(element, TextStyle.Heading3, output)
            "h4" -> addTextBlock(element, TextStyle.Heading4, output)
            "h5" -> addTextBlock(element, TextStyle.Heading5, output)
            "h6" -> addTextBlock(element, TextStyle.Heading6, output)
            "p" -> addParagraphWithImages(element, output)
            "blockquote" -> addTextBlock(element, TextStyle.Quote, output)
            "pre", "code" -> addTextBlock(element, TextStyle.Code, output)
            "ul" -> addListBlock(element, ordered = false, output)
            "ol" -> addListBlock(element, ordered = true, output)
            "img" -> addImageBlock(element, output)
            "hr" -> output.add(NovelBlock.Divider)
            "br" -> Unit
            else -> {
                if (element.hasStructuralChildren()) {
                    element.children().forEach { collectBlocks(it, output) }
                } else {
                    addParagraphWithImages(element, output)
                }
            }
        }
    }

    private fun addParagraphWithImages(element: Element, output: MutableList<NovelBlock>) {
        val clone = element.clone()
        clone.select("img").remove()
        val html = clone.html().trim()
        if (clone.text().cleanVisibleText().isNotBlank()) {
            output.add(NovelBlock.Text(html))
        }
        element.select("img").forEach { addImageBlock(it, output) }
    }

    private fun addTextBlock(element: Element, style: TextStyle, output: MutableList<NovelBlock>) {
        val clone = element.clone()
        clone.select("img").remove()
        val html = when (style) {
            TextStyle.Code -> Entities.escape(clone.wholeText().cleanVisibleText())
            else -> clone.html().trim()
        }
        if (clone.text().cleanVisibleText().isNotBlank() || style == TextStyle.Code && html.isNotBlank()) {
            output.add(NovelBlock.Text(html, style))
        }
        element.select("img").forEach { addImageBlock(it, output) }
    }

    private fun addListBlock(element: Element, ordered: Boolean, output: MutableList<NovelBlock>) {
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
        element.select("img").forEach { addImageBlock(it, output) }
    }

    private fun addImageBlock(element: Element, output: MutableList<NovelBlock>) {
        val url = resolveImageUrl(element) ?: return
        val alt = element.attr("alt").trim().takeIf { it.isNotBlank() }
        output.add(NovelBlock.Image(url, alt))
    }

    private fun resolveImageUrl(element: Element): String? {
        val attrs = listOf("src", "data-src", "data-original", "data-lazy-src", "data-url")
        attrs.forEach { attr ->
            val absolute = element.absUrl(attr).trim()
            if (absolute.isHttpUrl()) return absolute

            val raw = element.attr(attr).trim()
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
