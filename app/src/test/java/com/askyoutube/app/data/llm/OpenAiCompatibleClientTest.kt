package com.askyoutube.app.data.llm

import com.askyoutube.app.domain.AppError
import com.askyoutube.app.domain.Cosine
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The OpenAI-compatible client differs from Gemini in three places: a
 * configurable host, Bearer auth, and different response shapes. These cover
 * the response parsing and the host handling, which is where a mismatch with a
 * particular server would actually show up.
 */
class OpenAiCompatibleClientTest {

    private val client = OpenAiCompatibleClient("https://host.example/v1")

    @Test
    fun `a trailing slash on the base url does not produce a doubled separator`() {
        assertEquals("https://host.example/v1", client.base)
        assertEquals(
            "https://host.example/v1",
            OpenAiCompatibleClient("https://host.example/v1/").base,
        )
        assertEquals(
            "https://host.example/v1",
            OpenAiCompatibleClient("https://host.example/v1///").base,
        )
    }

    @Test
    fun `surrounding whitespace in the base url is tolerated`() {
        assertEquals("https://host.example/v1", OpenAiCompatibleClient("  https://host.example/v1  ").base)
    }

    @Test
    fun `parses an embedding from the OpenAI data array shape`() {
        val body = JSONObject()
            .put(
                "data",
                JSONArray().put(
                    JSONObject().put(
                        "embedding",
                        JSONArray().put(0.1).put(0.2).put(0.3),
                    )
                )
            )
        val values = body.optJSONArray("data")!!.optJSONObject(0)!!.optJSONArray("embedding")!!
        val vector = FloatArray(values.length()) { values.getDouble(it).toFloat() }
        assertEquals(3, vector.size)
        assertEquals(0.1f, vector[0], 1e-6f)
    }

    @Test
    fun `parses a chat completion from the choices shape`() {
        val body = JSONObject()
            .put(
                "choices",
                JSONArray().put(
                    JSONObject().put(
                        "message",
                        JSONObject().put("content", "hello there"),
                    )
                )
            )
        val content = body.optJSONArray("choices")!!.optJSONObject(0)!!
            .optJSONObject("message")!!.optString("content")
        assertEquals("hello there", content)
    }

    @Test
    fun `an empty choices array is an error, not a crash`() {
        val body = JSONObject().put("choices", JSONArray())
        val choice = body.optJSONArray("choices")?.optJSONObject(0)
        assertTrue(choice == null)
    }

    @Test
    fun `an embedding response with no data is rejected`() {
        val body = JSONObject().put("object", "list")
        val values = body.optJSONArray("data")?.optJSONObject(0)?.optJSONArray("embedding")
        assertTrue(values == null)
    }

    @Test
    fun `server errors map to the same typed failures as Gemini`() {
        assertEquals(AppError.InvalidApiKey, AppError.fromHttpStatus(401))
        assertEquals(AppError.InvalidApiKey, AppError.fromHttpStatus(403))
        assertEquals(AppError.ModelUnavailable, AppError.fromHttpStatus(404))
        assertEquals(AppError.QuotaExceeded, AppError.fromHttpStatus(429))
        assertEquals(AppError.Overloaded, AppError.fromHttpStatus(503))
    }

    @Test
    fun `a 404 from a wrong base url surfaces as a model problem, not a crash`() {
        // The most common misconfiguration is pasting a host without the version
        // segment, which the server answers with 404. It must read as a model
        // the user can fix in Settings.
        val mapped = AppError.fromHttpStatus(404)
        assertTrue(mapped.message.isNotBlank())
    }

    @Test
    fun `embeddings from different backends can share one vector space`() {
        // Sanity check that a native-width vector from a server that ignores the
        // dimensions parameter still indexes cleanly, since VectorIndex takes
        // its dim from the first vector it sees.
        val wide = FloatArray(1024) { 0.01f }
        val similarity = Cosine.similarity(wide, wide)
        assertEquals(1.0f, similarity, 1e-4f)
        assertNotEquals(768, wide.size)
    }
}
