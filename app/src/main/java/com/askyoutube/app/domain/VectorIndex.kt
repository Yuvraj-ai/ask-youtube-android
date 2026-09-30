package com.askyoutube.app.domain

import kotlin.math.sqrt

/**
 * A ranked hit from the index. [score] is cosine similarity in -1..1.
 */
data class Hit(val chunkIndex: Int, val score: Float)

/**
 * In-memory vector search.
 *
 * ponytail: a full linear scan, deliberately. A one-hour transcript is on the
 * order of 10^2 chunks at 768 floats — a few hundred thousand multiply-adds,
 * which is sub-millisecond on any phone. A vector database, an HNSW index, or
 * a disk-backed store would all be more code and more failure modes for no
 * measurable gain at this size. Upgrade to an approximate index only if the
 * corpus ever grows past what fits comfortably in memory, or if scan latency
 * ever shows up in a profile.
 */
class VectorIndex(val dim: Int) {

    private val vectors = ArrayList<FloatArray>(128)

    val size: Int get() = vectors.size

    fun add(vector: FloatArray) {
        require(vector.size == dim) { "expected $dim dimensions, got ${vector.size}" }
        vectors += vector
    }

    /** Top [k] chunks by cosine similarity, best first. */
    fun search(query: FloatArray, k: Int): List<Hit> {
        if (vectors.isEmpty() || k <= 0) return emptyList()
        require(query.size == dim) { "query has ${query.size} dimensions, index expects $dim" }

        val scored = ArrayList<Hit>(vectors.size)
        for (i in vectors.indices) {
            scored += Hit(i, Cosine.similarity(query, vectors[i]))
        }
        scored.sortByDescending { it.score }
        return if (scored.size <= k) scored else scored.subList(0, k).toList()
    }

    fun clear() = vectors.clear()
}

/** Cosine helpers, kept separate so they are trivially testable. */
object Cosine {

    fun similarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "dimension mismatch: ${a.size} vs ${b.size}" }
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            val x = a[i].toDouble()
            val y = b[i].toDouble()
            dot += x * y
            normA += x * x
            normB += y * y
        }
        if (normA == 0.0 || normB == 0.0) return 0f
        return (dot / (sqrt(normA) * sqrt(normB))).toFloat()
    }

    /**
     * Scales a vector to unit length. Gemini already returns unit-normalised
     * vectors when the output dimensionality is below 3072, so this is a
     * no-op in the normal path and exists for robustness if that ever changes.
     */
    fun normalize(v: FloatArray): FloatArray {
        var sum = 0.0
        for (x in v) sum += x * x
        val norm = sqrt(sum)
        if (norm == 0.0) return v
        return FloatArray(v.size) { (v[it] / norm).toFloat() }
    }
}
