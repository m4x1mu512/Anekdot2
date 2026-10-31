package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JokeDao {
    @Query("SELECT * FROM cached_jokes ORDER BY fetchedAt DESC LIMIT 200")
    fun observeCachedJokes(): Flow<List<CachedJokeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJokes(jokes: List<CachedJokeEntity>)

    @Query("DELETE FROM cached_jokes WHERE id NOT IN (SELECT id FROM cached_jokes ORDER BY fetchedAt DESC LIMIT 250)")
    suspend fun trimOldCachedJokes()

    @Query("DELETE FROM cached_jokes")
    suspend fun clearCachedJokes()

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 20")
    fun observeSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchHistory(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE normalizedQuery = :normalizedQuery")
    suspend fun deleteSearchHistoryItem(normalizedQuery: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}
