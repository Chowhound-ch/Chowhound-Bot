package per.chowhound.bot.mirai.config

import com.fasterxml.jackson.annotation.JsonAlias
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.auth.BotAuthorization
import net.mamoe.mirai.utils.BotConfiguration
import net.mamoe.mirai.utils.LoggerAdapters.asMiraiLogger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import per.chowhound.bot.mirai.common.utils.JacksonUtil
import per.chowhound.bot.mirai.common.utils.Utils.CLASS_LOADER
import java.io.File

@Configuration
class MiraiBotConfiguration {

    @Bean
    fun miraiBot(): Bot {
        val stream = CLASS_LOADER.getResourceAsStream("bots/mirai.bot")
        val (username, passwordInfo, config) = JacksonUtil.objectMapper.readValue(stream, MiraiBotInfo::class.java)


        // bot启动配置
        fun config(): BotConfiguration.() -> Unit = {
            fileBasedDeviceInfo()   // 使用设备信息缓存
            protocol = config.getProtocol() // 设置协议
            cacheDir = File("cache/$username") // 设置缓存目录
            botLoggerSupplier = {
                LoggerFactory.getLogger("net.mamoe.mirai.Bot.${it.id}").asMiraiLogger()
            }// 使用自定义日志
            networkLoggerSupplier = {
                LoggerFactory.getLogger("net.mamoe.mirai.Bot.${it.id}").asMiraiLogger()
            }// 使用自定义日志
        }

        val bot = if (passwordInfo.type == MiraiPasswordInfo.TYPE_TEXT) {

            BotFactory.newBot(username!!.toLong(), BotAuthorization.byQRCode(), configuration = config()) // 使用明文密码登录

        }else{

            BotFactory.newBot(username!!.toLong(), BotAuthorization.byQRCode(), configuration = config()) // 使用md5密码登录

        }

        runBlocking { bot.login() } // 登录
        return bot
    }

}

data class MiraiBotInfo(
    var username: String? = null,
    @field:JsonAlias("passwordInfo")
    var passwordInfo: MiraiPasswordInfo = MiraiPasswordInfo(),
    var config: MiraiBotConfig = MiraiBotConfig(),
)

@Suppress("unused")
data class MiraiPasswordInfo(
    var type: String = TYPE_TEXT,
    var text: String? = null
) {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_MD5 = "image"
    }
}

data class MiraiBotConfig(
    var protocol: String = "IPAD"
){
    fun getProtocol(): BotConfiguration.MiraiProtocol = BotConfiguration.MiraiProtocol.valueOf(protocol.uppercase())
}