package per.chowhound.bot.mirai.framework.permit

import net.mamoe.mirai.event.events.GroupMessageEvent
import org.springframework.stereotype.Controller
import per.chowhound.bot.mirai.framework.config.Listener

@Controller
class PermitListener {
    @Listener
    fun GroupMessageEvent.permit() {
        println("dwad")
    }
}