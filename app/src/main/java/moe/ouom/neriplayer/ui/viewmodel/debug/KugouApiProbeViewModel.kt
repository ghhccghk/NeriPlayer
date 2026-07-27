package moe.ouom.neriplayer.ui.viewmodel.debug

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
 * File: moe.ouom.neriplayer.ui.viewmodel.debug/KugouApiProbeViewModel
 * Created: 2025/8/14
 */

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.core.logging.NPLogger
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException

private const val TAG = "NERI-KugouApiProbeVM"

data class KugouProbeUiState(
    val running: Boolean = false,
    val lastMessage: String = "",
    val lastJsonPreview: String = "",
    val authSummary: String = "",
    val keyword: String = "",
    val hash: String = "",
    val quality: String = "128",
    val rankId: String = "",
    val resultSummary: String = ""
)

class KugouApiProbeViewModel(app: Application) : AndroidViewModel(app) {

    private val wrapper = AppContainer.kugouClient
    private val cookieRepo = AppContainer.kugouCookieRepo

    private val _ui = MutableStateFlow(
        KugouProbeUiState(
            authSummary = buildAuthSummary()
        )
    )
    val ui: StateFlow<KugouProbeUiState> = _ui.asStateFlow()

    fun onKeywordChange(value: String) {
        _ui.value = _ui.value.copy(keyword = value)
    }

    fun onHashChange(value: String) {
        _ui.value = _ui.value.copy(hash = value)
    }

    fun onQualityChange(value: String) {
        _ui.value = _ui.value.copy(quality = value)
    }

    fun onRankIdChange(value: String) {
        _ui.value = _ui.value.copy(rankId = value)
    }

