package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.JokeRepository
import com.example.data.local.SearchHistoryEntity
import com.example.data.model.JokeItem
import com.example.data.model.JokeSource
import com.example.data.model.SourceState
import com.example.data.model.SourceStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab {
    SEARCH,
    RANDOM,
    HISTORY
}

sealed interface FeedUiState {
    data object Idle : FeedUiState
    data object Loading : FeedUiState
    data class Success(val jokes: List<JokeItem>) : FeedUiState
    data class Empty(val query: String) : FeedUiState
    data class Error(val message: String) : FeedUiState
}

data class JokeUiState(
    val currentTab: AppTab = AppTab.SEARCH,
    val queryInput: String = "",
    val submittedQuery: String = "",
    val enabledSources: Set<JokeSource> = JokeSource.entries.toSet(),
    val searchFeedState: FeedUiState = FeedUiState.Idle,
    val searchSourceStatuses: List<SourceStatus> = emptyList(),
    val currentPage: Int = 1,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val randomFeedState: FeedUiState = FeedUiState.Idle,
    val randomSourceStatuses: List<SourceStatus> = emptyList(),
    val randomSourceFilter: JokeSource? = null,
    val historyFilterQuery: String = "",
    val selectedJoke: JokeItem? = null,
    val isReaderSheetOpen: Boolean = false,
    val isLargeReaderFont: Boolean = false,
    val darkThemeOverride: Boolean? = null
)

