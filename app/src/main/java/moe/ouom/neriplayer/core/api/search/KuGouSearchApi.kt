package moe.ouom.neriplayer.core.api.search

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: moe.ouom.neriplayer.core.api.search/KuGouSearchApi
 * Created: 2025/07/05
 */

import android.annotation.SuppressLint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import moe.ouom.neriplayer.core.api.kugou.KugouClientWrapper
import moe.ouom.neriplayer.core.api.lyrics.KugouLyricsClient
import moe.ouom.neriplayer.core.api.lyrics.kugouYrc
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.core.player.PlayerManager
import java.io.IOException

class KuGouSearchApi(
    private val client: KugouClientWrapper,
    private val lyricsClient: KugouLyricsClient? = null
) : SearchApi {

    override suspend fun search(keyword: String, page: Int): List<SongSearchInfo> {
        return searchPage(keyword = keyword, page = page).items
    }

    /**
     * 分页搜索酷狗歌曲，附带总数与页码信息。
     */
    suspend fun searchPage(keyword: String, page: Int): KugouSearchPageResult {
        return withContext(Dispatchers.IO) {
            val response = client.searchSongs(
                keywords = keyword,
                page = page,
            )

            if (response.status != 200) {
                return@withContext KugouSearchPageResult(items = emptyList(), page = page, total = 0)
            }

            val data = response.body["data"]?.jsonObject
                ?: return@withContext KugouSearchPageResult(items = emptyList(), page = page, total = 0)
            val info = data["lists"]?.jsonArray
                ?: return@withContext KugouSearchPageResult(items = emptyList(), page = page, total = 0)
            val total = data["total"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L

            val items = info.mapNotNull { item ->
                val song = item.jsonObject
                val hash = song["FileHash"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val songName = song["OriSongName"]?.jsonPrimitive?.content ?: "Unknown"
                val singer = song["SingerName"]?.jsonPrimitive?.content ?: "Unknown"
                val albumName = song["AlbumName"]?.jsonPrimitive?.content
                val coverUrl = song["Image"]?.jsonPrimitive?.content?.replace("/{size}/", "/")
                        ?: song["trans_param"]?.jsonObject?.get("union_cover")?.jsonPrimitive?.content?.replace("/{size}/", "/")
                val duration = song["Duration"]?.jsonPrimitive?.content?.toLongOrNull()
                    ?: song["duration"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L

                SongSearchInfo(
                    id = hash,
                    songName = songName,
                    singer = singer,
                    duration = formatDuration(duration),
                    source = MusicPlatform.KUGOU,
                    albumName = albumName,
                    coverUrl = coverUrl
                )
            }

            KugouSearchPageResult(
                items = items,
                page = page,
                total = total
            )
        }
    }

    override suspend fun getSongInfo(id: String): SongDetails {
        return withContext(Dispatchers.IO) {
            coroutineScope {
                val infoDeferred = async { client.getPrivilegeLite(id) }
                val lyricFallbackDeferred = async { searchAndFetchLyric(id) }

                val infoResponse = infoDeferred.await()
                if (infoResponse.status != 200) throw IOException("Failed to fetch song info for $id")

                val data = infoResponse.body["data"]?.jsonArray?.get(0)?.jsonObject
                    ?: throw IOException("Empty response for $id")

                val songName = data["name"]?.jsonPrimitive?.content ?: "Unknown"
                val singer = data["singername"]?.jsonPrimitive?.content ?: "Unknown"
                val info = data["info"]?.jsonObject

                val album = data["albumname"]?.jsonPrimitive?.content ?: ""
                val coverUrl = info?.get("image")?.jsonPrimitive?.content?.replace("/{size}/", "/")

                // 优先走 KugouLyricsClient 的完整解析管道（KRC 字词时轴 + 翻译），SDK 侧仅作回退
                val payloadDeferred = async { lyricsClient?.getLyricsByHash(hash = id, title = songName, artist = singer) }
                val payload = payloadDeferred.await()
                val fallbackLyric = lyricFallbackDeferred.await()

                val lyricText = payload?.lyrics
                    ?: fallbackLyric?.let { kugouYrc(it) }

                SongDetails(
                    id = id,
                    songName = songName.removePrefix("$singer - ").trim(),
                    singer = singer,
                    album = "${PlayerManager.KuGou_SOURCE_TAG}$album",
                    coverUrl = coverUrl,
                    lyric = lyricText
                        ?.takeIf { it.isNotBlank() },
                    translatedLyric = payload?.translatedLyrics
                )
            }
        }
    }

    suspend fun searchAndFetchLyric(hash: String, albumAudioId: Long = 0L): String? {
        NPLogger.d("KuGouSearchApi", "searchAndFetchLyric: hash=$hash, albumAudioId=$albumAudioId")
        val searchResponse = client.searchLyric(hash = hash, albumAudioId = albumAudioId)
        NPLogger.d("KuGouSearchApi", "searchAndFetchLyric: searchResponse.status=${searchResponse.status}")
        if (searchResponse.status != 200) {
            NPLogger.w("KuGouSearchApi", "searchAndFetchLyric: search failed, body=${searchResponse.body}")
            return null
        }

        val candidates = searchResponse.body["candidates"]?.jsonArray
            ?: searchResponse.body["info"]?.jsonArray
            ?: return null

        val candidate = candidates.firstOrNull()?.jsonObject ?: return null
        val id = candidate["id"]?.jsonPrimitive?.content ?: return null
        val accessKey = candidate["accesskey"]?.jsonPrimitive?.content ?: return null

        val lyricResponse = client.getLyric(id = id, accessKey = accessKey, decode = true, fmt = "krc")
        if (lyricResponse.status != 200) return null

        return lyricResponse.body["decodeContent"]?.jsonPrimitive?.content
    }

    @SuppressLint("DefaultLocale")
    private fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }
}


/** 酷狗分页搜索结果 */
data class KugouSearchPageResult(
    val items: List<SongSearchInfo>,
    val page: Int,
    val total: Long
)
