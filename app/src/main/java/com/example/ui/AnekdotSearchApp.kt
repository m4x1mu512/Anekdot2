package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.JokeItem
import com.example.ui.components.JokeDetailSidePane
import com.example.ui.components.JokeReaderBottomSheet
import com.example.ui.screens.HistoryAndCacheScreen
import com.example.ui.screens.RandomJokesScreen
import com.example.ui.screens.SearchScreen
import kotlinx.coroutines.launch

private data class NavDestination(
    val tab: AppTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private val Destinations = listOf(
    NavDestination(
        tab = AppTab.SEARCH,
        labelRes = R.string.nav_search,
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
        tag = "nav_tab_search"
    ),
    NavDestination(
        tab = AppTab.RANDOM,
        labelRes = R.string.nav_random,
        selectedIcon = Icons.Filled.Casino,
        unselectedIcon = Icons.Outlined.Casino,
        tag = "nav_tab_random"
    ),
    NavDestination(
        tab = AppTab.HISTORY,
        labelRes = R.string.nav_history,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        tag = "nav_tab_history"
    )
)

@Composable
fun AnekdotSearchApp(
    viewModel: JokeViewModel,
    isDarkTheme: Boolean
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cachedJokes by viewModel.cachedJokes.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.copied_snackbar)
    val shareTitle = stringResource(R.string.share_chooser_title)

    BackHandler(enabled = uiState.currentTab != AppTab.SEARCH) {
        viewModel.selectTab(AppTab.SEARCH)
    }

    val copyJoke: (JokeItem) -> Unit = { joke ->
        copyJokeToClipboard(context, joke)
        coroutineScope.launch {
            snackbarHostState.showSnackbar(copiedMessage)
        }
    }

    val shareJoke: (JokeItem) -> Unit = { joke ->
        shareJokeIntent(context, joke, shareTitle)
    }

    val openSource: (JokeItem) -> Unit = { joke ->
        openBrowserUrl(context, joke.sourceUrl)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedLayout = maxWidth >= 720.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!isExpandedLayout) {
                    NavigationBar(modifier = Modifier.testTag("bottom_navigation_bar")) {
                        Destinations.forEach { dest ->
                            val selected = uiState.currentTab == dest.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            if (isExpandedLayout) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("side_navigation_rail")
                    ) {
                        Destinations.forEach { dest ->
                            val selected = uiState.currentTab == dest.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.tag)
                            )
                        }
                    }

                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Box(modifier = Modifier.weight(1.15f)) {
                        ActiveTabContent(
                            uiState = uiState,
                            cachedJokes = cachedJokes,
                            searchHistory = searchHistory,
                            isDarkTheme = isDarkTheme,
                            isExpandedLayout = true,
                            viewModel = viewModel,
                            onCopyJoke = copyJoke,
                            onShareJoke = shareJoke,
                            onOpenSource = openSource
                        )
                    }

                    Box(modifier = Modifier.weight(0.95f)) {
                        JokeDetailSidePane(
                            joke = uiState.selectedJoke,
                            highlightQuery = if (uiState.currentTab == AppTab.SEARCH) {
                                uiState.submittedQuery
                            } else {
                                uiState.historyFilterQuery
                            },
                            isLargeFont = uiState.isLargeReaderFont,
                            onToggleFontSize = viewModel::toggleLargeReaderFont,
                            onCopy = copyJoke,
                            onShare = shareJoke,
                            onOpenSource = openSource
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ActiveTabContent(
                        uiState = uiState,
                        cachedJokes = cachedJokes,
                        searchHistory = searchHistory,
                        isDarkTheme = isDarkTheme,
                        isExpandedLayout = false,
                        viewModel = viewModel,
                        onCopyJoke = copyJoke,
                        onShareJoke = shareJoke,
                        onOpenSource = openSource
                    )
                }

                if (uiState.isReaderSheetOpen && uiState.selectedJoke != null) {
                    JokeReaderBottomSheet(
                        joke = uiState.selectedJoke!!,
                        highlightQuery = if (uiState.currentTab == AppTab.SEARCH) {
                            uiState.submittedQuery
                        } else {
                            uiState.historyFilterQuery
                        },
                        isLargeFont = uiState.isLargeReaderFont,
                        onToggleFontSize = viewModel::toggleLargeReaderFont,
                        onDismiss = viewModel::dismissReaderSheet,
                        onCopy = copyJoke,
                        onShare = shareJoke,
                        onOpenSource = openSource
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveTabContent(
    uiState: JokeUiState,
    cachedJokes: List<JokeItem>,
    searchHistory: List<com.example.data.local.SearchHistoryEntity>,
    isDarkTheme: Boolean,
    isExpandedLayout: Boolean,
    viewModel: JokeViewModel,
    onCopyJoke: (JokeItem) -> Unit,
    onShareJoke: (JokeItem) -> Unit,
    onOpenSource: (JokeItem) -> Unit
) {
    val selectedId = if (isExpandedLayout) uiState.selectedJoke?.id else null

    when (uiState.currentTab) {
        AppTab.SEARCH -> SearchScreen(
            queryInput = uiState.queryInput,
            submittedQuery = uiState.submittedQuery,
            enabledSources = uiState.enabledSources,
            feedState = uiState.searchFeedState,
            sourceStatuses = uiState.searchSourceStatuses,
            currentPage = uiState.currentPage,
            isLoadingMore = uiState.isLoadingMore,
            canLoadMore = uiState.canLoadMore,
            selectedJokeId = selectedId,
            isDarkTheme = isDarkTheme,
            onQueryChange = viewModel::updateQueryInput,
            onClearQuery = viewModel::clearQueryInput,
            onSubmitSearch = viewModel::submitSearch,
            onToggleSource = viewModel::toggleSource,
            onLoadMore = viewModel::loadMoreSearchResults,
            onSelectJoke = { joke ->
                viewModel.openJokeReader(joke, openModalSheet = !isExpandedLayout)
            },
            onCopyJoke = onCopyJoke,
            onShareJoke = onShareJoke,
            onOpenSource = onOpenSource,
            onToggleTheme = { viewModel.toggleDarkTheme(isDarkTheme) }
        )

        AppTab.RANDOM -> RandomJokesScreen(
            feedState = uiState.randomFeedState,
            sourceStatuses = uiState.randomSourceStatuses,
            selectedSourceFilter = uiState.randomSourceFilter,
            selectedJokeId = selectedId,
            onSelectSourceFilter = viewModel::setRandomSourceFilter,
            onRefresh = viewModel::refreshRandomJokes,
            onSelectJoke = { joke ->
                viewModel.openJokeReader(joke, openModalSheet = !isExpandedLayout)
            },
            onCopyJoke = onCopyJoke,
            onShareJoke = onShareJoke,
            onOpenSource = onOpenSource
        )

        AppTab.HISTORY -> HistoryAndCacheScreen(
            searchHistory = searchHistory,
            cachedJokes = cachedJokes,
            filterQuery = uiState.historyFilterQuery,
            selectedJokeId = selectedId,
            onFilterQueryChange = viewModel::updateHistoryFilter,
            onRepeatSearch = { query -> viewModel.submitSearch(query) },
            onDeleteHistoryQuery = viewModel::deleteHistoryQuery,
            onClearAll = viewModel::clearAllHistoryAndCache,
            onSelectJoke = { joke ->
                viewModel.openJokeReader(joke, openModalSheet = !isExpandedLayout)
            },
            onCopyJoke = onCopyJoke,
            onShareJoke = onShareJoke,
            onOpenSource = onOpenSource
        )
    }
}

private fun copyJokeToClipboard(context: Context, joke: JokeItem) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clipText = "${joke.text}\n\n— Источник: ${joke.source.displayName}"
    clipboard?.setPrimaryClip(ClipData.newPlainText("Анекдот", clipText))
}

private fun shareJokeIntent(context: Context, joke: JokeItem, chooserTitle: String) {
    val shareText = "${joke.text}\n\nИсточник: ${joke.source.displayName} (${joke.sourceUrl})"
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(chooser) }
}

private fun openBrowserUrl(context: Context, url: String) {
    if (!url.startsWith("http")) return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