class JokeViewModel(
    private val repository: JokeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(JokeUiState())
    val uiState: StateFlow<JokeUiState> = _uiState.asStateFlow()

    val cachedJokes: StateFlow<List<JokeItem>> = repository.cachedJokesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val searchHistory: StateFlow<List<SearchHistoryEntity>> = repository.searchHistoryFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var searchJob: Job? = null
    private var randomJob: Job? = null

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
        if (tab == AppTab.RANDOM && _uiState.value.randomFeedState is FeedUiState.Idle) {
            refreshRandomJokes()
        }
    }

    fun updateQueryInput(newQuery: String) {
        _uiState.update { it.copy(queryInput = newQuery) }
    }

    fun clearQueryInput() {
        _uiState.update {
            it.copy(
                queryInput = "",
                submittedQuery = "",
                searchFeedState = FeedUiState.Idle,
                searchSourceStatuses = emptyList(),
                currentPage = 1,
                canLoadMore = false
            )
        }
    }

    fun toggleSource(source: JokeSource) {
        _uiState.update { state ->
            val current = state.enabledSources
            val updated = if (source in current) {
                if (current.size > 1) current - source else current
            } else {
                current + source
            }
            state.copy(enabledSources = updated)
        }
        val activeQuery = _uiState.value.submittedQuery
        if (activeQuery.isNotBlank()) {
            submitSearch(activeQuery)
        }
    }

    fun submitSearch(explicitQuery: String? = null) {
        val targetQuery = (explicitQuery ?: _uiState.value.queryInput).trim()
        if (targetQuery.isEmpty()) return

        searchJob?.cancel()
        val enabled = _uiState.value.enabledSources
        val loadingStatuses = JokeSource.entries.map { src ->
            SourceStatus(
                source = src,
                state = if (src in enabled) SourceState.Loading else SourceState.Idle
            )
        }

        _uiState.update {
            it.copy(
                currentTab = AppTab.SEARCH,
                queryInput = targetQuery,
                submittedQuery = targetQuery,
                searchFeedState = FeedUiState.Loading,
                searchSourceStatuses = loadingStatuses,
                currentPage = 1,
                isLoadingMore = false,
                canLoadMore = false
            )
        }

        searchJob = viewModelScope.launch {
            runCatching {
                repository.searchJokes(
                    query = targetQuery,
                    enabledSources = enabled,
                    page = 1
                )
            }.onSuccess { response ->
                val allFailed = response.jokes.isEmpty() &&
                    response.sourceStatuses.any { it.state is SourceState.Error } &&
                    response.sourceStatuses.none { it.state is SourceState.Success }

                _uiState.update { state ->
                    val newFeedState = when {
                        response.jokes.isNotEmpty() -> FeedUiState.Success(response.jokes)
                        allFailed -> FeedUiState.Error(
                            "Сайты временно не ответили. Проверьте подключение к интернету и повторите попытку."
                        )
                        else -> FeedUiState.Empty(targetQuery)
                    }
                    state.copy(
                        searchFeedState = newFeedState,
                        searchSourceStatuses = response.sourceStatuses,
                        currentPage = 1,
                        canLoadMore = response.jokes.size >= 5,
                        selectedJoke = state.selectedJoke ?: response.jokes.firstOrNull()
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        searchFeedState = FeedUiState.Error(
                            err.localizedMessage ?: "Не удалось выполнить поиск по сайтам"
                        )
                    )
                }
            }
        }
    }

    fun loadMoreSearchResults() {
        val state = _uiState.value
        val currentSuccess = state.searchFeedState as? FeedUiState.Success ?: return
        if (state.isLoadingMore || state.submittedQuery.isBlank()) return

        val nextPage = state.currentPage + 1
        _uiState.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            runCatching {
                repository.searchJokes(
                    query = state.submittedQuery,
                    enabledSources = state.enabledSources,
                    page = nextPage
                )
            }.onSuccess { response ->
                _uiState.update { prev ->
                    val merged = (currentSuccess.jokes + response.jokes).distinctBy { it.id }
                    val addedNew = merged.size > currentSuccess.jokes.size
                    prev.copy(
                        searchFeedState = FeedUiState.Success(merged),
                        searchSourceStatuses = response.sourceStatuses,
                        currentPage = if (addedNew) nextPage else prev.currentPage,
                        isLoadingMore = false,
                        canLoadMore = addedNew && response.jokes.isNotEmpty()
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun refreshRandomJokes() {
        randomJob?.cancel()
        val enabled = _uiState.value.enabledSources
        val loadingStatuses = JokeSource.entries.map { src ->
            SourceStatus(
                source = src,
                state = if (src in enabled) SourceState.Loading else SourceState.Idle
            )
        }

        _uiState.update {
            it.copy(
                randomFeedState = FeedUiState.Loading,
                randomSourceStatuses = loadingStatuses
            )
        }

        randomJob = viewModelScope.launch {
            runCatching {
                repository.fetchFreshAndRandom(enabled)
            }.onSuccess { response ->
                _uiState.update { state ->
                    val newFeed = if (response.jokes.isNotEmpty()) {
                        FeedUiState.Success(response.jokes)
                    } else {
                        FeedUiState.Error("Не удалось загрузить свежие анекдоты с сайтов.")
                    }
                    state.copy(
                        randomFeedState = newFeed,
                        randomSourceStatuses = response.sourceStatuses,
                        selectedJoke = state.selectedJoke ?: response.jokes.firstOrNull()
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        randomFeedState = FeedUiState.Error(
                            err.localizedMessage ?: "Ошибка загрузки ленты"
                        )
                    )
                }
            }
        }
    }

    fun setRandomSourceFilter(source: JokeSource?) {
        _uiState.update { it.copy(randomSourceFilter = source) }
    }

    fun updateHistoryFilter(query: String) {
        _uiState.update { it.copy(historyFilterQuery = query) }
    }

    fun openJokeReader(joke: JokeItem, openModalSheet: Boolean) {
        _uiState.update {
            it.copy(
                selectedJoke = joke,
                isReaderSheetOpen = openModalSheet
            )
        }
    }

    fun dismissReaderSheet() {
        _uiState.update { it.copy(isReaderSheetOpen = false) }
    }

    fun toggleLargeReaderFont() {
        _uiState.update { it.copy(isLargeReaderFont = !it.isLargeReaderFont) }
    }

    fun toggleDarkTheme(currentlyDark: Boolean) {
        _uiState.update { state ->
            val next = !(state.darkThemeOverride ?: currentlyDark)
            state.copy(darkThemeOverride = next)
        }
    }

    fun deleteHistoryQuery(normalizedQuery: String) {
        viewModelScope.launch {
            repository.removeSearchHistoryQuery(normalizedQuery)
        }
    }

    fun clearAllHistoryAndCache() {
        viewModelScope.launch {
            repository.clearAllCacheAndHistory()
        }
    }

    companion object {
        fun provideFactory(repository: JokeRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return JokeViewModel(repository) as T
                }
            }
    }
}
