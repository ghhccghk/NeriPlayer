package moe.ouom.neriplayer.data.platform.kugou

import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.data.platform.youtube.stableYouTubeMusicId

const val KUGOU_CHANNEL_ID: String = "kugou"
const val KUGOU_ALBUM_PREFIX: String = "Kugou"

/** 统一酷狗检测：仅 channelId 或旧版 album 前缀，禁止读取 matchedLyricSource */
fun isKugouSong(song: SongItem): Boolean {
    if (song.channelId.equals(KUGOU_CHANNEL_ID, ignoreCase = true)) return true
    return song.album.startsWith(KUGOU_ALBUM_PREFIX, ignoreCase = true)
}

/** 从 audioId 提取 FileHash */
fun requireKugouHash(song: SongItem): String? {
    return song.audioId?.trim()?.takeIf { it.isNotBlank() }
}

/** 稳定 64 位 ID，基于 FileHash 的 SHA-256 */
fun stableKugouSongId(hash: String): Long {
    return stableYouTubeMusicId("kugou|$hash")
}
