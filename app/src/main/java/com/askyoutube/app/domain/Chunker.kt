package com.askyoutube.app.domain

/** One caption segment: the text and when it starts, in milliseconds. */
data class Cue(val startMs: Long, val text: String)

/** A window of transcript text plus the span of video time it covers. */
data class Chunk(
    val index: Int,
    val text: String,
    val startMs: Long,
    val endMs: Long,
)

/**
 * Splits a transcript into overlapping windows.
 *
 * chunkSize and overlap default to the same 1000/100 the source app used
 * (uany.py:21) so retrieval quality carries over. The one improvement is that
 * cue timings are preserved, which is what lets the UI show "12:04" beside a
 * source passage instead of an unattributed quote.
 */
object Chunker {

    const val DEFAULT_CHUNK_SIZE = 1000
    const val DEFAULT_OVERLAP = 100

    fun chunk(
        cues: List<Cue>,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        overlap: Int = DEFAULT_OVERLAP,
    ): List<Chunk> {
        if (cues.isEmpty()) return emptyList()
        require(chunkSize > 0) { "chunkSize must be positive" }
        // Overlap must be smaller than the window, or the carry-over would eat
        // the whole budget and the loop would not advance.
        val step = overlap.coerceIn(0, chunkSize - 1)

        val out = ArrayList<Chunk>()
        val sb = StringBuilder()
        // Offset of each contributing cue within sb, mapped to its start time.
        val marks = ArrayList<Pair<Int, Long>>()

        fun append(text: String, startMs: Long) {
            if (sb.isNotEmpty()) sb.append(' ')
            marks += (sb.length to startMs)
            sb.append(text)
        }

        for (cue in cues) {
            val text = cue.text.trim()
            if (text.isEmpty()) continue

            if (sb.isNotEmpty() && sb.length + 1 + text.length > chunkSize) {
                val full = sb.toString()
                // The carried tail begins at this offset in the text we just closed.
                val tailStart = (full.length - step).coerceAtLeast(0)
                val chunkStart = marks.firstOrNull { it.first >= tailStart }?.second
                    ?: marks.firstOrNull()?.second
                    ?: 0L

                out += Chunk(out.size, full.trim(), chunkStart, cue.startMs)

                val tail = full.takeLast(step)
                sb.setLength(0)
                marks.clear()
                if (tail.isNotBlank()) {
                    sb.append(tail)
                    marks += (0 to chunkStart)
                }
            }
            append(text, cue.startMs)
        }

        val remainder = sb.toString().trim()
        if (remainder.isNotEmpty()) {
            out += Chunk(
                index = out.size,
                text = remainder,
                startMs = marks.firstOrNull()?.second ?: 0L,
                endMs = cues.last().startMs,
            )
        }
        return out
    }
}
