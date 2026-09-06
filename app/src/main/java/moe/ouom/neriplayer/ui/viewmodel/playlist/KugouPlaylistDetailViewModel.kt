package moe.ouom.neriplayer.ui.viewmodel.playlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.data.platform.kugou.KUGOU_ALBUM_PREFIX
import moe.ouom.neriplayer.ui.viewmodel.playlist.IdData.getGlobalId
import moe.ouom.neriplayer.ui.viewmodel.tab.PlaylistSummary

private const val TAG = "NERI-KugouPlaylistVM"

data class KugouPlaylistDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val playlist: PlaylistSummary? = null,
    val tracks: List<SongItem> = emptyList(),
)

class KugouPlaylistDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(KugouPlaylistDetailUiState())
    val uiState: StateFlow<KugouPlaylistDetailUiState> = _uiState

    private var currentPlaylist: PlaylistSummary? = null
    private var currentType: String = "rank"

    fun start(playlist: PlaylistSummary, type: String = "rank") {
        NPLogger.d(TAG, "start: playlist.id=${playlist.id}, type=$type")
        currentPlaylist = playlist
        currentType = type
        _uiState.value = KugouPlaylistDetailUiState(
            loading = true,
            playlist = playlist
        )
        viewModelScope.launch { loadTracks(playlist, type) }
    }

    fun retry() {
        currentPlaylist?.let { start(it, currentType) }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadTracks(playlist: PlaylistSummary, type: String) {
        try {
            val id = playlist.id.toString()
            NPLogger.d(TAG, "loadTracks start: id=$id, type=$type")

            val response = withContext(Dispatchers.IO) {
                when (type) {
                    "user" -> {
                        val globalId = getGlobalId(playlist.id)
                        NPLogger.d(TAG, "loadTracks: globalId=$globalId for listId=${playlist.id}")
                        if (globalId != null) {
                            AppContainer.kugouClient.playlist.getPlaylistTracks(
                                id = globalId, page = 1, pageSize = 300
                            )
                        } else {
                            AppContainer.kugouClient.playlist.getPlaylistTracksNew(
                                listId = playlist.id.toString(), page = 1, pageSize = 300
                            )
                        }
                    }
                    else -> AppContainer.kugouClient.rank.getAudio(
                        rankId = id, page = 1, pageSize = 100
                    )
                }
            }

            NPLogger.d(TAG, "loadTracks response status=${response.status}")
            val tracks = if (type == "user") {
            NPLogger.d(TAG, "loadTracks response body=${response.body}")
                parseUserPlaylistTracks(response, playlist.id.toString())
            } else {
                parseSongItems(response, playlist.id.toString())
            }
            NPLogger.d(TAG, "loadTracks parsed: count=${tracks.size}")

            _uiState.value = _uiState.value.copy(
                loading = false,
                error = null,
                tracks = tracks
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NPLogger.e(TAG, "loadTracks failed", e)
            _uiState.value = _uiState.value.copy(
                loading = false,
                error = e.message ?: "Unknown error"
            )
        }
    }

    private fun parseUserPlaylistTracks(response: top.ghhccghk.multiplatform.kugouapi.core.KuGouResponse, playlistId: String): List<SongItem> {
        NPLogger.d(TAG, "parseUserPlaylistTracks: response.status=${response.status}")
        if (response.status != 200) return emptyList()
        
        // The body might be a JsonObject or a JsonPrimitive (string)
        val bodyElement = response.body
        val bodyObj = if (bodyElement is kotlinx.serialization.json.JsonObject) {
            bodyElement
        } else {
            val bodyStr = bodyElement.toString().removeSurrounding("\"")
            try {
                kotlinx.serialization.json.Json.parseToJsonElement(bodyStr) as kotlinx.serialization.json.JsonObject
            } catch (e: Exception) {
                NPLogger.e(TAG, "parseUserPlaylistTracks: failed to parse body", e)
                return emptyList()
            }
        }
        
        val data = bodyObj["data"]?.jsonObject ?: return emptyList()
        // Songs are in data.songs array
        val songs = data["songs"]?.jsonArray ?: return emptyList()
        NPLogger.d(TAG, "parseUserPlaylistTracks: songs count=${songs.size}")
        
        val result = mutableListOf<SongItem>()
        for (i in songs.indices) {
            val obj = songs[i].jsonObject
            
            // Skip items without hash (shielded songs)
            val hash = obj["hash"]?.jsonPrimitive?.contentOrNull ?: continue
            if (hash.isBlank()) continue
            
            val mixSongId = obj["mixsongid"]?.jsonPrimitive?.longOrNull ?: 0L
            
            // Song name format: "artist - title" or just "title"
            val rawName = obj["name"]?.jsonPrimitive?.contentOrNull ?: ""
            val songName = rawName.substringAfter(" - ", rawName)
            
            // Get artist from singerinfo array
            val singerInfo = obj["singerinfo"]?.jsonArray
            val artist = if (singerInfo != null && singerInfo.isNotEmpty()) {
                singerInfo.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull }.joinToString(", ")
            } else {
                rawName.substringBefore(" - ", "")
            }
            
            val albumName = obj["albuminfo"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
            // Use mixsongid as albumId for lyrics API (album_audio_id parameter)
            val albumId = obj["mixsongid"]?.jsonPrimitive?.longOrNull
                ?: obj["album_id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                ?: 0L
            
            val duration = obj["timelen"]?.jsonPrimitive?.longOrNull ?: 0L
            
            // Cover URL - replace {size} placeholder
            val cover = obj["cover"]?.jsonPrimitive?.contentOrNull
                ?.replace("{size}", "")
                ?: obj["trans_param"]?.jsonObject?.get("union_cover")?.jsonPrimitive?.contentOrNull
                ?.replace("{size}", "")
                ?: ""
            
            result.add(
                SongItem(
                    id = mixSongId,
                    name = songName,
                    artist = artist,
                    album = "${KUGOU_ALBUM_PREFIX}$albumName",
                    albumId = albumId,
                    durationMs = duration,
                    coverUrl = cover,
                    channelId = "kugou",
                    audioId = hash,
                    playlistContextId = "kugou:$playlistId"
                )
            )
        }
        NPLogger.d(TAG, "parseUserPlaylistTracks: parsed ${result.size} songs")
        return result
    }
    private fun parseSongItems(response: top.ghhccghk.multiplatform.kugouapi.core.KuGouResponse, playlistId: String): List<SongItem> {
        if (response.status != 200) return emptyList()
        val data = response.body["data"]?.jsonObject ?: return emptyList()
        val info = data["songlist"]?.jsonArray ?: return emptyList()
        val result = mutableListOf<SongItem>()
        for (i in info.indices) {
            val obj = info[i].jsonObject
            val audioInfo = obj["audio_info"]?.jsonObject
            val albumInfo = obj["album_info"]?.jsonObject

            val hash = audioInfo
                ?.get("hash_128")
                ?.jsonPrimitive
                ?.contentOrNull
                ?: obj["deprecated"]
                    ?.jsonObject
                    ?.get("hash")
                    ?.jsonPrimitive
                    ?.contentOrNull
                ?: continue

            val albumAudioId =
                obj["album_audio_id"]?.jsonPrimitive?.longOrNull ?: 0L

            val songName =
                obj["songname"]?.jsonPrimitive?.contentOrNull ?: ""

            val artist =
                obj["author_name"]?.jsonPrimitive?.contentOrNull ?: ""

            val album =
                albumInfo
                    ?.get("album_name")
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?: ""

            val duration =
                audioInfo
                    ?.get("duration_128")
                    ?.jsonPrimitive
                    ?.longOrNull
                    ?: 0L

            val cover =
                albumInfo
                    ?.get("sizable_cover")
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?.replace("/{size}/", "/")

            result.add(
                SongItem(
                    id = albumAudioId,
                    name = songName,
                    artist = artist,
                    album = "${KUGOU_ALBUM_PREFIX}$album",
                    albumId = albumAudioId,
                    durationMs = duration,
                    coverUrl = cover,
                    channelId = "kugou",
                    audioId = hash,
                    playlistContextId = "kugou:$playlistId"
                )
            )
        }
        return result
    }
}

    object IdData{
        // Store global_collection_id mapping for user playlists
        private val globalIdMap = mutableMapOf<Long, String>()

        fun storeGlobalId(listId: Long, globalId: String) {
            globalIdMap[listId] = globalId
        }

        fun getGlobalId(listId: Long): String? = globalIdMap[listId]
    }
