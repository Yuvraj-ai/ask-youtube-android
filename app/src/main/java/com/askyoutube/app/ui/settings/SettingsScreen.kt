package com.askyoutube.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.askyoutube.app.R
import com.askyoutube.app.data.settings.EmbeddingSource
import com.askyoutube.app.data.settings.Provider
import com.askyoutube.app.data.settings.ThemeMode
import com.askyoutube.app.ui.theme.LocalAppColors
import com.askyoutube.app.ui.theme.MonoBody
import com.askyoutube.app.ui.theme.MonoSmall

enum class KeyStatus { NONE, SAVED, CHECKING, VALID, INVALID, FAILED }

data class SettingsUiState(
    val apiKey: String = "",
    val hasStoredKey: Boolean = false,
    val keyStatus: KeyStatus = KeyStatus.NONE,
    val statusDetail: String = "",
    val topK: Int = 4,
    val embeddingModel: String = "",
    val chatModel: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val provider: Provider = Provider.GOOGLE,
    val baseUrl: String = "",
    val embeddingSource: EmbeddingSource = EmbeddingSource.SAME_AS_CHAT,
    val embeddingBaseUrl: String = "",
    val embeddingApiKey: String = "",
    val hasEmbeddingKey: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onApiKeyChange: (String) -> Unit,
    onSaveKey: () -> Unit,
    onValidateKey: () -> Unit,
    onClearKey: () -> Unit,
    onTopKChange: (Int) -> Unit,
    onEmbeddingModelChange: (String) -> Unit,
    onChatModelChange: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onProviderChange: (Provider) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onEmbeddingSourceChange: (EmbeddingSource) -> Unit,
    onEmbeddingBaseUrlChange: (String) -> Unit,
    onEmbeddingApiKeyChange: (String) -> Unit,
    onSaveEmbeddingKey: () -> Unit,
    onBack: () -> Unit,
) {
    val app = LocalAppColors.current
    var revealed by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = app.canvas,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = app.ink,
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
                .verticalScroll(rememberScrollState()),
        ) {
            // ---- Provider ----
            Section {
                Text(
                    stringResource(R.string.label_provider),
                    style = MaterialTheme.typography.titleMedium,
                    color = app.ink,
                )
                Text(
                    stringResource(R.string.label_provider_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = app.inkMuted,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Provider.GOOGLE to R.string.provider_google,
                        Provider.OPENAI_COMPATIBLE to R.string.provider_openai,
                    ).forEach { (value, label) ->
                        val sel = state.provider == value
                        FilterChip(
                            selected = sel,
                            onClick = { onProviderChange(value) },
                            label = { Text(stringResource(label)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = app.ember,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = sel,
                                borderColor = app.hairline,
                                selectedBorderColor = app.ember,
                            ),
                        )
                    }
                }
            }

            HorizontalDivider(color = app.hairline)

            // Only a custom server has an address to configure. Showing a base
            // URL box while Google is selected would be noise, since that host
            // is fixed.
            if (state.provider == Provider.OPENAI_COMPATIBLE) {
                Section {
                    Text(
                        stringResource(R.string.label_base_url),
                        style = MaterialTheme.typography.titleMedium,
                        color = app.ink,
                    )
                    Text(
                        stringResource(R.string.label_base_url_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = app.inkMuted,
                    )
                    OutlinedTextField(
                        value = state.baseUrl,
                        onValueChange = onBaseUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.hint_base_url)) },
                        textStyle = MonoBody.copy(color = app.ink),
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        colors = fieldColors(),
                    )
                }
                HorizontalDivider(color = app.hairline)
            }

            // ---- API key ----
            Section {
                Text(
                    stringResource(R.string.label_api_key),
                    style = MaterialTheme.typography.titleMedium,
                    color = app.ink,
                )
                Text(
                    stringResource(R.string.label_api_key_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = app.inkMuted,
                )
                OutlinedTextField(
                    value = state.apiKey,
                    onValueChange = onApiKeyChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.hint_api_key)) },
                    visualTransformation = if (revealed) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = MaterialTheme.shapes.medium,
                    colors = fieldColors(),
                )
                // One row, evenly spaced, so the three controls stop competing.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = onSaveKey,
                        enabled = state.apiKey.isNotBlank() && state.keyStatus != KeyStatus.CHECKING,
                        colors = ButtonDefaults.buttonColors(containerColor = app.ember),
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                    OutlinedButton(
                        onClick = onValidateKey,
                        enabled = state.apiKey.isNotBlank() && state.keyStatus != KeyStatus.CHECKING,
                        border = BorderStroke(1.dp, app.hairline),
                    ) {
                        Text(stringResource(R.string.action_validate), color = app.ink)
                    }
                    TextButton(onClick = { revealed = !revealed }) {
                        Text(
                            stringResource(
                                if (revealed) R.string.action_hide_key else R.string.action_show_key
                            ),
                            color = app.ember,
                        )
                    }
                }
                KeyStatusPill(state)
                if (state.hasStoredKey) {
                    TextButton(
                        onClick = onClearKey,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 0.dp, vertical = 4.dp,
                        ),
                    ) {
                        Text(stringResource(R.string.action_clear), color = app.danger)
                    }
                }
            }

            HorizontalDivider(color = app.hairline)

            // ---- Retrieval ----
            Section {
                Text(
                    stringResource(R.string.label_retrieval),
                    style = MaterialTheme.typography.titleMedium,
                    color = app.ink,
                )
                Text(
                    stringResource(R.string.label_retrieval_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = app.inkMuted,
                )
                // A number is data, so it gets the mono treatment.
                Text(state.topK.toString(), style = MonoBody, color = app.ink)
                Slider(
                    value = state.topK.toFloat(),
                    onValueChange = { onTopKChange(it.toInt()) },
                    valueRange = 1f..12f,
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = app.ember,
                        activeTrackColor = app.ember,
                        inactiveTrackColor = app.surfaceMuted,
                        activeTickColor = app.canvas,
                        inactiveTickColor = app.hairline,
                    ),
                )
            }

            HorizontalDivider(color = app.hairline)

            // ---- Embeddings: independent of the chat provider ----
            Section {
                Text(
                    stringResource(R.string.label_embeddings),
                    style = MaterialTheme.typography.titleMedium,
                    color = app.ink,
                )
                Text(
                    stringResource(R.string.label_embeddings_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = app.inkMuted,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        EmbeddingSource.SAME_AS_CHAT to R.string.embeddings_same,
                        EmbeddingSource.CUSTOM to R.string.embeddings_custom,
                    ).forEach { (value, label) ->
                        val sel = state.embeddingSource == value
                        FilterChip(
                            selected = sel,
                            onClick = { onEmbeddingSourceChange(value) },
                            label = { Text(stringResource(label)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = app.ember,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = sel,
                                borderColor = app.hairline,
                                selectedBorderColor = app.ember,
                            ),
                        )
                    }
                }
            }

            if (state.embeddingSource == EmbeddingSource.CUSTOM) {
                Section {
                    OutlinedTextField(
                        value = state.embeddingBaseUrl,
                        onValueChange = onEmbeddingBaseUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.label_embedding_base_url)) },
                        textStyle = MonoBody.copy(color = app.ink),
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        colors = fieldColors(),
                    )
                    Text(
                        stringResource(R.string.label_embedding_api_key),
                        style = MaterialTheme.typography.titleSmall,
                        color = app.ink,
                    )
                    OutlinedTextField(
                        value = state.embeddingApiKey,
                        onValueChange = onEmbeddingApiKeyChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.label_embedding_api_key)) },
                        textStyle = MonoBody.copy(color = app.ink),
                        shape = MaterialTheme.shapes.medium,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = fieldColors(),
                    )
                    Text(
                        stringResource(R.string.label_embedding_api_key_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = app.inkFaint,
                    )
                    Button(
                        onClick = onSaveEmbeddingKey,
                        enabled = state.embeddingApiKey.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = app.ember),
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                    if (state.hasEmbeddingKey) {
                        TextButton(
                            onClick = onSaveEmbeddingKey,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 0.dp, vertical = 4.dp,
                            ),
                        ) {
                            Text(stringResource(R.string.status_embedding_saved), color = app.inkMuted)
                        }
                    }
                }
                HorizontalDivider(color = app.hairline)
            }

            // ---- Models ----
            Section {
                Text(
                    stringResource(
                        if (state.provider == Provider.GOOGLE) {
                            R.string.label_models_help
                        } else {
                            R.string.label_models_help_custom
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = app.inkMuted,
                )
                OutlinedTextField(
                    value = state.embeddingModel,
                    onValueChange = onEmbeddingModelChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.label_embedding_model)) },
                    textStyle = MonoBody.copy(color = app.ink),
                    shape = MaterialTheme.shapes.medium,
                    colors = fieldColors(),
                )
                OutlinedTextField(
                    value = state.chatModel,
                    onValueChange = onChatModelChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.label_chat_model)) },
                    textStyle = MonoBody.copy(color = app.ink),
                    shape = MaterialTheme.shapes.medium,
                    colors = fieldColors(),
                )
            }

            HorizontalDivider(color = app.hairline)

            // ---- Appearance ----
            Section {
                Text(
                    stringResource(R.string.label_appearance),
                    style = MaterialTheme.typography.titleMedium,
                    color = app.ink,
                )
                Text(
                    stringResource(R.string.label_appearance_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = app.inkMuted,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        val selected = state.themeMode == mode
                        FilterChip(
                            selected = selected,
                            onClick = { onThemeModeChange(mode) },
                            label = { Text(stringResource(mode.labelRes())) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = app.ember,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = app.hairline,
                                selectedBorderColor = app.ember,
                            ),
                        )
                    }
                }
            }

            HorizontalDivider(color = app.hairline)

            // ---- About ----
            Text(
                text = stringResource(R.string.about_body),
                style = MaterialTheme.typography.bodySmall,
                color = app.inkFaint,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun Section(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun KeyStatusPill(state: SettingsUiState) {
    val app = LocalAppColors.current

    val (tint, onTint) = when (state.keyStatus) {
        KeyStatus.VALID, KeyStatus.SAVED -> app.surfaceMuted to app.ink
        KeyStatus.INVALID, KeyStatus.FAILED -> app.dangerContainer to app.onDangerContainer
        else -> app.surfaceMuted to app.inkMuted
    }

    val label = when (state.keyStatus) {
        KeyStatus.NONE -> stringResource(R.string.status_key_missing)
        KeyStatus.SAVED -> stringResource(R.string.status_key_saved)
        KeyStatus.CHECKING -> stringResource(R.string.status_validating)
        KeyStatus.VALID -> stringResource(R.string.status_valid)
        KeyStatus.INVALID -> stringResource(R.string.status_invalid)
        KeyStatus.FAILED -> stringResource(R.string.status_error, state.statusDetail)
    }

    // An understated chip, not an alert banner.
    Surface(color = tint, shape = MaterialTheme.shapes.small) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (state.keyStatus) {
                KeyStatus.CHECKING ->
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                KeyStatus.VALID, KeyStatus.SAVED ->
                    Icon(Icons.Filled.Check, null, tint = app.ember, modifier = Modifier.size(16.dp))
                KeyStatus.INVALID, KeyStatus.FAILED ->
                    Icon(Icons.Filled.Close, null, tint = app.danger, modifier = Modifier.size(16.dp))
                else -> Unit
            }
            Text(label, style = MaterialTheme.typography.bodySmall, color = onTint)
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

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}
