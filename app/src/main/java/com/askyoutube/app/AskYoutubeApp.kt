package com.askyoutube.app

import android.app.Application
import com.askyoutube.app.data.llm.LlmClientFactory
import com.askyoutube.app.data.settings.SettingsStore
import com.askyoutube.app.data.transcript.YouTubeTranscriptFetcher
import com.askyoutube.app.domain.RagEngine

/**
 * Hand-rolled dependency container.
 *
 * ponytail: no Hilt. The graph is five objects with no scoping beyond
 * "singleton for the process", so an annotation processor and a Gradle plugin
 * would cost more than the object it generates. Add a DI framework if the graph
 * grows bindings that differ per screen or need test doubles wired per test.
 */
class AppContainer(application: Application) {
    private val settings = SettingsStore(application)
    private val llmClients = LlmClientFactory(settings)
    private val transcripts = YouTubeTranscriptFetcher()
    private val rag = RagEngine(llmClients, transcripts, settings)

    fun settings(): SettingsStore = settings
    fun ragEngine(): RagEngine = rag
    fun llmClients(): LlmClientFactory = llmClients
}

class AskYoutubeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
