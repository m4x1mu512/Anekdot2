package com.example.data.remote

import com.example.data.model.JokeItem
import com.example.data.model.JokeSource
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest

object HtmlJokeParser {

    private val windows1251: Charset = Charset.forName("windows-1251")
    private val koi8r: Charset = runCatching { Charset.forName("KOI8-R") }.getOrDefault(Charsets.UTF_8)
    private val cyrillicRegex = Regex("[А-Яа-яЁё]{3,}")

    fun decodeBytes(bytes: ByteArray, contentTypeHeader: String?): String {
        if (bytes.isEmpty()) return ""

        val headerCharset = extractCharsetFromHeader(contentTypeHeader)
        if (headerCharset != null) {
            return String(bytes, headerCharset)
        }

        val asciiPreview = String(bytes, 0, minOf(bytes.size, 2500), Charsets.ISO_8859_1)
        val metaCharset = extractCharsetFromMeta(asciiPreview)
        if (metaCharset != null) {
            return String(bytes, metaCharset)
        }

        return if (isValidUtf8(bytes)) {
            String(bytes, Charsets.UTF_8)
        } else {
            String(bytes, windows1251)
        }
    }

    private fun extractCharsetFromHeader(contentType: String?): Charset? {
        if (contentType.isNullOrBlank()) return null
        val match = Regex("charset=([a-zA-Z0-9_-]+)", RegexOption.IGNORE_CASE).find(contentType)
            ?: return null
        return resolveCharsetName(match.groupValues[1])
    }

    private fun extractCharsetFromMeta(preview: String): Charset? {
        val match = Regex("charset=[\"']?([a-zA-Z0-9_-]+)", RegexOption.IGNORE_CASE).find(preview)
            ?: return null
        return resolveCharsetName(match.groupValues[1])
    }

    private fun resolveCharsetName(name: String): Charset? {
        val lower = name.lowercase()
        return when {
            lower.contains("1251") -> windows1251
            lower.contains("utf-8") || lower.contains("utf8") -> Charsets.UTF_8
            lower.contains("koi8") -> koi8r
            else -> runCatching { Charset.forName(name) }.getOrNull()
        }
    }

    private fun isValidUtf8(bytes: ByteArray): Boolean {
        return try {
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            decoder.decode(ByteBuffer.wrap(bytes))
            true
        } catch (_: CharacterCodingException) {
            false
        }
    }

    fun parseAnekdotRuHtml(html: String, query: String, fallbackUrl: String): List<JokeItem> {
        val results = mutableListOf<JokeItem>()
        val topicBoxes = extractBalancedDivsByClass(html, "topicbox")

        if (topicBoxes.isNotEmpty()) {
            for (box in topicBoxes) {
                val idMatch = Regex("data-id=[\"'](\\d+)[\"']").find(box.openTag)
                val permalink = idMatch?.groupValues?.get(1)?.let { "https://www.anekdot.ru/id/$it/" }
                    ?: fallbackUrl
                val textBlocks = extractBalancedDivsByClass(box.innerHtml, "text")
                for (tb in textBlocks) {
                    val cleaned = cleanHtmlToPlainJoke(tb.innerHtml)
                    if (isValidRussianJoke(cleaned)) {
                        results.add(buildJokeItem(cleaned, JokeSource.ANEKDOT_RU, permalink, query))
                    }
                }
            }
        } else {
            val textBlocks = extractBalancedDivsByClass(html, "text")
            for (tb in textBlocks) {
                val cleaned = cleanHtmlToPlainJoke(tb.innerHtml)
                if (isValidRussianJoke(cleaned)) {
                    results.add(buildJokeItem(cleaned, JokeSource.ANEKDOT_RU, fallbackUrl, query))
                }
            }
        }
        return results.distinctBy { it.id }
    }

