package com.askyoutube.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.askyoutube.app.AskYoutubeApp
import com.askyoutube.app.data.llm.LlmClient
import com.askyoutube.app.data.settings.EmbeddingSource
import com.askyoutube.app.data.settings.Provider
import com.askyoutube.app.data.settings.SettingsStore
import com.askyoutube.app.data.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as AskYoutubeApp).container
    private val settings: SettingsStore = container.settings()
    private val clients = container.llmClients()

    private val _state = MutableStateFlow(
        SettingsUiState(
            apiKey = settings.apiKey,
            hasStoredKey = settings.hasApiKey(),
            keyStatus = if (settings.hasApiKey()) KeyStatus.SAVED else KeyStatus.NONE,
            topK = settings.topK,
            embeddingModel = settings.embeddingModel,
            chatModel = settings.chatModel,
            themeMode = settings.themeMode,
            provider = settings.provider,
            baseUrl = settings.baseUrl,
            embeddingSource = settings.embeddingSource,
            embeddingBaseUrl = settings.embeddingBaseUrl,
            embeddingApiKey = settings.embeddingApiKey,
            hasEmbeddingKey = settings.hasEmbeddingKey(),
        )
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun onThemeModeChange(mode: ThemeMode) {
        settings.themeMode = mode
        _state.update { it.copy(themeMode = mode) }
    }

    /**
     * Switching provider swaps the model ids to that provider's defaults,
     * because a Gemini model id pointed at a different host is the fastest way
     * to get a confusing error.
     */
    fun onProviderChange(value: Provider) {
        val previous = settings.provider
        settings.provider = value
        clients.applyProviderDefaults(previous)
        _state.update {
            it.copy(
                provider = value,
                embeddingModel = settings.embeddingModel,
                chatModel = settings.chatModel,
                // The stored key belongs to the old provider.
                apiKey = "",
                hasStoredKey = false,
                keyStatus = KeyStatus.NONE,
            )
        }
    }

    fun onBaseUrlChange(value: String) = _state.update { it.copy(baseUrl = value) }

    fun onEmbeddingSourceChange(value: EmbeddingSource) {
        settings.embeddingSource = value
        _state.update { it.copy(embeddingSource = value) }
    }

    fun onEmbeddingBaseUrlChange(value: String) =
        _state.update { it.copy(embeddingBaseUrl = value) }

    fun onEmbeddingApiKeyChange(value: String) =
        _state.update { it.copy(embeddingApiKey = value) }

    /** Saves the embedding address and key together. */
    fun onSaveEmbeddingKey() {
        settings.embeddingBaseUrl = _state.value.embeddingBaseUrl
        settings.embeddingApiKey = _state.value.embeddingApiKey
        _state.update { it.copy(hasEmbeddingKey = settings.hasEmbeddingKey()) }
    }

    /** Persist the address on explicit save, alongside the key. */
    fun saveBaseUrl() {
        settings.baseUrl = _state.value.baseUrl
    }

    fun onApiKeyChange(value: String) =
        _state.update { it.copy(apiKey = value, keyStatus = KeyStatus.NONE) }

    fun onTopKChange(value: Int) {
        settings.topK = value
        _state.update { it.copy(topK = settings.topK) }
    }

    fun onEmbeddingModelChange(value: String) {
        settings.embeddingModel = value
        _state.update { it.copy(embeddingModel = settings.embeddingModel) }
    }

    fun onChatModelChange(value: String) {
        settings.chatModel = value
        _state.update { it.copy(chatModel = settings.chatModel) }
    }

    /** Saves without contacting Google, so the user is not forced to wait. */
    fun saveKey() {
        settings.apiKey = _state.value.apiKey
        saveBaseUrl()
        _state.update {
            it.copy(hasStoredKey = settings.hasApiKey(), keyStatus = KeyStatus.SAVED)
        }
    }

    /**
     * Confirms the key with Google.
     *
     * Uses a model listing rather than an embedding call: it costs nothing
     * against quota and proves the key can reach the API at all. This is the
     * fix for defect D9, where a wrong key only surfaced much later.
     */
    fun validateKey() {
        val candidate = _state.value.apiKey.trim()
        if (candidate.isEmpty()) return
        _state.update { it.copy(keyStatus = KeyStatus.CHECKING, statusDetail = "") }

        viewModelScope.launch {
            when (val result = clients.chatClient().validateKey(candidate)) {
                is LlmClient.ValidationResult.Valid -> {
                    // A key that works is worth keeping without a second tap.
                    settings.apiKey = candidate
                    _state.update { it.copy(keyStatus = KeyStatus.VALID, hasStoredKey = true) }
                }
                is LlmClient.ValidationResult.Invalid ->
                    _state.update { it.copy(keyStatus = KeyStatus.INVALID) }
                is LlmClient.ValidationResult.Failed ->
                    _state.update { it.copy(keyStatus = KeyStatus.FAILED, statusDetail = result.detail) }
            }
        }
    }

    fun clearKey() {
        settings.clearApiKey()
        _state.update {
            it.copy(
                apiKey = "",
                hasStoredKey = false,
                keyStatus = KeyStatus.NONE,
                statusDetail = "",
            )
        }
    }
}
