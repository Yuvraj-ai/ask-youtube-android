package com.askyoutube.app.domain

import com.askyoutube.app.data.llm.LlmClientFactory
import com.askyoutube.app.data.llm.LlmException
import com.askyoutube.app.data.settings.SettingsStore
import com.askyoutube.app.data.transcript.YouTubeTranscriptFetcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** An answer plus the transcript passages it was drawn from. */
data class Answer(
    val text: String,
    val sources: List<Source>,
    /** Transcript chunks the answer was drawn from, for follow-up prompts. */
    val passages: List<Chunk> = emptyList(),
) {
    data class Source(val text: String, val startMs: Long, val score: Float)
}

/** Progress of a question, so the UI can say what is happening. */
enum class RagStage { INDEXING, ANSWERING }

/**
 * The pipeline: transcript -> chunks -> embeddings -> top-k -> grounded answer.
 *
 * The index is cached per video id. This is the fix for defect D2: the source
 * app re-downloaded, re-chunked and re-embedded the entire transcript on every
 * Streamlit rerun, which for a one-hour video meant paying for the whole
 * embedding job on every keystroke.
 */
class RagEngine(
    private val clients: LlmClientFactory,
    private val transcripts: YouTubeTranscriptFetcher,
    private val settings: SettingsStore,
) {

    private data class Indexed(
        val videoId: String,
        val chunks: List<Chunk>,
        val index: VectorIndex,
    )

    // ponytail: one video cached, dropped when another is opened. Enough for
    // the ask-questions-about-one-video flow. Add an LRU if users start
    // flipping between several videos and complaining about re-indexing.
    private var cached: Indexed? = null

    suspend fun answer(
        url: String,
        question: String,
        onStage: (RagStage) -> Unit = {},
    ): Answer {
        val apiKey = settings.apiKey
        if (apiKey.isBlank()) throw AppException(AppError.NoApiKey)
        // Resolved per question so a Settings change takes effect immediately,
        // without restarting the app. Chat and embeddings are separate clients
        // because they can be different backends with different keys.
        val llm = clients.chatClient()
        val embedder = clients.embedClient()
        val embedKey = clients.embeddingApiKey()
        if (embedKey.isBlank()) throw AppException(AppError.NoEmbeddingKey)

        val videoId = VideoId.parse(url) ?: throw AppException(AppError.InvalidUrl)

        val indexed = cached?.takeIf { it.videoId == videoId }
            ?: buildIndex(videoId, url, apiKey, onStage)

        val queryVector = embedder.embed(
            text = question,
            model = settings.embeddingModel,
            apiKey = embedKey,
            dimensions = EMBED_DIMENSIONS,
            taskPrefix = "query: ",
        )

        val hits = indexed.index.search(queryVector, settings.topK)
        if (hits.isEmpty()) throw AppException(AppError.EmptyTranscript)

        val passages = hits.mapNotNull { hit ->
            indexed.chunks.getOrNull(hit.chunkIndex)?.let { it to hit.score }
        }

        val transcriptText = passages.joinToString("\n\n") { (chunk, _) ->
            "[${formatTimestamp(chunk.startMs)}] ${chunk.text}"
        }

        // Prompt intent preserved from uany.py:44-57, including the grounding
        // constraint and the "I don't know" escape hatch. The retrieved passages
        // carry timestamps so the model can refer to a moment in the video.
        val prompt = buildString {
            append(
                "You are a helpful assistant that can answer questions about a YouTube " +
                    "video based on the video's transcript.\n\n"
            )
            append("Answer the following question: ").append(question).append("\n\n")
            append("By searching the following video transcript: ")
            append(transcriptText).append("\n\n")
            append(
                "Only use the factual information from the transcript to answer the question.\n\n"
            )
            append(
                "If you feel like you don't have enough information to answer the question, " +
                    "say \"I don't know\".\n\n"
            )
            append("Your answers should be verbose and detailed.")
        }

        onStage(RagStage.ANSWERING)
        val text = llm.generate(prompt, settings.chatModel, apiKey)

        return Answer(
            text = text,
            sources = passages.map { (chunk, score) ->
                Answer.Source(chunk.text, chunk.startMs, score)
            },
            passages = passages.map { it.first },
        )
    }

    private suspend fun buildIndex(
        videoId: String,
        url: String,
        apiKey: String,
        onStage: (RagStage) -> Unit,
    ): Indexed {
        onStage(RagStage.INDEXING)

        val transcript = transcripts.fetch(url)
        val chunks = Chunker.chunk(transcript.cues)
        if (chunks.isEmpty()) throw AppException(AppError.EmptyTranscript)

        val embedder = clients.embedClient()
        val embedKey = clients.embeddingApiKey()
        // Bounded concurrency: a one-hour transcript is on the order of 10^2
        // chunks, and 1024 sequential round trips is a visibly slow first
        // question. Six at a time is enough to hide latency without tripping
        // per-project rate limits.
        val gate = Semaphore(EMBED_PARALLELISM)
        val vectors = coroutineScope {
            chunks.map { chunk ->
                async {
                    gate.withPermit {
                        embedder.embed(
                            text = chunk.text,
                            model = settings.embeddingModel,
                            apiKey = embedKey,
                            dimensions = EMBED_DIMENSIONS,
                            taskPrefix = "document: ",
                        )
                    }
                }
            }.awaitAll()
        }
        if (vectors.isEmpty()) throw AppException(AppError.EmptyTranscript)

        val index = VectorIndex(dim = vectors.first().size)
        vectors.forEach { index.add(it) }

        return Indexed(videoId, chunks, index).also { cached = it }
    }

    fun clearCache() {
        cached = null
    }

    companion object {
        /**
         * 768 is the size every current embedding model recommends and it keeps
         * the on-device cosine scan cheap. It is a request parameter, so a
         * backend that ignores it simply returns its own native width and the
         * index adapts, because VectorIndex takes its dim from the first vector.
         */
        const val EMBED_DIMENSIONS = 768
        const val EMBED_PARALLELISM = 6

        fun formatTimestamp(ms: Long): String {
            val totalSeconds = ms / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%d:%02d".format(minutes, seconds)
            }
        }
    }
}
