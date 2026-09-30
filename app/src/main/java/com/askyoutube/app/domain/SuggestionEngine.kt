package com.askyoutube.app.domain

/**
 * Builds follow-up questions from the transcript, on the device, for free.
 *
 * An earlier version asked the model to suggest these. That was the wrong
 * trade: it spends quota on a mechanical task, adds a second round trip to every
 * answer, and can fail after the user already has their answer. Picking
 * distinctive terms out of the transcript gives useful, grounded questions with
 * no network call and no cost.
 *
 * The heuristic is simple on purpose. A term is worth asking about when it is
 * mentioned often enough to matter but not so often that it is just vocabulary
 * filling the whole video, and when the viewer has not already asked about it.
 */
object SuggestionEngine {

    /** Hard ceiling. The user asked for no more than four at a time. */
    const val MAX_SUGGESTIONS = 4

    /**
     * Words that are grammatically load-bearing but make poor topics. A viewer
     * cannot usefully ask "what did they say about change", so these are
     * filtered on top of the ordinary stopwords.
     */
    private val LOW_VALUE = setOf(
        "change", "changes", "changed", "time", "times", "year", "years", "month",
        "months", "today", "next", "current", "number", "part", "point", "end",
        "start", "level", "kind", "sort", "way", "ways", "thing", "need", "needs",
        "want", "feel", "feels", "look", "looks", "come", "comes", "give", "gives",
        "work", "works", "working", "live", "keep", "keeps", "apply", "applies",
        "apply", "reason", "reasons", "everyone", "everything", "something",
        "anything", "nothing", "someone", "people", "person", "case", "cases",
        "fact", "facts", "order", "place", "places", "area", "areas", "state",
    )

    /**
     * How much a two-word phrase is preferred over a single word of the same
     * frequency. Phrases are almost always the better question — "free tier"
     * rather than "free" — so they win decisively and a lone word has to be
     * genuinely frequent to earn a slot.
     */
    private const val PHRASE_BONUS = 3

    /** A single word must be repeated at least this often to be a topic. */
    private const val MIN_UNIGRAM_COUNT = 3

    private val STOPWORDS = setOf(
        "the", "and", "for", "that", "this", "with", "you", "your", "are", "was",
        "were", "have", "has", "had", "not", "but", "they", "them", "their", "there",
        "here", "what", "when", "where", "which", "who", "whom", "why", "how", "all",
        "any", "can", "could", "would", "should", "will", "shall", "may", "might",
        "must", "get", "got", "getting", "go", "going", "went", "one", "two", "three",
        "just", "like", "know", "think", "want", "need", "make", "made", "take", "see",
        "say", "said", "thing", "things", "really", "very", "much", "many", "more",
        "some", "than", "then", "now", "new", "old", "good", "bad", "way", "even",
        "also", "back", "well", "yeah", "okay", "right", "sure", "about", "because",
        "into", "over", "out", "our", "its", "it's", "don't", "doesn't", "did",
        "does", "doing", "done", "let", "lets", "put", "run", "ran", "use", "used",
        "using", "lot", "bit", "actually", "basically", "probably", "maybe", "thank",
        "thanks", "please", "hello", "hi", "okay", "um", "uh", "hmm", "gonna", "wanna",
        // Prepositions and determiners, which otherwise pair up into meaningless
        // "phrases" such as "across every".
        "across", "every", "each", "both", "other", "others", "such", "own", "same",
        "during", "before", "after", "above", "below", "between", "through", "under",
        "until", "while", "upon", "per", "via", "off", "against", "among", "since",
        // Verbs and participles that describe speech rather than name a topic,
        // so they stop the real noun phrase from forming.
        "increased", "increase", "increases", "decreased", "decrease", "remains",
        "remain", "remained", "becomes", "become", "became", "covers", "covered",
        "mention", "mentions", "mentioned", "operate", "operating", "honour",
        "honours", "affect", "affects", "allow", "allows", "requires", "require",
    )

    /**
     * @param askedSoFar earlier questions, so suggestions do not repeat them.
     * @return at most [MAX_SUGGESTIONS] questions, best first.
     */
    fun suggest(
        chunks: List<Chunk>,
        askedSoFar: List<String> = emptyList(),
        max: Int = MAX_SUGGESTIONS,
    ): List<String> {
        if (chunks.isEmpty() || max <= 0) return emptyList()
        val limit = max.coerceAtMost(MAX_SUGGESTIONS)

        // Terms the viewer has already shown they know about.
        val askedTerms = askedSoFar.flatMap { termsIn(it) }.toSet()

        // A term in nearly every chunk is vocabulary, not a topic.
        val genericAbove = (chunks.size * 0.6).coerceAtLeast(1.0)

        val unigrams = HashMap<String, Int>()
        val bigrams = HashMap<String, Int>()
        val docFreq = HashMap<String, Int>()

        for (chunk in chunks) {
            val words = wordsIn(chunk.text)
            val seenHere = HashSet<String>()
            for (word in words) {
                if (word !in STOPWORDS) {
                    unigrams.merge(word, 1, Int::plus)
                    seenHere += word
                }
            }
            // Adjacent content words read far better as a question than a lone
            // word, so two-word phrases are preferred when scores are close.
            for (i in 0 until words.size - 1) {
                val a = words[i]
                val b = words[i + 1]
                if (a in STOPWORDS || b in STOPWORDS) continue
                val phrase = "$a $b"
                bigrams.merge(phrase, 1, Int::plus)
                seenHere += phrase
            }
            for (term in seenHere) docFreq.merge(term, 1, Int::plus)
        }

        data class Candidate(val term: String, val score: Int, val words: Int)

        val candidates = ArrayList<Candidate>()
        for ((term, count) in unigrams) {
            if (term.length !in 4..20) continue
            if (term in LOW_VALUE) continue
            if (count < MIN_UNIGRAM_COUNT) continue
            if (askedTerms.contains(term)) continue
            if ((docFreq[term] ?: 0) > genericAbove) continue
            candidates += Candidate(term, count, 1)
        }
        for ((term, count) in bigrams) {
            val (a, b) = term.split(' ', limit = 2)
            if (a.length < 3 || b.length < 3) continue
            if (a in LOW_VALUE || b in LOW_VALUE) continue
            if (askedTerms.contains(a) || askedTerms.contains(b) || askedTerms.contains(term)) continue
            if ((docFreq[term] ?: 0) > genericAbove) continue
            candidates += Candidate(term, count * PHRASE_BONUS, 2)
        }

        candidates.sortWith(
            compareByDescending<Candidate> { it.score }
                .thenBy { it.term }
        )

        val chosen = ArrayList<String>(limit)
        val usedWords = HashSet<String>()
        for (c in candidates) {
            if (chosen.size >= limit) break
            val parts = c.term.split(' ')
            // Skip anything that overlaps a term already suggested, otherwise
            // "free tier" and "free" both show up.
            if (parts.any { it in usedWords }) continue
            parts.forEach { usedWords += it }
            chosen += questionFor(c.term)
        }
        return chosen
    }

    private fun questionFor(term: String): String =
        "What did they say about $term?"

    private fun wordsIn(text: String): List<String> =
        text.lowercase()
            .split(NON_WORD)
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.all { it.isLetter() || it == '\'' } }
            .map { it.trim('\'') }
            .filter { it.isNotEmpty() }

    private fun termsIn(text: String): List<String> = wordsIn(text)

    private val NON_WORD = Regex("[^a-zA-Z']+")
}
