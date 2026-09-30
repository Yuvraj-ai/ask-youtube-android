package com.askyoutube.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionEngineTest {

    private fun chunk(text: String) = Chunk(0, text, 0, 0)

    @Test
    fun `no chunks yields no suggestions`() {
        assertTrue(SuggestionEngine.suggest(emptyList()).isEmpty())
    }

    @Test
    fun `never returns more than four`() {
        // Far more distinct terms than the cap.
        val text = (1..60).joinToString(" ") { "topic$it discussion" }
        val out = SuggestionEngine.suggest(List(12) { chunk(text) })
        assertTrue("expected at most 4, got ${out.size}", out.size <= 4)
    }

    @Test
    fun `a requested max above the ceiling is clamped to four`() {
        val text = (1..60).joinToString(" ") { "topic$it discussion" }
        assertTrue(SuggestionEngine.suggest(List(12) { chunk(text) }, max = 50).size <= 4)
    }

    @Test
    fun `a non positive max yields nothing`() {
        val out = SuggestionEngine.suggest(listOf(chunk("pricing tiers")), max = 0)
        assertTrue(out.isEmpty())
    }

    @Test
    fun `finds a distinctive topic in the transcript`() {
        val out = SuggestionEngine.suggest(
            listOf(
                chunk("The pricing change moves the free tier from three projects to two."),
                chunk("The pricing change applies at the next billing cycle for everyone."),
                chunk("That pricing change is the reason storage work is funded."),
            )
        )
        assertTrue("expected a suggestion, got $out", out.isNotEmpty())
        assertTrue(
            "expected a suggestion mentioning pricing or tier, got $out",
            out.any { it.contains("pricing") || it.contains("tier") || it.contains("billing") },
        )
    }

    @Test
    fun `terms already asked about are not suggested again`() {
        val chunks = listOf(
            chunk("Storage costs went up because the pricing change funds storage work."),
            chunk("The pricing change also reduces the free tier to two projects."),
        )
        val out = SuggestionEngine.suggest(chunks, askedSoFar = listOf("Tell me about pricing"))
        assertTrue("pricing should be excluded, got $out", out.none { it.contains("pricing") })
    }

    @Test
    fun `words appearing in every chunk are treated as vocabulary, not topics`() {
        // "video" is everywhere, so it should not be offered.
        val chunks = (1..8).map {
            chunk("The video covers billing, the video mentions storage, the video is long.")
        }
        val out = SuggestionEngine.suggest(chunks)
        assertTrue("'video' is generic and should be skipped, got $out", out.none { it.contains("video") })
    }

    @Test
    fun `stopwords are never suggested`() {
        val out = SuggestionEngine.suggest(
            listOf(chunk("that this with they have been would could should from into over"))
        )
        assertTrue("stopwords leaked into $out", out.none { s ->
            listOf("that", "this", "with", "they", "have", "would").any { s.startsWith("What did they say about $it") }
        })
    }

    @Test
    fun `suggestions do not overlap on words`() {
        val text = (1..40).joinToString(" ") { "alpha$it bravo$it charlie$it" }
        val out = SuggestionEngine.suggest(List(6) { chunk(text) })
        val words = out.flatMap { it.removePrefix("What did they say about ").removeSuffix("?").split(" ") }
        assertEquals("expected distinct terms, got $out", words.size, words.toSet().size)
    }

    @Test
    fun `every suggestion is a non empty question`() {
        val out = SuggestionEngine.suggest(
            listOf(chunk("Storage costs increased after the pricing change in January."))
        )
        out.forEach {
            assertTrue("blank suggestion", it.isNotBlank())
            assertTrue("not a question: $it", it.endsWith("?"))
        }
    }

    @Test
    fun `very long terms are skipped`() {
        val out = SuggestionEngine.suggest(
            listOf(chunk("supercalifragilisticexpialidocious and antidisestablishmentarianism"))
        )
        assertTrue("absurdly long terms leaked into $out", out.none { it.length > 60 })
    }

    @Test
    fun `a transcript with no distinct vocabulary yields nothing rather than noise`() {
        val out = SuggestionEngine.suggest(List(5) { chunk("the and for that this with you your are was") })
        assertTrue("expected no suggestions from pure stopwords, got $out", out.isEmpty())
    }
}
