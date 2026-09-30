package com.askyoutube.app.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Follow the OS setting, or override it in either direction. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Which backend answers. GOOGLE is the fixed host and needs no address;
 * OPENAI_COMPATIBLE covers self-hosted and third-party servers, which each have
 * their own address, model names and key.
 */
enum class Provider { GOOGLE, OPENAI_COMPATIBLE }

/**
 * Where embeddings come from, which is deliberately independent of [Provider].
 *
 * A server that hosts a chat model very often has no embedding endpoint for it,
 * so the two have to be configurable separately. SAME_AS_CHAT is the common
 * case and keeps the form short; CUSTOM is for when the vector model lives
 * somewhere else entirely, with its own address and its own key.
 */
enum class EmbeddingSource { SAME_AS_CHAT, CUSTOM }

/**
 * Local settings, including the Gemini API key.
 *
 * The key is held in EncryptedSharedPreferences so it is not readable from an
 * unencrypted app-data backup or by another app on a rooted device. It is never
 * logged and never leaves the device.
 *
 * The non-secret fields ride along in the same store. Splitting them across two
 * preference files would buy nothing but an extra class.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val app = context.applicationContext
        @Suppress("DEPRECATION")
        val masterKey = MasterKey.Builder(app)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            app,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    fun hasApiKey(): Boolean = apiKey.isNotBlank()

    /** Removes the key outright rather than storing an empty string. */
    fun clearApiKey() = prefs.edit().remove(KEY_API_KEY).apply()

    var embeddingModel: String
        get() = prefs.getString(KEY_EMBEDDING, DEFAULT_EMBEDDING_MODEL) ?: DEFAULT_EMBEDDING_MODEL
        set(value) = prefs.edit().putString(KEY_EMBEDDING, value.trim()).apply()

    var chatModel: String
        get() = prefs.getString(KEY_CHAT, DEFAULT_CHAT_MODEL) ?: DEFAULT_CHAT_MODEL
        set(value) = prefs.edit().putString(KEY_CHAT, value.trim()).apply()

    var topK: Int
        get() = prefs.getInt(KEY_TOP_K, DEFAULT_TOP_K)
        set(value) = prefs.edit().putInt(KEY_TOP_K, value.coerceIn(1, 12)).apply()

    var provider: Provider
        get() = runCatching {
            Provider.valueOf(prefs.getString(KEY_PROVIDER, null) ?: Provider.GOOGLE.name)
        }.getOrDefault(Provider.GOOGLE)
        set(value) = prefs.edit().putString(KEY_PROVIDER, value.name).apply()

    /** Only meaningful for OPENAI_COMPATIBLE, e.g. https://host.example/v1 */
    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_BASE_URL, value.trim()).apply()

    var embeddingSource: EmbeddingSource
        get() = runCatching {
            EmbeddingSource.valueOf(
                prefs.getString(KEY_EMB_SOURCE, null) ?: EmbeddingSource.SAME_AS_CHAT.name
            )
        }.getOrDefault(EmbeddingSource.SAME_AS_CHAT)
        set(value) = prefs.edit().putString(KEY_EMB_SOURCE, value.name).apply()

    /** Only meaningful when embeddingSource is CUSTOM. */
    var embeddingBaseUrl: String
        get() = prefs.getString(KEY_EMB_BASE_URL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_EMB_BASE_URL, value.trim()).apply()

    /**
     * Only meaningful when embeddingSource is CUSTOM. A separate secret, so it
     * is stored in the same encrypted store and never logged.
     */
    var embeddingApiKey: String
        get() = prefs.getString(KEY_EMB_API_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_EMB_API_KEY, value.trim()).apply()

    fun hasEmbeddingKey(): Boolean = embeddingApiKey.isNotBlank()

    fun clearEmbeddingKey() = prefs.edit().remove(KEY_EMB_API_KEY).apply()

    /** System, Light, or Dark. Stored by name so the enum can be reordered. */
    var themeMode: ThemeMode
        get() = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    companion object {
        const val FILE_NAME = "askyoutube_settings"

        const val KEY_API_KEY = "gemini_api_key"
        const val KEY_EMBEDDING = "embedding_model"
        const val KEY_CHAT = "chat_model"
        const val KEY_TOP_K = "top_k"
        const val KEY_THEME = "theme_mode"
        const val KEY_PROVIDER = "provider"
        const val KEY_BASE_URL = "base_url"
        const val KEY_EMB_SOURCE = "embedding_source"
        const val KEY_EMB_BASE_URL = "embedding_base_url"
        const val KEY_EMB_API_KEY = "embedding_api_key"

        // Verified against Google's documentation on 2026-09-30. The source app
        // used models/embedding-001 and gemini-2.0-flash, both now legacy.
        // Editable in Settings because model availability moves.
        const val DEFAULT_EMBEDDING_MODEL = "gemini-embedding-2"
        const val DEFAULT_CHAT_MODEL = "gemini-3.5-flash"
        const val DEFAULT_OPENAI_EMBEDDING_MODEL = "text-embedding-3-small"
        const val DEFAULT_OPENAI_CHAT_MODEL = "gemma-4-31B-it"
        const val DEFAULT_TOP_K = 4
    }
}
