package moe.ouom.neriplayer.ui.screen.debug

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
 * File: moe.ouom.neriplayer.ui.screen.debug/KugouApiProbeScreen
 * Created: 2025/8/14
 */

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.ui.LocalMiniPlayerHeight
import moe.ouom.neriplayer.ui.viewmodel.debug.KugouApiProbeViewModel

@Composable
fun KugouApiProbeScreen() {
    val context = LocalContext.current

    val vm: KugouApiProbeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = context.applicationContext as Application
                KugouApiProbeViewModel(app)
            }
        }
    )

    val ui by vm.ui.collectAsState()
    val scroll = rememberScrollState()
    val miniH = LocalMiniPlayerHeight.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = miniH),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.debug_kugou_probe),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(R.string.debug_kugou_probe_desc),
            style = MaterialTheme.typography.bodyMedium
        )

        // ── 鉴权状态 ────────────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.debug_kugou_probe_section_auth),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = ui.authSummary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // ── 输入参数 ────────────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.debug_kugou_probe_section_inputs),
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = ui.keyword,
                    onValueChange = vm::onKeywordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.debug_kugou_probe_input_keyword)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = ui.hash,
                    onValueChange = vm::onHashChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.debug_kugou_probe_input_hash)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = ui.quality,
                    onValueChange = vm::onQualityChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.debug_kugou_probe_input_quality)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = ui.rankId,
                    onValueChange = vm::onRankIdChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.debug_kugou_probe_input_rank_id)) },
                    singleLine = true
                )
            }
        }

        // ── 调试动作 ────────────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.debug_kugou_probe_section_actions),
                    style = MaterialTheme.typography.titleMedium
                )

                Button(
                    onClick = { vm.searchSongsAndCopy() },
                    enabled = !ui.running,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.debug_kugou_probe_action_search)) }

                OutlinedButton(
                    onClick = { vm.getPrivilegeLiteAndCopy() },
                    enabled = !ui.running,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.debug_kugou_probe_action_privilege)) }

                OutlinedButton(
                    onClick = { vm.getSongInfoAndCopy() },
                    enabled = !ui.running,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.debug_kugou_probe_action_song_info)) }

                OutlinedButton(
                    onClick = { vm.getSongUrlAndCopy() },
                    enabled = !ui.running,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.debug_kugou_probe_action_song_url)) }

                OutlinedButton(
                    onClick = { vm.searchLyricAndCopy() },
                    enabled = !ui.running,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.debug_kugou_probe_action_lyric_search)) }

                OutlinedButton(
                    onClick = { vm.getRankListAndCopy() },
                    enabled = !ui.running,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.debug_kugou_probe_action_rank_list)) }

                OutlinedButton(
                    onClick = { vm.getRankAudioAndCopy() },
                    enabled = !ui.running,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.debug_kugou_probe_action_rank_audio)) }

                if (ui.running) {
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // ── 调试结果 ────────────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.debug_kugou_probe_section_result),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.debug_status, ui.lastMessage),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = ui.resultSummary.ifBlank { stringResource(R.string.debug_kugou_probe_summary_empty) },
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = ui.lastJsonPreview.ifBlank { stringResource(R.string.debug_kugou_probe_raw_empty) },
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
