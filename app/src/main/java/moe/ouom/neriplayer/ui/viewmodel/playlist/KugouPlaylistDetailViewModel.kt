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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.data.platform.kugou.KUGOU_ALBUM_PREFIX
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

    fun start(playlist: PlaylistSummary) {
        currentPlaylist = playlist
        _uiState.value = KugouPlaylistDetailUiState(
            loading = true,
            playlist = playlist
        )
        viewModelScope.launch { loadTracks(playlist) }
    }

    fun retry() {
        currentPlaylist?.let { start(it) }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadTracks(playlist: PlaylistSummary) {
        try {
            val rankId = playlist.id.toString()
            NPLogger.d(TAG, "loadTracks start: rankId=$rankId")

            val response = withContext(Dispatchers.IO) {
                AppContainer.kugouClient.rank.getAudio(
                    rankId = rankId,
                    page = 1,
                    pageSize = 100
                )
            }

            NPLogger.d(TAG, "loadTracks response status=${response.status}")
            val tracks = parseSongItems(response, playlist.id.toString())
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