    fun parseAnekdotRuRss(xml: String, query: String): List<JokeItem> {
        val results = mutableListOf<JokeItem>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var insideItem = false
            var currentDescription = ""
            var currentLink = "https://www.anekdot.ru"
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name.lowercase()) {
                            "item" -> {
                                insideItem = true
                                currentDescription = ""
                                currentLink = "https://www.anekdot.ru"
                            }
                            "description" -> if (insideItem) {
                                currentDescription = runCatching { parser.nextText() }.getOrDefault("")
                            }
                            "link" -> if (insideItem) {
                                val linkText = runCatching { parser.nextText().trim() }.getOrDefault("")
                                if (linkText.startsWith("http")) currentLink = linkText
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.equals("item", ignoreCase = true) && insideItem) {
                            insideItem = false
                            val cleaned = cleanHtmlToPlainJoke(currentDescription)
                            if (isValidRussianJoke(cleaned)) {
                                results.add(
                                    buildJokeItem(
                                        text = cleaned,
                                        source = JokeSource.ANEKDOT_RU,
                                        sourceUrl = currentLink,
                                        query = query
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
            // Fallback regex extraction if XML has malformed entities
            val itemRegex = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
            for (match in itemRegex.findAll(xml)) {
                val block = match.groupValues[1]
                val desc = Regex("<description><!\\[CDATA\\[(.*?)]]></description>|<description>(.*?)</description>", RegexOption.DOT_MATCHES_ALL)
                    .find(block)
                    ?.let { it.groupValues[1].ifEmpty { it.groupValues[2] } }
                    .orEmpty()
                val link = Regex("<link>(.*?)</link>").find(block)?.groupValues?.get(1)?.trim()
                    ?: "https://www.anekdot.ru"
                val cleaned = cleanHtmlToPlainJoke(desc)
                if (isValidRussianJoke(cleaned)) {
                    results.add(buildJokeItem(cleaned, JokeSource.ANEKDOT_RU, link, query))
                }
            }
        }
        return results.distinctBy { it.id }
    }

    fun parseNekdoRuHtml(html: String, query: String, fallbackUrl: String): List<JokeItem> {
        val results = mutableListOf<JokeItem>()
        val textBlocks = extractBalancedDivsByClass(html, "text")
        for (tb in textBlocks) {
            val idMatch = Regex("id=[\"'](\\d+)[\"']").find(tb.openTag)
            val linkMatch = Regex("href=[\"'](/[^\"']+/)[\"']").find(tb.innerHtml)
            val permalink = when {
                idMatch != null -> "https://nekdo.ru/${idMatch.groupValues[1]}/"
                linkMatch != null -> "https://nekdo.ru${linkMatch.groupValues[1]}"
                else -> fallbackUrl
            }
            val cleaned = cleanHtmlToPlainJoke(tb.innerHtml)
            if (isValidRussianJoke(cleaned)) {
                results.add(buildJokeItem(cleaned, JokeSource.NEKDO_RU, permalink, query))
            }
        }
        return results.distinctBy { it.id }
    }

    fun parseAnekdotyRuHtml(
        html: String,
        query: String,
        fallbackUrl: String,
        categoryLabel: String = ""
    ): List<JokeItem> {
        val results = mutableListOf<JokeItem>()
        val holderBlocks = extractBalancedDivsByClass(html, "holder-body")

        for (hb in holderBlocks) {
            val linkMatch = Regex("href=[\"'](https://anekdoty\\.ru/[^\"']+/)[\"']").find(hb.innerHtml)
            val permalink = linkMatch?.groupValues?.get(1) ?: fallbackUrl
            // Strip embedded action/voting spans before extracting joke text
            val stripped = hb.innerHtml
                .replace(Regex("<div[^>]*class=[\"'][^\"']*(?:like|rate|panel|meta|share|comment)[^\"']*[\"'][^>]*>.*?</div>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
            val cleaned = cleanHtmlToPlainJoke(stripped)
            if (isValidRussianJoke(cleaned)) {
                results.add(
                    buildJokeItem(
                        text = cleaned,
                        source = JokeSource.ANEKDOTY_RU,
                        sourceUrl = permalink,
                        query = query,
                        categoryLabel = categoryLabel
                    )
                )
            }
        }

        if (results.isEmpty()) {
            val fallbackBlocks = extractBalancedDivsByClass(html, "anekdot") +
                extractBalancedDivsByClass(html, "text")
            for (fb in fallbackBlocks) {
                val cleaned = cleanHtmlToPlainJoke(fb.innerHtml)
                if (isValidRussianJoke(cleaned)) {
                    results.add(buildJokeItem(cleaned, JokeSource.ANEKDOTY_RU, fallbackUrl, query, categoryLabel))
                }
            }
        }
        return results.distinctBy { it.id }
    }

    fun parseBaneksRuHtml(html: String, query: String, fallbackUrl: String): List<JokeItem> {
        val results = mutableListOf<JokeItem>()
        val canonicalUrl = Regex("<link[^>]+rel=[\"']canonical[\"'][^>]+href=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)
            ?: fallbackUrl

        val articleRegex = Regex("<article[^>]*>(.*?)</article>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        for (match in articleRegex.findAll(html)) {
            val articleBody = match.groupValues[1]
                .replace(Regex("<h[1-6][^>]*>.*?</h[1-6]>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
            val cleaned = cleanHtmlToPlainJoke(articleBody)
            if (isValidRussianJoke(cleaned)) {
                results.add(buildJokeItem(cleaned, JokeSource.BANEKS_RU, canonicalUrl, query))
            }
        }

        if (results.isEmpty()) {
            val sectionMatch = Regex("<section[^>]*class=[\"'][^\"']*anek-view[^\"']*[\"'][^>]*>(.*?)</section>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                .find(html)
            if (sectionMatch != null) {
                val body = sectionMatch.groupValues[1]
                    .replace(Regex("<h[1-6][^>]*>.*?</h[1-6]>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
                val cleaned = cleanHtmlToPlainJoke(body)
                if (isValidRussianJoke(cleaned)) {
                    results.add(buildJokeItem(cleaned, JokeSource.BANEKS_RU, canonicalUrl, query))
                }
            }
        }
        return results.distinctBy { it.id }
    }

    fun parseRzhunemoguResponse(rawBody: String, query: String): List<JokeItem> {
        val trimmed = rawBody.trim()
        if (trimmed.isEmpty()) return emptyList()

        // Rzhunemogu RandJSON.aspx returns {"content":"..."} often with raw unescaped CR/LF newlines inside
        val jsonPrefixIndex = trimmed.indexOf("\"content\":\"")
        val extracted = if (jsonPrefixIndex >= 0) {
            val start = jsonPrefixIndex + "\"content\":\"".length
            val end = trimmed.lastIndexOf("\"}")
            if (end > start) trimmed.substring(start, end) else ""
        } else {
            Regex("<content>(.*?)</content>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                .find(trimmed)?.groupValues?.get(1).orEmpty()
        }

        val unescaped = extracted
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
        val cleaned = cleanHtmlToPlainJoke(unescaped)
        return if (isValidRussianJoke(cleaned)) {
            listOf(
                buildJokeItem(
                    text = cleaned,
                    source = JokeSource.RZHUNEMOGU_RU,
                    sourceUrl = "http://rzhunemogu.ru",
                    query = query
                )
            )
        } else {
            emptyList()
        }
    }

    data class ExtractedDiv(
        val openTag: String,
        val innerHtml: String
    )

    fun extractBalancedDivsByClass(html: String, targetClass: String): List<ExtractedDiv> {
        val results = mutableListOf<ExtractedDiv>()
        val openDivRegex = Regex(
            "<div\\b[^>]*\\bclass\\s*=\\s*[\"'][^\"']*\\b${Regex.escape(targetClass)}\\b[^\"']*[\"'][^>]*>",
            RegexOption.IGNORE_CASE
        )
        val anyDivTokenRegex = Regex("</?div\\b[^>]*>", RegexOption.IGNORE_CASE)

        var searchStart = 0
        while (searchStart < html.length) {
            val match = openDivRegex.find(html, searchStart) ?: break
            val openTag = match.value
            val contentStart = match.range.last + 1
            var depth = 1
            var cursor = contentStart
            var contentEnd = -1

            while (cursor < html.length && depth > 0) {
                val token = anyDivTokenRegex.find(html, cursor) ?: break
                val tagText = token.value
                if (tagText.startsWith("</", ignoreCase = true)) {
                    depth--
                    if (depth == 0) {
                        contentEnd = token.range.first
                        cursor = token.range.last + 1
                        break
                    }
                } else if (!tagText.endsWith("/>")) {
                    depth++
                }
                cursor = token.range.last + 1
            }

            if (contentEnd >= contentStart) {
                results.add(ExtractedDiv(openTag, html.substring(contentStart, contentEnd)))
                searchStart = contentEnd + 6
            } else {
                searchStart = contentStart
            }
        }
        return results
    }

    fun cleanHtmlToPlainJoke(rawHtml: String): String {
        if (rawHtml.isBlank()) return ""
        val withoutScripts = rawHtml
            .replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
            .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
            .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")

        val withLineBreaks = withoutScripts
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</p\\s*>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("<p\\b[^>]*>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("</div\\s*>", RegexOption.IGNORE_CASE), "\n")

        val withoutTags = withLineBreaks.replace(Regex("<[^>]+>"), "")
        val decoded = decodeHtmlEntities(withoutTags)

        return decoded
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\u00A0', ' ')
            .lines()
            .map { line ->
                val trimmed = line.trim().replace(Regex("[ \\t]{2,}"), " ")
                if (trimmed.startsWith("- ")) "— " + trimmed.drop(2) else trimmed
            }
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun decodeHtmlEntities(input: String): String {
        var result = input
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#039;", "'")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&laquo;", "«")
            .replace("&raquo;", "»")
            .replace("&hellip;", "…")

        result = Regex("&#(\\d+);").replace(result) { m ->
            val code = m.groupValues[1].toIntOrNull()
            if (code != null && code in 1..0x10FFFF) code.toChar().toString() else m.value
        }
        result = Regex("&#x([0-9a-fA-F]+);").replace(result) { m ->
            val code = m.groupValues[1].toIntOrNull(16)
            if (code != null && code in 1..0x10FFFF) code.toChar().toString() else m.value
        }
        return result
    }

    fun isValidRussianJoke(text: String): Boolean {
        if (text.length !in 18..3600) return false
        if (!cyrillicRegex.containsMatchIn(text)) return false
        val lower = text.lowercase()
        // Exclude site navigation / cookie banners / copyright notices
        if (lower.contains("все права защищены") ||
            lower.contains("политика конфиденциальности") ||
            lower.contains("использование cookies") ||
            lower.startsWith("реклама на сайте")
        ) {
            return false
        }
        return true
    }

    private fun buildJokeItem(
        text: String,
        source: JokeSource,
        sourceUrl: String,
        query: String,
        categoryLabel: String = ""
    ): JokeItem {
        val stems = RussianQueryMatcher.extractQueryStems(query)
        val score = RussianQueryMatcher.computeRelevanceScore(text, query, stems)
        return JokeItem(
            id = computeJokeHash(text),
            text = text,
            source = source,
            sourceUrl = sourceUrl,
            matchedQuery = query.trim(),
            categoryLabel = categoryLabel,
            relevanceScore = score
        )
    }

    fun computeJokeHash(text: String): String {
        val normalized = text.lowercase()
            .replace('ё', 'е')
            .replace(Regex("[^а-яa-z0-9]"), "")
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(Charsets.UTF_8))
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }
}
