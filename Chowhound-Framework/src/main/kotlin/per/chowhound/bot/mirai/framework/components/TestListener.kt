package per.chowhound.bot.mirai.framework.components

import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.messageChainOf
import net.mamoe.mirai.message.data.source
import org.springframework.stereotype.Component
import per.chowhound.bot.mirai.framework.common.MessageSender.reply
import per.chowhound.bot.mirai.framework.common.MessageSender.send
import per.chowhound.bot.mirai.framework.config.listener.Listener

/**
 * @author : Chowhound
 * @since : 2023/9/5 - 18:57
 */

@Component
class TestListener {
    @Listener
    suspend fun MessageEvent.test() {
        this.reply("daw")
    }

}