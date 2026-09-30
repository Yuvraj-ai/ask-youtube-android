package com.askyoutube.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.askyoutube.app.AskYoutubeApp
import com.askyoutube.app.data.settings.SettingsStore
import com.askyoutube.app.domain.AppError
import com.askyoutube.app.data.llm.LlmException
import com.askyoutube.app.domain.AppException
import com.askyoutube.app.domain.RagEngine
import com.askyoutube.app.domain.RagStage
import com.askyoutube.app.domain.SuggestionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Turn(
    val question: String,
    val answer: String,
    val sources: List<AnswerSource>,
    val failed: Boolean = false,
    /** Tap-to-ask follow-ups, filled in after the answer lands. */
    val suggestions: List<String> = emptyList(),
)

data class AnswerSource(val text: String, val timestamp: String)

enum class Phase { IDLE, INDEXING, ANSWERING }

data class HomeUiState(
    val videoUrl: String = "",
    val draft: String = "",
    val turns: List<Turn> = emptyList(),
    val phase: Phase = Phase.IDLE,
    val error: String? = null,
    val hasApiKey: Boolean = false,
) {
    val busy: Boolean get() = phase != Phase.IDLE
    val canSend: Boolean get() = !busy && videoUrl.isNotBlank() && draft.isNotBlank()
    val showEmpty: Boolean get() = turns.isEmpty() && !busy && error == null
}

/**
 * Holds the conversation and drives the pipeline.
 *
 * All I/O goes through RagEngine inside viewModelScope, so it survives
 * configuration changes and the UI only ever renders state.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as AskYoutubeApp).container
    private val engine: RagEngine = container.ragEngine()
    private val settings: SettingsStore = container.settings()

    private val _state = MutableStateFlow(HomeUiState(hasApiKey = settings.hasApiKey()))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun onUrlChange(value: String) = _state.update { it.copy(videoUrl = value) }

    fun onDraftChange(value: String) = _state.update { it.copy(draft = value) }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun onSettingsChanged() = _state.update { it.copy(hasApiKey = settings.hasApiKey()) }

    fun pasteVideoUrl(url: String) {
        _state.update { it.copy(videoUrl = url.trim(), error = null) }
    }

    fun send() {
        val current = _state.value
        if (!current.canSend) return

        val question = current.draft.trim()
        val url = current.videoUrl.trim()

        // Optimistically show the question, then fill in the answer.
        _state.update {
            it.copy(
                turns = it.turns + Turn(question = question, answer = "", sources = emptyList()),
                draft = "",
                phase = Phase.INDEXING,
                error = null,
            )
        }

        viewModelScope.launch {
            try {
                val answer = engine.answer(url, question) { stage ->
                    _state.update {
                        it.copy(
                            phase = when (stage) {
                                RagStage.INDEXING -> Phase.INDEXING
                                RagStage.ANSWERING -> Phase.ANSWERING
                            }
                        )
                    }
                }
                _state.update { s ->
                    s.copy(
                        phase = Phase.IDLE,
                        turns = s.turns.dropLast(1) + Turn(
                            question = question,
                            answer = answer.text,
                            sources = answer.sources.map {
                                AnswerSource(
                                    text = it.text,
                                    timestamp = RagEngine.formatTimestamp(it.startMs),
                                )
                            },
                            // Free and offline: picked out of the transcript the
                            // answer just used, capped at four.
                            suggestions = SuggestionEngine.suggest(
                                chunks = answer.passages,
                                askedSoFar = s.turns.map { it.question } + question,
                            ),
                        ),
                    )
                }
            } catch (e: AppException) {
                fail(e.error)
            } catch (e: LlmException) {
                fail(e.error)
            } catch (e: Exception) {
                fail(AppError.Unexpected(e.message ?: "unknown error"))
            }
        }
    }

    /** Fills the composer with a suggested question, so the user can edit it. */
    fun applySuggestion(question: String) =
        _state.update { it.copy(draft = question) }

    /** Retries the last question after a failure. */
    fun retry() {
        val last = _state.value.turns.lastOrNull { it.failed } ?: return
        _state.update { s -> s.copy(draft = last.question, turns = s.turns.dropLast(1)) }
        send()
    }

    private fun fail(error: AppError) {
        _state.update { s ->
            s.copy(
                phase = Phase.IDLE,
                error = error.message,
                turns = s.turns.dropLast(1),
            )
        }
    }
}
