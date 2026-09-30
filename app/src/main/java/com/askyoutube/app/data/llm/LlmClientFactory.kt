package com.askyoutube.app.data.llm

import com.askyoutube.app.data.gemini.GeminiClient
import com.askyoutube.app.data.settings.EmbeddingSource
import com.askyoutube.app.data.settings.Provider
import com.askyoutube.app.data.settings.SettingsStore

/**
 * Picks the clients for the currently selected configuration.
 *
 * Chat and embeddings are resolved separately, because they are genuinely
 * separate concerns: a server that hosts a chat model frequently has no
 * embedding endpoint for it, so pointing the vector model at a different host —
 * with a different key — is a normal setup rather than an edge case.
 *
 * Both are read fresh on every call so a Settings change takes effect on the
 * next question without restarting. Clients hold no per-request state, and the
 * OpenAI-compatible one is memoised on its address to avoid rebuilding it (and
 * its request path) for every chunk.
 */
class LlmClientFactory(private val settings: SettingsStore) {

    private val gemini = GeminiClient()
    private val cache = HashMap<String, OpenAiCompatibleClient>()

    /** Backend that produces answers. */
    fun chatClient(): LlmClient = when (settings.provider) {
        Provider.GOOGLE -> gemini
        Provider.OPENAI_COMPATIBLE -> openAi(settings.baseUrl)
    }

    /** Backend that produces vectors. */
    fun embedClient(): LlmClient = when (settings.embeddingSource) {
        EmbeddingSource.SAME_AS_CHAT -> chatClient()
        EmbeddingSource.CUSTOM -> openAi(settings.embeddingBaseUrl)
    }

    /** Key to send with embedding requests; differs from the chat key when custom. */
    fun embeddingApiKey(): String =
        if (settings.embeddingSource == EmbeddingSource.CUSTOM) {
            settings.embeddingApiKey
        } else {
            settings.apiKey
        }

    private fun openAi(baseUrl: String): OpenAiCompatibleClient =
        cache.getOrPut(baseUrl) { OpenAiCompatibleClient(baseUrl) }

    /**
     * Defaults for a provider, applied when the user switches. A Gemini model id
     * left pointed at a different host is the fastest way to a confusing 404, so
     * the ids are replaced rather than carried over.
     */
    fun applyProviderDefaults(previous: Provider) {
        val now = settings.provider
        if (previous == now) return
        when (now) {
            Provider.GOOGLE -> {
                settings.embeddingModel = SettingsStore.DEFAULT_EMBEDDING_MODEL
                settings.chatModel = SettingsStore.DEFAULT_CHAT_MODEL
            }
            Provider.OPENAI_COMPATIBLE -> {
                settings.embeddingModel = SettingsStore.DEFAULT_OPENAI_EMBEDDING_MODEL
                settings.chatModel = SettingsStore.DEFAULT_OPENAI_CHAT_MODEL
            }
        }
    }
}
