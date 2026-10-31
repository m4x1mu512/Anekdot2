package com.example.data

import com.example.data.local.CachedJokeEntity
import com.example.data.local.JokeDao
import com.example.data.local.SearchHistoryEntity
import com.example.data.model.AggregatedJokeResponse
import com.example.data.model.JokeItem
import com.example.data.model.JokeSource
import com.example.data.remote.JokeRemoteDataSource
import com.example.data.remote.RussianQueryMatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale

class JokeRepository(
    private val remoteDataSource: JokeRemoteDataSource,
    private val jokeDao: JokeDao
) {

    val cachedJokesFlow: Flow<List<JokeItem>> = jokeDao.observeCachedJokes()
        .map { entities -> entities.map { it.toDomain() } }

    val searchHistoryFlow: Flow<List<SearchHistoryEntity>> = jokeDao.observeSearchHistory()

    suspend fun searchJokes(
        query: String,
        enabledSources: Set<JokeSource>,
        page: Int = 1
    ): AggregatedJokeResponse {
        val cleanQuery = query.trim()
        val remoteResponse = remoteDataSource.searchJokesAcrossWeb(
            query = cleanQuery,
            enabledSources = enabledSources,
            page = page
        )

        if (remoteResponse.jokes.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val entities = remoteResponse.jokes.mapIndexed { index, item ->
                CachedJokeEntity.fromDomain(
                    item.copy(fetchedAt = now - index)
                )
            }
            jokeDao.upsertJokes(entities)
            jokeDao.trimOldCachedJokes()
        }

        val finalJokes = if (remoteResponse.jokes.isNotEmpty() || page > 1) {
            remoteResponse.jokes
        } else {
            // Also check local cache if websites had no match or temporary network error
            searchInLocalCache(cleanQuery, enabledSources)
        }

        if (cleanQuery.isNotEmpty() && page == 1) {
            jokeDao.upsertSearchHistory(
                SearchHistoryEntity(
                    normalizedQuery = cleanQuery.lowercase(Locale("ru", "RU")),
                    displayQuery = cleanQuery,
                    resultsCount = finalJokes.size,
                    searchedAt = System.currentTimeMillis()
                )
            )
        }

        return remoteResponse.copy(jokes = finalJokes)
    }

    suspend fun fetchFreshAndRandom(
        enabledSources: Set<JokeSource>
    ): AggregatedJokeResponse {
        val response = remoteDataSource.fetchFreshAndRandomJokes(enabledSources)
        if (response.jokes.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val entities = response.jokes.mapIndexed { index, item ->
                CachedJokeEntity.fromDomain(item.copy(fetchedAt = now - index))
            }
            jokeDao.upsertJokes(entities)
            jokeDao.trimOldCachedJokes()
        }
        return response
    }

    private suspend fun searchInLocalCache(
        query: String,
        enabledSources: Set<JokeSource>
    ): List<JokeItem> {
        val stems = RussianQueryMatcher.extractQueryStems(query)
        if (stems.isEmpty()) return emptyList()

        val cached = jokeDao.observeCachedJokes().first().map { it.toDomain() }
        return cached
            .filter { enabledSources.isEmpty() || it.source in enabledSources }
            .mapNotNull { item ->
                val score = RussianQueryMatcher.computeRelevanceScore(item.text, query, stems)
                if (score > 0) item.copy(relevanceScore = score) else null
            }
            .sortedByDescending { it.relevanceScore }
    }

    suspend fun removeSearchHistoryQuery(normalizedQuery: String) {
        jokeDao.deleteSearchHistoryItem(normalizedQuery)
    }

    suspend fun clearAllCacheAndHistory() {
        jokeDao.clearCachedJokes()
        jokeDao.clearSearchHistory()
    }
}