    private fun copyToClipboard(label: String, text: String) {
        val cm = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    private fun launchAndCopy(label: String, block: suspend () -> String) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                running = true,
                lastMessage = getApplication<Application>().getString(R.string.debug_calling, label),
                lastJsonPreview = "",
                resultSummary = "",
                authSummary = buildAuthSummary()
            )
            try {
                var raw = ""
                val elapsedMs = measureTimeMillis {
                    raw = withContext(Dispatchers.IO) { block() }
                }
                copyToClipboard("kugou_api_$label", raw)
                _ui.value = _ui.value.copy(
                    running = false,
                    lastMessage = getApplication<Application>().getString(R.string.debug_copied_label, label),
                    lastJsonPreview = formatJson(raw),
                    resultSummary = buildSummary(label, raw, elapsedMs),
                    authSummary = buildAuthSummary()
                )
            } catch (e: IOException) {
                _ui.value = _ui.value.copy(
                    running = false,
                    lastMessage = getApplication<Application>().getString(R.string.debug_network_error, e.message ?: e.javaClass.simpleName),
                    authSummary = buildAuthSummary()
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    running = false,
                    lastMessage = getApplication<Application>().getString(R.string.debug_call_failed, e.message ?: e.javaClass.simpleName),
                    authSummary = buildAuthSummary()
                )
            }
        }
    }

    // ── 功能按钮 ──────────────────────────────────────────────────

    /** 搜索歌曲 */
    fun searchSongsAndCopy() {
        val keyword = _ui.value.keyword.trim()
        if (keyword.isBlank()) {
            _ui.value = _ui.value.copy(
                lastMessage = getApplication<Application>().getString(R.string.debug_kugou_probe_keyword_required)
            )
            return
        }
        launchAndCopy("search") {
            val response = wrapper.searchSongs(keywords = keyword, page = 1)
            parseResponseToJson(response)
        }
    }

    /** 歌曲特权信息 (PrivilegeLite) */
    fun getPrivilegeLiteAndCopy() {
        val hash = _ui.value.hash.trim()
        if (hash.isBlank()) {
            _ui.value = _ui.value.copy(
                lastMessage = getApplication<Application>().getString(R.string.debug_kugou_probe_hash_required)
            )
            return
        }
        launchAndCopy("privilegeLite") {
            val response = wrapper.getPrivilegeLite(hash)
            parseResponseToJson(response)
        }
    }

    /** 歌曲音频信息 (AudioInfo) */
    fun getSongInfoAndCopy() {
        val hash = _ui.value.hash.trim()
        if (hash.isBlank()) {
            _ui.value = _ui.value.copy(
                lastMessage = getApplication<Application>().getString(R.string.debug_kugou_probe_hash_required)
            )
            return
        }
        launchAndCopy("songInfo") {
            val response = wrapper.getSongInfo(hash)
            parseResponseToJson(response)
        }
    }

    /** 获取播放 URL */
    fun getSongUrlAndCopy() {
        val hash = _ui.value.hash.trim()
        if (hash.isBlank()) {
            _ui.value = _ui.value.copy(
                lastMessage = getApplication<Application>().getString(R.string.debug_kugou_probe_hash_required)
            )
            return
        }
        launchAndCopy("songUrl") {
            val quality = _ui.value.quality.trim().ifBlank { "128" }
            val response = wrapper.getSongUrl(hash = hash, quality = quality)
            parseResponseToJson(response)
        }
    }

    /** 搜索歌词 */
    fun searchLyricAndCopy() {
        val hash = _ui.value.hash.trim()
        if (hash.isBlank()) {
            _ui.value = _ui.value.copy(
                lastMessage = getApplication<Application>().getString(R.string.debug_kugou_probe_hash_required)
            )
            return
        }
        launchAndCopy("lyricSearch") {
            val response = wrapper.searchLyric(hash)
            parseResponseToJson(response)
        }
    }

    /** 排行榜列表 */
    fun getRankListAndCopy() {
        launchAndCopy("rankList") {
            val response = wrapper.rank.getList()
            parseResponseToJson(response)
        }
    }

    /** 排行榜歌曲 */
    fun getRankAudioAndCopy() {
        val rankId = _ui.value.rankId.trim()
        if (rankId.isBlank()) {
            _ui.value = _ui.value.copy(
                lastMessage = getApplication<Application>().getString(R.string.debug_kugou_probe_rank_required)
            )
            return
        }
        launchAndCopy("rankAudio") {
            val response = wrapper.rank.getAudio(rankId = rankId, page = 1, pageSize = 30)
            parseResponseToJson(response)
        }
    }

    // ── 工具方法 ──────────────────────────────────────────────────

    /**
     * 将 KuGouResponse 转换为原始 JSON 字符串。
     * 如果响应包含嵌套的 JSON 字符串，尝试解析并嵌入。
     */
    private fun parseResponseToJson(
        response: top.ghhccghk.multiplatform.kugouapi.core.KuGouResponse
    ): String {
        return JSONObject().apply {
            put("status", response.status)
            put("body", response.body)
        }.toString()
    }

    private fun buildAuthSummary(): String {
        val loggedIn = wrapper.isLoggedIn()
        val cookies = wrapper.getCookies()
        if (!loggedIn && cookies.isEmpty()) {
            return getApplication<Application>().getString(R.string.debug_kugou_probe_auth_missing)
        }
        return getApplication<Application>().getString(
            R.string.debug_kugou_probe_auth_logged_in,
            cookies.size
        )
    }

    private fun buildSummary(action: String, raw: String, elapsedMs: Long): String {
        val parsed = runCatching { JSONTokener(raw).nextValue() }.getOrNull()
        val status = when (parsed) {
            is JSONObject -> parsed.opt("status")?.toString().orEmpty().ifBlank { "-" }
            else -> "-"
        }
        val topKeys = when (parsed) {
            is JSONObject -> buildList {
                val iterator = parsed.keys()
                while (iterator.hasNext()) {
                    add(iterator.next())
                }
            }.joinToString(", ").ifBlank { "-" }
            is JSONArray -> "[array]"
            else -> "-"
        }
        return getApplication<Application>().getString(
            R.string.debug_netease_probe_summary_template,
            action,
            elapsedMs,
            status,
            topKeys,
            raw.length
        )
    }

    private fun formatJson(raw: String): String {
        return runCatching {
            when (val parsed = JSONTokener(raw).nextValue()) {
                is JSONObject -> parsed.toString(2)
                is JSONArray -> parsed.toString(2)
                else -> raw
            }
        }.getOrDefault(raw)
    }

    private inline fun measureTimeMillis(block: () -> Unit): Long {
        val start = System.currentTimeMillis()
        block()
        return System.currentTimeMillis() - start
    }
}
