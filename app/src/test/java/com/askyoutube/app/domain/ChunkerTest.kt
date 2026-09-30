package com.askyoutube.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChunkerTest {

    /** Builds cues of roughly [wordsPerCue] words each, one second apart. */
    private fun cues(count: Int, wordsPerCue: Int = 10): List<Cue> =
        (0 until count).map { i ->
            Cue(
                startMs = i * 1000L,
                text = (1..wordsPerCue).joinToString(" ") { "w$it" },
            )
        }

    @Test
    fun `empty input produces no chunks`() {
        assertTrue(Chunker.chunk(emptyList()).isEmpty())
    }

    @Test
    fun `input without cues carrying text produces no chunks`() {
        val blank = listOf(Cue(0, "   "), Cue(1000, ""))
        assertTrue(Chunker.chunk(blank).isEmpty())
    }

    @Test
    fun `a short transcript stays as one chunk`() {
        val out = Chunker.chunk(cues(3), chunkSize = 1000, overlap = 100)
        assertEquals(1, out.size)
    }

    @Test
    fun `chunk indexes are sequential from zero`() {
        val out = Chunker.chunk(cues(200), chunkSize = 300, overlap = 50)
        assertEquals(0, out.first().index)
        out.forEachIndexed { i, c -> assertEquals(i, c.index) }
    }

    @Test
    fun `chunks respect the size budget except for the overlap carry`() {
        val chunkSize = 400
        val overlap = 100
        val out = Chunker.chunk(cues(300), chunkSize = chunkSize, overlap = overlap)

        assertTrue("expected several chunks", out.size > 1)
        // Every chunk after the first carries up to `overlap` characters of the
        // previous one, so the budget is chunkSize plus the overlap.
        out.forEach { c ->
            assertTrue(
                "chunk ${c.index} was ${c.text.length} chars, over $chunkSize+$overlap",
                c.text.length <= chunkSize + overlap,
            )
        }
    }

    @Test
    fun `no content is lost between chunks`() {
        val words = ("alpha beta gamma delta epsilon zeta eta theta iota kappa").split(" ")
        val input = (0 until 40).map { i ->
            Cue(i * 1000L, words[i % words.size])
        }
        val out = Chunker.chunk(input, chunkSize = 200, overlap = 50)

        // Every input word must appear somewhere in the output.
        val joined = out.joinToString(" ") { it.text }
        words.forEach { w ->
            assertTrue("word '$w' was dropped", joined.contains(w))
        }
    }

    @Test
    fun `chunks carry a start time from the transcript`() {
        val out = Chunker.chunk(cues(200), chunkSize = 300, overlap = 50)
        out.forEach { c ->
            assertTrue("chunk ${c.index} start ${c.startMs}", c.startMs >= 0)
            assertTrue("chunk ${c.index} end ${c.endMs}", c.endMs >= c.startMs)
        }
    }

    @Test
    fun `timestamps increase with the transcript`() {
        val out = Chunker.chunk(cues(400), chunkSize = 300, overlap = 50)
        out.zipWithNext().forEach { (a, b) ->
            assertTrue("chunk ${b.index} started before chunk ${a.index}", b.startMs >= a.startMs)
        }
    }

    @Test
    fun `overlap of zero is allowed and avoids repetition`() {
        val out = Chunker.chunk(cues(100, wordsPerCue = 20), chunkSize = 250, overlap = 0)
        assertTrue(out.size > 1)
        // With no overlap, consecutive chunks must not share a leading word.
        out.zipWithNext().forEach { (a, b) ->
            val lastWord = a.text.split(" ").last()
            assertTrue(
                "chunk ${b.index} repeats '${lastWord}' from chunk ${a.index}",
                !b.text.startsWith(lastWord),
            )
        }
    }

    @Test
    fun `an overlap larger than the chunk size is clamped rather than looping`() {
        // Distinct text per cue, so a genuinely stalled loop would show up as
        // repeated chunks rather than being masked by identical fixture text.
        val input = (0 until 200).map { i -> Cue(i * 1000L, "cue$i word") }
        val out = Chunker.chunk(input, chunkSize = 100, overlap = 5000)

        assertTrue("expected several chunks", out.size > 1)
        // At most one chunk is emitted per cue, which is what guarantees the
        // loop terminates no matter how the overlap is misconfigured.
        assertTrue("expected at most ${input.size} chunks, got ${out.size}", out.size <= input.size)
        // Start times never go backwards. With an overlap this large the carried
        // tail can begin at the same cue as the previous window, so they are
        // non-decreasing rather than strictly increasing.
        out.zipWithNext().forEach { (a, b) ->
            assertTrue(
                "chunk ${b.index} started at ${b.startMs}, before ${a.startMs}",
                b.startMs >= a.startMs,
            )
        }
    }

    @Test
    fun `a single cue longer than the chunk size is still emitted`() {
        val long = Cue(0, "x".repeat(2500))
        val out = Chunker.chunk(listOf(long), chunkSize = 1000, overlap = 100)
        assertEquals(1, out.size)
        assertEquals(2500, out.first().text.length)
    }
}
