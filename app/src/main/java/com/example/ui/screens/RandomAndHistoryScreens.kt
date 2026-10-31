package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SearchHistoryEntity
import com.example.data.model.JokeItem
import com.example.data.model.JokeSource
import com.example.data.model.SourceStatus
import com.example.data.remote.RussianQueryMatcher
import com.example.ui.FeedUiState
import com.example.ui.components.JokeCard
import com.example.ui.components.SourceTelemetryStrip

@Composable
fun RandomJokesScreen(
    feedState: FeedUiState,
    sourceStatuses: List<SourceStatus>,
    selectedSourceFilter: JokeSource?,
    selectedJokeId: String?,
    onSelectSourceFilter: (JokeSource?) -> Unit,
    onRefresh: () -> Unit,
    onSelectJoke: (JokeItem) -> Unit,
    onCopyJoke: (JokeItem) -> Unit,
    onShareJoke: (JokeItem) -> Unit,
    onOpenSource: (JokeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("random_lazy_column"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "random_header") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.random_header_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = scheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.random_header_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            item(key = "random_source_filters") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedSourceFilter == null,
                        onClick = { onSelectSourceFilter(null) },
                        label = { Text(stringResource(R.string.filter_all_sources)) },
                        modifier = Modifier.testTag("random_filter_all")
                    )
                    JokeSource.entries.forEach { src ->
                        FilterChip(
                            selected = selectedSourceFilter == src,
                            onClick = { onSelectSourceFilter(src) },
                            label = { Text(src.displayName) },
                            modifier = Modifier.testTag("random_filter_${src.id}")
                        )
                    }
                }
            }

            if (sourceStatuses.isNotEmpty()) {
                item(key = "random_telemetry") {
                    SourceTelemetryStrip(statuses = sourceStatuses)
                }
            }

            when (feedState) {
                FeedUiState.Idle, FeedUiState.Loading -> {
                    item(key = "random_loading") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = scheme.primary)
                                Text(
                                    text = stringResource(R.string.searching_sites_status),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }

                is FeedUiState.Error -> {
                    item(key = "random_error") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = feedState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Button(onClick = onRefresh) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.retry_button))
                                }
                            }
                        }
                    }
                }

                is FeedUiState.Empty -> {}

                is FeedUiState.Success -> {
                    val filtered = if (selectedSourceFilter == null) {
                        feedState.jokes
                    } else {
                        feedState.jokes.filter { it.source == selectedSourceFilter }
                    }

                    itemsIndexed(
                        items = filtered,
                        key = { _, item -> item.id }
                    ) { index, joke ->
                        JokeCard(
                            joke = joke,
                            highlightQuery = "",
                            isSelected = joke.id == selectedJokeId,
                            index = index,
                            onClick = { onSelectJoke(joke) },
                            onCopy = { onCopyJoke(joke) },
                            onShare = { onShareJoke(joke) },
                            onOpenSource = { onOpenSource(joke) }
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onRefresh,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Casino,
                    contentDescription = null
                )
            },
            text = { Text(stringResource(R.string.refresh_random_button)) },
            containerColor = scheme.primary,
            contentColor = scheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("refresh_random_fab")
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryAndCacheScreen(
    searchHistory: List<SearchHistoryEntity>,
    cachedJokes: List<JokeItem>,
    filterQuery: String,
    selectedJokeId: String?,
    onFilterQueryChange: (String) -> Unit,
    onRepeatSearch: (String) -> Unit,
    onDeleteHistoryQuery: (String) -> Unit,
    onClearAll: () -> Unit,
    onSelectJoke: (JokeItem) -> Unit,
    onCopyJoke: (JokeItem) -> Unit,
    onShareJoke: (JokeItem) -> Unit,
    onOpenSource: (JokeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val filteredJokes by remember(cachedJokes, filterQuery) {
        derivedStateOf {
            if (filterQuery.isBlank()) {
                cachedJokes
            } else {
                val stems = RussianQueryMatcher.extractQueryStems(filterQuery)
                cachedJokes.filter {
                    RussianQueryMatcher.computeRelevanceScore(it.text, filterQuery, stems) > 0 ||
                        it.matchedQuery.contains(filterQuery, ignoreCase = true)
                }
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("history_lazy_column"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "history_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.history_header_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = scheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    if (searchHistory.isNotEmpty() || cachedJokes.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onClearAll,
                            modifier = Modifier.testTag("clear_cache_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.clear_history_button))
                        }
                    }
                }
            }

            if (searchHistory.isNotEmpty()) {
                item(key = "recent_queries_card") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = scheme.surface),
                        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.55f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.recent_queries_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onSurface
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                searchHistory.forEachIndexed { idx, entry ->
                                    InputChip(
                                        selected = false,
                                        onClick = { onRepeatSearch(entry.displayQuery) },
                                        label = { Text("${entry.displayQuery} (${entry.resultsCount})") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.History,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { onDeleteHistoryQuery(entry.normalizedQuery) },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Clear,
                                                    contentDescription = stringResource(R.string.clear_search_cd),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        },
                                        modifier = Modifier.testTag("history_query_chip_$idx")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (cachedJokes.isEmpty() && searchHistory.isEmpty()) {
                item(key = "empty_history") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_history_card"),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = scheme.primary
                            )
                            Text(
                                text = stringResource(R.string.empty_history_title),
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.empty_history_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (cachedJokes.isNotEmpty()) {
                item(key = "cache_filter_header") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.cached_jokes_title, filteredJokes.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = filterQuery,
                            onValueChange = onFilterQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("history_filter_input"),
                            placeholder = { Text("Фильтр по сохранённому кэшу…") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = if (filterQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { onFilterQueryChange("") }) {
                                        Icon(
                                            imageVector = Icons.Filled.Clear,
                                            contentDescription = stringResource(R.string.clear_search_cd)
                                        )
                                    }
                                }
                            } else null,
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }

                itemsIndexed(
                    items = filteredJokes,
                    key = { _, item -> item.id }
                ) { index, joke ->
                    JokeCard(
                        joke = joke,
                        highlightQuery = filterQuery.ifBlank { joke.matchedQuery },
                        isSelected = joke.id == selectedJokeId,
                        index = index,
                        onClick = { onSelectJoke(joke) },
                        onCopy = { onCopyJoke(joke) },
                        onShare = { onShareJoke(joke) },
                        onOpenSource = { onOpenSource(joke) }
                    )
                }
            }
        }
    }
}
