package com.askyoutube.app.domain

/**
 * Extracts an 11-character YouTube video id from whatever the user pasted.
 *
 * Handles a bare id, a full watch URL, a short youtu.be link, and the
 * /embed/, /shorts/ and /live/ path forms. The source Streamlit app passed
 * whatever string it was given straight to YoutubeLoader and let it fail later,
 * so this is deliberately strict and returns null rather than throwing.
 */
object VideoId {

    private val PATH = Regex(
        "(?:v=|/v/|youtu\\.be/|/embed/|/shorts/|/live/|/e/)([A-Za-z0-9_-]{11})"
    )
    private val BARE = Regex("^[A-Za-z0-9_-]{11}$")

    fun parse(input: String): String? {
        val s = input.trim()
        if (s.isEmpty()) return null
        if (BARE.matches(s)) return s
        return PATH.find(s)?.groupValues?.get(1)
    }

    fun isValid(input: String): Boolean = parse(input) != null

    fun thumbnailUrl(videoId: String): String =
        "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
}
