package com.askyoutube.app.data.llm

import com.askyoutube.app.domain.AppError

/**
 * A model backend the app can talk to.
 *
 * Two things the pipeline needs from a backend, and no more:
 *  - turn a string into a vector, for retrieval
 *  - turn a grounded prompt into an answer
 *
 * Plus a cheap way to check the credentials before the user depends on them.
 *
 * The interface exists because Gemini and the OpenAI-compatible wire formats
 * differ in three separate places — the base URL, the auth header, and both
 * request and response shapes — and the alternative is one client full of `if`
 * branches on the provider name.
 */
interface LlmClient {

    /** Shown in Settings so the user knows which backend is live. */
    val label: String

    /**
     * @param taskPrefix "query: " for the user's question, "document: " for a
     *   transcript chunk. gemini-embedding-2 carries task intent in the prompt
     *   text rather than a task_type parameter, which is the vendor's migration
     *   guidance. Other backends largely ignore it, which is harmless.
     */
    suspend fun embed(
        text: String,
        model: String,
        apiKey: String,
        dimensions: Int,
        taskPrefix: String,
    ): FloatArray

    suspend fun generate(prompt: String, model: String, apiKey: String): String

    suspend fun validateKey(apiKey: String): ValidationResult

    sealed interface ValidationResult {
        data object Valid : ValidationResult
        data object Invalid : ValidationResult
        data class Failed(val detail: String) : ValidationResult
    }
}

/** Thrown internally; the ViewModel turns it into an [AppError] message. */
class LlmException(val error: AppError) : Exception(error.message)
