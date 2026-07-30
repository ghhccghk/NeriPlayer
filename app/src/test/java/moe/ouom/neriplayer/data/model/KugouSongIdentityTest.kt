package moe.ouom.neriplayer.data.model

import moe.ouom.neriplayer.core.api.search.MusicPlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class KugouSongIdentityTest {

    @Test
    fun `kugou song with channelId gets stable identity`() {
        val song = testSong(channelId = "kugou", audioId = "abc123")
        val identity = song.identity()
        assertNotNull(identity)
        assertEquals("kugou", identity.album)
    }

    @Test
    fun `kugou song with album prefix gets stable identity`() {
        val song = testSong(album = "KugouSomeAlbum", audioId = "abc123")
        val identity = song.identity()
        assertNotNull(identity)
        assertEquals("kugou", identity.album)
    }

    @Test
    fun `netease song with kugou matched lyrics gets netease identity`() {
        val song = testSong(
            channelId = "netease",
            album = "NeteaseAlbum",
            audioId = "12345",
            matchedLyricSource = MusicPlatform.KUGOU
        )
        val identity = song.identity()
        assertEquals("netease", identity.album)
    }

    @Test
    fun `same kugou hash produces same identity`() {
        val song1 = testSong(channelId = "kugou", audioId = "abc123")
        val song2 = testSong(channelId = "kugou", audioId = "abc123")
        assertEquals(song1.identity(), song2.identity())
    }

    @Test
    fun `different kugou hash produces different identity`() {
        val song1 = testSong(channelId = "kugou", audioId = "abc123")
        val song2 = testSong(channelId = "kugou", audioId = "def456")
        assertNotEquals(song1.identity(), song2.identity())
    }

    private fun testSong(
        channelId: String? = null,
        album: String = "",
        audioId: String? = null,
        matchedLyricSource: MusicPlatform? = null
    ) = SongItem(
        id = 999L,
        name = "Test",
        artist = "Artist",
        album = album,
        albumId = 999L,
        durationMs = 1000L,
        coverUrl = null,
        channelId = channelId,
        audioId = audioId,
        matchedLyricSource = matchedLyricSource
    )
}
