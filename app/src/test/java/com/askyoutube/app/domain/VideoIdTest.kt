package com.askyoutube.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoIdTest {

    @Test
    fun `parses a bare video id`() {
        assertEquals("dQw4w9WgXcQ", VideoId.parse("dQw4w9WgXcQ"))
    }

    @Test
    fun `parses a standard watch url`() {
        assertEquals(
            "dQw4w9WgXcQ",
            VideoId.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
        )
    }

    @Test
    fun `parses a watch url with extra query parameters`() {
        assertEquals(
            "dQw4w9WgXcQ",
            VideoId.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42s&list=PL123"),
        )
    }

    @Test
    fun `parses a short youtu dot be url`() {
        assertEquals("dQw4w9WgXcQ", VideoId.parse("https://youtu.be/dQw4w9WgXcQ"))
    }

    @Test
    fun `parses an embed url`() {
        assertEquals(
            "dQw4w9WgXcQ",
            VideoId.parse("https://www.youtube.com/embed/dQw4w9WgXcQ"),
        )
    }

    @Test
    fun `parses a shorts url`() {
        assertEquals(
            "dQw4w9WgXcQ",
            VideoId.parse("https://www.youtube.com/shorts/dQw4w9WgXcQ"),
        )
    }

    @Test
    fun `tolerates surrounding whitespace`() {
        assertEquals("dQw4w9WgXcQ", VideoId.parse("  dQw4w9WgXcQ\n"))
    }

    @Test
    fun `rejects text that is not a youtube link`() {
        assertNull(VideoId.parse("https://vimeo.com/12345"))
        assertNull(VideoId.parse("just some words"))
        assertNull(VideoId.parse(""))
    }

    @Test
    fun `rejects an id of the wrong length`() {
        assertNull(VideoId.parse("tooShort"))
        assertNull(VideoId.parse("waaaaaytoolongvideoid"))
    }

    @Test
    fun `isValid agrees with parse`() {
        assertTrue(VideoId.isValid("https://youtu.be/dQw4w9WgXcQ"))
        assertFalse(VideoId.isValid("nope"))
    }
}
