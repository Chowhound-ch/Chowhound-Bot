package per.chowhound.bot.mirai.core.permit

import net.mamoe.mirai.event.events.GroupMessageEvent
import org.springframework.stereotype.Controller
import per.chowhound.bot.mirai.config.Listener

@Controller
class PermitListener {
    @Listener
    fun GroupMessageEvent.permit() {
        println("dwad")
    }
}