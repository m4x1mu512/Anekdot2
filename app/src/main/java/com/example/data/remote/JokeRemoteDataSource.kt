package com.example.data.remote

import com.example.data.model.AggregatedJokeResponse
import com.example.data.model.JokeItem
import com.example.data.model.JokeSource
import com.example.data.model.SourceState
import com.example.data.model.SourceStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class JokeRemoteDataSource(
    private val httpClient: OkHttpClient = defaultOkHttpClient()
) {

    suspend fun searchJokesAcrossWeb(
        query: String,
        enabledSources: Set<JokeSource>,
        page: Int = 1
    ): AggregatedJokeResponse = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        val stems = RussianQueryMatcher.extractQueryStems(cleanQuery)
        val topicRoute = RussianQueryMatcher.resolveTopicRoute(cleanQuery)
        val activeSources = enabledSources.ifEmpty { JokeSource.entries.toSet() }

        supervisorScope {
            val tasks = JokeSource.entries.map { source ->
                async {
                    if (source !in activeSources) {
                        return@async Pair(
                            SourceStatus(source, SourceState.Idle),
                            emptyList<JokeItem>()
                        )
                    }
                    val startMs = System.currentTimeMillis()
                    runCatching {
                        val jokes = when (source) {
                            JokeSource.ANEKDOT_RU -> searchAnekdotRu(cleanQuery, stems, page)
                            JokeSource.NEKDO_RU -> searchNekdoRu(cleanQuery, stems, topicRoute, page)
                            JokeSource.ANEKDOTY_RU -> searchAnekdotyRu(cleanQuery, stems, topicRoute, page)
                            JokeSource.BANEKS_RU -> searchBaneksRu(cleanQuery, stems)
                            JokeSource.RZHUNEMOGU_RU -> searchRzhunemoguRu(cleanQuery, stems)
                        }
                        val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
                        Pair(
                            SourceStatus(source, SourceState.Success(jokes.size, elapsed)),
                            jokes
                        )
                    }.getOrElse { error ->
                        Pair(
                            SourceStatus(
                                source,
                                SourceState.Error(error.localizedMessage ?: "Ошибка соединения")
                            ),
                            emptyList()
                        )
                    }
                }
            }

            val outcomes = tasks.awaitAll()
            val statuses = outcomes.map { it.first }
            val merged = outcomes
                .flatMap { it.second }
                .distinctBy { it.id }
                .sortedWith(
                    compareByDescending<JokeItem> { it.relevanceScore }
                        .thenBy { it.text.length }
                )

            AggregatedJokeResponse(
                jokes = merged,
                sourceStatuses = statuses,
                query = cleanQuery,
                page = page
            )
        }
    }

    suspend fun fetchFreshAndRandomJokes(
        enabledSources: Set<JokeSource>
    ): AggregatedJokeResponse = withContext(Dispatchers.IO) {
        val activeSources = enabledSources.ifEmpty { JokeSource.entries.toSet() }

        supervisorScope {
            val tasks = JokeSource.entries.map { source ->
                async {
                    if (source !in activeSources) {
                        return@async Pair(
                            SourceStatus(source, SourceState.Idle),
                            emptyList<JokeItem>()
                        )
                    }
                    val startMs = System.currentTimeMillis()
                    runCatching {
                        val jokes = when (source) {
                            JokeSource.ANEKDOT_RU -> fetchRandomAnekdotRu()
                            JokeSource.NEKDO_RU -> fetchRandomNekdoRu()
                            JokeSource.ANEKDOTY_RU -> fetchFreshAnekdotyRu()
                            JokeSource.BANEKS_RU -> fetchRandomBaneksBatch(count = 4, query = "")
                            JokeSource.RZHUNEMOGU_RU -> fetchRandomRzhunemoguBatch(count = 5, query = "")
                        }
                        val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
                        Pair(
                            SourceStatus(source, SourceState.Success(jokes.size, elapsed)),
                            jokes
                        )
                    }.getOrElse { error ->
                        Pair(
                            SourceStatus(
                                source,
                                SourceState.Error(error.localizedMessage ?: "Недоступен")
                            ),
                            emptyList()
                        )
                    }
                }
            }

            val outcomes = tasks.awaitAll()
            val statuses = outcomes.map { it.first }
            // Interleave sources so every portal is represented near the top
            val grouped = outcomes.map { it.second }.filter { it.isNotEmpty() }
            val interleaved = interleaveLists(grouped).distinctBy { it.id }

            AggregatedJokeResponse(
                jokes = interleaved,
                sourceStatuses = statuses,
                query = "",
                page = 1
            )
        }
    }

    private fun searchAnekdotRu(
        query: String,
        stems: List<String>,
        page: Int
    ): List<JokeItem> {
        val encodedUtf8 = URLEncoder.encode(query, "UTF-8")
        val searchUrl = if (page <= 1) {
            "https://www.anekdot.ru/search/?kw=$encodedUtf8&mode=any&xcnt=25"
        } else {
            "https://www.anekdot.ru/search/?kw=$encodedUtf8&mode=any&xcnt=25&page=$page"
        }

        val html = executeGet(searchUrl)
        val parsed = HtmlJokeParser.parseAnekdotRuHtml(html, query, searchUrl)
        if (parsed.isNotEmpty()) {
            return filterOrRankByQuery(parsed, query, stems, allowUnmatchedIfDirectSearch = true)
        }

        // Fallback: check RSS feed + random page and filter by stem
        if (page == 1) {
            val rssXml = runCatching { executeGet("https://www.anekdot.ru/rss/export_j.xml") }.getOrDefault("")
            val rssItems = HtmlJokeParser.parseAnekdotRuRss(rssXml, query)
            return filterByStemsOnly(rssItems, query, stems)
        }
        return emptyList()
    }

    private fun searchNekdoRu(
        query: String,
        stems: List<String>,
        topicRoute: RussianQueryMatcher.TopicRoute?,
        page: Int
    ): List<JokeItem> {
        val collected = mutableListOf<JokeItem>()

        // 1. Direct query search on nekdo.ru
        if (page == 1) {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://nekdo.ru/search/?q=$encoded"
            val searchHtml = runCatching { executeGet(searchUrl) }.getOrDefault("")
            val fromSearch = HtmlJokeParser.parseNekdoRuHtml(searchHtml, query, searchUrl)
            collected.addAll(filterByStemsOnly(fromSearch, query, stems))
        }

        // 2. Category archive on nekdo.ru if query maps to a known Russian humor rubric
        val slug = topicRoute?.nekdoSlug
        if (slug != null) {
            val pagePath = if (page <= 1) "https://nekdo.ru/$slug/" else "https://nekdo.ru/$slug/$page/"
            val catHtml = runCatching { executeGet(pagePath) }.getOrDefault("")
            val catItems = HtmlJokeParser.parseNekdoRuHtml(catHtml, query, pagePath)
                .map { item ->
                    val boosted = maxOf(item.relevanceScore, 45)
                    item.copy(categoryLabel = topicRoute.label, relevanceScore = boosted)
                }
            collected.addAll(catItems)
        } else if (collected.isEmpty()) {
            val randomUrl = "https://nekdo.ru/random/"
            val randomHtml = runCatching { executeGet(randomUrl) }.getOrDefault("")
            val randomItems = HtmlJokeParser.parseNekdoRuHtml(randomHtml, query, randomUrl)
            collected.addAll(filterByStemsOnly(randomItems, query, stems))
        }

        return collected.distinctBy { it.id }
    }

    private fun searchAnekdotyRu(
        query: String,
        stems: List<String>,
        topicRoute: RussianQueryMatcher.TopicRoute?,
        page: Int
    ): List<JokeItem> {
        val collected = mutableListOf<JokeItem>()

        val slug = topicRoute?.anekdotySlug
        if (slug != null) {
            val categoryUrl = if (page <= 1) {
                "https://anekdoty.ru/$slug/"
            } else {
                "https://anekdoty.ru/$slug/$page/"
            }
            val html = runCatching { executeGet(categoryUrl) }.getOrDefault("")
            val parsed = HtmlJokeParser.parseAnekdotyRuHtml(
                html = html,
                query = query,
                fallbackUrl = categoryUrl,
                categoryLabel = topicRoute.label
            ).map { item ->
                item.copy(relevanceScore = maxOf(item.relevanceScore, 50))
            }
            collected.addAll(parsed)
        }

        if (collected.isEmpty() && page == 1) {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://anekdoty.ru/?s=$encoded"
            val html = runCatching { executeGet(searchUrl) }.getOrDefault("")
            val parsed = HtmlJokeParser.parseAnekdotyRuHtml(html, query, searchUrl)
            collected.addAll(filterByStemsOnly(parsed, query, stems))
        }

        return collected.distinctBy { it.id }
    }

    private suspend fun searchBaneksRu(
        query: String,
        stems: List<String>
    ): List<JokeItem> = supervisorScope {
        val batch = fetchRandomBaneksBatch(count = 5, query = query)
        filterByStemsOnly(batch, query, stems)
    }

    private suspend fun searchRzhunemoguRu(
        query: String,
        stems: List<String>
    ): List<JokeItem> = supervisorScope {
        val batch = fetchRandomRzhunemoguBatch(count = 6, query = query)
        filterByStemsOnly(batch, query, stems)
    }

    private fun fetchRandomAnekdotRu(): List<JokeItem> {
        val randomUrl = "https://www.anekdot.ru/random/anekdot/"
        val html = runCatching { executeGet(randomUrl) }.getOrDefault("")
        val parsed = HtmlJokeParser.parseAnekdotRuHtml(html, "", randomUrl)
        if (parsed.isNotEmpty()) return parsed.take(12)

        val rssXml = executeGet("https://www.anekdot.ru/rss/export_j.xml")
        return HtmlJokeParser.parseAnekdotRuRss(rssXml, "").take(12)
    }

    private fun fetchRandomNekdoRu(): List<JokeItem> {
        val url = "https://nekdo.ru/random/"
        val html = executeGet(url)
        return HtmlJokeParser.parseNekdoRuHtml(html, "", url).take(12)
    }

    private fun fetchFreshAnekdotyRu(): List<JokeItem> {
        val url = "https://anekdoty.ru/"
        val html = executeGet(url)
        return HtmlJokeParser.parseAnekdotyRuHtml(html, "", url, "Популярное").take(12)
    }

    private suspend fun fetchRandomBaneksBatch(count: Int, query: String): List<JokeItem> =
        supervisorScope {
            (1..count).map {
                async {
                    runCatching {
                        val url = "https://baneks.ru/random"
                        val html = executeGet(url)
                        HtmlJokeParser.parseBaneksRuHtml(html, query, url)
                    }.getOrDefault(emptyList())
                }
            }.awaitAll().flatten().distinctBy { it.id }
        }

    private suspend fun fetchRandomRzhunemoguBatch(count: Int, query: String): List<JokeItem> =
        supervisorScope {
            (1..count).map { idx ->
                async {
                    runCatching {
                        val url = "http://rzhunemogu.ru/RandJSON.aspx?CType=1&seed=${System.nanoTime() + idx}"
                        val body = executeGet(url)
                        HtmlJokeParser.parseRzhunemoguResponse(body, query)
                    }.getOrDefault(emptyList())
                }
            }.awaitAll().flatten().distinctBy { it.id }
        }

    private fun filterOrRankByQuery(
        items: List<JokeItem>,
        query: String,
        stems: List<String>,
        allowUnmatchedIfDirectSearch: Boolean
    ): List<JokeItem> {
        val scored = items.map { item ->
            val score = RussianQueryMatcher.computeRelevanceScore(item.text, query, stems)
            item.copy(
                relevanceScore = if (score == 0 && allowUnmatchedIfDirectSearch) 15 else score
            )
        }
        return if (allowUnmatchedIfDirectSearch) {
            scored
        } else {
            scored.filter { it.relevanceScore > 0 }
        }
    }

    private fun filterByStemsOnly(
        items: List<JokeItem>,
        query: String,
        stems: List<String>
    ): List<JokeItem> {
        if (stems.isEmpty()) return items
        return items.mapNotNull { item ->
            val score = RussianQueryMatcher.computeRelevanceScore(item.text, query, stems)
            if (score > 0) item.copy(relevanceScore = score) else null
        }
    }

    private fun <T> interleaveLists(lists: List<List<T>>): List<T> {
        val result = mutableListOf<T>()
        val maxSize = lists.maxOfOrNull { it.size } ?: 0
        for (i in 0 until maxSize) {
            for (list in lists) {
                if (i < list.size) {
                    result.add(list[i])
                }
            }
        }
        return result
    }

    private fun executeGet(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
            )
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/json;q=0.8,*/*;q=0.7")
            .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.5")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}")
            }
            val bytes = response.body?.bytes() ?: ByteArray(0)
            val contentType = response.header("Content-Type")
            return HtmlJokeParser.decodeBytes(bytes, contentType)
        }
    }

    companion object {
        fun defaultOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}
