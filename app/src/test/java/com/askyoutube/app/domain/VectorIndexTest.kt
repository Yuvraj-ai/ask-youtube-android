package com.askyoutube.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class VectorIndexTest {

    private fun vec(vararg v: Float) = v

    @Test
    fun `cosine of a vector with itself is one`() {
        val v = vec(1f, 2f, 3f)
        assertEquals(1.0f, Cosine.similarity(v, v), 1e-5f)
    }

    @Test
    fun `cosine of orthogonal vectors is zero`() {
        assertEquals(0.0f, Cosine.similarity(vec(1f, 0f), vec(0f, 1f)), 1e-5f)
    }

    @Test
    fun `cosine ignores magnitude`() {
        assertEquals(1.0f, Cosine.similarity(vec(1f, 1f), vec(5f, 5f)), 1e-5f)
    }

    @Test
    fun `cosine of a zero vector is zero rather than NaN`() {
        val zero = FloatArray(3)
        assertEquals(0.0f, Cosine.similarity(zero, vec(1f, 2f, 3f)), 1e-6f)
    }

    @Test
    fun `cosine handles opposing vectors`() {
        assertEquals(-1.0f, Cosine.similarity(vec(1f, 0f), vec(-1f, 0f)), 1e-5f)
    }

    @Test
    fun `normalize produces a unit vector`() {
        val n = Cosine.normalize(floatArrayOf(3f, 4f))
        val magnitude = kotlin.math.sqrt(n[0] * n[0] + n[1] * n[1])
        assertEquals(1.0f, magnitude, 1e-5f)
        assertTrue(abs(n[0] - 0.6f) < 1e-5f)
        assertTrue(abs(n[1] - 0.8f) < 1e-5f)
    }

    @Test
    fun `normalize does not change direction`() {
        val v = floatArrayOf(3f, 4f)
        val n = Cosine.normalize(v)
        assertEquals(1.0f, Cosine.similarity(v, n), 1e-5f)
    }

    @Test
    fun `normalize leaves a zero vector alone`() {
        val z = FloatArray(3)
        val n = Cosine.normalize(z)
        assertTrue(n.all { it == 0f })
    }

    @Test
    fun `search on an empty index returns nothing`() {
        val index = VectorIndex(dim = 3)
        assertTrue(index.search(vec(1f, 0f, 0f), k = 4).isEmpty())
    }

    @Test
    fun `search returns the closest vector first`() {
        val index = VectorIndex(dim = 3)
        index.add(vec(1f, 0f, 0f))   // 0 exact
        index.add(vec(0.9f, 0.1f, 0f)) // 1 close
        index.add(vec(0f, 0f, 1f))   // 2 orthogonal

        val hits = index.search(vec(1f, 0f, 0f), k = 3)
        assertEquals(0, hits[0].chunkIndex)
        assertEquals(1, hits[1].chunkIndex)
        assertEquals(2, hits[2].chunkIndex)
    }

    @Test
    fun `search honours the k limit`() {
        val index = VectorIndex(dim = 2)
        repeat(10) { index.add(vec(it.toFloat(), 0f)) }
        assertEquals(4, index.search(vec(1f, 0f), k = 4).size)
    }

    @Test
    fun `search returns everything when k exceeds the index size`() {
        val index = VectorIndex(dim = 2)
        index.add(vec(1f, 0f))
        index.add(vec(0f, 1f))
        assertEquals(2, index.search(vec(1f, 0f), k = 10).size)
    }

    @Test
    fun `scores come back sorted descending`() {
        val index = VectorIndex(dim = 3)
        index.add(vec(0f, 1f, 0f))
        index.add(vec(1f, 0f, 0f))
        index.add(vec(0.5f, 0.5f, 0f))

        val hits = index.search(vec(1f, 0f, 0f), k = 3)
        hits.zipWithNext().forEach { (a, b) ->
            assertTrue("scores not descending: ${a.score} then ${b.score}", a.score >= b.score)
        }
    }

    @Test
    fun `a non-positive k returns nothing instead of failing`() {
        val index = VectorIndex(dim = 2)
        index.add(vec(1f, 0f))
        assertTrue(index.search(vec(1f, 0f), k = 0).isEmpty())
        assertTrue(index.search(vec(1f, 0f), k = -1).isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `adding a vector of the wrong size is rejected`() {
        VectorIndex(dim = 3).add(vec(1f, 0f))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `query of the wrong size is rejected`() {
        val index = VectorIndex(dim = 3)
        index.add(vec(1f, 0f, 0f))
        index.search(vec(1f, 0f), k = 1)
    }

    @Test
    fun `size reflects what was added and clear resets it`() {
        val index = VectorIndex(dim = 2)
        assertEquals(0, index.size)
        index.add(vec(1f, 0f))
        index.add(vec(0f, 1f))
        assertEquals(2, index.size)
        index.clear()
        assertEquals(0, index.size)
    }
}
