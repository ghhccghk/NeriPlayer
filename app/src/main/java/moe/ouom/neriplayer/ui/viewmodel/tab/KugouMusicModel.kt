package moe.ouom.neriplayer.ui.viewmodel.tab

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


/**
 * 酷狗排行榜元数据
 * @param rankid 排行榜 id
 * @param rankCid 排行榜 cid
 * @param title 标题
 * @param subTitle 内容简介
 * @param coverUrl 封面图片
 * @param playTimeCount 播放次数
 * @param songCount 歌曲总数
 */
@Parcelize
data class KugouMusicTopList(
    val rankid: String,
    val rankCid: String,
    val title: String,
    val subTitle: String,
    val coverUrl: String,
    val playTimeCount: Long = 0,
    val songCount: Long = 0
) : Parcelable
