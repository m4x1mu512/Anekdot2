package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.JokeItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JokeReaderBottomSheet(
    joke: JokeItem,
    highlightQuery: String,
    isLargeFont: Boolean,
    onToggleFontSize: () -> Unit,
    onDismiss: () -> Unit,
    onCopy: (JokeItem) -> Unit,
    onShare: (JokeItem) -> Unit,
    onOpenSource: (JokeItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("joke_reader_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            JokeReaderContent(
                joke = joke,
                highlightQuery = highlightQuery,
                isLargeFont = isLargeFont,
                showCloseButton = true,
                onToggleFontSize = onToggleFontSize,
                onClose = onDismiss,
                onCopy = { onCopy(joke) },
                onShare = { onShare(joke) },
                onOpenSource = { onOpenSource(joke) }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun JokeDetailSidePane(
    joke: JokeItem?,
    highlightQuery: String,
    isLargeFont: Boolean,
    onToggleFontSize: () -> Unit,
    onCopy: (JokeItem) -> Unit,
    onShare: (JokeItem) -> Unit,
    onOpenSource: (JokeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("joke_detail_side_pane"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        if (joke == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FormatQuote,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = scheme.primary.copy(alpha = 0.5f)
                    )
                    Text(
                        text = stringResource(R.string.select_joke_pane_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                JokeReaderContent(
                    joke = joke,
                    highlightQuery = highlightQuery,
                    isLargeFont = isLargeFont,
                    showCloseButton = false,
                    onToggleFontSize = onToggleFontSize,
                    onClose = {},
                    onCopy = { onCopy(joke) },
                    onShare = { onShare(joke) },
                    onOpenSource = { onOpenSource(joke) }
                )
            }
        }
    }
}

@Composable
private fun JokeReaderContent(
    joke: JokeItem,
    highlightQuery: String,
    isLargeFont: Boolean,
    showCloseButton: Boolean,
    onToggleFontSize: () -> Unit,
    onClose: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onOpenSource: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val (badgeBg, badgeFg) = sourceBadgeColors(joke.source)

    val annotated = remember(joke.text, highlightQuery, scheme.secondaryContainer, scheme.onSecondaryContainer) {
        buildHighlightedText(
            text = joke.text,
            query = highlightQuery,
            highlightBg = scheme.secondaryContainer,
            highlightFg = scheme.onSecondaryContainer
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = badgeBg,
                contentColor = badgeFg,
                shape = CircleShape
            ) {
                Text(
                    text = joke.source.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
            FilterChip(
                selected = isLargeFont,
                onClick = onToggleFontSize,
                label = { Text(stringResource(R.string.font_size_toggle)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.FormatSize,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.testTag("reader_font_size_chip")
            )
        }

        if (showCloseButton) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.testTag("close_reader_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.close_reader_cd)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = joke.source.shortDescription,
        style = MaterialTheme.typography.bodySmall,
        color = scheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(20.dp))

    SelectionContainer {
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = if (isLargeFont) 20.sp else 17.sp,
                lineHeight = if (isLargeFont) 31.sp else 27.sp
            ),
            color = scheme.onSurface
        )
    }

    Spacer(modifier = Modifier.height(24.dp))
    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onCopy,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("reader_copy_button")
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_copy))
        }

        FilledTonalButton(
            onClick = onShare,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("reader_share_button")
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_share))
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedButton(
        onClick = onOpenSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("reader_open_site_button")
    ) {
        Text(text = "${stringResource(R.string.action_open_site)} (${joke.source.displayName})")
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
    }
}
