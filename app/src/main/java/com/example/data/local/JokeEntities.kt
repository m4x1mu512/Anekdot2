package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.JokeItem
import com.example.data.model.JokeSource

@Entity(tableName = "cached_jokes")
data class CachedJokeEntity(
    @PrimaryKey val id: String,
    val text: String,
    val sourceId: String,
    val sourceUrl: String,
    val matchedQuery: String,
    val categoryLabel: String,
    val fetchedAt: Long
) {
    fun toDomain(): JokeItem = JokeItem(
        id = id,
        text = text,
        source = JokeSource.fromId(sourceId),
        sourceUrl = sourceUrl,
        matchedQuery = matchedQuery,
        categoryLabel = categoryLabel,
        fetchedAt = fetchedAt
    )

    companion object {
        fun fromDomain(item: JokeItem): CachedJokeEntity = CachedJokeEntity(
            id = item.id,
            text = item.text,
            sourceId = item.source.id,
            sourceUrl = item.sourceUrl,
            matchedQuery = item.matchedQuery,
            categoryLabel = item.categoryLabel,
            fetchedAt = item.fetchedAt
        )
    }
}

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val normalizedQuery: String,
    val displayQuery: String,
    val resultsCount: Int,
    val searchedAt: Long
)
