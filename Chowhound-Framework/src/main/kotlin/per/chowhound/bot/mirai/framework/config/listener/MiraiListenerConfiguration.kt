package per.chowhound.bot.mirai.framework.config.listener;

import net.mamoe.mirai.event.events.FriendMessageEvent
import org.springframework.context.annotation.Configuration;
import per.chowhound.bot.mirai.framework.config.listener.utils.Listener

/**
 * @author : Chowhound
 * @since : 2023/9/1 - 21:59
 */
@Configuration
class MiraiListenerConfiguration {


    @Listener
    fun FriendMessageEvent.test(){

    }

}
