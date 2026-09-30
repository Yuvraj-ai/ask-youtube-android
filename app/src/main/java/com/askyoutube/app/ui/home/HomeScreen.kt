package com.askyoutube.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.askyoutube.app.R
import com.askyoutube.app.ui.theme.LocalAppColors
import com.askyoutube.app.ui.theme.MonoSmall

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onUrlChange: (String) -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onApplySuggestion: (String) -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val app = LocalAppColors.current
    val listState = rememberLazyListState()

    LaunchedEffect(state.turns.size) {
        if (state.turns.isNotEmpty()) listState.animateScrollToItem(state.turns.lastIndex)
    }

    Scaffold(
        containerColor = app.canvas,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title), style = MaterialTheme.typography.titleLarge) },
                // No YouTube or other third-party mark: the title is the brand.
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.action_settings),
                            tint = app.inkMuted,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = app.canvas,
                    titleContentColor = app.ink,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            // ---- video field region, divided from the conversation ----
            OutlinedTextField(
                value = state.videoUrl,
                onValueChange = onUrlChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text(stringResource(R.string.hint_video_url)) },
                singleLine = true,
                enabled = !state.busy,
                shape = MaterialTheme.shapes.medium,
                colors = fieldColors(),
            )

            if (state.phase != Phase.IDLE) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = when (state.phase) {
                            Phase.INDEXING -> stringResource(R.string.state_indexing)
                            Phase.ANSWERING -> stringResource(R.string.state_answering)
                            Phase.IDLE -> ""
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = app.inkMuted,
                    )
                    // A thin bar, not a spinner. A spinner in a reading app is
                    // visual noise.
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(2.dp),
                        color = app.ember,
                        trackColor = app.surfaceMuted,
                    )
                }
            }

            HorizontalDivider(color = app.hairline)

            state.error?.let { ErrorCard(it, onRetry, onDismissError) }

            // The empty state is centred in whatever space is left. Pinning it
            // to the top left a dead zone beneath it. The list is omitted
            // entirely while empty — giving both children weight(1f) split the
            // area in half and pushed the group back up.
            if (state.showEmpty) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    EmptyState(hasApiKey = state.hasApiKey, onOpenSettings = onOpenSettings)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.turns) { TurnCard(it, onApplySuggestion) }
                    if (state.busy) {
                        item(key = "busy") {
                            Text(
                                stringResource(R.string.state_answering),
                                style = MaterialTheme.typography.bodySmall,
                                color = app.inkFaint,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = app.hairline)

            InputRow(
                value = state.draft,
                enabled = !state.busy,
                canSend = state.canSend,
                onValueChange = onDraftChange,
                onSend = onSend,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TurnCard(turn: Turn, onApplySuggestion: (String) -> Unit) {
    val app = LocalAppColors.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Question: right-aligned, square on the bottom-right corner to imply
        // direction of speech.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Surface(
                color = app.surfaceMuted,
                shape = RoundedCornerShape(
                    topStart = 20.dp, topEnd = 20.dp,
                    bottomStart = 20.dp, bottomEnd = 4.dp,
                ),
            ) {
                Text(
                    text = turn.question,
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = app.ink,
                )
            }
        }

        // Answer: hairline border, no elevation. Answers sit in a conversation,
        // not on a stack of floating cards.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = app.surface,
            shape = MaterialTheme.shapes.largeIncreased,
            border = androidx.compose.foundation.BorderStroke(1.dp, app.hairline),
        ) {
            Column(Modifier.padding(16.dp)) {
                // Rendered verbatim so the model's paragraph breaks survive. The
                // source app did response.replace("\n", "  ") then
                // textwrap.fill(..., 80), destroying them; defect D4.
                Text(turn.answer, style = MaterialTheme.typography.bodyLarge, color = app.ink)
                if (turn.sources.isNotEmpty()) SourcesSection(turn.sources)
                if (turn.suggestions.isNotEmpty()) {
                    SuggestionRow(turn.suggestions, onApplySuggestion)
                }
            }
        }
    }
}

/**
 * Tap-to-ask follow-ups. Tapping fills the composer rather than sending
 * outright, so a suggestion can be edited before it costs a request.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionRow(suggestions: List<String>, onPick: (String) -> Unit) {
    val app = LocalAppColors.current
    Column(Modifier.padding(top = 4.dp)) {
        Text(
            text = stringResource(R.string.suggestions_title),
            style = MaterialTheme.typography.labelMedium,
            color = app.inkFaint,
        )
        FlowRow(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            suggestions.forEach { text ->
                Surface(
                    onClick = { onPick(text) },
                    shape = MaterialTheme.shapes.small,
                    color = app.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, app.hairline),
                ) {
                    Text(
                        text = text,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = app.ink,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourcesSection(sources: List<AnswerSource>) {
    val app = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }

    Column {
        TextButton(
            onClick = { expanded = !expanded },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 0.dp, vertical = 4.dp,
            ),
        ) {
            Text(
                text = if (expanded) {
                    stringResource(R.string.sources_hide)
                } else {
                    stringResource(R.string.sources_count, sources.size)
                },
                style = MaterialTheme.typography.labelLarge,
                color = app.inkMuted,
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = app.inkFaint,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(18.dp),
            )
        }

        AnimatedVisibility(visible = expanded) {
            // Plain rows separated by hairlines, not a card per source.
            Column {
                sources.forEachIndexed { i, source ->
                    if (i > 0) HorizontalDivider(color = app.hairline)
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text(source.timestamp, style = MonoSmall, color = app.ember)
                        Text(
                            text = source.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = app.inkMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit, onDismiss: () -> Unit) {
    val app = LocalAppColors.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = app.dangerContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = app.onDangerContainer)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_dismiss), color = app.onDangerContainer)
                }
                Spacer(Modifier.width(4.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = app.danger,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(R.string.action_retry),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(hasApiKey: Boolean, onOpenSettings: () -> Unit) {
    val app = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = app.ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = app.inkMuted,
            textAlign = TextAlign.Center,
        )
        if (!hasApiKey) {
            Text(
                text = stringResource(R.string.empty_no_key),
                style = MaterialTheme.typography.bodyMedium,
                color = app.danger,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = app.surfaceMuted),
            ) {
                Text(stringResource(R.string.action_settings), color = app.ink)
            }
        }
    }
}

@Composable
private fun InputRow(
    value: String,
    enabled: Boolean,
    canSend: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val app = LocalAppColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(app.canvas)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.hint_question), color = app.inkFaint) },
            enabled = enabled,
            maxLines = 4,
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors(),
        )
        // Small and calm. The previous 48dp filled circle read as a warning.
        Surface(
            onClick = onSend,
            enabled = canSend,
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = if (canSend) app.ember else app.surfaceMuted,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.action_send),
                    tint = if (canSend) MaterialTheme.colorScheme.onPrimary else app.inkFaint,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = LocalAppColors.current.surface,
    unfocusedContainerColor = LocalAppColors.current.surface,
    focusedBorderColor = LocalAppColors.current.ember,
    unfocusedBorderColor = LocalAppColors.current.hairline,
    focusedLabelColor = LocalAppColors.current.ember,
    unfocusedLabelColor = LocalAppColors.current.inkMuted,
    cursorColor = LocalAppColors.current.ember,
    focusedTextColor = LocalAppColors.current.ink,
    unfocusedTextColor = LocalAppColors.current.ink,
)
