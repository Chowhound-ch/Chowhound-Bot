package per.chowhound.bot.mirai.framework.components.music.kugou

import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.*
import org.springframework.stereotype.Controller
import per.chowhound.bot.mirai.framework.common.MessageSender.send
import per.chowhound.bot.mirai.framework.components.music.kugou.entity.Music
import per.chowhound.bot.mirai.framework.config.listener.Listener
import per.chowhound.bot.mirai.framework.config.listener.waitMessage

/**
 * @Author: Chowhound
 * @Date: 2023/4/25 - 22:49
 * @Description:
 */
@Suppress("unused")
@Controller
class KugouListener(val kuGouMusic: KuGouMusic) {

    @Listener("^/点歌\\s*{{param,(-d|D)?}}\\s*{{keyword}}", desc = "kugou点歌")
    suspend fun MessageEvent.kugouMusic(param: String,
                                        keyword: String){
        val searchRes = kuGouMusic.getSearchRes(keyword, if (param.isNotEmpty()) 8 else 1)
        val  desMusic = when (searchRes.size) {
            0 -> {
                send("未找到关键字为 $keyword 的歌曲")
                return
            }
            //如果目标音乐只有一首则直接分享该音乐
            1 -> searchRes[0]
            else -> {
                buildMessageChain {
                    for ( i in 0 until  searchRes.size) {
                        searchRes[i].let {
                            this.append("${i + 1} 、")
                            it.url?.let { this.append("(本地)") }
                        }
                        this.append(searchRes[i].audioName ?: "未知歌曲").append("\n")
                    }
                }.let { send(it) }


                val nextMsgEvent = waitMessage { it.sender.id == 825352674L } ?: run { send("会话超时");return }

                searchRes[nextMsgEvent.message.content.toInt() - 1]
            }
        }

        send(
            desMusic.let {

                if (it.url == null && it.fileHash != null){//如果该音乐不是从本地找到的则从网络获取
                    kuGouMusic.getMusicUrlByAlbumIDAndHash(it)
                }else{ it }

            }?.let {
                if (it.imgUrl == null ) it.imgUrl = sender.avatarUrl

                getMusicShare(it)
            } ?: buildMessageChain { this.append("未找到结果或歌曲为付费歌曲") })
    }

    fun getMusicShare(music: Music): Message?{
        return try {
            MusicShare(
                MusicKind.KugouMusic, music.title!!, music.audioName!!, music.url!!,
                music.imgUrl!!, music.url!!)
        }catch (_: NullPointerException){ null }
    }

}