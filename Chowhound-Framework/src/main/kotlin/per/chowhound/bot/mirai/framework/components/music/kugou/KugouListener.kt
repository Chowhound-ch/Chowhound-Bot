package per.chowhound.bot.mirai.framework.components.music.kugou

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.*
import org.springframework.stereotype.Controller
import per.chowhound.bot.mirai.framework.common.MessageSender.send
import per.chowhound.bot.mirai.framework.components.music.kugou.entity.Music
import per.chowhound.bot.mirai.framework.config.FilterValue
import per.chowhound.bot.mirai.framework.config.Listener
import kotlin.time.Duration.Companion.seconds

/**
 * @Author: Chowhound
 * @Date: 2023/4/25 - 22:49
 * @Description:
 */
@Suppress("unused")
@Controller
class KugouListener(val kuGouMusic: KuGouMusic) {

    @Listener("^/点歌\\s*{{param,(-d|D)?}}\\s*{{keyword}}")
    suspend fun MessageEvent.uploadKugouMusic(@FilterValue("param") param: String,
                                              @FilterValue("keyword") keyword: String){
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


                val desIndex = try {
                    withTimeout(120.seconds){

                        return@withTimeout buildMessageChain {  }
                    }
                }catch (e: TimeoutCancellationException){
                    send("会话因超时(120s)自动关闭")
                    return
                }
                searchRes[desIndex[0].content.toInt() - 1]

            }
        }

        send(getMusicShare(desMusic)!!)


    }

    fun getMusicShare(music: Music): Message?{
        return try {
            MusicShare(
                MusicKind.KugouMusic, music.title!!, music.audioName!!, music.url!!,
                music.imgUrl!!, music.url!!)
        }catch (_: NullPointerException){ null }
    }

}