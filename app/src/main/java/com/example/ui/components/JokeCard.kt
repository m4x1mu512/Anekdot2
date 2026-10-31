package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.JokeItem
import com.example.data.model.JokeSource
import com.example.data.model.SourceState
import com.example.data.model.SourceStatus
import com.example.data.remote.RussianQueryMatcher

@Composable
fun sourceBadgeColors(source: JokeSource): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (source) {
        JokeSource.ANEKDOT_RU -> scheme.primaryContainer to scheme.onPrimaryContainer
        JokeSource.NEKDO_RU -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        JokeSource.ANEKDOTY_RU -> scheme.secondaryContainer to scheme.onSecondaryContainer
        JokeSource.BANEKS_RU -> scheme.surfaceContainerHigh to scheme.onSurface
        JokeSource.RZHUNEMOGU_RU -> scheme.primary.copy(alpha = 0.14f) to scheme.primary
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JokeCard(
    joke: JokeItem,
    highlightQuery: String,
    isSelected: Boolean,
    index: Int,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onOpenSource: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val (badgeBg, badgeFg) = sourceBadgeColors(joke.source)
    val highlightBg = scheme.secondaryContainer
    val highlightFg = scheme.onSecondaryContainer

    val annotatedJokeText = remember(joke.text, highlightQuery, highlightBg, highlightFg) {
        buildHighlightedText(
            text = joke.text,
            query = highlightQuery,
            highlightBg = highlightBg,
            highlightFg = highlightFg
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("joke_card_$index")
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                scheme.surfaceContainer
            } else {
                scheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) scheme.primary else scheme.outlineVariant.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = badgeBg,
                    contentColor = badgeFg,
                    shape = CircleShape
                ) {
                    Text(
                        text = joke.source.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                if (joke.categoryLabel.isNotBlank()) {
                    Surface(
                        color = scheme.surfaceContainerHigh,
                        contentColor = scheme.onSurfaceVariant,
                        shape = CircleShape
                    ) {
                        Text(
                            text = joke.categoryLabel,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = annotatedJokeText,
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurface,
                maxLines = 14,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onCopy,
                        modifier = Modifier.testTag("copy_joke_button_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.action_copy),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_copy),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.testTag("share_joke_button_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.action_share),
                            tint = scheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(
                    onClick = onOpenSource,
                    modifier = Modifier.testTag("open_source_button_$index")
                ) {
                    Text(
                        text = stringResource(R.string.action_open_site),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = stringResource(R.string.action_open_site),
                        modifier = Modifier.size(16.dp),
                        tint = scheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
fun SourceTelemetryStrip(
    statuses: List<SourceStatus>,
    modifier: Modifier = Modifier
) {
    val activeStatuses = statuses.filter { it.state !is SourceState.Idle }
    if (activeStatuses.isEmpty()) return

    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        activeStatuses.forEach { item ->
            Surface(
                shape = CircleShape,
                color = scheme.surfaceContainer,
                border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (val st = item.state) {
                        is SourceState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = scheme.primary
                            )
                            Text(
                                text = item.source.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                        is SourceState.Success -> {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (st.count > 0) scheme.tertiary else scheme.outline
                            )
                            Text(
                                text = "${item.source.displayName}: ${st.count}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (st.count > 0) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (st.count > 0) scheme.onSurface else scheme.onSurfaceVariant
                            )
                        }
                        is SourceState.Error -> {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = scheme.error
                            )
                            Text(
                                text = "${item.source.displayName}: недоступен",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                        SourceState.Idle -> Box(Modifier)
                    }
                }
            }
        }
    }
}

fun buildHighlightedText(
    text: String,
    query: String,
    highlightBg: Color,
    highlightFg: Color
): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val ranges = RussianQueryMatcher.findHighlightRanges(text, query)
    if (ranges.isEmpty()) return AnnotatedString(text)

    return buildAnnotatedString {
        append(text)
        for (range in ranges) {
            val endExclusive = (range.last + 1).coerceAtMost(text.length)
            if (range.first in 0 until endExclusive) {
                addStyle(
                    style = SpanStyle(
                        background = highlightBg,
                        color = highlightFg,
                        fontWeight = FontWeight.SemiBold
                    ),
                    start = range.first,
                    end = endExclusive
                )
            }
        }
    }
}
