package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.JokeItem
import com.example.data.model.JokeSource
import com.example.data.model.PopularRussianTopics
import com.example.data.model.SourceStatus
import com.example.ui.FeedUiState
import com.example.ui.components.JokeCard
import com.example.ui.components.SourceTelemetryStrip

@Composable
fun SearchScreen(
    queryInput: String,
    submittedQuery: String,
    enabledSources: Set<JokeSource>,
    feedState: FeedUiState,
    sourceStatuses: List<SourceStatus>,
    currentPage: Int,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    selectedJokeId: String?,
    isDarkTheme: Boolean,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSubmitSearch: (String?) -> Unit,
    onToggleSource: (JokeSource) -> Unit,
    onLoadMore: () -> Unit,
    onSelectJoke: (JokeItem) -> Unit,
    onCopyJoke: (JokeItem) -> Unit,
    onShareJoke: (JokeItem) -> Unit,
    onOpenSource: (JokeItem) -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("search_lazy_column"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "hero_header") {
                EditorialHeroHeader(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme
                )
            }

            item(key = "search_box") {
                SearchInputCard(
                    queryInput = queryInput,
                    onQueryChange = onQueryChange,
                    onClearQuery = onClearQuery,
                    onSubmit = {
                        focusManager.clearFocus()
                        onSubmitSearch(null)
                    }
                )
            }

            item(key = "sources_selector") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.sources_section_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        JokeSource.entries.forEach { source ->
                            val selected = source in enabledSources
                            FilterChip(
                                selected = selected,
                                onClick = { onToggleSource(source) },
                                label = { Text(source.displayName) },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                modifier = Modifier.testTag("source_chip_${source.id}")
                            )
                        }
                    }
                }
            }

            item(key = "popular_topics") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.popular_topics_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PopularRussianTopics.forEachIndexed { idx, topic ->
                            val isCurrent = submittedQuery.equals(topic.query, ignoreCase = true)
                            AssistChip(
                                onClick = {
                                    focusManager.clearFocus()
                                    onSubmitSearch(topic.query)
                                },
                                label = { Text(topic.label) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isCurrent) {
                                        scheme.secondaryContainer
                                    } else {
                                        scheme.surfaceContainerLow
                                    },
                                    labelColor = if (isCurrent) {
                                        scheme.onSecondaryContainer
                                    } else {
                                        scheme.onSurface
                                    }
                                ),
                                modifier = Modifier.testTag("topic_chip_$idx")
                            )
                        }
                    }
                }
            }

            if (sourceStatuses.isNotEmpty()) {
                item(key = "telemetry_strip") {
                    SourceTelemetryStrip(statuses = sourceStatuses)
                }
            }

            when (feedState) {
                FeedUiState.Idle -> {
                    item(key = "idle_illustration") {
                        InitialSearchPromptCard()
                    }
                }

                FeedUiState.Loading -> {
                    item(key = "loading_state") {
                        LoadingSearchCard()
                    }
                }

                is FeedUiState.Empty -> {
                    item(key = "empty_state") {
                        EmptySearchResultsCard(query = feedState.query)
                    }
                }

                is FeedUiState.Error -> {
                    item(key = "error_state") {
                        ErrorSearchCard(
                            message = feedState.message,
                            onRetry = { onSubmitSearch(submittedQuery.ifBlank { queryInput }) }
                        )
                    }
                }

                is FeedUiState.Success -> {
                    item(key = "results_summary") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.results_count_format, feedState.jokes.size),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onBackground
                            )
                            if (submittedQuery.isNotBlank()) {
                                Surface(
                                    color = scheme.secondaryContainer,
                                    contentColor = scheme.onSecondaryContainer,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "«$submittedQuery»",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(
                        items = feedState.jokes,
                        key = { _, item -> item.id }
                    ) { index, joke ->
                        JokeCard(
                            joke = joke,
                            highlightQuery = submittedQuery,
                            isSelected = joke.id == selectedJokeId,
                            index = index,
                            onClick = { onSelectJoke(joke) },
                            onCopy = { onCopyJoke(joke) },
                            onShare = { onShareJoke(joke) },
                            onOpenSource = { onOpenSource(joke) }
                        )
                    }

                    if (canLoadMore) {
                        item(key = "load_more_footer") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                FilledTonalButton(
                                    onClick = onLoadMore,
                                    enabled = !isLoadingMore,
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("load_more_button")
                                ) {
                                    if (isLoadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    Text(stringResource(R.string.load_more_button, currentPage + 1))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorialHeroHeader(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner_1793468886746),
                contentDescription = stringResource(R.string.hero_banner_cd),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xE6221512),
                                Color(0xCC3B1A12),
                                Color(0x88221512)
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Color(0xFFFFB958),
                        contentColor = Color(0xFF2A1800),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "5 ПОРТАЛОВ РУНЕТА",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.app_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF5E6DF)
                    )
                }

                IconButton(
                    onClick = onToggleTheme,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.16f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .align(Alignment.Top)
                        .testTag("toggle_theme_button")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = stringResource(R.string.toggle_theme_cd)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchInputCard(
    queryInput: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSubmit: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = queryInput,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_query_input"),
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_placeholder),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = scheme.primary
                    )
                },
                trailingIcon = if (queryInput.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = onClearQuery,
                            modifier = Modifier.testTag("clear_query_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.clear_search_cd)
                            )
                        }
                    }
                } else null,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = scheme.primary,
                    unfocusedBorderColor = scheme.outlineVariant
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() })
            )

            Button(
                onClick = onSubmit,
                enabled = queryInput.isNotBlank(),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("search_submit_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.search_button),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun InitialSearchPromptCard() {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("initial_prompt_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_search_illustration_1793468888082),
                contentDescription = stringResource(R.string.search_illustration_cd),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(132.dp)
                    .clip(CircleShape)
            )
            Text(
                text = stringResource(R.string.empty_initial_title),
                style = MaterialTheme.typography.headlineSmall,
                color = scheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.empty_initial_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LoadingSearchCard() {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("loading_search_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(color = scheme.primary)
            Text(
                text = stringResource(R.string.searching_sites_status),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptySearchResultsCard(query: String) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_results_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.empty_no_results_title, query),
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.empty_no_results_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorSearchCard(
    message: String,
    onRetry: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("error_results_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = scheme.errorContainer.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = scheme.error
            )
            Text(
                text = stringResource(R.string.error_network_title),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("retry_search_button")
            ) {
                Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.retry_button))
            }
        }
    }
}
