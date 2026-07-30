package moe.ouom.neriplayer.data.platform.kugou

import moe.ouom.neriplayer.core.api.search.MusicPlatform
import moe.ouom.neriplayer.data.model.SongItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KugouSongSupportTest {

    @Test
    fun `isKugouSong by channelId`() {
        val song = testSong(channelId = "kugou")
        assertTrue(isKugouSong(song))
    }

    @Test
    fun `isKugouSong by channelId case insensitive`() {
        val song = testSong(channelId = "Kugou")
        assertTrue(isKugouSong(song))
    }

    @Test
    fun `isKugouSong by album prefix`() {
        val song = testSong(album = "KugouSomeAlbum")
        assertTrue(isKugouSong(song))
    }

    @Test
    fun `isKugouSong by album prefix case insensitive`() {
        val song = testSong(album = "kugouSomeAlbum")
        assertTrue(isKugouSong(song))
    }

    @Test
    fun `isKugouSong false for netease with kugou matched lyric source`() {
        val song = testSong(
            channelId = "netease",
            album = "NeteaseAlbum",
            matchedLyricSource = MusicPlatform.KUGOU
        )
        assertFalse(isKugouSong(song))
    }

    @Test
    fun `isKugouSong false for unknown song`() {
        val song = testSong(channelId = null, album = "SomeAlbum")
        assertFalse(isKugouSong(song))
    }

    @Test
    fun `requireKugouHash returns audioId`() {
        val song = testSong(audioId = "abc123def456")
        assertEquals("abc123def456", requireKugouHash(song))
    }

    @Test
    fun `requireKugouHash returns null when audioId blank`() {
        val song = testSong(audioId = null)
        assertNull(requireKugouHash(song))
    }

    @Test
    fun `stableKugouSongId is deterministic`() {
        val id1 = stableKugouSongId("abc123")
        val id2 = stableKugouSongId("abc123")
        assertEquals(id1, id2)
    }

    @Test
    fun `stableKugouSongId differs for different hashes`() {
        val id1 = stableKugouSongId("abc123")
        val id2 = stableKugouSongId("def456")
        assertNotEquals(id1, id2)
    }

    @Test
    fun `stableKugouSongId never returns zero`() {
        val id = stableKugouSongId("test")
        assertTrue(id != 0L)
    }

    private fun testSong(
        channelId: String? = null,
        album: String = "",
        audioId: String? = null,
        matchedLyricSource: MusicPlatform? = null
    ) = SongItem(
        id = 1L,
        name = "Test",
        artist = "Artist",
        album = album,
        albumId = 1L,
        durationMs = 1000L,
        coverUrl = null,
        channelId = channelId,
        audioId = audioId,
        matchedLyricSource = matchedLyricSource
    )
}
