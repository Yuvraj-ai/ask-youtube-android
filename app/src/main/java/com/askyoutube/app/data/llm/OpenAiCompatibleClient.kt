package com.askyoutube.app.data.llm

import com.askyoutube.app.domain.AppError
import com.askyoutube.app.network.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Any backend that speaks the OpenAI wire format, which is what most
 * self-hosted and third-party model servers expose.
 *
 * Covers serving models such as gemma-* on a compatible endpoint. Three
 * differences from Google's native format, all handled here:
 *  - the host is configurable rather than fixed
 *  - auth is `Authorization: Bearer` rather than the x-goog-api-key header
 *  - the endpoints and JSON shapes are /chat/completions and /embeddings
 *
 * The base URL is used exactly as typed apart from a trailing slash. Hosts
 * differ on whether the version segment is part of it, so the field is not
 * second-guessed; the Settings helper text says to paste the OpenAI-compatible
 * base, usually ending in /v1.
 */
class OpenAiCompatibleClient(private val baseUrl: String) : LlmClient {

    override val label: String = "OpenAI-compatible"

    private val json = "application/json".toMediaType()

    /**
     * Whitespace is trimmed and trailing slashes removed, so a pasted
     * " https://host/v1/ " cannot produce a malformed request or a doubled
     * separator like ".../v1//embeddings", which plenty of servers answer with a
     * 404. Internal so the normalisation is testable.
     */
    internal val base: String get() = baseUrl.trim().trimEnd('/')

    override suspend fun embed(
        text: String,
        model: String,
        apiKey: String,
        dimensions: Int,
        taskPrefix: String,
    ): FloatArray = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw LlmException(AppError.NoApiKey)
        if (base.isBlank()) throw LlmException(AppError.NoBaseUrl)

        val payload = JSONObject()
            .put("model", model)
            .put("input", taskPrefix + text)
            // Not every server honours this, but the ones that do return
            // smaller vectors, which is cheaper to search on device.
            .put("dimensions", dimensions)
            .toString()

        val response = call("/embeddings", payload, apiKey)
        val values = response.optJSONArray("data")?.optJSONObject(0)?.optJSONArray("embedding")
            ?: throw LlmException(AppError.Unexpected("no embedding in response"))
        FloatArray(values.length()) { values.getDouble(it).toFloat() }
    }

    override suspend fun generate(prompt: String, model: String, apiKey: String): String =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) throw LlmException(AppError.NoApiKey)
            if (base.isBlank()) throw LlmException(AppError.NoBaseUrl)

            val payload = JSONObject()
                .put("model", model)
                .put(
                    "messages",
                    JSONArray().put(
                        JSONObject()
                            .put("role", "user")
                            .put("content", prompt)
                    ),
                )
                .put("temperature", 0)
                .toString()

            val response = call("/chat/completions", payload, apiKey)
            val choice = response.optJSONArray("choices")?.optJSONObject(0)
                ?: throw LlmException(AppError.Unexpected("no choices in response"))
            choice.optJSONObject("message")?.optString("content")?.takeIf { it.isNotBlank() }
                ?: throw LlmException(AppError.Unexpected("choice contained no text"))
        }

    override suspend fun validateKey(apiKey: String): LlmClient.ValidationResult =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank() || base.isBlank()) return@withContext LlmClient.ValidationResult.Invalid
            val request = Request.Builder()
                .url("$base/models")
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()
            try {
                Http.client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        LlmClient.ValidationResult.Valid
                    } else {
                        LlmClient.ValidationResult.Invalid
                    }
                }
            } catch (e: IOException) {
                LlmClient.ValidationResult.Failed(e.message ?: "network error")
            }
        }

    private fun call(path: String, body: String, apiKey: String): JSONObject {
        val request = Request.Builder()
            .url(base + path)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body.toRequestBody(json))
            .build()

        return try {
            Http.client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw LlmException(AppError.fromHttpStatus(response.code))
                try {
                    JSONObject(text)
                } catch (e: Exception) {
                    throw LlmException(AppError.Unexpected("malformed response"))
                }
            }
        } catch (e: IOException) {
            throw LlmException(AppError.Network)
        }
    }
}
