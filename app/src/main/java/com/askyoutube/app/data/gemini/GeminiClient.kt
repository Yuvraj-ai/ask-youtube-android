package com.askyoutube.app.data.gemini

import com.askyoutube.app.data.llm.LlmClient
import com.askyoutube.app.data.llm.LlmException

import com.askyoutube.app.domain.AppError
import com.askyoutube.app.domain.AppException
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
 * Thin client for the two Gemini calls this app makes.
 *
 * Uses the REST endpoints directly rather than an SDK, so there is no client
 * library to keep in step with model churn and no hidden default behaviour.
 *
 * The key is passed in per call and only ever placed in the x-goog-api-key
 * header. It is never logged and never included in an exception message.
 */
class GeminiClient : LlmClient {

    override val label: String = "Google Gemini"

    private val base = "https://generativelanguage.googleapis.com/v1beta"
    private val mediaTypeJson = "application/json".toMediaType()

    /**
     * Embeds one string.
     *
     * @param taskPrefix "query: " for the user's question, "document: " for a
     *   transcript chunk. gemini-embedding-2 carries task intent in the prompt
     *   text rather than in a task_type parameter, which is the vendor's own
     *   migration guidance.
     */
    override suspend fun embed(
        text: String,
        model: String,
        apiKey: String,
        dimensions: Int,
        taskPrefix: String,
    ): FloatArray = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw AppException(AppError.NoApiKey)

        val body = JSONObject()
            .put("model", "models/$model")
            .put(
                "content",
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", taskPrefix + text)),
                ),
            )
            .put("outputDimensionality", dimensions)
            .toString()

        val response = call("/models/$model:embedContent", body, apiKey)
        val values = response.optJSONObject("embedding")?.optJSONArray("values")
            ?: throw AppException(AppError.Unexpected("embedding response had no values"))
        FloatArray(values.length()) { values.getDouble(it).toFloat() }
    }

    /** Embeds many chunks with bounded concurrency, preserving order. */
    suspend fun embedAll(
        texts: List<String>,
        model: String,
        apiKey: String,
        dimensions: Int = 768,
        parallelism: Int = 6,
    ): List<FloatArray> = withContext(Dispatchers.IO) {
        val gate = Semaphore(parallelism)
        texts.map { text ->
            async { gate.withPermit { embed(text, model, apiKey, dimensions, "document: ") } }
        }.awaitAll()
    }

    /** Produces the grounded answer. temperature 0 matches the source app. */
    override suspend fun generate(
        prompt: String,
        model: String,
        apiKey: String,
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw AppException(AppError.NoApiKey)

        val body = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt)),
                    ),
                ),
            )
            .put("generationConfig", JSONObject().put("temperature", 0))
            .toString()

        val response = call("/models/$model:generateContent", body, apiKey)
        val candidates = response.optJSONArray("candidates")
            ?: throw AppException(AppError.Unexpected("no candidates in response"))
        val first = candidates.optJSONObject(0)
            ?: throw AppException(AppError.Unexpected("empty candidate list"))

        val parts = first.optJSONObject("content")?.optJSONArray("parts")
        if (parts != null) {
            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                parts.optJSONObject(i)?.optString("text")?.let { sb.append(it) }
            }
            if (sb.isNotEmpty()) return@withContext sb.toString()
        }
        throw AppException(AppError.Unexpected("candidate contained no text"))
    }

    /**
     * Cheap key check for the Settings screen.
     *
     * Listing models is far cheaper than an embedding round trip and doubles as
     * a check that the key can actually see the models the app wants to use.
     */
    override suspend fun validateKey(apiKey: String): LlmClient.ValidationResult =
        withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext LlmClient.ValidationResult.Invalid
        val request = Request.Builder()
            .url("$base/models")
            .header("x-goog-api-key", apiKey)
            .get()
            .build()
        try {
            Http.client.newCall(request).execute().use { response ->
                if (response.isSuccessful) LlmClient.ValidationResult.Valid else LlmClient.ValidationResult.Invalid
            }
        } catch (e: IOException) {
            LlmClient.ValidationResult.Failed(e.message ?: "network error")
        }
    }

    private fun call(path: String, body: String, apiKey: String): JSONObject {
        val request = Request.Builder()
            .url(base + path)
            .header("x-goog-api-key", apiKey)
            .header("Content-Type", "application/json")
            .post(body.toRequestBody(mediaTypeJson))
            .build()

        return try {
            Http.client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw AppException(AppError.fromHttpStatus(response.code))
                try {
                    JSONObject(text)
                } catch (e: Exception) {
                    throw AppException(AppError.Unexpected("malformed response"))
                }
            }
        } catch (e: IOException) {
            throw AppException(AppError.Network)
        }
    }
}
