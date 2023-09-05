package per.chowhound.bot.mirai.framework.config

import net.mamoe.mirai.event.events.MessageEvent
import org.springframework.stereotype.Component
import per.chowhound.bot.mirai.framework.common.utils.LoggerUtils.logInfo
import per.chowhound.bot.mirai.framework.config.listener.Listener

/**
 * @author Chowhound
 * @date   2023/5/31 - 21:18
 */
@Component
class CommonMessageListener {

    @Listener(".*", isBoot = true, desc = "默认消息监听器，监听所有消息")
    suspend fun MessageEvent.defaultMessageListener() {
        logInfo("监听到事件：{}", this)
    }
}