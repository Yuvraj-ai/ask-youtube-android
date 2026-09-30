package com.askyoutube.app.data.transcript

import com.askyoutube.app.domain.AppError
import com.askyoutube.app.domain.AppException
import com.askyoutube.app.domain.Cue
import com.askyoutube.app.domain.VideoId
import com.askyoutube.app.network.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException

/**
 * Fetches a video's transcript.
 *
 * Two requests: read the watch page, find the caption track in the player
 * response embedded in it, then fetch that track as json3. There is no official
 * YouTube captions API, so this scrapes an undocumented surface and will need
 * maintenance when YouTube changes its page.
 */
class YouTubeTranscriptFetcher {

    /**
     * @param onStage optional progress callback, e.g. "Reading transcript…".
     */
    suspend fun fetch(url: String, onStage: ((String) -> Unit)? = null): Transcript =
        withContext(Dispatchers.IO) {
            val videoId = VideoId.parse(url) ?: throw AppException(AppError.InvalidUrl)
            onStage?.invoke("Reading transcript…")

            val html = get(
                "https://www.youtube.com/watch?v=$videoId&hl=en",
                referer = null,
            )
            val tracks = TranscriptParser.extractCaptionTracks(html)
            if (tracks.isEmpty()) throw AppException(AppError.NoCaptions)

            val track = TranscriptParser.selectTrack(tracks)
                ?: throw AppException(AppError.NoCaptions)

            val payload = get("${track.baseUrl}&fmt=json3", referer = WATCH_REFERER)
            // YouTube answers 200 with a zero-length body rather than an error
            // status when it declines to serve a track, notably to datacenter
            // IPs. Treat that as its own failure rather than an empty transcript.
            if (payload.isBlank()) throw AppException(AppError.EmptyTranscript)

            val cues = TranscriptParser.parseCues(payload)
            if (cues.isEmpty()) throw AppException(AppError.EmptyTranscript)

            Transcript(videoId = videoId, cues = cues)
        }

    private fun get(url: String, referer: String?): String {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", Http.DESKTOP_UA)
            .header("Accept-Language", "en-US,en;q=0.9")
        if (referer != null) builder.header("Referer", referer)

        return try {
            Http.client.newCall(builder.get().build()).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code in 400..499) {
                        throw AppException(AppError.InvalidUrl)
                    }
                    throw AppException(AppError.Network)
                }
                response.body?.string().orEmpty()
            }
        } catch (e: IOException) {
            throw AppException(AppError.Network)
        }
    }

    data class Transcript(val videoId: String, val cues: List<Cue>)

    private companion object {
        const val WATCH_REFERER = "https://www.youtube.com/"
    }
}
